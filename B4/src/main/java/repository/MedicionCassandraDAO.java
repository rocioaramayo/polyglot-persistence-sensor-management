package repository;

import modelo.Medicion;
import connections.CassandraPool;
import exceptions.ErrorConexionCassandraException;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
            UUID id = UUID.randomUUID();
            medicion.setId(id.toString());

            String cql = "INSERT INTO mediciones (sensor_id, fecha, id, temperatura, humedad) " +
                    "VALUES (?, toTimestamp(now()), ?, ?, ?)";

            session.execute(cql, UUID.fromString(medicion.getSensorId()), id,
                    medicion.getTemperatura(), medicion.getHumedad());
        } catch (Exception e) {
            throw new ErrorConexionCassandraException("Error al crear medicion", e);
        }
    }

    public void guardar(Medicion medicion) throws ErrorConexionCassandraException {
        crear(medicion);
    }

    public List<Medicion> obtenerPorSensor(String sensorId) throws ErrorConexionCassandraException {
        List<Medicion> mediciones = new ArrayList<>();
        try {
            CqlSession session = CassandraPool.getInstance().getSession();
            String cql = "SELECT * FROM mediciones WHERE sensor_id = ? LIMIT 100";
            ResultSet rs = session.execute(cql, UUID.fromString(sensorId));
            for (Row row : rs) {
                mediciones.add(mapearMedicion(row));
            }
        } catch (Exception e) {
            throw new ErrorConexionCassandraException("Error al obtener mediciones", e);
        }
        return mediciones;
    }

    public List<Medicion> obtenerPorSensor(String sensorId, Instant fechaInicio, Instant fechaFin, int limite)
            throws ErrorConexionCassandraException {
        List<Medicion> mediciones = new ArrayList<>();
        if (limite <= 0) {
            limite = 500;
        }
        try {
            CqlSession session = CassandraPool.getInstance().getSession();
            StringBuilder cql = new StringBuilder("SELECT * FROM mediciones WHERE sensor_id = ?");
            List<Object> params = new ArrayList<>();
            params.add(UUID.fromString(sensorId));
            if (fechaInicio != null) {
                cql.append(" AND fecha >= ?");
                params.add(fechaInicio);
            }
            if (fechaFin != null) {
                cql.append(" AND fecha <= ?");
                params.add(fechaFin);
            }
            cql.append(" LIMIT ").append(limite);
            ResultSet rs = session.execute(cql.toString(), params.toArray());
            for (Row row : rs) {
                mediciones.add(mapearMedicion(row));
            }
        } catch (Exception e) {
            throw new ErrorConexionCassandraException("Error al obtener mediciones por rango", e);
        }
        return mediciones;
    }

    public List<Medicion> obtenerUltimas(int cantidad) throws ErrorConexionCassandraException {
        List<Medicion> mediciones = new ArrayList<>();
        try {
            CqlSession session = CassandraPool.getInstance().getSession();
            String cql = "SELECT * FROM mediciones LIMIT 100";
            ResultSet rs = session.execute(cql);
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
        medicion.setId(row.getUuid("id").toString());
        medicion.setSensorId(row.getUuid("sensor_id").toString());
        medicion.setTemperatura(row.getDouble("temperatura"));
        medicion.setHumedad(row.getDouble("humedad"));
        Instant fecha = row.getInstant("fecha");
        if (fecha != null) {
            medicion.setFechaHora(LocalDateTime.ofInstant(fecha, ZoneId.systemDefault()));
        }
        return medicion;
    }
}
