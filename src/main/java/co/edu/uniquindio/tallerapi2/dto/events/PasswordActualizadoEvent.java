package co.edu.uniquindio.tallerapi2.dto.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;

public class PasswordActualizadoEvent {
    private UUID usuarioId;
    private String email;

    // 1. AÑADIR CAMPO 'nombre' QUE FALTABA
    private String nombre;

    // 2. RENOMBRAR 'timestamp' A 'fechaActualizacion' (coincide con Pydantic) Y AÑADIR FORMATO
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime fechaActualizacion;

    private String eventType;

    public PasswordActualizadoEvent() {
        this.fechaActualizacion = LocalDateTime.now(); // 3. Usar el nuevo nombre
        this.eventType = "PASSWORD_ACTUALIZADO";
    }

    public PasswordActualizadoEvent(UUID usuarioId, String email) {
        this();
        this.usuarioId = usuarioId;
        this.email = email;
    }

    // 4. ACTUALIZAR CONSTRUCTOR
    public PasswordActualizadoEvent(UUID usuarioId, String email, String nombre) {
        this();
        this.usuarioId = usuarioId;
        this.email = email;
        this.nombre = nombre; // 5. Asignar nombre
    }

    // Getters y Setters (Generados manualmente)

    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // 6. AÑADIR GETTER Y SETTER PARA 'nombre'
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    // 7. ACTUALIZAR GETTER Y SETTER PARA 'fechaActualizacion'
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
}