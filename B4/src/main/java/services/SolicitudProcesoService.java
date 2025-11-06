package services;

import exceptions.ErrorConexionCassandraException;
import exceptions.ErrorConexionMongoException;
import exceptions.ErrorConexionMySQLException;
import modelo.Alerta;
import modelo.CuentaCorriente;
import modelo.Factura;
import modelo.HistorialEjecucion;
import modelo.Mensaje;
import modelo.Proceso;
import modelo.Sensor;
import modelo.SolicitudProceso;
import modelo.Usuario;
import repository.AlertaMongoDAO;
import repository.CuentaMySQLRepository;
import repository.FacturaMySQLRepository;
import repository.HistorialEjecucionCassandraDAO;
import repository.MensajeMongoDAO;
import repository.MovimientoMySQLRepository;
import repository.ProcesoMongoDAO;
import repository.SolicitudProcesoMongoDAO;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolicitudProcesoService {

    private static SolicitudProcesoService instance;

    private SolicitudProcesoService() {}

    public static SolicitudProcesoService getInstance() {
        if (instance == null) instance = new SolicitudProcesoService();
        return instance;
    }

    public String crearSolicitud(String usuarioId, String procesoId, Map<String, Object> parametros)
            throws ErrorConexionMongoException {
        SolicitudProceso s = new SolicitudProceso(usuarioId, procesoId, parametros);
        SolicitudProcesoMongoDAO dao = new SolicitudProcesoMongoDAO();
        return dao.insertar(s);
    }

    public void asignarTecnico(String solicitudId, String tecnicoId) throws ErrorConexionMongoException {
        validarUUID(solicitudId);
        new SolicitudProcesoMongoDAO().asignarTecnico(solicitudId, tecnicoId);
    }

    public void aprobarSolicitud(String solicitudId) throws ErrorConexionMongoException {
        validarUUID(solicitudId);
        new SolicitudProcesoMongoDAO().actualizarEstado(solicitudId, "APROBADO");
    }

    public void ejecutarSolicitud(String solicitudId) throws ErrorConexionMongoException, ErrorConexionCassandraException {
        validarUUID(solicitudId);
        SolicitudProcesoMongoDAO dao = new SolicitudProcesoMongoDAO();
        SolicitudProceso s = dao.buscarPorId(solicitudId);
        if (s == null) throw new ErrorConexionMongoException("Solicitud no encontrada: " + solicitudId, null);

        // Estado y registro de inicio
        dao.actualizarEstado(solicitudId, "EN_EJECUCION");
        HistorialEjecucion historial = new HistorialEjecucion();
        historial.setSolicitudId(solicitudId);
        historial.setProcesoId(s.getProcesoId());
        try { historial.setUsuarioId(Integer.parseInt(s.getUsuarioId())); } catch (Exception ignored) { historial.setUsuarioId(null); }
        try { historial.setTecnicoId(s.getTecnicoAsignadoId() != null ? Integer.parseInt(s.getTecnicoAsignadoId()) : null); } catch (Exception ignored) { historial.setTecnicoId(null); }
        historial.setEstado("INICIADO");
        new HistorialEjecucionCassandraDAO().insertar(historial);
    }

    public void completarSolicitud(String solicitudId) throws ErrorConexionMongoException, ErrorConexionMySQLException {
        validarUUID(solicitudId);
        SolicitudProcesoMongoDAO dao = new SolicitudProcesoMongoDAO();
        SolicitudProceso s = dao.buscarPorId(solicitudId);
        if (s == null) throw new ErrorConexionMongoException("Solicitud no encontrada: " + solicitudId, null);

        // Marcar completado
        dao.actualizarEstado(solicitudId, "COMPLETADO");

        // Generar resultado (best-effort)
        String resultadoGenerado = null;
        String observacionesGeneradas = null;
        try {
            Proceso proceso = new ProcesoMongoDAO().buscarPorId(s.getProcesoId());
            if (proceso != null) {
                ProcesoEjecucionService.ProcesoResultado resumen =
                        ProcesoEjecucionService.getInstance().generarResultado(proceso, s);
                if (resumen != null) {
                    resultadoGenerado = resumen.getResultado();
                    observacionesGeneradas = resumen.getObservaciones();
                }
            } else {
                observacionesGeneradas = "No se encontro la definicion del proceso asociado (" + s.getProcesoId() + ").";
            }
        } catch (Exception ex) {
            observacionesGeneradas = "No se pudo generar el informe automatico: " + ex.getMessage();
        }
        if (resultadoGenerado != null || observacionesGeneradas != null) {
            dao.actualizarResultado(solicitudId, resultadoGenerado, observacionesGeneradas);
            s.setResultado(resultadoGenerado);
            s.setObservaciones(observacionesGeneradas);
        }

        // Factura y cuenta
        Double monto = 0.0;
        try {
            Proceso p = new ProcesoMongoDAO().buscarPorId(s.getProcesoId());
            if (p != null && p.getCosto() != null) monto = p.getCosto();
        } catch (Exception ignored) {}

        Integer usuarioIdInt = null;
        try { usuarioIdInt = Integer.parseInt(s.getUsuarioId()); } catch (Exception ignored) { usuarioIdInt = null; }

        Factura factura = new Factura(usuarioIdInt, monto, "Factura por solicitud " + solicitudId);
        factura.setSolicitudId(solicitudId);
        try {
            Usuario usuario = UsuarioService.getInstance().obtenerPorId(s.getUsuarioId());
            if (usuario != null) {
                factura.setNombreFacturacion(usuario.getNombre());
                factura.setApellidoFacturacion(usuario.getApellido());
                factura.setDireccionFacturacion(usuario.getDireccion());
                factura.setTelefonoFacturacion(usuario.getTelefono());
            }
        } catch (ErrorConexionMongoException ignored) {}
        FacturaMySQLRepository.getInstance().crear(factura);

        if (usuarioIdInt != null) {
            CuentaCorriente cuenta = CuentaMySQLRepository.getInstance().obtenerPorUsuario(usuarioIdInt);
            if (cuenta == null) {
                cuenta = new CuentaCorriente(usuarioIdInt);
                CuentaMySQLRepository.getInstance().crear(cuenta);
                cuenta = CuentaMySQLRepository.getInstance().obtenerPorUsuario(usuarioIdInt);
            }
            if (cuenta != null) {
                Double nuevoSaldo = cuenta.getSaldo() - monto;
                CuentaMySQLRepository.getInstance().actualizarSaldo(cuenta.getId(), nuevoSaldo);
                var mov = new modelo.Movimiento(cuenta.getId(), "CARGO", monto, "Factura por solicitud " + solicitudId);
                MovimientoMySQLRepository.getInstance().crear(mov);
            }
        }

        // Notificación
        try {
            Alerta alerta = new Alerta("PROCESO_COMPLETADO", "Su informe para la solicitud " + solicitudId + " esta listo.", "MEDIA");
            alerta.setUsuarioId(s.getUsuarioId());
            new AlertaMongoDAO().insertar(alerta);

            Mensaje mensaje = new Mensaje();
            mensaje.setId(java.util.UUID.randomUUID().toString());
            mensaje.setRemitente("system");
            mensaje.setDestinatario(s.getUsuarioId());
            mensaje.setContenido("Su informe solicitado (" + solicitudId + ") ha sido completado.");
            mensaje.setTipo("PRIVADO");
            MensajeMongoDAO.getInstance().crear(mensaje);
        } catch (Exception ignored) {}
    }

    public List<SolicitudProceso> listarPendientes() throws ErrorConexionMongoException {
        return new SolicitudProcesoMongoDAO().listarPendientes();
    }

    public List<SolicitudProceso> listarAsignadas(String tecnicoId) throws ErrorConexionMongoException {
        if (tecnicoId == null || tecnicoId.isBlank()) return Collections.emptyList();
        return new SolicitudProcesoMongoDAO().listarPorTecnico(tecnicoId);
    }

    public void rechazarSolicitud(String solicitudId) throws ErrorConexionMongoException {
        new SolicitudProcesoMongoDAO().actualizarEstado(solicitudId, "RECHAZADO");
    }

    public List<SolicitudProceso> listarPorUsuario(String usuarioId) throws ErrorConexionMongoException {
        if (usuarioId == null || usuarioId.isBlank()) return Collections.emptyList();
        return new SolicitudProcesoMongoDAO().listarPorUsuario(usuarioId);
    }

    public String crearSolicitudUsuario(String usuarioId, String procesoId, Map<String, Object> parametros)
            throws ErrorConexionMongoException, ErrorConexionCassandraException {
        Map<String, Object> params = parametros != null ? new HashMap<>(parametros) : new HashMap<>();
        String sensorId = seleccionarSensorParaSolicitud(params);
        if (sensorId != null && !params.containsKey("sensorId")) params.put("sensorId", sensorId);
        return crearSolicitud(usuarioId, procesoId, params);
    }

    private String seleccionarSensorParaSolicitud(Map<String, Object> parametros) throws ErrorConexionCassandraException {
        List<Sensor> sensores = SensorService.getInstance().listarTodos();
        if (sensores.isEmpty()) return null;
        Sensor candidato = null;
        String ciudad = obtenerParametro(parametros, "ciudad");
        String pais = obtenerParametro(parametros, "pais");
        String tipo = obtenerParametro(parametros, "tipoSensor");
        for (Sensor sensor : sensores) {
            String estado = sensor.getEstado();
            if (estado != null && !"ACTIVO".equalsIgnoreCase(estado)) continue;
            if (tipo != null && sensor.getTipo() != null && !sensor.getTipo().equalsIgnoreCase(tipo)) continue;
            if (ciudad != null && sensor.getCiudad() != null && sensor.getCiudad().equalsIgnoreCase(ciudad)) return sensor.getId();
            if (pais != null && sensor.getPais() != null && sensor.getPais().equalsIgnoreCase(pais)) candidato = sensor;
            else if (candidato == null) candidato = sensor;
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

