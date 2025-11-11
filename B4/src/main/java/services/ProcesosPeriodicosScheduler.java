package services;

import exceptions.ErrorConexionCassandraException;
import exceptions.ErrorConexionMongoException;
import exceptions.ErrorConexionMySQLException;
import modelo.ConsultaPeriodica;
import modelo.Proceso;
import modelo.ReportePeriodico;
import modelo.SolicitudProceso;
import repository.ReportePeriodicoMongoDAO;
import repository.SolicitudProcesoMongoDAO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ProcesosPeriodicosScheduler {
    private static final String SYSTEM_USER = "SYSTEM_PERIODICO";
    private static ProcesosPeriodicosScheduler instance;

    private final ScheduledExecutorService executor;
    private volatile boolean iniciado;
    private final Object cicloLock = new Object();

    private ProcesosPeriodicosScheduler() {
        this.executor = Executors.newScheduledThreadPool(1, new DaemonThreadFactory());
    }

    public static ProcesosPeriodicosScheduler getInstance() {
        if (instance == null) {
            synchronized (ProcesosPeriodicosScheduler.class) {
                if (instance == null) {
                    instance = new ProcesosPeriodicosScheduler();
                }
            }
        }
        return instance;
    }

    public synchronized void iniciar() {
        if (iniciado) {
            return;
        }
        long delayInicial = 30; // segundos
        executor.scheduleAtFixedRate(this::ejecutarPendientes, delayInicial, TimeUnit.MINUTES.toSeconds(1), TimeUnit.SECONDS);
        iniciado = true;
    }

    public void ejecutarCicloInmediato() {
        iniciar();
        ejecutarPendientes();
    }

    private void ejecutarPendientes() {
        synchronized (cicloLock) {
            try {
                List<ConsultaPeriodica> consultas = ConsultasPeriodicasService.getInstance()
                        .listarPendientes(LocalDateTime.now());
                if (consultas.isEmpty()) {
                    return;
                }
                for (ConsultaPeriodica consulta : consultas) {
                    ejecutarConsulta(consulta);
                }
            } catch (Exception e) {
                System.err.println("Error al listar consultas periódicas: " + e.getMessage());
            }
        }
    }

    private void ejecutarConsulta(ConsultaPeriodica consulta) {
        String solicitudId = null;
        try {
            Proceso proceso = obtenerProceso(consulta);
            if (proceso == null) {
                System.err.println("Proceso no encontrado para consulta periódica " + consulta.getId());
                ConsultasPeriodicasService.getInstance().desactivar(consulta.getId(), "Proceso no disponible");
                return;
            }
            Map<String, Object> parametros = consulta.getParametros() != null ? consulta.getParametros() : Map.of();
            String usuarioDestino = consulta.getUsuarioId() != null && !consulta.getUsuarioId().isBlank()
                    ? consulta.getUsuarioId() : SYSTEM_USER;
            solicitudId = SolicitudProcesoService.getInstance()
                    .crearSolicitudAutomatica(usuarioDestino, proceso.getId(), parametros);
            if (consulta.getTecnicoId() != null && !consulta.getTecnicoId().isBlank()) {
                try {
                    SolicitudProcesoService.getInstance().asignarTecnico(solicitudId, consulta.getTecnicoId());
                } catch (Exception ignored) {
                }
            }
            SolicitudProcesoService.getInstance().ejecutarSolicitud(solicitudId);
            SolicitudProcesoService.getInstance().completarSolicitud(solicitudId);
            SolicitudProceso solicitudFinal = new SolicitudProcesoMongoDAO().buscarPorId(solicitudId);

            ReportePeriodico reporte = new ReportePeriodico();
            reporte.setProgramacionId(consulta.getId());
            reporte.setNombre(consulta.getNombre());
            reporte.setProcesoId(proceso.getId());
            reporte.setTipoProceso(proceso.getTipo());
            reporte.setSolicitudId(solicitudId);
            if (solicitudFinal != null) {
                reporte.setParametros(solicitudFinal.getParametros());
                reporte.setResultado(solicitudFinal.getResultado());
                reporte.setObservaciones(solicitudFinal.getObservaciones());
            } else {
                reporte.setParametros(consulta.getParametros());
            }
            reporte.setFechaEjecucion(LocalDateTime.now());
            reporte.setEstado("OK");
            new ReportePeriodicoMongoDAO().guardar(reporte);
            ConsultasPeriodicasService.getInstance().registrarEjecucionExitosa(consulta);
        } catch (ErrorConexionMongoException | ErrorConexionCassandraException | ErrorConexionMySQLException e) {
            registrarError(consulta, e, solicitudId);
        } catch (Exception e) {
            registrarError(consulta, e, solicitudId);
        }
    }

    private void registrarError(ConsultaPeriodica consulta, Exception e, String solicitudId) {
        System.err.println("Error en proceso periódico '" + consulta.getNombre() + "': " + e.getMessage());
        try {
            ReportePeriodico reporte = new ReportePeriodico();
            reporte.setProgramacionId(consulta.getId());
            reporte.setNombre(consulta.getNombre());
            reporte.setTipoProceso(consulta.getTipoProceso());
            reporte.setSolicitudId(solicitudId);
            reporte.setParametros(consulta.getParametros());
            reporte.setEstado("ERROR");
            reporte.setMensajeError(e.getMessage());
            reporte.setFechaEjecucion(LocalDateTime.now());
            new ReportePeriodicoMongoDAO().guardar(reporte);
        } catch (Exception ignored) {
        }
    }

    private Proceso obtenerProceso(ConsultaPeriodica consulta) throws ErrorConexionMongoException {
        Proceso proceso = null;
        String procesoId = consulta.getProcesoId();
        if (procesoId != null && !procesoId.isBlank()) {
            try {
                proceso = ProcesoService.getInstance().obtenerPorId(procesoId);
            } catch (IllegalArgumentException invalidId) {
                // versiones anteriores podían almacenar el tipo en lugar del ObjectId
            }
        }
        if (proceso == null && consulta.getTipoProceso() != null) {
            proceso = ProcesoService.getInstance().obtenerPorTipo(consulta.getTipoProceso());
        }
        return proceso;
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setDaemon(true);
            thread.setName("ProcesosPeriodicos-" + thread.getId());
            return thread;
        }
    }
}
