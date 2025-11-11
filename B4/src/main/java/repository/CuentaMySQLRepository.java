package repository;

import modelo.CuentaCorriente;
import connections.MySQLPool;
import exceptions.ErrorConexionMySQLException;
import java.sql.*;

public class CuentaMySQLRepository {
    private static CuentaMySQLRepository instance;

    private CuentaMySQLRepository() {}

    public static CuentaMySQLRepository getInstance() {
        if (instance == null) {
            instance = new CuentaMySQLRepository();
        }
        return instance;
    }

    // Transaccional: crear usando conexión provista
    public void crear(Connection conn, CuentaCorriente cuenta) throws SQLException {
        String sql = "INSERT INTO cuentas_corrientes (usuario_id, saldo) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cuenta.getUsuarioId());
            stmt.setDouble(2, cuenta.getSaldo());
            stmt.executeUpdate();
        }
    }

    public void crear(CuentaCorriente cuenta) throws ErrorConexionMySQLException {
        try (Connection conn = MySQLPool.getInstance().getConnection()) {
            crear(conn, cuenta);
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al crear cuenta", e);
        }
    }

    // Transaccional: obtener usando conexión provista
    public CuentaCorriente obtenerPorUsuario(Connection conn, String usuarioId) throws SQLException {
        String sql = "SELECT * FROM cuentas_corrientes WHERE usuario_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    CuentaCorriente cuenta = new CuentaCorriente();
                    cuenta.setId(rs.getInt("id"));
                    cuenta.setUsuarioId(rs.getString("usuario_id"));
                    cuenta.setSaldo(rs.getDouble("saldo"));
                    cuenta.setLimite(1000.0);
                    return cuenta;
                }
            }
        }
        return null;
    }

    public CuentaCorriente obtenerPorUsuario(String usuarioId) throws ErrorConexionMySQLException {
        try (Connection conn = MySQLPool.getInstance().getConnection()) {
            return obtenerPorUsuario(conn, usuarioId);
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al obtener cuenta", e);
        }
    }

    // Transaccional: actualizar saldo
    public void actualizarSaldo(Connection conn, Integer cuentaId, Double nuevoSaldo) throws SQLException {
        String sql = "UPDATE cuentas_corrientes SET saldo = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, nuevoSaldo);
            stmt.setInt(2, cuentaId);
            stmt.executeUpdate();
        }
    }

    public void actualizarSaldo(Integer cuentaId, Double nuevoSaldo) throws ErrorConexionMySQLException {
        try (Connection conn = MySQLPool.getInstance().getConnection()) {
            actualizarSaldo(conn, cuentaId, nuevoSaldo);
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al actualizar saldo", e);
        }
    }
}
