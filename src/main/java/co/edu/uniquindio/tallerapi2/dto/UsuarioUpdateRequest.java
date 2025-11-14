package co.edu.uniquindio.tallerapi2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para actualizar un usuario (sin contraseña)")
public class UsuarioUpdateRequest {

    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Schema(description = "Correo electrónico del usuario", example = "juan.perez@email.com")
    @Email(message = "El formato del email no es válido")
    private String email;

    // Getters y Setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}