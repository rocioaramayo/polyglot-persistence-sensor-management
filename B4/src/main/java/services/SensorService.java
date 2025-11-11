package services;

import exceptions.AuthorizationException;
import modelo.Sensor;
import repository.SensorCassandraDAO;
import exceptions.ErrorConexionCassandraException;
import utils.AuthorizationHelper;
import utils.Roles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de sensores.
 * 
 * RBAC Rules:
 * - listarActivosSoloAdmin(): Solo ADMINISTRADOR puede ver sensores activos con estado completo
 * - listarTodosMascaraEstado(): Usuarios no-admin ven sensores pero sin el campo estado
 */
public class SensorService {
    private static SensorService instance;

    private SensorService() {}

    public static SensorService getInstance() {
        if (instance == null) {
            instance = new SensorService();
        }
        return instance;
    }

    public void crearSensor(String nombre,
                            String codigo,
                            String tipo,
                            Double latitud,
                            Double longitud,
                            String ciudad,
                            String zona,
                            String pais,
                            String estado,
                            LocalDate fechaInstalacion,
                            String observaciones) throws ErrorConexionCassandraException {
        Sensor sensor = new Sensor();
        sensor.setNombre(nombre);
        sensor.setCodigo(codigo);
        sensor.setTipo(tipo);
        sensor.setLatitud(latitud);
        sensor.setLongitud(longitud);
        sensor.setCiudad(ciudad);
        sensor.setZona(zona);
        sensor.setPais(pais);
        sensor.setEstado(estado != null && !estado.isBlank() ? estado : "ACTIVO");
        sensor.setFechaInicio(LocalDateTime.now());
        sensor.setFechaInstalacion(
                fechaInstalacion != null ? fechaInstalacion.atStartOfDay() : LocalDateTime.now()
        );
        sensor.setUltimaActualizacion(LocalDateTime.now());
        sensor.setObservaciones(observaciones);
        sensor.setActivo(!"INACTIVO".equalsIgnoreCase(sensor.getEstado()));
        SensorCassandraDAO.getInstance().crear(sensor);
    }

    public Sensor obtenerSensor(String id) throws ErrorConexionCassandraException {
        return SensorCassandraDAO.getInstance().obtenerPorId(id);
    }

    public List<Sensor> obtenerSensoresPorCiudad(String ciudad) throws ErrorConexionCassandraException {
        return SensorCassandraDAO.getInstance().obtenerPorCiudad(ciudad);
    }

    public List<Sensor> listarTodos() throws ErrorConexionCassandraException {
        return SensorCassandraDAO.getInstance().listarTodos();
    }

    public void actualizarEstado(String id, String nuevoEstado) throws ErrorConexionCassandraException {
        SensorCassandraDAO.getInstance().actualizarEstado(id, nuevoEstado);
    }

    // ========================================
    // MÉTODOS CON VALIDACIÓN DE ROLES (RBAC)
    // ========================================

    /**
     * Lista todos los sensores activos.
     * RBAC: Solo ADMINISTRADOR puede ver el listado completo de sensores activos.
     * 
     * @param token Token de sesión del usuario
     * @return Lista de sensores activos
     * @throws AuthorizationException si el usuario no es ADMINISTRADOR
     * @throws ErrorConexionCassandraException si hay error en la base de datos
     */
    public List<Sensor> listarActivosSoloAdmin(String token) 
            throws AuthorizationException, ErrorConexionCassandraException {
        // RBAC: Solo ADMIN puede ver sensores activos
        AuthorizationHelper.validarAdmin(token);
        
        // Retornar solo sensores activos
        return listarTodos().stream()
                .filter(s -> "ACTIVO".equalsIgnoreCase(s.getEstado()))
                .collect(Collectors.toList());
    }

    /**
     * Lista todos los sensores, pero enmascara el campo 'estado' si el usuario no es ADMIN.
     * RBAC: Usuarios no-admin ven sensores pero sin información del estado.
     * 
     * @param token Token de sesión del usuario
     * @return Lista de sensores (con estado enmascarado para no-admin)
     * @throws AuthorizationException si el token es inválido
     * @throws ErrorConexionCassandraException si hay error en la base de datos
     */
    public List<Sensor> listarTodosMascaraEstado(String token) 
            throws AuthorizationException, ErrorConexionCassandraException {
        String rol = AuthorizationHelper.obtenerRol(token);
        List<Sensor> sensores = listarTodos();
        
        // Si no es admin, enmascarar el estado
        if (!Roles.esAdmin(rol)) {
            sensores = sensores.stream()
                    .peek(s -> s.setEstado(null)) // Ocultar estado para no-admin
                    .collect(Collectors.toList());
        }
        
        return sensores;
    }

    /**
     * Crea un sensor con validación de rol.
     * RBAC: Solo ADMINISTRADOR puede crear sensores.
     * 
     * @param token Token de sesión
     * @throws AuthorizationException si el usuario no es ADMINISTRADOR
     */
    public void crearSensorConValidacion(String token,
                                         String nombre,
                                         String codigo,
                                         String tipo,
                                         Double latitud,
                                         Double longitud,
                                         String ciudad,
                                         String zona,
                                         String pais,
                                         String estado,
                                         LocalDate fechaInstalacion,
                                         String observaciones) 
            throws AuthorizationException, ErrorConexionCassandraException {
        // RBAC: Solo ADMIN puede crear sensores
        AuthorizationHelper.validarAdmin(token);
        
        crearSensor(nombre, codigo, tipo, latitud, longitud, ciudad, zona, pais, 
                    estado, fechaInstalacion, observaciones);
    }

    /**
     * Actualiza el estado de un sensor con validación de rol.
     * RBAC: Solo ADMINISTRADOR puede cambiar el estado de sensores.
     * 
     * @param token Token de sesión
     * @param id ID del sensor
     * @param nuevoEstado Nuevo estado del sensor
     * @throws AuthorizationException si el usuario no es ADMINISTRADOR
     * @throws ErrorConexionCassandraException si hay error en la base de datos
     */
    public void actualizarEstadoConValidacion(String token, String id, String nuevoEstado) 
            throws AuthorizationException, ErrorConexionCassandraException {
        // RBAC: Solo ADMIN puede actualizar estado de sensores
        AuthorizationHelper.validarAdmin(token);
        
        actualizarEstado(id, nuevoEstado);
    }
}
