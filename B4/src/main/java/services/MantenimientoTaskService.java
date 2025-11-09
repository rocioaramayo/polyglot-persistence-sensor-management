package services;

import exceptions.ErrorConexionCassandraException;
import exceptions.ErrorConexionMongoException;
import modelo.ControlSensor;
import modelo.MantenimientoConfig;
import modelo.MantenimientoTask;
import modelo.Sensor;
import repository.ControlSensorMongoDAO;
import repository.MantenimientoConfigMongoDAO;
import repository.MantenimientoTaskMongoDAO;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class MantenimientoTaskService {
    private static MantenimientoTaskService instance;

    private MantenimientoTaskService() {}

    public static MantenimientoTaskService getInstance() {
        if (instance == null) {
            instance = new MantenimientoTaskService();
        }
        return instance;
    }

    public void configurarFrecuencia(String sensorId, int dias) throws ErrorConexionMongoException {
        if (sensorId == null || sensorId.isBlank()) {
            throw new IllegalArgumentException("Sensor inválido");
        }
        if (dias <= 0) {
            throw new IllegalArgumentException("La frecuencia debe ser mayor a cero");
        }
        MantenimientoConfig existente = obtenerConfig(sensorId);
        MantenimientoConfig config = new MantenimientoConfig();
        config.setSensorId(sensorId);
        config.setFrecuenciaDias(dias);
        if (existente != null) {
            config.setUltimaRevision(existente.getUltimaRevision());
        } else {
            config.setUltimaRevision(LocalDateTime.now());
        }
        new MantenimientoConfigMongoDAO().upsert(config);
    }

    public MantenimientoConfig obtenerConfig(String sensorId) {
        try {
            return new MantenimientoConfigMongoDAO().obtenerPorSensor(sensorId);
        } catch (Exception e) {
            return null;
        }
    }

    public LocalDateTime obtenerUltimaRevisionReal(String sensorId) {
        try {
            ControlSensor control = new ControlSensorMongoDAO().obtenerUltimoPorSensor(sensorId);
            return control != null ? control.getFechaRevision() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public LocalDateTime obtenerUltimaRevisionCompleta(String sensorId) {
        LocalDateTime fecha = obtenerUltimaRevisionReal(sensorId);
        if (fecha != null) {
            return fecha;
        }
        try {
            MantenimientoTask ultima = new MantenimientoTaskMongoDAO().obtenerUltimaCompletadaPorSensor(sensorId);
            if (ultima != null) {
                if (ultima.getFechaCompletada() != null) {
                    return ultima.getFechaCompletada();
                }
                if (ultima.getFechaProgramada() != null) {
                    return ultima.getFechaProgramada();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public void registrarRevisionManual(String sensorId, String tecnicoId, String observaciones)
            throws ErrorConexionMongoException {
        new MantenimientoConfigMongoDAO().actualizarUltimaRevision(sensorId, LocalDateTime.now());
        ControlSensor control = new ControlSensor(sensorId, null, "ACTIVO");
        control.setObservaciones(observaciones);
        control.setFechaRevision(LocalDateTime.now());
        new ControlSensorMongoDAO().insertar(control);
    }

    public void crearTareaFalla(String sensorId, String sensorNombre, String motivo) {
        try {
            if (sensorId == null) return;
            MantenimientoTaskMongoDAO dao = new MantenimientoTaskMongoDAO();
            if (dao.existePendiente(sensorId, "REPARACION_FALLA")) {
                return;
            }
            MantenimientoTask task = new MantenimientoTask();
            task.setSensorId(sensorId);
            task.setSensorNombre(sensorNombre);
            task.setTipo("REPARACION_FALLA");
            task.setMotivo(motivo != null ? motivo : "Reparación por falla");
            task.setFechaProgramada(LocalDateTime.now());
            dao.insertar(task);
        } catch (Exception ignored) {
        }
    }

    public List<MantenimientoTask> listarPendientes() {
        try {
            return new MantenimientoTaskMongoDAO().listarPendientes();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<MantenimientoTask> listarHistorial(int limit) {
        try {
            return new MantenimientoTaskMongoDAO().listarCompletadas(limit);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<MantenimientoTask> listarDisponibles() {
        try {
            return new MantenimientoTaskMongoDAO().listarDisponibles();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<MantenimientoTask> listarPorTecnico(String tecnicoId) {
        try {
            return new MantenimientoTaskMongoDAO().listarPorTecnico(tecnicoId);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void tomarTarea(String tareaId, String tecnicoId, String tecnicoNombre) throws ErrorConexionMongoException {
        if (tareaId == null || tecnicoId == null) {
            throw new IllegalArgumentException("Datos inválidos");
        }
        MantenimientoTaskMongoDAO dao = new MantenimientoTaskMongoDAO();
        MantenimientoTask task = dao.buscarPorId(tareaId);
        if (task == null) {
            throw new IllegalArgumentException("Tarea no encontrada");
        }
        if (task.getTecnicoId() != null && !task.getTecnicoId().equals(tecnicoId)) {
            throw new IllegalStateException("La tarea ya fue tomada por otro técnico");
        }
        task.setTecnicoId(tecnicoId);
        task.setTecnicoNombre(tecnicoNombre);
        task.setEstado("EN_PROGRESO");
        dao.actualizar(task);
    }

    public void asignarTecnico(String tareaId, String tecnicoId, String tecnicoNombre) throws ErrorConexionMongoException {
        if (tareaId == null || tecnicoId == null) {
            throw new IllegalArgumentException("Datos inválidos");
        }
        MantenimientoTaskMongoDAO dao = new MantenimientoTaskMongoDAO();
        MantenimientoTask task = dao.buscarPorId(tareaId);
        if (task == null) {
            throw new IllegalArgumentException("Tarea no encontrada");
        }
        task.setTecnicoId(tecnicoId);
        task.setTecnicoNombre(tecnicoNombre);
        if (!"COMPLETADA".equalsIgnoreCase(task.getEstado())) {
            task.setEstado("EN_PROGRESO");
        }
        dao.actualizar(task);
    }

    public void crearTareaManual(String sensorId, String sensorNombre, String motivo, LocalDateTime fecha)
            throws ErrorConexionMongoException {
        if (sensorId == null) {
            throw new IllegalArgumentException("Sensor requerido");
        }
        MantenimientoTask task = new MantenimientoTask();
        task.setSensorId(sensorId);
        task.setSensorNombre(sensorNombre);
        task.setTipo("REVISION_MANUAL");
        task.setMotivo(motivo != null ? motivo : "Revisión manual");
        task.setFechaProgramada(fecha != null ? fecha : LocalDateTime.now());
        new MantenimientoTaskMongoDAO().insertar(task);
    }

    public void completarTarea(String tareaId, String tecnicoId, String observaciones, boolean reactivarSensor)
            throws ErrorConexionMongoException, ErrorConexionCassandraException {
        if (tareaId == null || tecnicoId == null) {
            throw new IllegalArgumentException("Datos inválidos");
        }
        MantenimientoTaskMongoDAO dao = new MantenimientoTaskMongoDAO();
        MantenimientoTask task = dao.buscarPorId(tareaId);
        if (task == null) {
            throw new IllegalArgumentException("Tarea no encontrada");
        }
        if (task.getTecnicoId() == null) {
            task.setTecnicoId(tecnicoId);
        } else if (!task.getTecnicoId().equals(tecnicoId)) {
            throw new IllegalStateException("La tarea está asignada a otro técnico");
        }
        task.setEstado("COMPLETADA");
        task.setFechaCompletada(LocalDateTime.now());
        task.setObservaciones(observaciones);
        dao.actualizar(task);

        // actualizar config de mantenimiento
        try {
            new MantenimientoConfigMongoDAO().actualizarUltimaRevision(task.getSensorId(), LocalDateTime.now());
        } catch (ErrorConexionMongoException ignored) {}

        // registrar control
        try {
            ControlSensor control = new ControlSensor(task.getSensorId(), null,
                    reactivarSensor ? "ACTIVO" : "MANTENIMIENTO");
            control.setObservaciones("Tarea " + task.getTipo() + ": " + (observaciones != null ? observaciones : "sin notas"));
            control.setFechaRevision(LocalDateTime.now());
            new ControlSensorMongoDAO().insertar(control);
        } catch (Exception ignored) {}

        if (reactivarSensor) {
            SensorService.getInstance().actualizarEstado(task.getSensorId(), "ACTIVO");
        }
    }

    public void generarTareasProgramadas() {
        try {
            List<MantenimientoConfig> configs = new MantenimientoConfigMongoDAO().listarTodas();
            MantenimientoTaskMongoDAO dao = new MantenimientoTaskMongoDAO();
            List<Sensor> sensores = SensorService.getInstance().listarTodos();
            for (MantenimientoConfig config : configs) {
                if (config.getFrecuenciaDias() == null || config.getFrecuenciaDias() <= 0) {
                    continue;
                }
                LocalDateTime ultima = config.getUltimaRevision();
                LocalDateTime proxima = ultima != null ? ultima.plusDays(config.getFrecuenciaDias()) : LocalDateTime.now();
                if (proxima.isAfter(LocalDateTime.now())) {
                    continue;
                }
                if (dao.existePendiente(config.getSensorId(), "REVISION_PROGRAMADA")) {
                    continue;
                }
                Sensor sensor = sensores.stream()
                        .filter(s -> s.getId().equals(config.getSensorId()))
                        .findFirst().orElse(null);
                MantenimientoTask task = new MantenimientoTask();
                task.setSensorId(config.getSensorId());
                task.setSensorNombre(sensor != null ? sensor.getNombre() : config.getSensorId());
                task.setTipo("REVISION_PROGRAMADA");
                task.setMotivo("Revisión periódica");
                task.setFechaProgramada(proxima);
                dao.insertar(task);
            }
        } catch (Exception e) {
            System.err.println("No se pudieron generar tareas programadas: " + e.getMessage());
        }
    }
}
