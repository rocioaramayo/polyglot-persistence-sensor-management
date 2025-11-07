package services;

import exceptions.ErrorConexionMongoException;
import modelo.ConsultaPeriodica;
import modelo.Proceso;
import modelo.SolicitudProceso;
import repository.ConsultaPeriodicaMongoDAO;
import repository.ProcesoMongoDAO;
import repository.SolicitudProcesoMongoDAO;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConsultasPeriodicasService {
    private static ConsultasPeriodicasService instance;

    public static ConsultasPeriodicasService getInstance() {
        if (instance == null) {
            instance = new ConsultasPeriodicasService();
        }
        return instance;
    }

    public ConsultaPeriodica agendarDesdeSolicitud(String solicitudId, String tecnicoId)
            throws ErrorConexionMongoException {
        SolicitudProcesoMongoDAO solicitudDao = new SolicitudProcesoMongoDAO();
        SolicitudProceso solicitud = solicitudDao.buscarPorId(solicitudId);
        if (solicitud == null) {
            throw new IllegalStateException("Solicitud no encontrada para agendar");
        }
        ConsultaPeriodicaMongoDAO periodicaDao = new ConsultaPeriodicaMongoDAO();
        ConsultaPeriodica existente = periodicaDao.buscarPorSolicitud(solicitudId);
        if (existente != null && existente.isActivo()) {
            throw new IllegalStateException("La solicitud ya se encuentra en la lista de ejecución periódica");
        }

        Map<String, Object> parametros = solicitud.getParametros() != null
                ? new HashMap<>(solicitud.getParametros())
                : new HashMap<>();
        String periodicidad = obtenerPeriodicidad(parametros);
        long intervalo = resolverIntervaloMinutos(periodicidad);
        if (intervalo <= 0) {
            periodicidad = "MENSUAL";
            intervalo = resolverIntervaloMinutos(periodicidad);
        }

        Proceso proceso = null;
        try {
            proceso = new ProcesoMongoDAO().buscarPorId(solicitud.getProcesoId());
        } catch (Exception ignored) {
        }

        ConsultaPeriodica consulta = new ConsultaPeriodica();
        consulta.setSolicitudBaseId(solicitud.getId());
        consulta.setProcesoId(solicitud.getProcesoId());
        if (proceso != null) {
            consulta.setNombre(proceso.getNombre());
            consulta.setDescripcion(proceso.getDescripcion());
            consulta.setTipoProceso(proceso.getTipo());
        }
        consulta.setUsuarioId(solicitud.getUsuarioId());
        consulta.setTecnicoId(tecnicoId);
        consulta.setParametros(parametros);
        consulta.setPeriodicidad(periodicidad != null ? periodicidad : "MENSUAL");
        consulta.setIntervaloMinutos(intervalo);
        consulta.setProximaEjecucion(calcularProximaEjecucion(consulta.getPeriodicidad()));
        consulta.setActivo(true);

        if (consulta.getNombre() == null || consulta.getNombre().isBlank()) {
            consulta.setNombre("Proceso periódico " + solicitud.getId());
        }
        if ((consulta.getTipoProceso() == null || consulta.getTipoProceso().isBlank())
                && parametros.containsKey("tipoProceso")) {
            consulta.setTipoProceso(String.valueOf(parametros.get("tipoProceso")));
        }

        periodicaDao.guardar(consulta);
        return consulta;
    }

    public List<ConsultaPeriodica> listarPendientes(LocalDateTime hasta) throws ErrorConexionMongoException {
        if (hasta == null) {
            hasta = LocalDateTime.now();
        }
        return new ConsultaPeriodicaMongoDAO().listarActivasHasta(hasta);
    }

    public void registrarEjecucionExitosa(ConsultaPeriodica consulta) throws ErrorConexionMongoException {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime siguiente = ahora.plusMinutes(Math.max(consulta.getIntervaloMinutos(), 1));
        new ConsultaPeriodicaMongoDAO().actualizarProgramacion(consulta.getId(), siguiente, ahora);
    }

    public void desactivar(String programacionId, String motivo) throws ErrorConexionMongoException {
        new ConsultaPeriodicaMongoDAO().desactivar(programacionId, motivo);
    }

    private String obtenerPeriodicidad(Map<String, Object> params) {
        Object valor = params.get("periodicidad");
        if (valor == null) {
            return null;
        }
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto.toUpperCase(Locale.ROOT);
    }

    private long resolverIntervaloMinutos(String periodicidad) {
        if (periodicidad == null) {
            return 0;
        }
        String valor = periodicidad.toUpperCase(Locale.ROOT);
        switch (valor) {
            case "DIARIA":
                return 60L * 24;
            case "SEMANAL":
                return 60L * 24 * 7;
            case "MENSUAL":
                return 60L * 24 * 30;
            case "TRIMESTRAL":
                return 60L * 24 * 90;
            case "ANUAL":
            case "ANUALIZADA":
            case "ANUALIZADAS":
                return 60L * 24 * 365;
            default:
                return 60L * 24 * 30; // por defecto mensual
        }
    }

    private LocalDateTime calcularProximaEjecucion(String periodicidad) {
        LocalDateTime ahora = LocalDateTime.now();
        if (periodicidad == null) {
            return ahora.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        String valor = periodicidad.toUpperCase(Locale.ROOT);
        switch (valor) {
            case "DIARIA":
                return truncarDia(ahora.plusDays(1));
            case "SEMANAL":
                return truncarDia(ahora.plusWeeks(1));
            case "TRIMESTRAL":
                return truncarDia(ahora.plusMonths(3)).withDayOfMonth(1);
            case "ANUAL":
            case "ANUALIZADA":
            case "ANUALIZADAS":
                return truncarDia(ahora.plusYears(1)).withDayOfYear(1);
            case "MENSUAL":
            default:
                return truncarDia(ahora.plusMonths(1)).withDayOfMonth(1);
        }
    }

    private LocalDateTime truncarDia(LocalDateTime fecha) {
        return fecha.withHour(0).withMinute(0).withSecond(0).withNano(0);
    }
}
