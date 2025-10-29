package co.edu.uniquindio.tallerapi2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Datos requeridos para enviar un saludo")
public class SaludoRequest {

    @Schema(description = "Mensaje del saludo", example = "Hola mundo!", required = true)
    @NotBlank(message = "El mensaje es obligatorio")
    private String mensaje;

    @Schema(description = "ID del usuario que envía el saludo",
            example = "123e4567-e89b-12d3-a456-426614174000",
            required = true)
    @NotNull(message = "El ID de usuario es obligatorio")
    private UUID usuarioId;

    // Constructores
    public SaludoRequest() {}

    public SaludoRequest(String mensaje, UUID usuarioId) {
        this.mensaje = mensaje;
        this.usuarioId = usuarioId;
    }

    // Getters y setters
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }
}