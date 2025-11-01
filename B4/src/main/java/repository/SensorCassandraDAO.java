package repository;

import modelo.Sensor;
import connections.CassandraPool;
import exceptions.ErrorConexionCassandraException;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SensorCassandraDAO {
    private static SensorCassandraDAO instance;

    private SensorCassandraDAO() {}

    public static SensorCassandraDAO getInstance() {
        if (instance == null) {
            instance = new SensorCassandraDAO();
        }
        return instance;
    }

    public void crear(Sensor sensor) throws ErrorConexionCassandraException {
        try {
            CqlSession session = CassandraPool.getInstance().getSession();
            String id = UUID.randomUUID().toString();
            sensor.setId(id);

            String cql = "INSERT INTO sensores_by_id (id, nombre, tipo, latitud, longitud, ciudad, pais, estado, fecha_inicio) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, toTimestamp(now()))";

            session.execute(cql, id, sensor.getNombre(), sensor.getTipo(), 
                    sensor.getLatitud(), sensor.getLongitud(), sensor.getCiudad(), 
                    sensor.getPais(), sensor.getEstado());
        } catch (Exception e) {
            throw new ErrorConexionCassandraException("Error al crear sensor", e);
        }
    }

    public Sensor obtenerPorId(String id) throws ErrorConexionCassandraException {
        try {
            CqlSession session = CassandraPool.getInstance().getSession();
            String cql = "SELECT * FROM sensores_by_id WHERE id = ?";
            ResultSet rs = session.execute(cql, id);
            Row row = rs.one();
            if (row != null) {
                return mapearSensor(row);
            }
        } catch (Exception e) {
            throw new ErrorConexionCassandraException("Error al obtener sensor", e);
        }
        return null;
    }

    public List<Sensor> obtenerPorCiudad(String ciudad) throws ErrorConexionCassandraException {
        List<Sensor> sensores = new ArrayList<>();
        try {
            CqlSession session = CassandraPool.getInstance().getSession();
            String cql = "SELECT * FROM sensores_by_id WHERE ciudad = ? ALLOW FILTERING";
            ResultSet rs = session.execute(cql, ciudad);
            for (Row row : rs) {
                sensores.add(mapearSensor(row));
            }
        } catch (Exception e) {
            throw new ErrorConexionCassandraException("Error al obtener sensores", e);
        }
        return sensores;
    }

    private Sensor mapearSensor(Row row) {
        Sensor sensor = new Sensor();
        sensor.setId(row.getString("id"));
        sensor.setNombre(row.getString("nombre"));
        sensor.setTipo(row.getString("tipo"));
        sensor.setLatitud(row.getDouble("latitud"));
        sensor.setLongitud(row.getDouble("longitud"));
        sensor.setCiudad(row.getString("ciudad"));
        sensor.setPais(row.getString("pais"));
        sensor.setEstado(row.getString("estado"));
        return sensor;
    }
}
