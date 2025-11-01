package services;

import modelo.Usuario;
import repository.UsuarioMongoDAO;
import repository.SesionRedisDAO;
import exceptions.ErrorConexionMySQLException;
import exceptions.ErrorConexionRedisException;
import exceptions.ErrorConexionMongoException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class AuthService {
    private static AuthService instance;

    private AuthService() {}

    public static AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    public String login(String email, String password) throws ErrorConexionMongoException, ErrorConexionRedisException {
        Usuario usuario;
        try {
            UsuarioMongoDAO um = new UsuarioMongoDAO();
            usuario = um.buscarPorEmail(email);
        } catch (ErrorConexionMongoException e) {
            throw e;
        }
        if (usuario == null) {
            return null;
        }

        String passwordHash = hashPassword(password);
        if (!usuario.getPasswordHash().equals(passwordHash)) {
            return null;
        }

        String token = UUID.randomUUID().toString();
        // Guardamos el id de usuario como String (compatible con MongoDB ObjectId o id numérico)
            SesionRedisDAO.getInstance().guardarSesion(token, usuario.getId(), usuario.getRol());
        return token;
    }

    public void registrar(String nombre, String email, String password, String rol) 
                throws exceptions.ErrorConexionMongoException {
            Usuario usuario = new Usuario(nombre, "", email, hashPassword(password), rol);
            try {
                UsuarioMongoDAO um = new UsuarioMongoDAO();
                String id = um.insertar(usuario);
                usuario.setId(id);
            } catch (exceptions.ErrorConexionMongoException e) {
                throw e;
            }
    }

    public Usuario validarToken(String token) throws ErrorConexionRedisException, ErrorConexionMySQLException {
        var sesion = SesionRedisDAO.getInstance().obtenerSesion(token);
        if (sesion == null) {
            return null;
        }
            String usuarioIdStr = sesion.optString("usuarioId", null);
            if (usuarioIdStr == null) return null;
            try {
                UsuarioMongoDAO um = new UsuarioMongoDAO();
                return um.buscarPorId(usuarioIdStr);
            } catch (exceptions.ErrorConexionMongoException e) {
                throw new ErrorConexionMySQLException("Error al validar token (mongo)", e);
        }
    }

    public void logout(String token) throws ErrorConexionRedisException {
        SesionRedisDAO.getInstance().eliminarSesion(token);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
