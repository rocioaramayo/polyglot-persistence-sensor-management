package services;

import exceptions.ErrorConexionCassandraException;
import exceptions.ErrorConexionMongoException;
import modelo.AlertRule;
import modelo.Alerta;
import modelo.Medicion;
import modelo.Sensor;
import repository.AlertaMongoDAO;
import repository.MedicionCassandraDAO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class AutomaticAlertScheduler {
    private static final long INTERVALO_SEGUNDOS = 120;
    private static AutomaticAlertScheduler instance;

    private final ScheduledExecutorService executor;
    private volatile boolean iniciado;

    private AutomaticAlertScheduler() {
        this.executor = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
    }

    public static AutomaticAlertScheduler getInstance() {
        if (instance == null) {
            synchronized (AutomaticAlertScheduler.class) {
                if (instance == null) {
                    instance = new AutomaticAlertScheduler();
                }
            }
        }
        return instance;
    }

    public synchronized void iniciar() {
        if (iniciado) {
            return;
        }
        executor.scheduleAtFixedRate(this::evaluarReglas, 45, INTERVALO_SEGUNDOS, TimeUnit.SECONDS);
        iniciado = true;
    }

    private void evaluarReglas() {
        try {
            List<AlertRule> reglas = AlertRuleService.getInstance().listarActivas();
            if (reglas.isEmpty()) {
                return;
            }
            List<Sensor> sensores = obtenerSensores();
            if (sensores.isEmpty()) {
                return;
            }
            for (AlertRule regla : reglas) {
                evaluarRegla(regla, sensores);
            }
        } catch (Exception e) {
            System.err.println("No se pudieron evaluar las reglas de alertas automáticas: " + e.getMessage());
        }
    }

    private void evaluarRegla(AlertRule regla, List<Sensor> sensores) {
        if (regla == null || Boolean.FALSE.equals(regla.getActivo())) {
            return;
        }
        List<Sensor> candidatos = filtrarSensores(sensores, regla);
        if (candidatos.isEmpty()) {
            registrarSeguimiento(regla, false);
            return;
        }
        int ventana = regla.getVentanaMinutos() != null && regla.getVentanaMinutos() > 0
                ? regla.getVentanaMinutos()
                : 60;
        LocalDateTime limiteTemporal = LocalDateTime.now().minusMinutes(ventana);

        for (Sensor sensor : candidatos) {
            try {
                List<Medicion> mediciones = obtenerMedicionesRecientes(sensor.getId());
                for (Medicion medicion : mediciones) {
                    if (medicion == null || medicion.getFechaHora() == null) {
                        continue;
                    }
                    if (ventana > 0 && medicion.getFechaHora().isBefore(limiteTemporal)) {
                        continue;
                    }
                    if (!cumpleRangoFechas(regla, medicion.getFechaHora())) {
                        continue;
                    }
                    if (disparaAlerta(regla, medicion)) {
                        boolean generada = crearAlerta(regla, sensor, medicion);
                        registrarSeguimiento(regla, generada);
                        if (generada) {
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("No se pudo evaluar mediciones para el sensor " + sensor.getNombre() + ": " + e.getMessage());
            }
        }
        registrarSeguimiento(regla, false);
    }

    private List<Medicion> obtenerMedicionesRecientes(String sensorId) throws ErrorConexionCassandraException {
        List<Medicion> mediciones = MedicionCassandraDAO.getInstance().obtenerPorSensor(sensorId);
        return mediciones != null ? mediciones : new ArrayList<>();
    }

    private boolean crearAlerta(AlertRule regla, Sensor sensor, Medicion medicion) {
        try {
            String ubicacion = construirUbicacion(sensor);
            String descripcion = String.format(
                    "Alerta automática en %s: %.1f°C | %.1f%% de humedad (sensor %s)",
                    ubicacion,
                    medicion.getTemperatura(),
                    medicion.getHumedad(),
                    sensor.getNombre() != null ? sensor.getNombre() : sensor.getId()
            );
            Alerta alerta = new Alerta("CLIMATICA_AUTO", descripcion,
                    regla.getSeveridad() != null ? regla.getSeveridad() : "MEDIA");
            alerta.setSensorId(sensor.getId());
            new AlertaMongoDAO().insertar(alerta);
            System.out.println("Alerta automática generada por regla '" + regla.getNombre() + "'");
            return true;
        } catch (Exception e) {
            System.err.println("No se pudo registrar la alerta automática: " + e.getMessage());
            return false;
        }
    }

    private List<Sensor> filtrarSensores(List<Sensor> sensores, AlertRule regla) {
        List<Sensor> resultado = new ArrayList<>();
        for (Sensor sensor : sensores) {
            if (sensor == null || sensor.getId() == null) {
                continue;
            }
            if (regla.getCiudad() != null && !regla.getCiudad().isBlank()) {
                if (sensor.getCiudad() == null || !sensor.getCiudad().equalsIgnoreCase(regla.getCiudad())) {
                    continue;
                }
            }
            if (regla.getZona() != null && !regla.getZona().isBlank()) {
                if (sensor.getZona() == null || !sensor.getZona().equalsIgnoreCase(regla.getZona())) {
                    continue;
                }
            }
            if (regla.getPais() != null && !regla.getPais().isBlank()) {
                if (sensor.getPais() == null || !sensor.getPais().equalsIgnoreCase(regla.getPais())) {
                    continue;
                }
            }
            resultado.add(sensor);
        }
        return resultado;
    }

    private boolean cumpleRangoFechas(AlertRule regla, LocalDateTime fechaMedicion) {
        if (regla.getFechaDesde() != null && fechaMedicion.isBefore(regla.getFechaDesde())) {
            return false;
        }
        if (regla.getFechaHasta() != null && fechaMedicion.isAfter(regla.getFechaHasta())) {
            return false;
        }
        return true;
    }

    private boolean disparaAlerta(AlertRule regla, Medicion medicion) {
        boolean disparo = false;
        if (regla.getTemperaturaMin() != null && medicion.getTemperatura() != null
                && medicion.getTemperatura() < regla.getTemperaturaMin()) {
            disparo = true;
        }
        if (regla.getTemperaturaMax() != null && medicion.getTemperatura() != null
                && medicion.getTemperatura() > regla.getTemperaturaMax()) {
            disparo = true;
        }
        if (regla.getHumedadMin() != null && medicion.getHumedad() != null
                && medicion.getHumedad() < regla.getHumedadMin()) {
            disparo = true;
        }
        if (regla.getHumedadMax() != null && medicion.getHumedad() != null
                && medicion.getHumedad() > regla.getHumedadMax()) {
            disparo = true;
        }
        return disparo;
    }

    private void registrarSeguimiento(AlertRule regla, boolean generoAlerta) {
        try {
            AlertRuleService.getInstance().registrarSeguimiento(
                    regla.getId(),
                    LocalDateTime.now(),
                    generoAlerta ? LocalDateTime.now() : null
            );
        } catch (ErrorConexionMongoException e) {
            System.err.println("No se pudo actualizar el seguimiento de la regla " + regla.getNombre());
        }
    }

    private List<Sensor> obtenerSensores() {
        try {
            return SensorService.getInstance().listarTodos();
        } catch (ErrorConexionCassandraException e) {
            System.err.println("No se pudieron obtener sensores para las alertas automáticas: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private String construirUbicacion(Sensor sensor) {
        List<String> partes = new ArrayList<>();
        if (sensor.getCiudad() != null && !sensor.getCiudad().isBlank()) {
            partes.add(sensor.getCiudad());
        }
        if (sensor.getZona() != null && !sensor.getZona().isBlank()) {
            partes.add(sensor.getZona());
        }
        if (sensor.getPais() != null && !sensor.getPais().isBlank()) {
            partes.add(sensor.getPais());
        }
        if (partes.isEmpty()) {
            return sensor.getNombre() != null ? sensor.getNombre() : sensor.getId();
        }
        return String.join(" / ", partes);
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setDaemon(true);
            thread.setName("AutomaticAlertScheduler-" + thread.getId());
            return thread;
        }
    }
}
