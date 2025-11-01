package exceptions;

public class ErrorConexionRedisException extends Exception {
    private static final long serialVersionUID = 1L;

    public ErrorConexionRedisException(String mensaje) {
        super(mensaje);
    }

    public ErrorConexionRedisException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
