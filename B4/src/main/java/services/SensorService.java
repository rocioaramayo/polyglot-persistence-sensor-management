package services;

import modelo.Sensor;
import repository.SensorCassandraDAO;
import exceptions.ErrorConexionCassandraException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
}
