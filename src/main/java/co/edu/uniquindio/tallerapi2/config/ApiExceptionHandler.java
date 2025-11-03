package co.edu.uniquindio.tallerapi2.config;

import co.edu.uniquindio.tallerapi2.dto.ApiError;
import co.edu.uniquindio.tallerapi2.exception.IntegracionKeycloakException;
import co.edu.uniquindio.tallerapi2.exception.UsuarioDuplicadoException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Mapea errores comunes a códigos esperados por los tests, evitando 500 genéricos.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("DataIntegrityViolation en {}: {}", req.getRequestURI(), ex.getMessage());
        ApiError err = new ApiError(
                409,
                "Conflict",
                "Ya existe un usuario con ese email",
                req.getRequestURI()
        );
        return ResponseEntity.status(409).body(err);
    }

    @ExceptionHandler(UsuarioDuplicadoException.class)
    public ResponseEntity<ApiError> handleUsuarioDuplicado(UsuarioDuplicadoException ex, HttpServletRequest req) {
        log.warn("Usuario duplicado en {}: {}", req.getRequestURI(), ex.getMessage());
        ApiError err = new ApiError(
                409,
                "Conflict",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(409).body(err);
    }

    @ExceptionHandler(IntegracionKeycloakException.class)
    public ResponseEntity<ApiError> handleIntegracionKeycloak(IntegracionKeycloakException ex, HttpServletRequest req) {
        log.error("Error de integración con Keycloak en {}: {}", req.getRequestURI(), ex.getMessage());

        // Si el error de Keycloak es 409, mapeamos a 409
        if (ex.getMessage() != null && ex.getMessage().contains("409")) {
            ApiError err = new ApiError(
                    409,
                    "Conflict",
                    "El usuario ya existe en el sistema de autenticación",
                    req.getRequestURI()
            );
            return ResponseEntity.status(409).body(err);
        }

        // Para otros errores de Keycloak, devolvemos 502 (Bad Gateway)
        ApiError err = new ApiError(
                502,
                "Bad Gateway",
                "Error comunicándose con el servicio de autenticación",
                req.getRequestURI()
        );
        return ResponseEntity.status(502).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Datos inválidos");

        log.warn("Validación fallida en {}: {}", req.getRequestURI(), mensaje);
        ApiError err = new ApiError(
                400,
                "Bad Request",
                mensaje,
                req.getRequestURI()
        );
        return ResponseEntity.status(400).body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        // NO capturar errores de actuator - dejar que Spring los maneje
        if (req.getRequestURI().startsWith("/actuator")) {
            log.debug("Ignorando excepción en actuator: {}", req.getRequestURI());
            throw new RuntimeException(ex);
        }

        log.error("Error no manejado en {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage(), ex);
        ApiError err = new ApiError(
                500,
                "Internal Server Error",
                "Error interno del servidor",
                req.getRequestURI()
        );
        return ResponseEntity.status(500).body(err);
    }
}