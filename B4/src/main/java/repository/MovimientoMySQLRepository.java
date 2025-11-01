package repository;

import modelo.Movimiento;
import connections.MySQLPool;
import exceptions.ErrorConexionMySQLException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MovimientoMySQLRepository {
    private static MovimientoMySQLRepository instance;

    private MovimientoMySQLRepository() {}

    public static MovimientoMySQLRepository getInstance() {
        if (instance == null) {
            instance = new MovimientoMySQLRepository();
        }
        return instance;
    }

    public void crear(Movimiento movimiento) throws ErrorConexionMySQLException {
        String sql = "INSERT INTO movimientos (cuenta_id, tipo, monto, fecha, descripcion) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, movimiento.getCuentaId());
            stmt.setString(2, movimiento.getTipo());
            stmt.setDouble(3, movimiento.getMonto());
            stmt.setTimestamp(4, Timestamp.valueOf(movimiento.getFecha()));
            stmt.setString(5, movimiento.getDescripcion());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al crear movimiento", e);
        }
    }

    public List<Movimiento> obtenerPorCuenta(Integer cuentaId) throws ErrorConexionMySQLException {
        List<Movimiento> movimientos = new ArrayList<>();
        String sql = "SELECT * FROM movimientos WHERE cuenta_id = ? ORDER BY fecha DESC";
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cuentaId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                movimientos.add(mapearMovimiento(rs));
            }
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al obtener movimientos", e);
        }
        return movimientos;
    }

    private Movimiento mapearMovimiento(ResultSet rs) throws SQLException {
        Movimiento movimiento = new Movimiento();
        movimiento.setId(rs.getInt("id"));
        movimiento.setCuentaId(rs.getInt("cuenta_id"));
        movimiento.setTipo(rs.getString("tipo"));
        movimiento.setMonto(rs.getDouble("monto"));
        movimiento.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        movimiento.setDescripcion(rs.getString("descripcion"));
        return movimiento;
    }
}
