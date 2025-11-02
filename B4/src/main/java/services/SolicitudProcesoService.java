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
import services.SensorService;

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
        SolicitudProcesoMongoDAO dao = new SolicitudProcesoMongoDAO();
        dao.asignarTecnico(solicitudId, tecnicoId);
    }

    public void aprobarSolicitud(String solicitudId) throws ErrorConexionMongoException {
        SolicitudProcesoMongoDAO dao = new SolicitudProcesoMongoDAO();
        dao.actualizarEstado(solicitudId, "aprobado");
    }

    public void ejecutarSolicitud(String solicitudId) throws ErrorConexionMongoException, ErrorConexionCassandraException {
        SolicitudProcesoMongoDAO dao = new SolicitudProcesoMongoDAO();
        SolicitudProceso s = dao.buscarPorId(solicitudId);
        if (s == null) throw new ErrorConexionMongoException("Solicitud no encontrada: " + solicitudId, null);
        // marcar en estado ejecucion
        SolicitudProcesoMongoDAO dao2 = new SolicitudProcesoMongoDAO();
        dao2.actualizarEstado(solicitudId, "en_ejecucion");

        // registrar historial en Cassandra
        HistorialEjecucion historial = new HistorialEjecucion();
        historial.setSolicitudId(solicitudId);
        historial.setProcesoId(s.getProcesoId());
        // intentar parsear ids a Integer, si no, dejar null
        try { historial.setUsuarioId(Integer.parseInt(s.getUsuarioId())); } catch (Exception e) { historial.setUsuarioId(null); }
        try { historial.setTecnicoId(s.getTecnicoAsignadoId() != null ? Integer.parseInt(s.getTecnicoAsignadoId()) : null); } catch (Exception e) { historial.setTecnicoId(null); }
        historial.setEstado("INICIADO");

    HistorialEjecucionCassandraDAO histDao = new HistorialEjecucionCassandraDAO();
        histDao.insertar(historial);
    }

    public void completarSolicitud(String solicitudId) throws ErrorConexionMongoException, ErrorConexionMySQLException {
    SolicitudProcesoMongoDAO dao3 = new SolicitudProcesoMongoDAO();
    SolicitudProceso s = dao3.buscarPorId(solicitudId);
        if (s == null) throw new ErrorConexionMongoException("Solicitud no encontrada: " + solicitudId, null);

        // marcar completada
    SolicitudProcesoMongoDAO dao4 = new SolicitudProcesoMongoDAO();
    dao4.actualizarEstado(solicitudId, "completado");

        // generar factura en MySQL basada en el costo del proceso
        Proceso proceso = null;
        try {
            ProcesoMongoDAO pm = new ProcesoMongoDAO();
            proceso = pm.buscarPorId(s.getProcesoId());
        } catch (Exception e) {
            // si no se puede obtener proceso, usamos monto por defecto
        }

        Double monto = proceso != null && proceso.getCosto() != null ? proceso.getCosto() : 0.0;

        Integer usuarioIdInt = null;
        try { usuarioIdInt = Integer.parseInt(s.getUsuarioId()); } catch (Exception e) { usuarioIdInt = null; }

        // Crear factura en MySQL (usuarioId puede ser null si el usuario está en Mongo)
        Factura factura = new Factura(usuarioIdInt, monto, "Factura por solicitud " + solicitudId);
        try {
            Usuario usuario = UsuarioService.getInstance().obtenerPorId(s.getUsuarioId());
            if (usuario != null) {
                factura.setNombreFacturacion(usuario.getNombre());
                factura.setApellidoFacturacion(usuario.getApellido());
                factura.setDireccionFacturacion(usuario.getDireccion());
                factura.setTelefonoFacturacion(usuario.getTelefono());
            }
        } catch (ErrorConexionMongoException ex) {
            System.err.println("No se pudo obtener información adicional del usuario " + s.getUsuarioId() + ": " + ex.getMessage());
        }
        FacturaMySQLRepository.getInstance().crear(factura);

        // Si tenemos un usuario numérico, actualizar cuenta y crear movimiento
        if (usuarioIdInt != null) {
            CuentaCorriente cuenta = CuentaMySQLRepository.getInstance().obtenerPorUsuario(usuarioIdInt);
            if (cuenta == null) {
                cuenta = new CuentaCorriente(usuarioIdInt);
                CuentaMySQLRepository.getInstance().crear(cuenta);
                // volver a leer para obtener id
                cuenta = CuentaMySQLRepository.getInstance().obtenerPorUsuario(usuarioIdInt);
            }
            if (cuenta != null) {
                Double nuevoSaldo = cuenta.getSaldo() - monto;
                CuentaMySQLRepository.getInstance().actualizarSaldo(cuenta.getId(), nuevoSaldo);

                Movimiento mov = new Movimiento(cuenta.getId(), "CARGO", monto, "Factura por solicitud " + solicitudId);
                MovimientoMySQLRepository.getInstance().crear(mov);
            }
        }

        // Crear alerta y mensaje para notificar al usuario que su informe está listo
        try {
            Alerta alerta = new Alerta("PROCESO_COMPLETADO", "Su informe para la solicitud " + solicitudId + " está listo.", "MEDIA");
            // usuarioId en Solicitud es String (Mongo id), preferimos notificar con ese id
            alerta.setUsuarioId(s.getUsuarioId());
            AlertaMongoDAO alertaDao = new AlertaMongoDAO();
            alertaDao.insertar(alerta);

            Mensaje mensaje = new Mensaje();
            mensaje.setId(java.util.UUID.randomUUID().toString());
            mensaje.setRemitente("system"); // sistema como id simbólico
            mensaje.setDestinatario(s.getUsuarioId());
            mensaje.setContenido("Su informe solicitado (" + solicitudId + ") ha sido completado.");
            mensaje.setTipo("PRIVADO");
            MensajeMongoDAO.getInstance().crear(mensaje);
        } catch (Exception e) {
            // si falla la notificación, no impedir el flujo principal
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
        dao.actualizarEstado(solicitudId, "rechazado");
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
        return crearSolicitud(usuarioId, procesoId, params);
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
}
