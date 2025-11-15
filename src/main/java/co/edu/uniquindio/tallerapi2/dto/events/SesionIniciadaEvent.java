package co.edu.uniquindio.tallerapi2.dto.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SesionIniciadaEvent {
    private UUID usuarioId;
    private String email;
    private String nombre;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;

    private String ipAddress;
    private String userAgent;
    private String deviceInfo;
    private String location;

    /**
     * Constructor básico
     */
    public SesionIniciadaEvent(UUID usuarioId, String email, String nombre) {
        this.usuarioId = usuarioId;
        this.email = email;
        this.nombre = nombre;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor completo con datos de seguridad
     */
    public SesionIniciadaEvent(UUID usuarioId, String email, String nombre,
                               String ipAddress, String userAgent, String deviceInfo) {
        this.usuarioId = usuarioId;
        this.email = email;
        this.nombre = nombre;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.deviceInfo = deviceInfo;
        this.timestamp = LocalDateTime.now();
    }
}