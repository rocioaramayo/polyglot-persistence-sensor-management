package repository;

import modelo.Medicion;
import connections.CassandraPool;
import exceptions.ErrorConexionCassandraException;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MedicionCassandraDAO {
    private static MedicionCassandraDAO instance;

    private MedicionCassandraDAO() {}

    public static MedicionCassandraDAO getInstance() {
        if (instance == null) {
            instance = new MedicionCassandraDAO();
        }
        return instance;
    }

    public void crear(Medicion medicion) throws ErrorConexionCassandraException {
        try {
            CqlSession session = CassandraPool.getInstance().getSession();
            String id = UUID.randomUUID().toString();
            medicion.setId(id);

            String cql = "INSERT INTO mediciones_by_sensor (sensor_id, fecha_hora, id, temperatura, humedad) " +
                    "VALUES (?, toTimestamp(now()), ?, ?, ?)";

            session.execute(cql, medicion.getSensorId(), id, 
                    medicion.getTemperatura(), medicion.getHumedad());
        } catch (Exception e) {
            throw new ErrorConexionCassandraException("Error al crear medición", e);
        }
    }

    public void guardar(Medicion medicion) throws ErrorConexionCassandraException {
        crear(medicion);
    }

    public List<Medicion> obtenerPorSensor(String sensorId) throws ErrorConexionCassandraException {
        List<Medicion> mediciones = new ArrayList<>();
        try {
            CqlSession session = CassandraPool.getInstance().getSession();
            String cql = "SELECT * FROM mediciones_by_sensor WHERE sensor_id = ? LIMIT 100";
            ResultSet rs = session.execute(cql, sensorId);
            for (Row row : rs) {
                mediciones.add(mapearMedicion(row));
            }
        } catch (Exception e) {
            throw new ErrorConexionCassandraException("Error al obtener mediciones", e);
        }
        return mediciones;
    }

    public List<Medicion> obtenerUltimas(int cantidad) throws ErrorConexionCassandraException {
        List<Medicion> mediciones = new ArrayList<>();
        try {
            CqlSession session = CassandraPool.getInstance().getSession();
            String cql = "SELECT * FROM mediciones_by_sensor LIMIT ?";
            ResultSet rs = session.execute(cql, cantidad);
            for (Row row : rs) {
                mediciones.add(mapearMedicion(row));
            }
        } catch (Exception e) {
            throw new ErrorConexionCassandraException("Error al obtener mediciones", e);
        }
        return mediciones;
    }

    private Medicion mapearMedicion(Row row) {
        Medicion medicion = new Medicion();
        medicion.setId(row.getString("id"));
        medicion.setSensorId(row.getString("sensor_id"));
        medicion.setTemperatura(row.getDouble("temperatura"));
        medicion.setHumedad(row.getDouble("humedad"));
        return medicion;
    }
}
