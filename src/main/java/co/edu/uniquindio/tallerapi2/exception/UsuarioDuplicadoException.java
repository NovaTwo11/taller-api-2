package co.edu.uniquindio.tallerapi2.exception;

public class UsuarioDuplicadoException extends RuntimeException {
    public UsuarioDuplicadoException(String message) {
        super(message);
    }

    public UsuarioDuplicadoException(String message, Throwable cause) {
        super(message, cause);
    }
}