package co.edu.uniquindio.tallerapi2.repository;

import co.edu.uniquindio.tallerapi2.model.Saludo;
import co.edu.uniquindio.tallerapi2.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SaludoRepository extends JpaRepository<Saludo, UUID> {

    /**
     * Buscar saludos por usuario
     */
    List<Saludo> findByUsuario(Usuario usuario);

    /**
     * Buscar saludos por ID de usuario
     */
    List<Saludo> findByUsuarioId(UUID usuarioId);

    /**
     * Buscar saludos que contengan un texto específico en el mensaje
     */
    List<Saludo> findByMensajeContainingIgnoreCase(String texto);

    /**
     * Buscar saludos por rango de fechas
     */
    List<Saludo> findByFechaBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    /**
     * Buscar saludos de un usuario en un rango de fechas
     */
    @Query("SELECT s FROM Saludo s WHERE s.usuario.id = :usuarioId AND s.fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY s.fecha DESC")
    List<Saludo> findByUsuarioIdAndFechaBetween(
            @Param("usuarioId") UUID usuarioId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

    /**
     * Obtener los últimos N saludos ordenados por fecha
     */
    @Query("SELECT s FROM Saludo s ORDER BY s.fecha DESC")
    List<Saludo> findAllOrderByFechaDesc();

    /**
     * Contar saludos por usuario
     */
    @Query("SELECT COUNT(s) FROM Saludo s WHERE s.usuario.id = :usuarioId")
    Long countByUsuarioId(@Param("usuarioId") UUID usuarioId);

    /**
     * Buscar saludos recientes (últimas 24 horas)
     */
    @Query("SELECT s FROM Saludo s WHERE s.fecha >= :fechaLimite ORDER BY s.fecha DESC")
    List<Saludo> findSaludosRecientes(@Param("fechaLimite") LocalDateTime fechaLimite);

    /**
     * Eliminar todos los saludos de un usuario
     */
    void deleteByUsuario(Usuario usuario);

    /**
     * Eliminar saludos por ID de usuario
     */
    void deleteByUsuarioId(UUID usuarioId);
}