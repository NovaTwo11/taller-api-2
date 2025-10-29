package co.edu.uniquindio.tallerapi2.service;

import co.edu.uniquindio.tallerapi2.model.PasswordResetToken;
import co.edu.uniquindio.tallerapi2.model.Usuario;
import co.edu.uniquindio.tallerapi2.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@Transactional
public class PasswordResetTokenService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetTokenService.class);
    private static final int TOKEN_EXPIRATION_HOURS = 1;
    private static final int MAX_TOKENS_PER_USER = 3;

    private final PasswordResetTokenRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetTokenService(PasswordResetTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Genera un token seguro de reset para un usuario
     */
    public String generarTokenReset(Usuario usuario) {
        // Verificar límite de tokens activos por usuario
        Long tokensActivos = tokenRepository.countActiveTokensByUser(usuario.getId(), LocalDateTime.now());
        if (tokensActivos >= MAX_TOKENS_PER_USER) {
            throw new RuntimeException("Demasiadas solicitudes de reset activas. Intenta más tarde.");
        }

        // Limpiar tokens expirados del usuario
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());

        // Generar token seguro (32 bytes = 256 bits)
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // Crear y guardar token en BD
        LocalDateTime expiration = LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS);
        PasswordResetToken resetToken = new PasswordResetToken(token, expiration, usuario);
        tokenRepository.save(resetToken);

        log.info("Token de reset generado para usuario: {} (expira: {})", usuario.getEmail(), expiration);
        return token;
    }

    /**
     * Valida un token de reset
     */
    public Optional<PasswordResetToken> validarToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findValidToken(token, LocalDateTime.now());

        if (tokenOpt.isEmpty()) {
            log.warn("Token de reset inválido o expirado: {}", token);
            return Optional.empty();
        }

        PasswordResetToken resetToken = tokenOpt.get();
        log.info("Token válido encontrado para usuario: {}", resetToken.getUsuario().getEmail());
        return tokenOpt;
    }

    /**
     * Invalida un token después de usarlo
     */
    public void invalidarToken(String token) {
        tokenRepository.deleteByToken(token);
        log.info("Token invalidado: {}", token);
    }

    /**
     * Limpia todos los tokens expirados
     */
    public int limpiarTokensExpirados() {
        int eliminados = tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Tokens expirados eliminados: {}", eliminados);
        return eliminados;
    }

    /**
     * Verifica si un token es válido sin recuperar la entidad completa
     */
    public boolean esTokenValido(String token) {
        return tokenRepository.isTokenValidAndNotExpired(token, LocalDateTime.now());
    }
}