package repository;

import connections.MySQLPool;
import exceptions.ErrorConexionMySQLException;
import modelo.Pago;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PagoMySQLRepository {
    private static PagoMySQLRepository instance;

    private PagoMySQLRepository() {}

    public static PagoMySQLRepository getInstance() {
        if (instance == null) {
            instance = new PagoMySQLRepository();
        }
        return instance;
    }

    public void insertar(Pago pago) throws SQLException, ErrorConexionMySQLException {
        String sql = "INSERT INTO pagos (factura_id, usuario_id, monto, fecha_pago, metodo_pago, referencia, estado) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, pago.getFacturaId());
            stmt.setString(2, pago.getUsuarioId());
            stmt.setDouble(3, pago.getMonto());
            stmt.setTimestamp(4, Timestamp.valueOf(pago.getFechaPago()));
            stmt.setString(5, pago.getMetodoPago());
            stmt.setString(6, pago.getReferencia());
            stmt.setString(7, pago.getEstado());
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    pago.setId(rs.getInt(1));
                }
            }
        }
    }

    public List<Pago> listarPorFactura(Integer facturaId) throws SQLException, ErrorConexionMySQLException {
        String sql = "SELECT * FROM pagos WHERE factura_id = ? ORDER BY fecha_pago DESC";
        List<Pago> pagos = new ArrayList<>();
        
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, facturaId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Pago pago = new Pago();
                    pago.setId(rs.getInt("id"));
                    pago.setFacturaId(rs.getInt("factura_id"));
                    pago.setUsuarioId(rs.getString("usuario_id"));
                    pago.setMonto(rs.getDouble("monto"));
                    pago.setFechaPago(rs.getTimestamp("fecha_pago").toLocalDateTime());
                    pago.setMetodoPago(rs.getString("metodo_pago"));
                    pago.setReferencia(rs.getString("referencia"));
                    pago.setEstado(rs.getString("estado"));
                    pagos.add(pago);
                }
            }
        }
        
        return pagos;
    }

    public Double calcularTotalPagado(Integer facturaId) throws SQLException, ErrorConexionMySQLException {
        String sql = "SELECT COALESCE(SUM(monto), 0) as total FROM pagos WHERE factura_id = ? AND estado = 'COMPLETADO'";
        
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, facturaId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        }
        
        return 0.0;
    }
}
