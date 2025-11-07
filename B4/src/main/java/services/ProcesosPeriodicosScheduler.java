package services;

import exceptions.ErrorConexionCassandraException;
import exceptions.ErrorConexionMongoException;
import exceptions.ErrorConexionMySQLException;
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

    private final List<ConsultaProgramada> consultas;
    private final ScheduledExecutorService executor;
    private volatile boolean iniciado;

    private ProcesosPeriodicosScheduler() {
        this.consultas = List.of(
                ConsultaProgramada.mensual("Extremos Buenos Aires",
                        "INFORME_MAX_MIN",
                        "tbonomo@uade.edu.ar",
                        Map.of(
                                "ciudad", "buenos aires",
                                "tipoSensor", "temperatura",
                                "tipoProceso", "INFORME_MAX_MIN",
                                "periodicidad", "MENSUAL"
                        )),
                ConsultaProgramada.mensual("Promedios Argentina",
                        "INFORME_PROMEDIOS",
                        "cliente.corporativo@polyglot.local",
                        Map.of(
                                "pais", "argentina",
                                "tipoProceso", "INFORME_PROMEDIOS",
                                "periodicidad", "MENSUAL"
                        )),
                ConsultaProgramada.trimestral("Extremos Zona Oeste",
                        "INFORME_MAX_MIN",
                        "cliente.zonaoeste@polyglot.local",
                        Map.of(
                                "zona", "oeste",
                                "tipoProceso", "INFORME_MAX_MIN",
                                "periodicidad", "TRIMESTRAL"
                        ))
        );
        this.executor = Executors.newScheduledThreadPool(
                Math.max(1, consultas.size()),
                new DaemonThreadFactory());
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
        for (int i = 0; i < consultas.size(); i++) {
            ConsultaProgramada consulta = consultas.get(i);
            long delay = delayInicial + (i * 15L);
            executor.scheduleAtFixedRate(() -> ejecutarConsulta(consulta),
                    delay,
                    consulta.periodoMinutos,
                    TimeUnit.MINUTES);
        }
        iniciado = true;
    }

    private void ejecutarConsulta(ConsultaProgramada consulta) {
        String solicitudId = null;
        try {
            Proceso proceso = ProcesoService.getInstance().obtenerPorTipo(consulta.tipoProceso);
            if (proceso == null) {
                System.err.println("Proceso no encontrado para tipo " + consulta.tipoProceso);
                return;
            }
            String usuarioDestino = consulta.usuarioId != null && !consulta.usuarioId.isBlank()
                    ? consulta.usuarioId : SYSTEM_USER;
            solicitudId = SolicitudProcesoService.getInstance()
                    .crearSolicitudAutomatica(usuarioDestino, proceso.getId(), consulta.parametros);
            SolicitudProcesoService.getInstance().ejecutarSolicitud(solicitudId);
            SolicitudProcesoService.getInstance().completarSolicitud(solicitudId);
            SolicitudProceso solicitudFinal = new SolicitudProcesoMongoDAO().buscarPorId(solicitudId);

            ReportePeriodico reporte = new ReportePeriodico();
            reporte.setNombre(consulta.nombre);
            reporte.setProcesoId(proceso.getId());
            reporte.setTipoProceso(proceso.getTipo());
            reporte.setSolicitudId(solicitudId);
            if (solicitudFinal != null) {
                reporte.setParametros(solicitudFinal.getParametros());
                reporte.setResultado(solicitudFinal.getResultado());
                reporte.setObservaciones(solicitudFinal.getObservaciones());
            } else {
                reporte.setParametros(consulta.parametros);
            }
            reporte.setFechaEjecucion(LocalDateTime.now());
            reporte.setEstado("OK");
            new ReportePeriodicoMongoDAO().guardar(reporte);
        } catch (ErrorConexionMongoException | ErrorConexionCassandraException | ErrorConexionMySQLException e) {
            registrarError(consulta, e, solicitudId);
        } catch (Exception e) {
            registrarError(consulta, e, solicitudId);
        }
    }

    private void registrarError(ConsultaProgramada consulta, Exception e, String solicitudId) {
        System.err.println("Error en proceso periódico '" + consulta.nombre + "': " + e.getMessage());
        try {
            ReportePeriodico reporte = new ReportePeriodico();
            reporte.setNombre(consulta.nombre);
            reporte.setTipoProceso(consulta.tipoProceso);
            reporte.setSolicitudId(solicitudId);
            reporte.setParametros(consulta.parametros);
            reporte.setEstado("ERROR");
            reporte.setMensajeError(e.getMessage());
            reporte.setFechaEjecucion(LocalDateTime.now());
            new ReportePeriodicoMongoDAO().guardar(reporte);
        } catch (Exception ignored) {
        }
    }

    private static class ConsultaProgramada {
        private final String nombre;
        private final String tipoProceso;
        private final long periodoMinutos;
        private final String usuarioId;
        private final Map<String, Object> parametros;

        private ConsultaProgramada(String nombre, String tipoProceso, long periodoMinutos,
                                   String usuarioId, Map<String, Object> parametros) {
            this.nombre = nombre;
            this.tipoProceso = tipoProceso;
            this.periodoMinutos = periodoMinutos;
            this.usuarioId = usuarioId;
            this.parametros = parametros;
        }

        private static ConsultaProgramada mensual(String nombre, String tipoProceso, String usuarioId, Map<String, Object> parametros) {
            return new ConsultaProgramada(nombre, tipoProceso, TimeUnit.DAYS.toMinutes(30), usuarioId, parametros);
        }

        private static ConsultaProgramada trimestral(String nombre, String tipoProceso, String usuarioId, Map<String, Object> parametros) {
            return new ConsultaProgramada(nombre, tipoProceso, TimeUnit.DAYS.toMinutes(90), usuarioId, parametros);
        }
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
