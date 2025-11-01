package exceptions;

public class ErrorConexionMongoException extends Exception {
    private static final long serialVersionUID = 1L;

    public ErrorConexionMongoException(String mensaje) {
        super(mensaje);
    }

    public ErrorConexionMongoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
