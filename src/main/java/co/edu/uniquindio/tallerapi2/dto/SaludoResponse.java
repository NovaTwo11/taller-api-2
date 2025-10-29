package co.edu.uniquindio.tallerapi2.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Respuesta de un saludo enviado")
public class SaludoResponse {

    @Schema(description = "ID del saludo", example = "321e4567-e89b-12d3-a456-426614174abc")
    private UUID id;

    @Schema(description = "Mensaje del saludo", example = "Hola mundo!")
    private String mensaje;

    @Schema(description = "Fecha y hora del saludo")
    private LocalDateTime fecha;

    @Schema(description = "Usuario que envió el saludo", example = "Juan Pérez")
    private String usuarioNombre;

    // Constructores
    public SaludoResponse() {}

    public SaludoResponse(UUID id, String mensaje, LocalDateTime fecha, String usuarioNombre) {
        this.id = id;
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.usuarioNombre = usuarioNombre;
    }

    // Getters y setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }
}