package services;

import modelo.Sensor;
import repository.SensorCassandraDAO;
import exceptions.ErrorConexionCassandraException;
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

    public void crearSensor(String nombre, String tipo, Double latitud, Double longitud, 
                           String ciudad, String pais) throws ErrorConexionCassandraException {
        Sensor sensor = new Sensor(nombre, tipo, latitud, longitud, ciudad, pais);
        SensorCassandraDAO.getInstance().crear(sensor);
    }

    public Sensor obtenerSensor(String id) throws ErrorConexionCassandraException {
        return SensorCassandraDAO.getInstance().obtenerPorId(id);
    }

    public List<Sensor> obtenerSensoresPorCiudad(String ciudad) throws ErrorConexionCassandraException {
        return SensorCassandraDAO.getInstance().obtenerPorCiudad(ciudad);
    }
}
