package services;

import exceptions.ErrorConexionCassandraException;
import exceptions.ErrorConexionMongoException;
import exceptions.ErrorConexionMySQLException;
import modelo.Factura;
import modelo.Usuario;
import modelo.HistorialEjecucion;
import modelo.SolicitudProceso;
import modelo.Proceso;
import modelo.CuentaCorriente;
import modelo.Movimiento;
import modelo.Sensor;
import repository.SolicitudProcesoMongoDAO;
import repository.HistorialEjecucionCassandraDAO;
import repository.FacturaMySQLRepository;
import repository.ProcesoMongoDAO;
import repository.CuentaMySQLRepository;
import repository.MovimientoMySQLRepository;
import repository.AlertaMongoDAO;
import repository.MensajeMongoDAO;
import modelo.Mensaje;
import modelo.Alerta;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolicitudProcesoService {

    private static SolicitudProcesoService instance;
    public static final String ORIGEN_MANUAL = "MANUAL";
    public static final String ORIGEN_AUTOMATICO = "AUTOMATICO";

    private SolicitudProcesoService() {}

    public static SolicitudProcesoService getInstance() {
        if (instance == null) instance = new SolicitudProcesoService();
        return instance;
    }

    public String crearSolicitud(String usuarioId, String procesoId, Map<String, Object> parametros)
            throws ErrorConexionMongoException {
        return crearSolicitud(usuarioId, procesoId, parametros, ORIGEN_MANUAL);
    }

    public String crearSolicitudAutomatica(String usuarioId, String procesoId, Map<String, Object> parametros)
            throws ErrorConexionMongoException {
        return crearSolicitud(usuarioId, procesoId, parametros, ORIGEN_AUTOMATICO);
    }

    public String crearSolicitud(String usuarioId, String procesoId, Map<String, Object> parametros, String origen)
            throws ErrorConexionMongoException {
        SolicitudProceso s = new SolicitudProceso(usuarioId, procesoId, parametros);
        s.setOrigen(origen != null ? origen : ORIGEN_MANUAL);
        SolicitudProcesoMongoDAO dao = new SolicitudProcesoMongoDAO();
        return dao.insertar(s);
    }

    public void asignarTecnico(String solicitudId, String tecnicoId) throws ErrorConexionMongoException {
        validarUUID(solicitudId);
        SolicitudProcesoMongoDAO dao = new SolicitudProcesoMongoDAO();
        dao.asignarTecnico(solicitudId, tecnicoId);
    }

    public void aprobarSolicitud(String solicitudId) throws ErrorConexionMongoException {
        validarUUID(solicitudId);
        SolicitudProcesoMongoDAO dao = new SolicitudProcesoMongoDAO();
        dao.actualizarEstado(solicitudId, "APROBADO");
    }

    public void ejecutarSolicitud(String solicitudId) throws ErrorConexionMongoException, ErrorConexionCassandraException {
        validarUUID(solicitudId);
        SolicitudProcesoMongoDAO dao = new SolicitudProcesoMongoDAO();
        SolicitudProceso s = dao.buscarPorId(solicitudId);
        if (s == null) throw new ErrorConexionMongoException("Solicitud no encontrada: " + solicitudId, null);
        // marcar en estado ejecucion
        SolicitudProcesoMongoDAO dao2 = new SolicitudProcesoMongoDAO();
        dao2.actualizarEstado(solicitudId, "EN_EJECUCION");

        // registrar historial en Cassandra
        HistorialEjecucion historial = new HistorialEjecucion();
        historial.setSolicitudId(solicitudId);
        historial.setProcesoId(s.getProcesoId());
        // intentar parsear ids a Integer, si no, dejar null
        try { historial.setUsuarioId(Integer.parseInt(s.getUsuarioId())); } catch (Exception e) { historial.setUsuarioId(null); }
        try { historial.setTecnicoId(s.getTecnicoAsignadoId() != null ? Integer.parseInt(s.getTecnicoAsignadoId()) : null); } catch (Exception e) { historial.setTecnicoId(null); }
        historial.setEstado("INICIADO");
        java.time.LocalDateTime inicio = java.time.LocalDateTime.now();
        historial.setFechaEjecucion(inicio);
        historial.setFechaInicio(inicio);

        HistorialEjecucionCassandraDAO histDao = new HistorialEjecucionCassandraDAO();
        histDao.insertar(historial);
    }

    public void completarSolicitud(String solicitudId) throws ErrorConexionMongoException, ErrorConexionMySQLException {
        validarUUID(solicitudId);
        SolicitudProcesoMongoDAO dao3 = new SolicitudProcesoMongoDAO();
        SolicitudProceso s = dao3.buscarPorId(solicitudId);
        if (s == null) throw new ErrorConexionMongoException("Solicitud no encontrada: " + solicitudId, null);

        // marcar completada
        SolicitudProcesoMongoDAO dao4 = new SolicitudProcesoMongoDAO();
        dao4.actualizarEstado(solicitudId, "COMPLETADO");

        // generar resultado y guardar; luego factura
        Proceso proceso = null;
        try {
            ProcesoMongoDAO pm = new ProcesoMongoDAO();
            proceso = pm.buscarPorId(s.getProcesoId());
        } catch (Exception e) {
            // si no se puede obtener proceso, usamos monto por defecto
        }

                String resultadoGenerado = null;
        String observacionesGeneradas = null;
        if (proceso != null) {
            try {
                ProcesoEjecucionService.ProcesoResultado resumen =
                        ProcesoEjecucionService.getInstance().generarResultado(proceso, s);
                if (resumen != null) {
                    resultadoGenerado = resumen.getResultado();
                    observacionesGeneradas = resumen.getObservaciones();
                }
            } catch (ErrorConexionCassandraException | ErrorConexionMongoException ex) {
                System.err.println("No se pudo generar informe para la solicitud " + solicitudId + ": " + ex.getMessage());
                observacionesGeneradas = "No se pudo generar el informe automatico: " + ex.getMessage();
            }
        } else {
            observacionesGeneradas = "No se encontro la definicion del proceso asociado (" + s.getProcesoId() + ").";
        }

        if (resultadoGenerado != null || observacionesGeneradas != null) {
            dao4.actualizarResultado(solicitudId, resultadoGenerado, observacionesGeneradas);
            s.setResultado(resultadoGenerado);
            s.setObservaciones(observacionesGeneradas);
        }

        Double monto = proceso != null && proceso.getCosto() != null ? proceso.getCosto() : 0.0;

        String usuarioIdCanonical = s.getUsuarioId();

        // Hook de facturación: crear factura atómica y registrar cargo en una única transacción
        try {
            String descripcionFactura = "Factura por solicitud " + solicitudId + (proceso != null && proceso.getNombre() != null ? " - " + proceso.getNombre() : "");
            FacturaService.getInstance().crearFactura(usuarioIdCanonical, solicitudId, monto, descripcionFactura);
        } catch (ErrorConexionMySQLException ex) {
            // Registrar el error pero no abortar la finalización; podría reintentarse luego
            System.err.println("Error al generar factura para solicitud " + solicitudId + ": " + ex.getMessage());
        }

        // Crear alerta y mensaje para notificar al usuario que su informe estÃƒÂ¡ listo
        try {
            Alerta alerta = new Alerta("PROCESO_COMPLETADO", "Su informe para la solicitud " + solicitudId + " estÃƒÂ¡ listo.", "MEDIA");
            AlertaMongoDAO alertaDao = new AlertaMongoDAO();
            alertaDao.insertar(alerta);

            Mensaje mensaje = new Mensaje();
            mensaje.setId(java.util.UUID.randomUUID().toString());
            mensaje.setRemitente("system"); // sistema como id simbÃƒÂ³lico
            mensaje.setDestinatario(s.getUsuarioId());
            mensaje.setContenido("Su informe solicitado (" + solicitudId + ") ha sido completado.");
            mensaje.setTipo("PRIVADO");
            MensajeMongoDAO.getInstance().crear(mensaje);
        } catch (Exception e) {
            // si falla la notificaciÃƒÂ³n, no impedir el flujo principal
        }
    }

    public List<SolicitudProceso> listarPendientes() throws ErrorConexionMongoException {
        return new SolicitudProcesoMongoDAO().listarPendientes();
    }

    public List<SolicitudProceso> listarAsignadas(String tecnicoId) throws ErrorConexionMongoException {
        if (tecnicoId == null || tecnicoId.isBlank()) {
            return Collections.emptyList();
        }
        return new SolicitudProcesoMongoDAO().listarPorTecnico(tecnicoId);
    }

    public void rechazarSolicitud(String solicitudId) throws ErrorConexionMongoException {
        SolicitudProcesoMongoDAO dao = new SolicitudProcesoMongoDAO();
        dao.actualizarEstado(solicitudId, "RECHAZADO");
    }

    public void cancelarSolicitudUsuario(String solicitudId, String usuarioId) throws ErrorConexionMongoException {
        if (solicitudId == null || solicitudId.isBlank()) {
            throw new IllegalArgumentException("ID de solicitud requerido para cancelar.");
        }
        validarUUID(solicitudId);
        SolicitudProcesoMongoDAO dao = new SolicitudProcesoMongoDAO();
        SolicitudProceso solicitud = dao.buscarPorId(solicitudId);
        if (solicitud == null) {
            throw new IllegalStateException("Solicitud no encontrada.");
        }
        if (usuarioId == null || !usuarioId.equals(solicitud.getUsuarioId())) {
            throw new IllegalStateException("No podés cancelar una solicitud de otro usuario.");
        }
        String estado = solicitud.getEstado() != null ? solicitud.getEstado().toUpperCase() : "";
        if ("COMPLETADO".equals(estado) || "EN_EJECUCION".equals(estado)) {
            throw new IllegalStateException("No podés cancelar una solicitud que ya se ejecutó o está en ejecución.");
        }
        if ("CANCELADO".equals(estado)) {
            return;
        }
        dao.actualizarEstado(solicitudId, "CANCELADO");
    }

    public List<SolicitudProceso> listarPorUsuario(String usuarioId) throws ErrorConexionMongoException {
        if (usuarioId == null || usuarioId.isBlank()) {
            return Collections.emptyList();
        }
        return new SolicitudProcesoMongoDAO().listarPorUsuario(usuarioId);
    }

    public String crearSolicitudUsuario(String usuarioId, String procesoId, Map<String, Object> parametros)
            throws ErrorConexionMongoException, ErrorConexionCassandraException {
        Map<String, Object> params = parametros != null ? new HashMap<>(parametros) : new HashMap<>();
        String sensorId = seleccionarSensorParaSolicitud(params);
        if (sensorId != null && !params.containsKey("sensorId")) {
            params.put("sensorId", sensorId);
        }
        return crearSolicitud(usuarioId, procesoId, params, ORIGEN_MANUAL);
    }

    private String seleccionarSensorParaSolicitud(Map<String, Object> parametros) throws ErrorConexionCassandraException {
        List<Sensor> sensores = SensorService.getInstance().listarTodos();
        if (sensores.isEmpty()) {
            return null;
        }
        Sensor candidato = null;
        String ciudad = obtenerParametro(parametros, "ciudad");
        String pais = obtenerParametro(parametros, "pais");
        String tipo = obtenerParametro(parametros, "tipoSensor");

        for (Sensor sensor : sensores) {
            String estado = sensor.getEstado();
            if (estado != null && !"ACTIVO".equalsIgnoreCase(estado)) {
                continue;
            }
            if (tipo != null && sensor.getTipo() != null && !sensor.getTipo().equalsIgnoreCase(tipo)) {
                continue;
            }
            if (ciudad != null && sensor.getCiudad() != null && sensor.getCiudad().equalsIgnoreCase(ciudad)) {
                return sensor.getId();
            }
            if (pais != null && sensor.getPais() != null && sensor.getPais().equalsIgnoreCase(pais)) {
                candidato = sensor;
            } else if (candidato == null) {
                candidato = sensor;
            }
        }
        return candidato != null ? candidato.getId() : sensores.get(0).getId();
    }

    private String obtenerParametro(Map<String, Object> parametros, String clave) {
        Object valor = parametros.get(clave);
        if (valor instanceof String) {
            String str = ((String) valor).trim();
            return str.isEmpty() ? null : str;
        }
        return null;
    }
    
    private void validarUUID(String id) {
        try {
            java.util.UUID.fromString(id);
        } catch (Exception e) {
            throw new IllegalArgumentException("ID de solicitud no es un UUID valido: " + id);
        }
    }
}












