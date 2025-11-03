package co.edu.uniquindio.tallerapi2.exception;

public class IntegracionKeycloakException extends RuntimeException {
    public IntegracionKeycloakException(String message) {
        super(message);
    }

    public IntegracionKeycloakException(String message, Throwable cause) {
        super(message, cause);
    }
}