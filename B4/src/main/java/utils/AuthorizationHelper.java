package utils;

import exceptions.AuthorizationException;
import exceptions.ErrorConexionRedisException;
import exceptions.ErrorConexionMySQLException;
import modelo.Usuario;
import org.json.JSONObject;
import repository.SesionRedisDAO;
import repository.UsuarioMongoDAO;

/**
 * Helper para validación de autorización basada en roles (RBAC).
 * Valida tokens de sesión y verifica permisos según el rol del usuario.
 */
public class AuthorizationHelper {

    private AuthorizationHelper() {
        // Clase utilitaria, no instanciable
    }

    /**
     * Valida el token de sesión y retorna la información de sesión.
     * 
     * @param token El token de sesión a validar
     * @return JSONObject con la información de sesión (usuarioId, rol, timestamp)
     * @throws AuthorizationException si el token es inválido o ha expirado
     */
    public static JSONObject validarToken(String token) throws AuthorizationException {
        if (token == null || token.isBlank()) {
            throw new AuthorizationException("Token de sesión requerido");
        }

        try {
            JSONObject sesion = SesionRedisDAO.getInstance().obtenerSesion(token);
            if (sesion == null) {
                throw new AuthorizationException("Sesión inválida o expirada");
            }
            return sesion;
        } catch (ErrorConexionRedisException e) {
            throw new AuthorizationException("Error al validar sesión: " + e.getMessage(), e);
        }
    }

    /**
     * Valida que el usuario tenga uno de los roles permitidos.
     * 
     * @param token El token de sesión
     * @param rolesPermitidos Los roles permitidos para esta operación
     * @throws AuthorizationException si el usuario no tiene ninguno de los roles permitidos
     */
    public static void validarRol(String token, String... rolesPermitidos) throws AuthorizationException {
        JSONObject sesion = validarToken(token);
        String rolUsuario = sesion.optString("rol", "");

        for (String rolPermitido : rolesPermitidos) {
            if (rolUsuario.equalsIgnoreCase(rolPermitido)) {
                return; // Rol válido
            }
        }

        // No tiene ninguno de los roles permitidos
        throw new AuthorizationException(
            String.format("Acceso denegado. Rol requerido: %s. Rol actual: %s",
                String.join(" o ", rolesPermitidos), rolUsuario)
        );
    }

    /**
     * Valida que el usuario NO tenga ninguno de los roles bloqueados.
     * 
     * @param token El token de sesión
     * @param rolesBloqueados Los roles que NO deben tener acceso
     * @throws AuthorizationException si el usuario tiene alguno de los roles bloqueados
     */
    public static void prohibirRoles(String token, String... rolesBloqueados) throws AuthorizationException {
        JSONObject sesion = validarToken(token);
        String rolUsuario = sesion.optString("rol", "");

        for (String rolBloqueado : rolesBloqueados) {
            if (rolUsuario.equalsIgnoreCase(rolBloqueado)) {
                throw new AuthorizationException(
                    String.format("Acceso denegado. El rol %s no puede realizar esta operación", rolUsuario)
                );
            }
        }
    }

    /**
     * Obtiene el usuarioId del token de sesión.
     * 
     * @param token El token de sesión
     * @return El ID del usuario
     * @throws AuthorizationException si el token es inválido
     */
    public static String obtenerUsuarioId(String token) throws AuthorizationException {
        JSONObject sesion = validarToken(token);
        String usuarioId = sesion.optString("usuarioId", null);
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new AuthorizationException("ID de usuario no encontrado en sesión");
        }
        return usuarioId;
    }

    /**
     * Obtiene el rol del usuario del token de sesión.
     * 
     * @param token El token de sesión
     * @return El rol del usuario
     * @throws AuthorizationException si el token es inválido
     */
    public static String obtenerRol(String token) throws AuthorizationException {
        JSONObject sesion = validarToken(token);
        String rol = sesion.optString("rol", null);
        if (rol == null || rol.isBlank()) {
            throw new AuthorizationException("Rol no encontrado en sesión");
        }
        return rol;
    }

    /**
     * Valida que el usuario sea ADMINISTRADOR.
     * 
     * @param token El token de sesión
     * @throws AuthorizationException si el usuario no es administrador
     */
    public static void validarAdmin(String token) throws AuthorizationException {
        validarRol(token, Roles.ADMINISTRADOR);
    }

    /**
     * Valida que el usuario sea USUARIO (cliente).
     * 
     * @param token El token de sesión
     * @throws AuthorizationException si el usuario no es un cliente
     */
    public static void validarUsuario(String token) throws AuthorizationException {
        validarRol(token, Roles.USUARIO);
    }

    /**
     * Valida que el usuario sea TECNICO.
     * 
     * @param token El token de sesión
     * @throws AuthorizationException si el usuario no es técnico
     */
    public static void validarTecnico(String token) throws AuthorizationException {
        validarRol(token, Roles.TECNICO);
    }

    /**
     * Verifica si el usuario tiene un rol específico.
     * 
     * @param token El token de sesión
     * @param rol El rol a verificar
     * @return true si el usuario tiene el rol, false en caso contrario
     */
    public static boolean tieneRol(String token, String rol) {
        try {
            JSONObject sesion = validarToken(token);
            String rolUsuario = sesion.optString("rol", "");
            return rolUsuario.equalsIgnoreCase(rol);
        } catch (AuthorizationException e) {
            return false;
        }
    }

    /**
     * Obtiene el objeto Usuario completo del token.
     * 
     * @param token El token de sesión
     * @return El objeto Usuario
     * @throws AuthorizationException si el token es inválido
     * @throws ErrorConexionMySQLException si hay error al obtener el usuario
     */
    public static Usuario obtenerUsuario(String token) 
            throws AuthorizationException, ErrorConexionMySQLException {
        String usuarioId = obtenerUsuarioId(token);
        try {
            UsuarioMongoDAO um = new UsuarioMongoDAO();
            Usuario usuario = um.buscarPorId(usuarioId);
            if (usuario == null) {
                throw new AuthorizationException("Usuario no encontrado: " + usuarioId);
            }
            return usuario;
        } catch (exceptions.ErrorConexionMongoException e) {
            throw new ErrorConexionMySQLException("Error al obtener usuario: " + e.getMessage(), e);
        }
    }
}
