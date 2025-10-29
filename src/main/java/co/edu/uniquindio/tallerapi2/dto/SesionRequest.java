package co.edu.uniquindio.tallerapi2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales para iniciar sesión")
public class SesionRequest {

    @Schema(description = "Correo electrónico del usuario", example = "juan.perez@email.com", required = true)
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String email;

    @Schema(description = "Contraseña del usuario", example = "miPassword123", required = true)
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    // Constructores
    public SesionRequest() {}

    public SesionRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Getters y setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}