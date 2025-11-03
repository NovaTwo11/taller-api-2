package co.edu.uniquindio.tallerapi2.service;

import co.edu.uniquindio.tallerapi2.exception.IntegracionKeycloakException;
import co.edu.uniquindio.tallerapi2.exception.UsuarioDuplicadoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.core.ParameterizedTypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakAdminService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminService.class);

    private final WebClient webClient;

    @Value("${keycloak.admin.url}")
    private String keycloakUrl;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    @Value("${app.keycloak.enabled:true}")
    private boolean keycloakEnabled;

    public KeycloakAdminService() {
        this.webClient = WebClient.builder().build();
    }

    private String getAdminAccessToken() {
        if (!keycloakEnabled) {
            log.info("Keycloak deshabilitado, retornando token dummy");
            return "dummy-token";
        }

        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realm);

        try {
            Map<String, Object> tokenResponse = webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("client_id", clientId)
                            .with("client_secret", clientSecret)
                            .with("grant_type", "client_credentials"))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String token = (String) tokenResponse.get("access_token");
            log.debug("Token de admin obtenido exitosamente");
            return token;
        } catch (WebClientResponseException e) {
            log.error("Error obteniendo token de admin. Status: {}, Body: {}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new IntegracionKeycloakException(
                    "Error obteniendo token de admin de Keycloak (status " + e.getStatusCode().value() + "): "
                            + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Error inesperado obteniendo token: {}", e.getMessage(), e);
            throw new IntegracionKeycloakException("Error conectando con Keycloak: " + e.getMessage(), e);
        }
    }

    public void crearUsuarioEnKeycloak(String nombre, String email, String password) {
        if (!keycloakEnabled) {
            log.info("Keycloak deshabilitado, omitiendo creación de usuario: {}", email);
            return;
        }

        log.info("Iniciando creación de usuario en Keycloak: {}", email);
        String accessToken = getAdminAccessToken();

        String usersUrl = String.format("%s/admin/realms/%s/users", keycloakUrl, realm);
        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("username", email);
        userPayload.put("email", email);
        userPayload.put("firstName", nombre);
        userPayload.put("enabled", true);
        userPayload.put("emailVerified", true);

        String userId;
        try {
            // Paso 1: Crear usuario
            log.debug("POST a {}", usersUrl);
            ResponseEntity<Void> response = webClient.post()
                    .uri(usersUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(userPayload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            // 201 esperado
            String location = response != null ? response.getHeaders().getFirst("Location") : null;
            if (location != null) {
                userId = location.substring(location.lastIndexOf("/") + 1);
                log.info("Usuario creado con ID: {}", userId);
            } else {
                // Buscar por email si no hay Location
                userId = buscarUserIdPorEmail(accessToken, email);
                if (userId == null) {
                    throw new IntegracionKeycloakException("No se pudo obtener el ID del usuario en Keycloak");
                }
                log.info("Usuario encontrado con ID: {}", userId);
            }
        } catch (WebClientResponseException e) {
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            log.error("Error creando usuario en Keycloak. Status: {}, Body: {}", status, body);

            // Si es 409 (conflict), el usuario ya existe
            if (status == 409) {
                log.warn("Usuario ya existe en Keycloak: {}", email);
                userId = buscarUserIdPorEmail(accessToken, email);
                if (userId == null) {
                    throw new UsuarioDuplicadoException("Usuario ya existe en Keycloak pero no se pudo obtener su ID: " + email);
                }
                log.info("Usuario duplicado encontrado con ID: {}", userId);
                // Continuamos para intentar setear password (puede que no la tenga)
            } else if (status >= 400 && status < 500) {
                // Otros errores 4xx
                throw new IntegracionKeycloakException("Error de cliente en Keycloak (" + status + "): " + body, e);
            } else {
                // Errores 5xx
                throw new IntegracionKeycloakException("Error del servidor Keycloak (" + status + "): " + body, e);
            }
        } catch (Exception e) {
            log.error("Error inesperado creando usuario: {}", e.getMessage(), e);
            throw new IntegracionKeycloakException("Error inesperado creando usuario en Keycloak: " + e.getMessage(), e);
        }

        // Paso 2: set password
        try {
            setPasswordConToken(accessToken, userId, password);
            log.info("Password establecida para usuario: {}", email);
        } catch (Exception e) {
            log.error("Error estableciendo password para {}: {}", email, e.getMessage(), e);
            throw new IntegracionKeycloakException("Error estableciendo password en Keycloak: " + e.getMessage(), e);
        }

        // Paso 3 (opcional): update user – NO BLOQUEANTE
        try {
            String userUrl = String.format("%s/admin/realms/%s/users/%s", keycloakUrl, realm, userId);
            Map<String, Object> updatePayload = new HashMap<>();
            updatePayload.put("enabled", true);
            updatePayload.put("emailVerified", true);
            updatePayload.put("username", email);
            updatePayload.put("email", email);
            updatePayload.put("firstName", nombre);

            webClient.put()
                    .uri(userUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updatePayload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.debug("Usuario actualizado en Keycloak: {}", email);
        } catch (Exception ex) {
            log.warn("⚠️ No crítico: fallo en update de usuario en Keycloak: {}", ex.getMessage());
        }

        // Paso 4-5 (opcional): asignar roles – NO BLOQUEANTE
        try {
            asignarRolesPorDefecto(accessToken, userId);
            log.debug("Roles asignados para usuario: {}", email);
        } catch (Exception ex) {
            log.warn("⚠️ No crítico: fallo asignando roles por defecto: {}", ex.getMessage());
        }

        log.info("✅ Usuario Keycloak OK: {} (ID: {})", email, userId);
    }

    private String buscarUserIdPorEmail(String accessToken, String email) {
        if (!keycloakEnabled) {
            return "dummy-user-id";
        }

        try {
            String searchUrl = String.format("%s/admin/realms/%s/users?email=%s", keycloakUrl, realm, email);
            log.debug("Buscando usuario por email: {}", email);

            List<Map<String, Object>> users = webClient.get()
                    .uri(searchUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .collectList()
                    .block();

            if (users == null || users.isEmpty()) {
                log.warn("No se encontró usuario con email: {}", email);
                return null;
            }

            String userId = (String) users.get(0).get("id");
            log.debug("Usuario encontrado: {} -> {}", email, userId);
            return userId;
        } catch (WebClientResponseException e) {
            log.error("Error buscando usuario por email. Status: {}, Body: {}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Error inesperado buscando usuario: {}", e.getMessage(), e);
            return null;
        }
    }

    private void setPasswordConToken(String accessToken, String userId, String password) {
        if (!keycloakEnabled) {
            return;
        }

        String resetPasswordUrl = String.format("%s/admin/realms/%s/users/%s/reset-password",
                keycloakUrl, realm, userId);
        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", password);
        credential.put("temporary", false);

        try {
            webClient.put()
                    .uri(resetPasswordUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(credential)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.debug("Password establecida exitosamente para userId: {}", userId);
        } catch (WebClientResponseException e) {
            log.error("Error estableciendo password. Status: {}, Body: {}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new IntegracionKeycloakException(
                    "Error estableciendo password (status " + e.getStatusCode().value() + "): "
                            + e.getResponseBodyAsString(), e);
        }
    }

    private void asignarRolesPorDefecto(String accessToken, String userId) {
        if (!keycloakEnabled) {
            return;
        }

        try {
            String realmRolesUrl = String.format("%s/admin/realms/%s/roles", keycloakUrl, realm);
            List<Map<String, Object>> allRoles = webClient.get()
                    .uri(realmRolesUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .collectList()
                    .block();

            if (allRoles == null || allRoles.isEmpty()) {
                log.debug("No hay roles disponibles para asignar");
                return;
            }

            List<Map<String, Object>> defaultRoles = allRoles.stream()
                    .filter(role -> {
                        String name = (String) role.get("name");
                        return "offline_access".equals(name) || "uma_authorization".equals(name);
                    })
                    .toList();

            if (!defaultRoles.isEmpty()) {
                String rolesMappingUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm",
                        keycloakUrl, realm, userId);
                webClient.post()
                        .uri(rolesMappingUrl)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(defaultRoles)
                        .retrieve()
                        .toBodilessEntity()
                        .block();
                log.debug("Roles asignados: {}", defaultRoles.size());
            }
        } catch (Exception e) {
            log.warn("Error asignando roles (no crítico): {}", e.getMessage());
        }
    }

    public void actualizarPasswordUsuario(String email, String nuevaClave) {
        if (!keycloakEnabled) {
            log.info("Keycloak deshabilitado, omitiendo actualización de password para: {}", email);
            return;
        }

        log.info("Actualizando password para usuario: {}", email);
        String accessToken = getAdminAccessToken();

        // Buscar usuario por email
        String searchUrl = String.format("%s/admin/realms/%s/users?email=%s", keycloakUrl, realm, email);

        try {
            List<Map<String, Object>> users = webClient.get()
                    .uri(searchUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .collectList()
                    .block();

            if (users == null || users.isEmpty()) {
                throw new IntegracionKeycloakException("Usuario no encontrado en Keycloak: " + email);
            }

            String userId = (String) users.get(0).get("id");

            // Reset password
            String resetUrl = String.format("%s/admin/realms/%s/users/%s/reset-password", keycloakUrl, realm, userId);

            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", nuevaClave);
            credential.put("temporary", false);

            webClient.put()
                    .uri(resetUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(credential)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("✅ Contraseña actualizada en Keycloak para: {}", email);
        } catch (WebClientResponseException e) {
            log.error("Error actualizando contraseña. Status: {}, Body: {}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new IntegracionKeycloakException(
                    "Error actualizando contraseña en Keycloak (status " + e.getStatusCode().value() + "): "
                            + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Error inesperado actualizando contraseña: {}", e.getMessage(), e);
            throw new IntegracionKeycloakException("Error inesperado actualizando contraseña: " + e.getMessage(), e);
        }
    }
}