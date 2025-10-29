package co.edu.uniquindio.tallerapi2.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Información de sesión iniciada")
public class SesionResponse {

    @Schema(description = "Mensaje de confirmación", example = "Sesión iniciada exitosamente")
    private String mensaje;

    @Schema(description = "ID del usuario", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID usuarioId;

    @Schema(description = "Email del usuario", example = "juan.perez@email.com")
    private String email;

    @Schema(description = "Nombre del usuario", example = "Juan Pérez")
    private String nombre;

    // Constructores
    public SesionResponse() {}

    public SesionResponse(String mensaje, UUID usuarioId, String email, String nombre) {
        this.mensaje = mensaje;
        this.usuarioId = usuarioId;
        this.email = email;
        this.nombre = nombre;
    }

    // Getters y setters
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}