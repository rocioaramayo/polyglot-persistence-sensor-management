package exceptions;

public class ErrorConexionCassandraException extends Exception {
    private static final long serialVersionUID = 1L;

    public ErrorConexionCassandraException(String mensaje) {
        super(mensaje);
    }

    public ErrorConexionCassandraException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
