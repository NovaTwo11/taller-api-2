package co.edu.uniquindio.tallerapi2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para restablecer contraseña")
public class PasswordResetRequest {

    @Schema(description = "Token de recuperación", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    @NotBlank(message = "El token es obligatorio")
    private String token;

    @Schema(description = "Nueva contraseña", example = "nuevaPassword123", required = true)
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 4, max = 50, message = "La contraseña debe tener entre 4 y 50 caracteres")
    private String newPassword;

    // Constructores
    public PasswordResetRequest() {}

    public PasswordResetRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    // Getters y setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}