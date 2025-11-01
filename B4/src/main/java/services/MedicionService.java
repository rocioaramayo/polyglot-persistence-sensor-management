package services;

import modelo.Medicion;
import repository.MedicionCassandraDAO;
import exceptions.ErrorConexionCassandraException;
import java.util.List;

public class MedicionService {
    private static MedicionService instance;

    private MedicionService() {}

    public static MedicionService getInstance() {
        if (instance == null) {
            instance = new MedicionService();
        }
        return instance;
    }

    public void registrarMedicion(String sensorId, Double temperatura, Double humedad) 
            throws ErrorConexionCassandraException {
        Medicion medicion = new Medicion(sensorId, temperatura, humedad);
        MedicionCassandraDAO.getInstance().crear(medicion);
    }

    public List<Medicion> obtenerMedicionesSensor(String sensorId) throws ErrorConexionCassandraException {
        return MedicionCassandraDAO.getInstance().obtenerPorSensor(sensorId);
    }

    public Double calcularPromedioTemperatura(String sensorId) throws ErrorConexionCassandraException {
        List<Medicion> mediciones = obtenerMedicionesSensor(sensorId);
        if (mediciones.isEmpty()) return 0.0;
        return mediciones.stream()
                .mapToDouble(Medicion::getTemperatura)
                .average()
                .orElse(0.0);
    }

    public Double calcularPromedioHumedad(String sensorId) throws ErrorConexionCassandraException {
        List<Medicion> mediciones = obtenerMedicionesSensor(sensorId);
        if (mediciones.isEmpty()) return 0.0;
        return mediciones.stream()
                .mapToDouble(Medicion::getHumedad)
                .average()
                .orElse(0.0);
    }
}
