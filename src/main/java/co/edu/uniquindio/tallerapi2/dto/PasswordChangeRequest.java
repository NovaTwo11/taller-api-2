package co.edu.uniquindio.tallerapi2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para cambiar la contraseña")
public class PasswordChangeRequest {

    @Schema(description = "Contraseña actual", required = true)
    @NotBlank(message = "La contraseña actual es obligatoria")
    private String currentPassword;

    @Schema(description = "Nueva contraseña", required = true)
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 4, max = 50, message = "La contraseña debe tener entre 4 y 50 caracteres")
    private String newPassword;

    // Getters y Setters
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}