package services;

import exceptions.ErrorConexionCassandraException;
import exceptions.ErrorConexionMongoException;
import modelo.Alerta;
import modelo.ControlSensor;
import modelo.Medicion;
import modelo.Sensor;
import repository.AlertaMongoDAO;
import repository.ControlSensorMongoDAO;
import repository.MedicionCassandraDAO;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Monitoreo proactivo de sensores. Detecta ausencia de datos, valores fuera de rango
 * o lecturas congeladas y genera controles técnicos + alertas para el administrador.
 */
public class ControlTecnicoMonitorService {
    private static final long INTERVALO_SEGUNDOS = 120;
    private static final int REVISION_MUESTRAS = 6;
    private static final int MINUTOS_SIN_DATOS = 90;
    private static final double TEMP_MIN = -90;
    private static final double TEMP_MAX = 90;
    private static final double HUM_MIN = 0;
    private static final double HUM_MAX = 100;
    private static final double DELTA_CONGELADO = 0.05;
    private static final long COOLDOWN_ALERTA_MIN = 180;

    private static ControlTecnicoMonitorService instance;

    private final ScheduledExecutorService executor;
    private volatile boolean iniciado;

    private ControlTecnicoMonitorService() {
        this.executor = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
    }

    public static ControlTecnicoMonitorService getInstance() {
        if (instance == null) {
            synchronized (ControlTecnicoMonitorService.class) {
                if (instance == null) {
                    instance = new ControlTecnicoMonitorService();
                }
            }
        }
        return instance;
    }

    public synchronized void iniciar() {
        if (iniciado) {
            return;
        }
        executor.scheduleAtFixedRate(this::evaluarSensores, 90, INTERVALO_SEGUNDOS, TimeUnit.SECONDS);
        iniciado = true;
    }

    private void evaluarSensores() {
        try {
            List<Sensor> sensores = SensorService.getInstance().listarTodos();
            for (Sensor sensor : sensores) {
                evaluarSensor(sensor);
            }
        } catch (Exception e) {
            System.err.println("No se pudo ejecutar el monitoreo técnico: " + e.getMessage());
        }
    }

    private void evaluarSensor(Sensor sensor) throws ErrorConexionCassandraException, ErrorConexionMongoException {
        if (sensor.getId() == null) {
            return;
        }
        List<Medicion> mediciones = MedicionCassandraDAO.getInstance()
                .obtenerPorSensor(sensor.getId(), null, null, REVISION_MUESTRAS);
        if (mediciones == null) {
            mediciones = new ArrayList<>();
        }
        boolean alertaCritica = false;
        String descripcion = null;
        LocalDateTime ahora = LocalDateTime.now();

        if (mediciones.isEmpty() || mediciones.get(0).getFechaHora() == null ||
                Duration.between(mediciones.get(0).getFechaHora(), ahora).toMinutes() > MINUTOS_SIN_DATOS) {
            alertaCritica = true;
            descripcion = "Sin datos en las últimas " + MINUTOS_SIN_DATOS + " min";
        } else {
            Medicion ultima = mediciones.get(0);
            if (fueraDeRango(ultima)) {
                alertaCritica = true;
                descripcion = "Medición fuera de rango físico: " + formatear(ultima);
            } else if (sensorCongelado(mediciones)) {
                alertaCritica = false;
                descripcion = "Lecturas repetidas detectadas: " + formatear(ultima);
            }
        }

        if (descripcion == null) {
            return;
        }

        if (debeSuprimir(sensor.getId(), descripcion)) {
            return;
        }

        ControlSensor control = new ControlSensor(sensor.getId(), null,
                alertaCritica ? "FALLA" : "MANTENIMIENTO");
        control.setObservaciones(descripcion);
        control.setFechaRevision(LocalDateTime.now());
        new ControlSensorMongoDAO().insertar(control);

        try {
            Alerta alerta = new Alerta("SENSOR", descripcion + " (" + sensor.getNombre() + ")",
                    alertaCritica ? "ALTA" : "MEDIA");
            alerta.setSensorId(sensor.getId());
            new AlertaMongoDAO().insertar(alerta);
        } catch (Exception ex) {
            System.err.println("No se pudo registrar alerta técnica: " + ex.getMessage());
        }

        if (alertaCritica) {
            MantenimientoTaskService.getInstance()
                    .crearTareaFalla(sensor.getId(), sensor.getNombre(), descripcion);
        }
    }

    private boolean fueraDeRango(Medicion medicion) {
        Double t = medicion.getTemperatura();
        if (t != null && (t < TEMP_MIN || t > TEMP_MAX)) {
            return true;
        }
        Double h = medicion.getHumedad();
        if (h != null && (h < HUM_MIN || h > HUM_MAX)) {
            return true;
        }
        return false;
    }

    private boolean sensorCongelado(List<Medicion> mediciones) {
        if (mediciones.size() < REVISION_MUESTRAS) {
            return false;
        }
        Medicion ref = mediciones.get(0);
        Double tempRef = ref.getTemperatura();
        Double humRef = ref.getHumedad();
        for (int i = 1; i < REVISION_MUESTRAS; i++) {
            Medicion m = mediciones.get(i);
            if (!comparar(tempRef, m.getTemperatura())) {
                return false;
            }
            if (!comparar(humRef, m.getHumedad())) {
                return false;
            }
        }
        return true;
    }

    private boolean comparar(Double ref, Double val) {
        if (ref == null && val == null) {
            return true;
        }
        if (ref == null || val == null) {
            return false;
        }
        return Math.abs(ref - val) <= DELTA_CONGELADO;
    }

    private boolean debeSuprimir(String sensorId, String descripcion) {
        try {
            List<ControlSensor> ultimos = new ControlSensorMongoDAO().listarRecientes(1);
            if (ultimos.isEmpty()) {
                return false;
            }
            ControlSensor ultimo = ultimos.get(0);
            if (!sensorId.equals(ultimo.getSensorId())) {
                return false;
            }
            if (ultimo.getObservaciones() == null || !ultimo.getObservaciones().equals(descripcion)) {
                return false;
            }
            if (ultimo.getFechaRevision() == null) {
                return false;
            }
            return Duration.between(ultimo.getFechaRevision(), LocalDateTime.now()).toMinutes() < COOLDOWN_ALERTA_MIN;
        } catch (Exception e) {
            return false;
        }
    }

    private String formatear(Medicion medicion) {
        return String.format("T=%.2f°C H=%.2f%%",
                medicion.getTemperatura() != null ? medicion.getTemperatura() : Double.NaN,
                medicion.getHumedad() != null ? medicion.getHumedad() : Double.NaN);
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setDaemon(true);
            thread.setName("ControlTecnicoMonitor-" + thread.getId());
            return thread;
        }
    }
}
