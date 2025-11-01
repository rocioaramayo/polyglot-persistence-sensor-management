package repository;

import modelo.Factura;
import connections.MySQLPool;
import exceptions.ErrorConexionMySQLException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacturaMySQLRepository {
    private static FacturaMySQLRepository instance;

    private FacturaMySQLRepository() {}

    public static FacturaMySQLRepository getInstance() {
        if (instance == null) {
            instance = new FacturaMySQLRepository();
        }
        return instance;
    }

    public void crear(Factura factura) throws ErrorConexionMySQLException {
        String sql = "INSERT INTO facturas (usuario_id, fecha_emision, monto, estado, descripcion, nombre_facturacion, apellido_facturacion, direccion_facturacion, telefono_facturacion, numero_factura) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (factura.getUsuarioId() != null) {
                stmt.setInt(1, factura.getUsuarioId());
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
            }
            stmt.setTimestamp(2, Timestamp.valueOf(factura.getFechaEmision()));
            stmt.setDouble(3, factura.getMonto());
            stmt.setString(4, factura.getEstado());
            stmt.setString(5, factura.getDescripcion());
            stmt.setString(6, factura.getNombreFacturacion());
            stmt.setString(7, factura.getApellidoFacturacion());
            stmt.setString(8, factura.getDireccionFacturacion());
            stmt.setString(9, factura.getTelefonoFacturacion());
            // numero_factura simple: F-<epochMillis>
            stmt.setString(10, "F-" + System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al crear factura", e);
        }
    }

    public void guardar(Factura factura) throws ErrorConexionMySQLException {
        crear(factura);
    }

    public List<Factura> obtenerPorUsuario(Integer usuarioId) throws ErrorConexionMySQLException {
        List<Factura> facturas = new ArrayList<>();
        String sql = "SELECT * FROM facturas WHERE usuario_id = ?";
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                facturas.add(mapearFactura(rs));
            }
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al obtener facturas", e);
        }
        return facturas;
    }

    private Factura mapearFactura(ResultSet rs) throws SQLException {
        Factura factura = new Factura();
        factura.setId(rs.getInt("id"));
        Object uidObj = rs.getObject("usuario_id");
        if (uidObj != null) {
            factura.setUsuarioId(rs.getInt("usuario_id"));
        } else {
            factura.setUsuarioId(null);
        }
        factura.setFechaEmision(rs.getTimestamp("fecha_emision").toLocalDateTime());
        factura.setMonto(rs.getDouble("monto"));
        factura.setEstado(rs.getString("estado"));
        factura.setDescripcion(rs.getString("descripcion"));
        try { factura.setNombreFacturacion(rs.getString("nombre_facturacion")); } catch (Exception ignored) {}
        try { factura.setApellidoFacturacion(rs.getString("apellido_facturacion")); } catch (Exception ignored) {}
        try { factura.setDireccionFacturacion(rs.getString("direccion_facturacion")); } catch (Exception ignored) {}
        try { factura.setTelefonoFacturacion(rs.getString("telefono_facturacion")); } catch (Exception ignored) {}
        return factura;
    }
}
