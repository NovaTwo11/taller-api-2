package co.edu.uniquindio.tallerapi2.repository;

import co.edu.uniquindio.tallerapi2.model.PasswordResetToken;
import co.edu.uniquindio.tallerapi2.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Buscar token por su valor (usado en reset password)
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Buscar todos los tokens de un usuario específico
     */
    List<PasswordResetToken> findByUsuario(Usuario usuario);

    /**
     * Buscar tokens por ID de usuario
     */
    List<PasswordResetToken> findByUsuarioId(UUID usuarioId);

    /**
     * Eliminar todos los tokens de un usuario (usado antes de crear uno nuevo)
     */
    @Transactional
    void deleteByUsuario(Usuario usuario);

    /**
     * Eliminar tokens por ID de usuario
     */
    @Transactional
    void deleteByUsuarioId(UUID usuarioId);

    /**
     * Verificar si existe un token válido (no expirado) para un usuario
     */
    @Query("SELECT COUNT(t) > 0 FROM PasswordResetToken t WHERE t.usuario.id = :usuarioId AND t.expiration > :now")
    boolean existsValidTokenForUser(@Param("usuarioId") UUID usuarioId, @Param("now") LocalDateTime now);

    /**
     * Buscar tokens expirados (para limpieza automática)
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.expiration < :now")
    List<PasswordResetToken> findExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Eliminar todos los tokens expirados (limpieza automática)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiration < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Buscar token válido (no expirado) por su valor
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.token = :token AND t.expiration > :now")
    Optional<PasswordResetToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

    /**
     * Contar tokens activos por usuario
     */
    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.usuario.id = :usuarioId AND t.expiration > :now")
    Long countActiveTokensByUser(@Param("usuarioId") UUID usuarioId, @Param("now") LocalDateTime now);

    /**
     * Buscar tokens que expiran en un rango de tiempo específico
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.expiration BETWEEN :start AND :end")
    List<PasswordResetToken> findTokensExpiringBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Eliminar token específico por su valor (después de usarlo)
     */
    @Transactional
    void deleteByToken(String token);

    /**
     * Verificar si un token específico existe y no ha expirado
     */
    @Query("SELECT COUNT(t) > 0 FROM PasswordResetToken t WHERE t.token = :token AND t.expiration > :now")
    boolean isTokenValidAndNotExpired(@Param("token") String token, @Param("now") LocalDateTime now);
}