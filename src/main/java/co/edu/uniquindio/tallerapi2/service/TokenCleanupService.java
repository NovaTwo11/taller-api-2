package co.edu.uniquindio.tallerapi2.service;

import co.edu.uniquindio.tallerapi2.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupService.class);

    private final PasswordResetTokenRepository tokenRepository;

    public TokenCleanupService(PasswordResetTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Limpieza automática cada hora de tokens expirados
     */
    @Scheduled(fixedRate = 3600000) // cada 1 hora
    public void cleanup() {
        int deleted = tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Eliminados {} tokens expirados de recuperación de contraseña", deleted);
        }
    }
}