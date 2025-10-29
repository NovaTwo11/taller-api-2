package co.edu.uniquindio.tallerapi2.controller;

import co.edu.uniquindio.tallerapi2.dto.*;
import co.edu.uniquindio.tallerapi2.model.PasswordResetToken;
import co.edu.uniquindio.tallerapi2.model.Usuario;
import co.edu.uniquindio.tallerapi2.repository.PasswordResetTokenRepository;
import co.edu.uniquindio.tallerapi2.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;

    public AuthController(UsuarioRepository usuarioRepository,
                          PasswordResetTokenRepository tokenRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
    }

    /**
     * Login → recibe SesionRequest y devuelve SesionResponse
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody SesionRequest request,
                                   HttpServletRequest httpRequest) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail());

        if (usuarioOpt.isEmpty() || !usuarioOpt.get().getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(401)
                    .body(new ApiError(401, "Unauthorized",
                            "Credenciales inválidas", httpRequest.getRequestURI()));
        }

        Usuario usuario = usuarioOpt.get();

        SesionResponse response = new SesionResponse(
                "Inicio de sesión exitoso",
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Enviar solicitud de recuperación de contraseña
     */
    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody PasswordRecoveryRequest request,
                                            HttpServletRequest httpRequest) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(400, "Bad Request",
                            "No existe un usuario con ese email", httpRequest.getRequestURI()));
        }

        Usuario usuario = usuarioOpt.get();
        tokenRepository.deleteByUsuario(usuario);

        String tokenValue = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken(
                tokenValue,
                LocalDateTime.now().plusMinutes(30),
                usuario
        );
        tokenRepository.save(token);

        String resetUrl = "http://localhost:8080/api/auth/reset-password?token=" + tokenValue;

        Map<String, Object> resp = new HashMap<>();
        resp.put("mensaje", "Token de recuperación generado (simulado envío por correo)");
        resp.put("resetUrl", resetUrl);

        return ResponseEntity.ok(resp);
    }

    /**
     * Restablecer la contraseña
     */
    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetRequest request,
                                           HttpServletRequest httpRequest) {

        Optional<PasswordResetToken> tokenOpt =
                tokenRepository.findValidToken(request.getToken(), LocalDateTime.now());

        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiError(400, "Bad Request",
                            "Token inválido o expirado", httpRequest.getRequestURI()));
        }

        PasswordResetToken token = tokenOpt.get();
        Usuario usuario = token.getUsuario();

        usuario.setPassword(request.getNewPassword()); // ⚠️ en producción se debe cifrar con BCrypt
        usuarioRepository.save(usuario);

        tokenRepository.deleteByToken(request.getToken());

        Map<String, Object> resp = new HashMap<>();
        resp.put("mensaje", "Contraseña actualizada correctamente");

        return ResponseEntity.ok(resp);
    }
}