package co.edu.uniquindio.tallerapi2.controller;

import co.edu.uniquindio.tallerapi2.dto.ApiError;
import co.edu.uniquindio.tallerapi2.dto.events.PasswordActualizadoEvent;
import co.edu.uniquindio.tallerapi2.dto.events.PasswordResetSolicitadoEvent;
import co.edu.uniquindio.tallerapi2.model.PasswordResetToken;
import co.edu.uniquindio.tallerapi2.model.Usuario;
import co.edu.uniquindio.tallerapi2.repository.UsuarioRepository;
import co.edu.uniquindio.tallerapi2.service.EventPublisherService;
import co.edu.uniquindio.tallerapi2.service.KeycloakAdminService;
import co.edu.uniquindio.tallerapi2.service.PasswordResetTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

@RestController
@RequestMapping("/api/password")
public class PasswordController {

    private final UsuarioRepository usuarioRepository;
    private final EventPublisherService eventPublisher;
    private final KeycloakAdminService keycloakAdminService;
    private final PasswordResetTokenService tokenService;

    public PasswordController(UsuarioRepository usuarioRepository,
                              EventPublisherService eventPublisher,
                              KeycloakAdminService keycloakAdminService,
                              PasswordResetTokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.eventPublisher = eventPublisher;
        this.keycloakAdminService = keycloakAdminService;
        this.tokenService = tokenService;
    }

