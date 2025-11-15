package co.edu.uniquindio.tallerapi2.controller;

// --- Imports Modificados ---
import co.edu.uniquindio.tallerapi2.config.AppRabbitMQProperties; // <--- AÑADIDO
import co.edu.uniquindio.tallerapi2.dto.ApiError;
import co.edu.uniquindio.tallerapi2.dto.SesionRequest;
import co.edu.uniquindio.tallerapi2.dto.SesionResponse;
import co.edu.uniquindio.tallerapi2.model.Usuario;
import co.edu.uniquindio.tallerapi2.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/sesiones")
@Tag(name = "Sesiones", description = "Gestión de sesiones de usuario")
public class SesionController {

    private final UsuarioRepository usuarioRepository;
    private final RabbitTemplate rabbitTemplate;
    private final AppRabbitMQProperties rabbitMQProperties; // <--- AÑADIDO

    // --- Constructor Modificado ---
    public SesionController(UsuarioRepository usuarioRepository,
                            RabbitTemplate rabbitTemplate,
                            AppRabbitMQProperties rabbitMQProperties) { // <--- MODIFICADO
        this.usuarioRepository = usuarioRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMQProperties = rabbitMQProperties; // <--- AÑADIDO
    }

    @Operation(summary = "Iniciar sesión",
            description = "Autentica un usuario con email y contraseña")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión iniciada exitosamente"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<?> iniciarSesion(
            @Valid @RequestBody SesionRequest request,
            HttpServletRequest httpRequest) {

        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail());

            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(404, "Not Found", "Usuario no encontrado", httpRequest.getRequestURI()));
            }

            Usuario usuario = usuarioOpt.get();

            // NOTA: Esta validación de contraseña en texto plano es insegura para producción.
            if (!usuario.getPassword().equals(request.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiError(401, "Unauthorized", "Credenciales inválidas", httpRequest.getRequestURI()));
            }

            // 🔥 PUBLICAR EVENTO DE SESIÓN INICIADA
            publishSessionStartedEvent(usuario, httpRequest); // <--- Modificado para pasar request

            SesionResponse response = new SesionResponse(
                    "Sesión iniciada exitosamente",
                    usuario.getId(),
                    usuario.getEmail(),
                    usuario.getNombre()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(500, "Internal Server Error", "Error interno del servidor", httpRequest.getRequestURI()));
        }
    }

    @Operation(summary = "Solicitar recuperación de contraseña",
            description = "Genera un token de recuperación y envía notificación por email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitud de recuperación procesada"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request,
                                            HttpServletRequest httpRequest) {
        try {
            String email = request.get("email");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(400, "Bad Request", "El email es obligatorio", httpRequest.getRequestURI()));
            }

            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(404, "Not Found", "Usuario no encontrado", httpRequest.getRequestURI()));
            }

            Usuario usuario = usuarioOpt.get();
            String resetToken = UUID.randomUUID().toString();

            // 🔥 PUBLICAR EVENTO DE PASSWORD RESET SOLICITADO
            publishPasswordResetEvent(usuario, resetToken);

            return ResponseEntity.ok(Map.of(
                    "message", "Email de recuperación solicitado",
                    "email", email
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(500, "Internal Server Error", "Error interno del servidor", httpRequest.getRequestURI()));
        }
    }

    @Operation(summary = "Confirmar cambio de contraseña",
            description = "Notifica que la contraseña fue actualizada exitosamente")
    @PostMapping("/password-updated")
    public ResponseEntity<?> passwordUpdated(@RequestBody Map<String, String> request,
                                             HttpServletRequest httpRequest) {
        try {
            String email = request.get("email");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(400, "Bad Request", "El email es obligatorio", httpRequest.getRequestURI()));
            }

            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(404, "Not Found", "Usuario no encontrado", httpRequest.getRequestURI()));
            }

            Usuario usuario = usuarioOpt.get();

            // 🔥 PUBLICAR EVENTO DE PASSWORD ACTUALIZADO
            publishPasswordUpdatedEvent(usuario);

            return ResponseEntity.ok(Map.of(
                    "message", "Notificación de cambio de contraseña enviada",
                    "email", email
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(500, "Internal Server Error", "Error interno del servidor", httpRequest.getRequestURI()));
        }
    }

    // ========== MÉTODOS PRIVADOS PARA PUBLICAR EVENTOS (CORREGIDOS) ==========

    private void publishSessionStartedEvent(Usuario usuario, HttpServletRequest httpRequest) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("usuarioId", usuario.getId().toString()); // El orquestador espera un string (UUID) o int, pero el Map lo manda como String.
            event.put("email", usuario.getEmail());
            event.put("nombre", usuario.getNombre());
            event.put("timestamp", LocalDateTime.now().toString());

            // --- AÑADIDO: Datos adicionales para el correo de login ---
            event.put("ipAddress", httpRequest.getRemoteAddr());
            event.put("userAgent", httpRequest.getHeader("User-Agent"));
            event.put("deviceInfo", "Desconocido"); // Se puede enriquecer más adelante
            event.put("location", "Desconocida"); // Se puede enriquecer con GeoIP

            // --- CORREGIDO: Publicar al EXCHANGE con el ROUTING KEY ---
            rabbitTemplate.convertAndSend(
                    rabbitMQProperties.getExchange(),
                    rabbitMQProperties.getSesiones().getRoutingKey(), // "sesiones.iniciada"
                    event
            );
            System.out.println("✅ Evento SESSION_STARTED publicado para: " + usuario.getEmail());

        } catch (Exception e) {
            System.err.println("❌ Error publicando evento SESSION_STARTED: " + e.getMessage());
        }
    }

    private void publishPasswordResetEvent(Usuario usuario, String resetToken) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("usuarioId", usuario.getId().toString());
            event.put("email", usuario.getEmail());
            event.put("nombre", usuario.getNombre());
            event.put("token", resetToken); // El orquestador espera 'token'
            event.put("fechaSolicitud", LocalDateTime.now().toString()); // El orquestador espera 'fechaSolicitud'

            // --- CORREGIDO: Publicar al EXCHANGE con el ROUTING KEY ---
            rabbitTemplate.convertAndSend(
                    rabbitMQProperties.getExchange(),
                    "password.reset.requested", // Routing key esperado por el orquestador
                    event
            );
            System.out.println("✅ Evento PASSWORD_RESET_SOLICITADO publicado para: " + usuario.getEmail());

        } catch (Exception e) {
            System.err.println("❌ Error publicando evento PASSWORD_RESET: " + e.getMessage());
        }
    }

    private void publishPasswordUpdatedEvent(Usuario usuario) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("usuarioId", usuario.getId().toString());
            event.put("email", usuario.getEmail());
            event.put("nombre", usuario.getNombre());
            event.put("fechaActualizacion", LocalDateTime.now().toString()); // El orquestador espera 'fechaActualizacion'

            // --- CORREGIDO: Publicar al EXCHANGE con el ROUTING KEY ---
            rabbitTemplate.convertAndSend(
                    rabbitMQProperties.getExchange(),
                    "password.updated", // Routing key esperado por el orquestador
                    event
            );
            System.out.println("✅ Evento PASSWORD_UPDATED publicado para: " + usuario.getEmail());

        } catch (Exception e) {
            System.err.println("❌ Error publicando evento PASSWORD_UPDATED: " + e.getMessage());
        }
    }
}