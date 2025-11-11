package utils;

/**
 * Constantes de roles del sistema.
 * RBAC: Role-Based Access Control
 */
public class Roles {
    public static final String USUARIO = "USUARIO";
    public static final String TECNICO = "TECNICO";
    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String ADMIN = "ADMINISTRADOR"; // alias
    
    private Roles() {
        // Clase de constantes, no instanciable
    }
    
    /**
     * Verifica si el rol dado es ADMINISTRADOR.
     */
    public static boolean esAdmin(String rol) {
        return ADMINISTRADOR.equalsIgnoreCase(rol) || ADMIN.equalsIgnoreCase(rol);
    }
    
    /**
     * Verifica si el rol dado es TECNICO.
     */
    public static boolean esTecnico(String rol) {
        return TECNICO.equalsIgnoreCase(rol);
    }
    
    /**
     * Verifica si el rol dado es USUARIO.
     */
    public static boolean esUsuario(String rol) {
        return USUARIO.equalsIgnoreCase(rol);
    }
}
