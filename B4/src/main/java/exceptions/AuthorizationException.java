package exceptions;

/**
 * Excepción lanzada cuando un usuario intenta realizar una acción
 * para la cual no tiene permisos basados en su rol.
 */
public class AuthorizationException extends Exception {
    private static final long serialVersionUID = 1L;

    public AuthorizationException(String mensaje) {
        super(mensaje);
    }

    public AuthorizationException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
