package co.edu.uniquindio.tallerapi2.dto.events;

import java.util.UUID;

public class PasswordResetSolicitadoEvent {
    private UUID usuarioId;
    private String email;
    private String nombre;
    private String token;

    public PasswordResetSolicitadoEvent() {}

    public PasswordResetSolicitadoEvent(UUID usuarioId, String email, String nombre, String token) {
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

    @Override
    public String toString() {
        return "PasswordResetSolicitadoEvent{" +
                "usuarioId=" + usuarioId +
                ", email='" + email + '\'' +
                ", nombre='" + nombre + '\'' +
                ", token='" + token + '\'' +
                '}';
    }
}