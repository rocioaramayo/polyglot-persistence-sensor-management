package repository;

import modelo.Sensor;
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
            UUID id = UUID.randomUUID();
            sensor.setId(id.toString());

            LocalDateTime fechaInicio = sensor.getFechaInicio() != null ? sensor.getFechaInicio() : LocalDateTime.now();
            LocalDateTime fechaInstalacion = sensor.getFechaInstalacion() != null ? sensor.getFechaInstalacion() : fechaInicio;
            LocalDateTime ultimaActualizacion = sensor.getUltimaActualizacion() != null ? sensor.getUltimaActualizacion() : LocalDateTime.now();

            Instant inicio = fechaInicio.atZone(ZoneId.systemDefault()).toInstant();
            Instant instalacion = fechaInstalacion.atZone(ZoneId.systemDefault()).toInstant();
            Instant ultima = ultimaActualizacion.atZone(ZoneId.systemDefault()).toInstant();

            String cql = "INSERT INTO sensores (id, nombre, codigo, tipo_sensor, latitud, longitud, ciudad, zona, pais, estado, fecha_inicio_emision, fecha_instalacion, ultima_actualizacion, observaciones) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            session.execute(cql,
                    id,
                    sensor.getNombre(),
                    sensor.getCodigo(),
                    sensor.getTipo(),
                    sensor.getLatitud(),
                    sensor.getLongitud(),
                    sensor.getCiudad(),
                    sensor.getZona(),
                    sensor.getPais(),
                    sensor.getEstado(),
                    inicio,
                    instalacion,
                    ultima,
                    sensor.getObservaciones());
        } catch (Exception e) {
            throw new ErrorConexionCassandraException("Error al crear sensor", e);
        }
    }

    public Sensor obtenerPorId(String id) throws ErrorConexionCassandraException {
        try {
            CqlSession session = CassandraPool.getInstance().getSession();
            String cql = "SELECT * FROM sensores WHERE id = ?";
            ResultSet rs = session.execute(cql, UUID.fromString(id));
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
        if (ciudad == null || ciudad.isBlank()) {
            return listarTodos();
        }
        List<Sensor> filtrados = new ArrayList<>();
        for (Sensor sensor : listarTodos()) {
            if (sensor.getCiudad() != null && sensor.getCiudad().equalsIgnoreCase(ciudad)) {
                filtrados.add(sensor);
            }
        }
        return filtrados;
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
        sensor.setId(row.getUuid("id").toString());
        sensor.setNombre(row.getString("nombre"));
        sensor.setCodigo(row.getString("codigo"));
        // El esquema usa columna tipo_sensor
        sensor.setTipo(row.getString("tipo_sensor"));
        sensor.setLatitud(row.getDouble("latitud"));
        sensor.setLongitud(row.getDouble("longitud"));
        sensor.setCiudad(row.getString("ciudad"));
        sensor.setZona(row.getString("zona"));
        sensor.setPais(row.getString("pais"));
        sensor.setEstado(row.getString("estado"));
        Instant inicio = row.getInstant("fecha_inicio_emision");
        if (inicio != null) {
            sensor.setFechaInicio(LocalDateTime.ofInstant(inicio, ZoneId.systemDefault()));
        }
        Instant instalacion = row.getInstant("fecha_instalacion");
        if (instalacion != null) {
            sensor.setFechaInstalacion(LocalDateTime.ofInstant(instalacion, ZoneId.systemDefault()));
        }
        Instant ultima = row.getInstant("ultima_actualizacion");
        if (ultima != null) {
            sensor.setUltimaActualizacion(LocalDateTime.ofInstant(ultima, ZoneId.systemDefault()));
        }
        sensor.setObservaciones(row.getString("observaciones"));
        return sensor;
    }

    public void actualizarEstado(String id, String nuevoEstado) throws ErrorConexionCassandraException {
        try {
            CqlSession session = CassandraPool.getInstance().getSession();
            String cql = "UPDATE sensores SET estado = ?, ultima_actualizacion = toTimestamp(now()) WHERE id = ?";
            session.execute(cql, nuevoEstado, UUID.fromString(id));
        } catch (Exception e) {
            throw new ErrorConexionCassandraException("Error al actualizar estado del sensor", e);
        }
    }
}
