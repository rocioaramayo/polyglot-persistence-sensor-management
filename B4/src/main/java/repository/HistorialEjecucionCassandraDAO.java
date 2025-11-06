package repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import connections.CassandraPool;
import modelo.HistorialEjecucion;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HistorialEjecucionCassandraDAO {
    private final CqlSession session;
    private final PreparedStatement insertStmt;
    private final PreparedStatement selectBySolicitudStmt;

    public HistorialEjecucionCassandraDAO() throws exceptions.ErrorConexionCassandraException {
        this.session = CassandraPool.getInstance().getSession();
        
        this.insertStmt = session.prepare(
            "INSERT INTO historial_ejecucion (solicitud_id, fecha_ejecucion, id, proceso_id, usuario_id, " +
            "tecnico_id, estado, resultado, registros_procesados, tiempo_ejecucion_ms, fecha_inicio, fecha_fin) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        );
        
        this.selectBySolicitudStmt = session.prepare(
            "SELECT * FROM historial_ejecucion WHERE solicitud_id = ?"
        );
    }

    public void insertar(HistorialEjecucion historial) {
        session.execute(insertStmt.bind(
            UUID.fromString(historial.getSolicitudId()),
            Instant.from(historial.getFechaEjecucion().atZone(ZoneId.systemDefault())),
            UUID.randomUUID(),
            historial.getProcesoId(),
            historial.getUsuarioId(),
            historial.getTecnicoId(),
            historial.getEstado(),
            historial.getResultado(),
            historial.getRegistrosProcesados(),
            historial.getTiempoEjecucionMs(),
            Instant.from(historial.getFechaInicio().atZone(ZoneId.systemDefault())),
            historial.getFechaFin() != null ? 
                Instant.from(historial.getFechaFin().atZone(ZoneId.systemDefault())) : null
        ));
    }

    public List<HistorialEjecucion> listarPorSolicitud(String solicitudId) {
        List<HistorialEjecucion> historiales = new ArrayList<>();
        ResultSet rs = session.execute(selectBySolicitudStmt.bind(UUID.fromString(solicitudId)));
        
        for (Row row : rs) {
            HistorialEjecucion historial = new HistorialEjecucion();
            historial.setSolicitudId(row.getUuid("solicitud_id").toString());
            historial.setId(row.getUuid("id").toString());
            historial.setProcesoId(row.getString("proceso_id"));
            historial.setUsuarioId(row.getInt("usuario_id"));
            historial.setTecnicoId(row.getInt("tecnico_id"));
            historial.setEstado(row.getString("estado"));
            historial.setResultado(row.getString("resultado"));
            historial.setRegistrosProcesados(row.getLong("registros_procesados"));
            historial.setTiempoEjecucionMs(row.getLong("tiempo_ejecucion_ms"));
            
            Instant fechaEjecucion = row.getInstant("fecha_ejecucion");
            if (fechaEjecucion != null) {
                historial.setFechaEjecucion(LocalDateTime.ofInstant(fechaEjecucion, ZoneId.systemDefault()));
            }
            
            historiales.add(historial);
        }
        
        return historiales;
    }
}
