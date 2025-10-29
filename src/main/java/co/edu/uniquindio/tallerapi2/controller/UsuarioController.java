package co.edu.uniquindio.tallerapi2.controller;

import co.edu.uniquindio.tallerapi2.dto.*;
import co.edu.uniquindio.tallerapi2.dto.events.PasswordActualizadoEvent;
import co.edu.uniquindio.tallerapi2.dto.events.PasswordResetSolicitadoEvent;
import co.edu.uniquindio.tallerapi2.model.PasswordResetToken;
import co.edu.uniquindio.tallerapi2.model.Usuario;
import co.edu.uniquindio.tallerapi2.repository.PasswordResetTokenRepository;
import co.edu.uniquindio.tallerapi2.repository.UsuarioRepository;
import co.edu.uniquindio.tallerapi2.service.KeycloakAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import co.edu.uniquindio.tallerapi2.dto.events.UsuarioCreadoEvent;
import co.edu.uniquindio.tallerapi2.service.EventPublisherService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EventPublisherService eventPublisher;
    private final KeycloakAdminService keycloakAdminService;

    public UsuarioController(UsuarioRepository usuarioRepository,
                             PasswordResetTokenRepository tokenRepository,
                             EventPublisherService eventPublisher,
                             KeycloakAdminService keycloakAdminService) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.eventPublisher = eventPublisher;
        this.keycloakAdminService = keycloakAdminService;
    }

    @Operation(summary = "Listar usuarios con paginación",
            description = "Obtiene una lista paginada de todos los usuarios registrados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de paginación inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> listarUsuarios(
            @Parameter(description = "Número de página (inicia en 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Tamaño de página", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Campo por el cual ordenar", example = "nombre")
            @RequestParam(defaultValue = "nombre") String sortBy,

            @Parameter(description = "Dirección del ordenamiento", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir) {

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() :
                    Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Usuario> usuariosPage = usuarioRepository.findAll(pageable);

            Page<UsuarioResponse> responsePage = usuariosPage.map(UsuarioResponse::fromEntity);

            return ResponseEntity.ok(responsePage);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Obtener usuario por ID",
            description = "Busca y retorna un usuario específico por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerUsuario(
            @Parameter(description = "ID único del usuario", required = true)
            @PathVariable UUID id,
            HttpServletRequest request) {

        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);

            if (usuarioOpt.isPresent()) {
                return ResponseEntity.ok(UsuarioResponse.fromEntity(usuarioOpt.get()));
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError(404, "Not Found", "Usuario no encontrado", request.getRequestURI()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(400, "Bad Request", "ID de usuario inválido", request.getRequestURI()));
        }
    }

    @Operation(summary = "Crear nuevo usuario",
            description = "Registra un nuevo usuario en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "El email ya está registrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<?> crearUsuario(
            @Valid @RequestBody UsuarioRequest request,
            HttpServletRequest httpRequest) {

        try {
            // Verificar si el email ya existe
            if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiError(409, "Conflict", "Ya existe un usuario con ese email", httpRequest.getRequestURI()));
            }

            Usuario usuario = new Usuario();
            usuario.setNombre(request.getNombre());
            usuario.setEmail(request.getEmail());
            usuario.setPassword(request.getPassword());

            Usuario usuarioGuardado = usuarioRepository.save(usuario);

            // 🚀 Crear también en Keycloak
            keycloakAdminService.crearUsuarioEnKeycloak(
                    usuarioGuardado.getNombre(),
                    usuarioGuardado.getEmail(),
                    request.getPassword()
            );

            // 🚀 PUBLICAR EVENTO DE USUARIO CREADO
            UsuarioCreadoEvent evento = UsuarioCreadoEvent.fromUsuario(usuarioGuardado);
            eventPublisher.publishUsuarioCreado(evento);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "mensaje", "Usuario creado exitosamente",
                            "id", usuarioGuardado.getId().toString(),
                            "usuario", UsuarioResponse.fromEntity(usuarioGuardado),
                            "eventoPublicado", true
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(500, "Internal Server Error", "Error interno del servidor", httpRequest.getRequestURI()));
        }
    }

    @Operation(summary = "Actualizar usuario",
            description = "Actualiza los datos de un usuario existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "El email ya está en uso por otro usuario",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(
            @Parameter(description = "ID único del usuario", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody UsuarioRequest request,
            HttpServletRequest httpRequest) {

        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);

            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(404, "Not Found", "Usuario no encontrado", httpRequest.getRequestURI()));
            }

            // Verificar si el email ya existe en otro usuario
            Optional<Usuario> usuarioConEmail = usuarioRepository.findByEmail(request.getEmail());
            if (usuarioConEmail.isPresent() && !usuarioConEmail.get().getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiError(409, "Conflict", "Ya existe otro usuario con ese email", httpRequest.getRequestURI()));
            }

            Usuario usuario = usuarioOpt.get();
            usuario.setNombre(request.getNombre());
            usuario.setEmail(request.getEmail());
            usuario.setPassword(request.getPassword());

            Usuario usuarioActualizado = usuarioRepository.save(usuario);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Usuario actualizado exitosamente",
                    "usuario", UsuarioResponse.fromEntity(usuarioActualizado)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(400, "Bad Request", "ID de usuario inválido", httpRequest.getRequestURI()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(500, "Internal Server Error", "Error interno del servidor", httpRequest.getRequestURI()));
        }
    }

    @Operation(summary = "Eliminar usuario",
            description = "Elimina un usuario del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarUsuario(
            @Parameter(description = "ID único del usuario", required = true)
            @PathVariable UUID id,
            HttpServletRequest request) {

        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);

            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(404, "Not Found", "Usuario no encontrado", request.getRequestURI()));
            }

            usuarioRepository.deleteById(id);

            return ResponseEntity.ok(Map.of("mensaje", "Usuario eliminado exitosamente"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(400, "Bad Request", "ID de usuario inválido", request.getRequestURI()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(500, "Internal Server Error", "Error interno del servidor", request.getRequestURI()));
        }
    }

    // ========== ENDPOINTS DE RECUPERACIÓN DE CONTRASEÑA ==========

    @Operation(summary = "Solicitar recuperación de contraseña",
            description = "Genera un token de recuperación y lo envía por email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token de recuperación generado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/recovery")
    public ResponseEntity<?> solicitarRecuperacion(
            @Valid @RequestBody PasswordRecoveryRequest request,
            HttpServletRequest httpRequest) {

        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail());

            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(404, "Not Found", "No existe un usuario registrado con ese correo", httpRequest.getRequestURI()));
            }

            Usuario usuario = usuarioOpt.get();

            // Eliminar tokens previos del usuario
            tokenRepository.deleteByUsuario(usuario);

            // Generar nuevo token
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUsuario(usuario);
            resetToken.setExpiration(LocalDateTime.now().plusMinutes(15));

            tokenRepository.save(resetToken);

            // 🚀 PUBLICAR EVENTO DE RECUPERACIÓN SOLICITADA
            PasswordResetSolicitadoEvent evento = new PasswordResetSolicitadoEvent(
                    usuario.getId(), usuario.getEmail(), usuario.getNombre(), token
            );
            eventPublisher.publishPasswordResetSolicitado(evento);

            // TODO: Enviar email real
            // emailService.sendPasswordResetEmail(request.getEmail(), token);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Token de recuperación generado exitosamente",
                    "token", token,
                    "expiration", "15 minutos",
                    "resetUrl", "POST /api/usuarios/reset-password"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(500, "Internal Server Error", "Error interno del servidor", httpRequest.getRequestURI()));
        }
    }

    @Operation(summary = "Restablecer contraseña",
            description = "Cambia la contraseña usando un token de recuperación válido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Token inválido o expirado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Token no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<?> restablecerPassword(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {

        try {
            Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(request.getToken());

            if (tokenOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(404, "Not Found", "Token no válido", httpRequest.getRequestURI()));
            }

            PasswordResetToken resetToken = tokenOpt.get();

            if (resetToken.isExpired()) {
                tokenRepository.delete(resetToken);
                return ResponseEntity.badRequest()
                        .body(new ApiError(400, "Bad Request", "El token ha expirado. Solicita una nueva recuperación", httpRequest.getRequestURI()));
            }

            // Actualizar contraseña
            Usuario usuario = resetToken.getUsuario();
            usuario.setPassword(request.getNewPassword());
            usuarioRepository.save(usuario);

            // Eliminar token usado
            tokenRepository.delete(resetToken);

            // 🚀 PUBLICAR EVENTO DE PASSWORD ACTUALIZADO
            PasswordActualizadoEvent evento = new PasswordActualizadoEvent(
                    usuario.getId(), usuario.getEmail()
            );
            eventPublisher.publishPasswordActualizado(evento);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Contraseña actualizada exitosamente",
                    "usuarioId", usuario.getId().toString(),
                    "email", usuario.getEmail()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(500, "Internal Server Error", "Error interno del servidor", httpRequest.getRequestURI()));
        }
    }

    @Operation(summary = "Verificar token de recuperación",
            description = "Valida si un token de recuperación es válido y no ha expirado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token válido"),
            @ApiResponse(responseCode = "400", description = "Token inválido o expirado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/verify-token/{token}")
    public ResponseEntity<?> verificarToken(
            @Parameter(description = "Token de recuperación", required = true)
            @PathVariable String token,
            HttpServletRequest request) {

        try {
            Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

            if (tokenOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(400, "Bad Request", "Token no válido", request.getRequestURI()));
            }

            PasswordResetToken resetToken = tokenOpt.get();

            if (resetToken.isExpired()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(400, "Bad Request", "Token expirado", request.getRequestURI()));
            }

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Token válido",
                    "email", resetToken.getUsuario().getEmail(),
                    "expiration", resetToken.getExpiration().toString()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(500, "Internal Server Error", "Error interno del servidor", request.getRequestURI()));
        }
    }
}