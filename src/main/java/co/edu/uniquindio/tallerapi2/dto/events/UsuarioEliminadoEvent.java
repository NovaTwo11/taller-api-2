package co.edu.uniquindio.tallerapi2.dto.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;

// DTO para el evento de usuario eliminado
public class UsuarioEliminadoEvent {

    private UUID usuarioId;
    private String email;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private String eventType;

    // Constructores
    public UsuarioEliminadoEvent() {
        this.timestamp = LocalDateTime.now();
        this.eventType = "USUARIO_ELIMINADO";
    }

    public UsuarioEliminadoEvent(UUID usuarioId, String email) {
        this();
        this.usuarioId = usuarioId;
        this.email = email;
    }

    // Getters y Setters
    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
}