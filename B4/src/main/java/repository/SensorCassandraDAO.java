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

            String cql = "INSERT INTO sensores (id, nombre, tipo_sensor, latitud, longitud, ciudad, pais, estado, fecha_inicio_emision) " +
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
            String cql = "SELECT * FROM sensores WHERE id = ?";
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
            String cql = "SELECT * FROM sensores WHERE ciudad = ? ALLOW FILTERING";
            ResultSet rs = session.execute(cql, ciudad);
            for (Row row : rs) {
                sensores.add(mapearSensor(row));
            }
        } catch (Exception e) {
            throw new ErrorConexionCassandraException("Error al obtener sensores", e);
        }
        return sensores;
    }

    public List<Sensor> listarTodos() throws ErrorConexionCassandraException {
        List<Sensor> sensores = new ArrayList<>();
        try {
            CqlSession session = CassandraPool.getInstance().getSession();
            String cql = "SELECT * FROM sensores";
            ResultSet rs = session.execute(cql);
            for (Row row : rs) {
                sensores.add(mapearSensor(row));
            }
        } catch (Exception e) {
            throw new ErrorConexionCassandraException("Error al listar sensores", e);
        }
        return sensores;
    }

    private Sensor mapearSensor(Row row) {
        Sensor sensor = new Sensor();
        sensor.setId(row.getString("id"));
        sensor.setNombre(row.getString("nombre"));
        // El esquema usa columna tipo_sensor
        sensor.setTipo(row.getString("tipo_sensor"));
        sensor.setLatitud(row.getDouble("latitud"));
        sensor.setLongitud(row.getDouble("longitud"));
        sensor.setCiudad(row.getString("ciudad"));
        sensor.setPais(row.getString("pais"));
        sensor.setEstado(row.getString("estado"));
        return sensor;
    }

    public void actualizarEstado(String id, String nuevoEstado) throws ErrorConexionCassandraException {
        try {
            CqlSession session = CassandraPool.getInstance().getSession();
            String cql = "UPDATE sensores SET estado = ? WHERE id = ?";
            session.execute(cql, nuevoEstado, id);
        } catch (Exception e) {
            throw new ErrorConexionCassandraException("Error al actualizar estado del sensor", e);
        }
    }
}