    // ========================================
    // 📧 API Endpoint: Solicitar reset
    // ========================================
    @PostMapping("/solicitar-reset")
    public ResponseEntity<?> solicitarReset(@RequestParam String email, HttpServletRequest req) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError(404, "Not Found", "Usuario no encontrado", req.getRequestURI()));
        }

        try {
            Usuario usuario = usuarioOpt.get();

            // Generar token seguro y guardarlo en BD
            String token = tokenService.generarTokenReset(usuario);

            // ✅ Publicar SOLO evento de solicitud de reset (NO de actualización)
            PasswordResetSolicitadoEvent event = new PasswordResetSolicitadoEvent(
                    usuario.getId(),
                    email,
                    usuario.getNombre(),
                    token
            );
            eventPublisher.publishPasswordResetSolicitado(event);

            return ResponseEntity.ok("Solicitud de reset enviada. Revisa tu correo electrónico.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiError(429, "Too Many Requests", e.getMessage(), req.getRequestURI()));
        }
    }

    // ========================================
    // 🌐 HTML Endpoint: Mostrar formulario de reset
    // ========================================
    @GetMapping(value = "/reset", produces = MediaType.TEXT_HTML_VALUE)
    public String mostrarFormularioReset(@RequestParam String email,
                                         @RequestParam String token) {

        // Validar si el token es válido
        boolean valido = tokenService.esTokenValido(token);

        if (!valido) {
            return generarHtmlError("Token Inválido",
                    "El token de recuperación no es válido o ha expirado.",
                    "Por favor, solicita un nuevo enlace de recuperación.");
        }

        // Renderizar formulario de reset mejorado
        return generarHtmlFormulario(email, token);
    }

    // ========================================
    // 🔄 API/HTML Endpoint: Procesar reset
    // ========================================
    @PostMapping("/reset")
    public Object resetPassword(@RequestParam String email,
                                @RequestParam String nuevaClave,
                                @RequestParam String token,
                                @RequestHeader(value = "Accept", defaultValue = "") String acceptHeader,
                                HttpServletRequest req) {

        // Validar token en BD
        Optional<PasswordResetToken> tokenOpt = tokenService.validarToken(token);
        if (tokenOpt.isEmpty()) {
            return responseHtmlOrJson("Token Inválido",
                    "El token de recuperación no es válido o ha expirado.",
                    "Por favor, solicita un nuevo enlace de recuperación.",
                    req, acceptHeader, HttpStatus.BAD_REQUEST);
        }

        PasswordResetToken resetToken = tokenOpt.get();

        // Verificar que el email coincida con el del token
        if (!resetToken.getUsuario().getEmail().equals(email)) {
            return responseHtmlOrJson("Error de Validación",
                    "El token no corresponde al email proporcionado.",
                    "Verifica que estés usando el enlace correcto.",
                    req, acceptHeader, HttpStatus.BAD_REQUEST);
        }

        try {
            // ✅ Actualizar contraseña en Keycloak
            keycloakAdminService.actualizarPasswordUsuario(email, nuevaClave);

            // Invalidar token (eliminarlo de BD para evitar reuso)
            tokenService.invalidarToken(token);

            // ✅ AHORA SÍ publicar evento de confirmación (contraseña actualizada)
            PasswordActualizadoEvent event = new PasswordActualizadoEvent(
                    resetToken.getUsuario().getId(),
                    email
            );
            eventPublisher.publishPasswordActualizado(event);

            return responseHtmlOrJson("¡Contraseña Actualizada!",
                    "Tu contraseña ha sido cambiada exitosamente.",
                    "Ya puedes iniciar sesión con tu nueva contraseña.",
                    req, acceptHeader, HttpStatus.OK);

        } catch (Exception e) {
            return responseHtmlOrJson("Error del Servidor",
                    "No se pudo actualizar la contraseña.",
                    "Error: " + e.getMessage(),
                    req, acceptHeader, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ========================================
    // 🔍 API Endpoint: Validar token
    // ========================================
    @GetMapping("/validar-token")
    public ResponseEntity<?> validarToken(@RequestParam String token, HttpServletRequest req) {
        if (tokenService.esTokenValido(token)) {
            return ResponseEntity.ok("Token válido");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiError(400, "Bad Request", "Token inválido o expirado", req.getRequestURI()));
        }
    }

    // ========================================
    // 🛠️ Helper: Respuesta HTML o JSON
    // ========================================
    private Object responseHtmlOrJson(String titulo, String mensaje, String detalle,
                                      HttpServletRequest req, String acceptHeader, HttpStatus status) {
        if (acceptHeader.contains("text/html")) {
            if (status.is2xxSuccessful()) {
                return generarHtmlExito(titulo, mensaje, detalle);
            } else {
                return generarHtmlError(titulo, mensaje, detalle);
            }
        }
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), status.getReasonPhrase(), mensaje, req.getRequestURI()));
    }

    // ========================================
    // 🎨 Métodos auxiliares para generar HTML
    // ========================================

    private String generarHtmlFormulario(String email, String token) {
        return """
        <!DOCTYPE html>
        <html lang='es'>
        <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <title>Restablecer Contraseña</title>
            <style>
                * { box-sizing: border-box; }
                body { 
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                    background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                    margin: 0; padding: 20px; min-height: 100vh; 
                    display: flex; align-items: center; justify-content: center;
                }
                .container { 
                    background: white; padding: 40px; border-radius: 12px; 
                    box-shadow: 0 8px 32px rgba(0,0,0,0.1); max-width: 450px; width: 100%%;
                }
                h2 { 
                    color: #333; text-align: center; margin-bottom: 30px; 
                    font-size: 24px; font-weight: 600;
                }
                .info { 
                    background: #e3f2fd; padding: 15px; border-radius: 8px; 
                    margin-bottom: 25px; border-left: 4px solid #2196f3;
                    font-size: 14px;
                }
                .form-group { margin-bottom: 20px; }
                label { 
                    display: block; margin-bottom: 8px; font-weight: 600; 
                    color: #555; font-size: 14px;
                }
                input[type='password'] { 
                    width: 100%%; padding: 14px; border: 2px solid #e1e5e9; 
                    border-radius: 8px; font-size: 16px; transition: border-color 0.3s;
                }
                input[type='password']:focus { 
                    outline: none; border-color: #2196f3; 
                    box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
                }
                .password-requirements { 
                    font-size: 12px; color: #666; margin-top: 5px; 
                    display: flex; align-items: center; gap: 5px;
                }
                .error-msg { 
                    color: #f44336; font-size: 14px; margin-bottom: 15px; 
                    padding: 10px; background: #ffebee; border-radius: 6px; display: none;
                }
                button { 
                    width: 100%%; padding: 14px; background: linear-gradient(135deg, #2196f3, #21cbf3);
                    color: white; border: none; border-radius: 8px; font-size: 16px; 
                    font-weight: 600; cursor: pointer; transition: transform 0.2s;
                }
                button:hover { transform: translateY(-2px); }
                button:disabled { 
                    background: #ccc; cursor: not-allowed; transform: none; 
                }
                .strength-meter { 
                    height: 4px; background: #e1e5e9; border-radius: 2px; 
                    margin-top: 8px; overflow: hidden;
                }
                .strength-fill { 
                    height: 100%%; transition: width 0.3s, background-color 0.3s; 
                    width: 0%%; background: #f44336;
                }
            </style>
        </head>
        <body>
            <div class='container'>
                <h2>🔐 Restablecer Contraseña</h2>
                <div class='info'>
                    <strong>📧 Email:</strong> %s
                </div>
                
                <form method='post' action='/api/password/reset' onsubmit='return validateForm(event)'>
                    <input type='hidden' name='email' value='%s'/>
                    <input type='hidden' name='token' value='%s'/>
                    
                    <div class='form-group'>
                        <label for='nuevaClave'>Nueva Contraseña:</label>
                        <input type='password' id='nuevaClave' name='nuevaClave' 
                               required minlength='8' oninput='checkPasswordStrength()'/>
                        <div class='strength-meter'>
                            <div class='strength-fill' id='strengthFill'></div>
                        </div>
                        <div class='password-requirements'>
                            <span>🔒</span> Mínimo 8 caracteres
                        </div>
                    </div>
                    
                    <div class='form-group'>
                        <label for='confirmClave'>Confirmar Contraseña:</label>
                        <input type='password' id='confirmClave' required minlength='8' 
                               oninput='validatePasswords()'/>
                    </div>
                    
                    <div id='errorMsg' class='error-msg'></div>
                    
                    <button type='submit' id='submitBtn'>
                        🔄 Cambiar Contraseña
                    </button>
                </form>
            </div>
            
            <script>
                function checkPasswordStrength() {
                    const password = document.getElementById('nuevaClave').value;
                    const fill = document.getElementById('strengthFill');
                    
                    let strength = 0;
                    if (password.length >= 8) strength += 25;
                    if (/[A-Z]/.test(password)) strength += 25;
                    if (/[0-9]/.test(password)) strength += 25;
                    if (/[^A-Za-z0-9]/.test(password)) strength += 25;
                    
                    fill.style.width = strength + '%%';
                    if (strength < 50) fill.style.background = '#f44336';
                    else if (strength < 75) fill.style.background = '#ff9800';
                    else fill.style.background = '#4caf50';
                    
                    validatePasswords();
                }
                
                function validatePasswords() {
                    const pass1 = document.getElementById('nuevaClave').value;
                    const pass2 = document.getElementById('confirmClave').value;
                    const errorMsg = document.getElementById('errorMsg');
                    const submitBtn = document.getElementById('submitBtn');
                    
                    if (pass2 && pass1 !== pass2) {
                        errorMsg.textContent = '❌ Las contraseñas no coinciden';
                        errorMsg.style.display = 'block';
                        submitBtn.disabled = true;
                    } else {
                        errorMsg.style.display = 'none';
                        submitBtn.disabled = false;
                    }
                }
                
                function validateForm(event) {
                    const pass1 = document.getElementById('nuevaClave').value;
                    const pass2 = document.getElementById('confirmClave').value;
                    
                    if (pass1 !== pass2) {
                        event.preventDefault();
                        document.getElementById('errorMsg').textContent = '❌ Las contraseñas no coinciden';
                        document.getElementById('errorMsg').style.display = 'block';
                        return false;
                    }
                    
                    if (pass1.length < 8) {
                        event.preventDefault();
                        document.getElementById('errorMsg').textContent = '❌ La contraseña debe tener al menos 8 caracteres';
                        document.getElementById('errorMsg').style.display = 'block';
                        return false;
                    }
                    
                    return true;
                }
            </script>
        </body>
        </html>
        """.formatted(email, email, token);
    }

    private String generarHtmlExito(String titulo, String mensaje, String detalle) {
        return """
        <!DOCTYPE html>
        <html lang='es'>
        <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <title>%s</title>
            <style>
                * { box-sizing: border-box; }
                body { 
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                    background: linear-gradient(135deg, #4caf50 0%%, #45a049 100%%);
                    margin: 0; padding: 20px; min-height: 100vh; 
                    display: flex; align-items: center; justify-content: center;
                }
                .container { 
                    background: white; padding: 40px; border-radius: 12px; 
                    box-shadow: 0 8px 32px rgba(0,0,0,0.1); max-width: 450px; 
                    width: 100%%; text-align: center;
                }
                .success-icon { 
                    font-size: 64px; margin-bottom: 20px; 
                    animation: bounce 1s ease-in-out;
                }
                @keyframes bounce {
                    0%%, 20%%, 50%%, 80%%, 100%% { transform: translateY(0); }
                    40%% { transform: translateY(-10px); }
                    60%% { transform: translateY(-5px); }
                }
                h2 { 
                    color: #4caf50; margin-bottom: 20px; font-size: 24px; 
                    font-weight: 600;
                }
                p { 
                    color: #666; line-height: 1.6; margin-bottom: 20px; 
                    font-size: 16px;
                }
                .detail { 
                    background: #e8f5e8; padding: 20px; border-radius: 8px; 
                    border-left: 4px solid #4caf50; font-size: 14px; color: #2e7d32;
                }
                .login-link {
                    margin-top: 20px; padding: 12px 24px; background: #2196f3;
                    color: white; text-decoration: none; border-radius: 6px;
                    display: inline-block; font-weight: 600; transition: background 0.3s;
                }
                .login-link:hover { background: #1976d2; }
            </style>
        </head>
        <body>
            <div class='container'>
                <div class='success-icon'>✅</div>
                <h2>%s</h2>
                <p>%s</p>
                <div class='detail'>%s</div>
                <a href='/api/sesiones' class='login-link'>🔑 Ir a Iniciar Sesión</a>
            </div>
        </body>
        </html>
        """.formatted(titulo, titulo, mensaje, detalle);
    }

    private String generarHtmlError(String titulo, String mensaje, String detalle) {
        return """
        <!DOCTYPE html>
        <html lang='es'>
        <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <title>%s</title>
            <style>
                * { box-sizing: border-box; }
                body { 
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                    background: linear-gradient(135deg, #f44336 0%%, #d32f2f 100%%);
                    margin: 0; padding: 20px; min-height: 100vh; 
                    display: flex; align-items: center; justify-content: center;
                }
                .container { 
                    background: white; padding: 40px; border-radius: 12px; 
                    box-shadow: 0 8px 32px rgba(0,0,0,0.1); max-width: 450px; 
                    width: 100%%; text-align: center;
                }
                .error-icon { 
                    font-size: 64px; margin-bottom: 20px; color: #f44336;
                    animation: shake 0.5s ease-in-out;
                }
                @keyframes shake {
                    0%%, 100%% { transform: translateX(0); }
                    25%% { transform: translateX(-5px); }
                    75%% { transform: translateX(5px); }
                }
                h2 { 
                    color: #f44336; margin-bottom: 20px; font-size: 24px; 
                    font-weight: 600;
                }
                p { 
                    color: #666; line-height: 1.6; margin-bottom: 20px; 
                    font-size: 16px;
                }
                .detail { 
                    background: #ffebee; padding: 20px; border-radius: 8px; 
                    border-left: 4px solid #f44336; font-size: 14px; color: #c62828;
                }
                .retry-link {
                    margin-top: 20px; padding: 12px 24px; background: #2196f3;
                    color: white; text-decoration: none; border-radius: 6px;
                    display: inline-block; font-weight: 600; transition: background 0.3s;
                }
                .retry-link:hover { background: #1976d2; }
            </style>
        </head>
        <body>
            <div class='container'>
                <div class='error-icon'>❌</div>
                <h2>%s</h2>
                <p>%s</p>
                <div class='detail'>%s</div>
                <a href='/api/password/solicitar-reset' class='retry-link'>🔄 Solicitar Nuevo Enlace</a>
            </div>
        </body>
        </html>
        """.formatted(titulo, titulo, mensaje, detalle);
    }
}