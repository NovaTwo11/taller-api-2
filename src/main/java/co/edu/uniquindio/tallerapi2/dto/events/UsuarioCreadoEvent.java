package co.edu.uniquindio.tallerapi2.dto.events;

import co.edu.uniquindio.tallerapi2.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UsuarioCreadoEvent {
    private UUID usuarioId;
    private String email;
    private String nombre;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;
    private String activationToken;
    private String baseUrl;

    /**
     * Constructor básico (sin token de activación)
     */
    public UsuarioCreadoEvent(UUID usuarioId, String email, String nombre) {
        this.usuarioId = usuarioId;
        this.email = email;
        this.nombre = nombre;
        this.timestamp = LocalDateTime.now();
    }
    public UsuarioCreadoEvent() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor completo (con token de activación)
     */
    public UsuarioCreadoEvent(UUID usuarioId, String email, String nombre, String activationToken, String baseUrl) {
        this.usuarioId = usuarioId;
        this.email = email;
        this.nombre = nombre;
        this.activationToken = activationToken;
        this.baseUrl = baseUrl;
        this.timestamp = LocalDateTime.now();
    }

    public static UsuarioCreadoEvent fromUsuario(Usuario usuario) {
        return new UsuarioCreadoEvent(
                usuario.getId(), // UUID directamente
                usuario.getEmail(),
                usuario.getNombre()
        );
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getActivationToken() {
        return activationToken;
    }

    public void setActivationToken(String activationToken) {
        this.activationToken = activationToken;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}