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

    public void crear(CuentaCorriente cuenta) throws ErrorConexionMySQLException {
        String sql = "INSERT INTO cuentas_corrientes (usuario_id, saldo) VALUES (?, ?)";
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cuenta.getUsuarioId());
            stmt.setDouble(2, cuenta.getSaldo());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al crear cuenta", e);
        }
    }

    public CuentaCorriente obtenerPorUsuario(Integer usuarioId) throws ErrorConexionMySQLException {
        String sql = "SELECT * FROM cuentas_corrientes WHERE usuario_id = ?";
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                CuentaCorriente cuenta = new CuentaCorriente();
                cuenta.setId(rs.getInt("id"));
                cuenta.setUsuarioId(rs.getInt("usuario_id"));
                cuenta.setSaldo(rs.getDouble("saldo"));
                return cuenta;
            }
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al obtener cuenta", e);
        }
        return null;
    }

    public void actualizarSaldo(Integer cuentaId, Double nuevoSaldo) throws ErrorConexionMySQLException {
        String sql = "UPDATE cuentas_corrientes SET saldo = ? WHERE id = ?";
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, nuevoSaldo);
            stmt.setInt(2, cuentaId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al actualizar saldo", e);
        }
    }
}
