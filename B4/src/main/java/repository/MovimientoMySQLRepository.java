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
        String sql = "INSERT INTO movimientos (cuenta_id, tipo, monto, saldo_anterior, saldo_nuevo, fecha, descripcion, referencia_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, movimiento.getCuentaId());
            stmt.setString(2, movimiento.getTipo());
            stmt.setDouble(3, movimiento.getMonto());
            stmt.setDouble(4, movimiento.getSaldoAnterior());
            stmt.setDouble(5, movimiento.getSaldoNuevo());
            Timestamp fecha = movimiento.getFecha() != null
                    ? Timestamp.valueOf(movimiento.getFecha())
                    : new Timestamp(System.currentTimeMillis());
            stmt.setTimestamp(6, fecha);
            stmt.setString(7, movimiento.getDescripcion());
            stmt.setString(8, movimiento.getReferenciaId());
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
        movimiento.setSaldoAnterior(rs.getDouble("saldo_anterior"));
        movimiento.setSaldoNuevo(rs.getDouble("saldo_nuevo"));
        Timestamp fecha = rs.getTimestamp("fecha");
        if (fecha != null) {
            movimiento.setFecha(fecha.toLocalDateTime());
        }
        movimiento.setDescripcion(rs.getString("descripcion"));
        movimiento.setReferenciaId(rs.getString("referencia_id"));
        return movimiento;
    }
}
