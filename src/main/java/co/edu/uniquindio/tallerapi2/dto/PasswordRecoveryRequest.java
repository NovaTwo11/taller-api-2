package co.edu.uniquindio.tallerapi2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Solicitud de recuperación de contraseña")
public class PasswordRecoveryRequest {

    @Schema(description = "Correo electrónico del usuario", example = "juan.perez@email.com", required = true)
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String email;

    // Constructores
    public PasswordRecoveryRequest() {}

    public PasswordRecoveryRequest(String email) {
        this.email = email;
    }

    // Getters y setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}