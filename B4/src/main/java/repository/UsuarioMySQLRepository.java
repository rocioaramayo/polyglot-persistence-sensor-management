package repository;

import modelo.Usuario;
import connections.MySQLPool;
import exceptions.ErrorConexionMySQLException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioMySQLRepository {
    private static UsuarioMySQLRepository instance;

    private UsuarioMySQLRepository() {}

    public static UsuarioMySQLRepository getInstance() {
        if (instance == null) {
            instance = new UsuarioMySQLRepository();
        }
        return instance;
    }

    public void crear(Usuario usuario) throws ErrorConexionMySQLException {
        String sql = "INSERT INTO usuarios (nombre, email, password_hash, rol, estado, fecha_registro) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getPasswordHash());
            stmt.setString(4, usuario.getRol());
            stmt.setString(5, usuario.getActivo() ? "ACTIVO" : "INACTIVO");
            stmt.setTimestamp(6, Timestamp.valueOf(usuario.getFechaRegistro()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al crear usuario", e);
        }
    }

    public void guardar(Usuario usuario) throws ErrorConexionMySQLException {
        crear(usuario);
    }

    public Usuario obtenerPorEmail(String email) throws ErrorConexionMySQLException {
        String sql = "SELECT * FROM usuarios WHERE email = ?";
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapearUsuario(rs);
            }
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al obtener usuario", e);
        }
        return null;
    }

    public Usuario obtenerPorId(Integer id) throws ErrorConexionMySQLException {
        String sql = "SELECT * FROM usuarios WHERE id = ?";
        try (Connection conn = MySQLPool.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapearUsuario(rs);
            }
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al obtener usuario", e);
        }
        return null;
    }

    public List<Usuario> obtenerTodos() throws ErrorConexionMySQLException {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuarios";
        try (Connection conn = MySQLPool.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                usuarios.add(mapearUsuario(rs));
            }
        } catch (SQLException e) {
            throw new ErrorConexionMySQLException("Error al obtener usuarios", e);
        }
        return usuarios;
    }

    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(String.valueOf(rs.getInt("id")));
        usuario.setNombre(rs.getString("nombre"));
        usuario.setEmail(rs.getString("email"));
        usuario.setPasswordHash(rs.getString("password_hash"));
        usuario.setRol(rs.getString("rol"));
    usuario.setActivo("ACTIVO".equalsIgnoreCase(rs.getString("estado")));
        usuario.setFechaRegistro(rs.getTimestamp("fecha_registro").toLocalDateTime());
        return usuario;
    }
}
