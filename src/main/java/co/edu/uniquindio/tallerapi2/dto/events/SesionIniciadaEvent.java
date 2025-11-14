package co.edu.uniquindio.tallerapi2.dto.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SesionIniciadaEvent {
    private Long usuarioId;
    private String email;
    private String nombre;
    private LocalDateTime timestamp;

    private String ipAddress;
    private String userAgent;
    private String deviceInfo;
    private String location;

    /**
     * Constructor básico
     */
    public SesionIniciadaEvent(Long usuarioId, String email, String nombre) {
        this.usuarioId = usuarioId;
        this.email = email;
        this.nombre = nombre;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor completo con datos de seguridad
     */
    public SesionIniciadaEvent(Long usuarioId, String email, String nombre,
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