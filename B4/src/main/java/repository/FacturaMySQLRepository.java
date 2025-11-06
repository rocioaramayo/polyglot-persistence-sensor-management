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
        String sql = "INSERT INTO facturas (usuario_id, solicitud_id, fecha_emision, monto, estado, descripcion, nombre_facturacion, apellido_facturacion, direccion_facturacion, telefono_facturacion, numero_factura) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (factura.getUsuarioId() != null) {
                stmt.setInt(1, factura.getUsuarioId());
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
            }
            stmt.setString(2, factura.getSolicitudId());
            stmt.setTimestamp(3, Timestamp.valueOf(factura.getFechaEmision()));
            stmt.setDouble(4, factura.getMonto());
            stmt.setString(5, factura.getEstado());
            stmt.setString(6, factura.getDescripcion());
            stmt.setString(7, factura.getNombreFacturacion());
            stmt.setString(8, factura.getApellidoFacturacion());
            stmt.setString(9, factura.getDireccionFacturacion());
            stmt.setString(10, factura.getTelefonoFacturacion());
            // numero_factura simple: F-<epochMillis>
            stmt.setString(11, "F-" + System.currentTimeMillis());
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

    public Factura obtenerPorId(Integer facturaId) throws ErrorConexionMySQLException {
        String sql = "SELECT * FROM facturas WHERE id = ?";
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, facturaId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapearFactura(rs);
            }
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al obtener factura", e);
        }
        return null;
    }

    public void actualizarEstado(Integer facturaId, String nuevoEstado) throws ErrorConexionMySQLException {
        String sql = "UPDATE facturas SET estado = ? WHERE id = ?";
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, facturaId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al actualizar estado de la factura", e);
        }
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
