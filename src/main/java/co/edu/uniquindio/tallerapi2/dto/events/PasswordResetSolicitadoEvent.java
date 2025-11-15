package co.edu.uniquindio.tallerapi2.dto.events;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public class PasswordResetSolicitadoEvent {
    private UUID usuarioId;
    private String email;
    private String nombre;
    private String token;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime fechaSolicitud;

    public PasswordResetSolicitadoEvent() {
        this.fechaSolicitud = LocalDateTime.now(); // 4. INICIALIZAR
    }
    public PasswordResetSolicitadoEvent(UUID usuarioId, String email, String nombre, String token) {
        this();
        this.usuarioId = usuarioId;
        this.email = email;
        this.nombre = nombre;
        this.token = token;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    @Override
    public String toString() {
        return "PasswordResetSolicitadoEvent{" +
                "usuarioId=" + usuarioId +
                ", email='" + email + '\'' +
                ", nombre='" + nombre + '\'' +
                ", token='" + token + '\'' +
                ", fechaSolicitud=" + fechaSolicitud + // Añadido
                '}';
    }
}