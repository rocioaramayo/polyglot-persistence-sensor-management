package exceptions;

public class ErrorConexionMySQLException extends Exception {
    private static final long serialVersionUID = 1L;

    public ErrorConexionMySQLException(String mensaje) {
        super(mensaje);
    }

    public ErrorConexionMySQLException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
