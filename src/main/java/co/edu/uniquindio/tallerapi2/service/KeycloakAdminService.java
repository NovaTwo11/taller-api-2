package co.edu.uniquindio.tallerapi2.service;

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

    private final WebClient webClient;

    @Value("${keycloak.admin.url}")
    private String keycloakUrl;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    public KeycloakAdminService() {
        this.webClient = WebClient.builder().build();
    }

    private String getAdminAccessToken() {
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

            return (String) tokenResponse.get("access_token");
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Error obteniendo token de admin de Keycloak: " + e.getResponseBodyAsString(), e);
        }
    }

    public void crearUsuarioEnKeycloak(String nombre, String email, String password) {
        String accessToken = getAdminAccessToken();
        String usersUrl = String.format("%s/admin/realms/%s/users", keycloakUrl, realm);

        // 🔥 PASO 1: Crear usuario (SIN credentials)
        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("username", email);
        userPayload.put("email", email);
        userPayload.put("firstName", nombre);
        userPayload.put("enabled", true);
        userPayload.put("emailVerified", true);  // ✅ Evita "Account not fully set up"

        try {
            ResponseEntity<Void> response = webClient.post()
                    .uri(usersUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(userPayload)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            errorResponse -> errorResponse.bodyToMono(String.class).map(body ->
                                    new RuntimeException("Keycloak error al crear usuario: " + errorResponse.statusCode() + " - " + body)
                            )
                    )
                    .toBodilessEntity()
                    .block();

            // 🔥 PASO 2: Extraer userId del Location header
            String location = response.getHeaders().getFirst("Location");
            if (location == null) {
                throw new RuntimeException("No se pudo obtener el ID del usuario creado en Keycloak");
            }
            String userId = location.substring(location.lastIndexOf("/") + 1);

            // 🔥 PASO 3: Asignar contraseña vía reset-password
            String resetPasswordUrl = String.format("%s/admin/realms/%s/users/%s/reset-password", keycloakUrl, realm, userId);

            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", password);
            credential.put("temporary", false);  // ✅ No forzar cambio de contraseña

            webClient.put()
                    .uri(resetPasswordUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(credential)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            errorResponse -> errorResponse.bodyToMono(String.class).map(body ->
                                    new RuntimeException("Keycloak error al asignar contraseña: " + errorResponse.statusCode() + " - " + body)
                            )
                    )
                    .toBodilessEntity()
                    .block();

            // 🔥 PASO 4: Actualizar usuario para asegurar configuración completa
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
                    .onStatus(
                            status -> status.isError(),
                            errorResponse -> errorResponse.bodyToMono(String.class).map(body ->
                                    new RuntimeException("Keycloak error al actualizar usuario: " + errorResponse.statusCode() + " - " + body)
                            )
                    )
                    .toBodilessEntity()
                    .block();

            // 🔥 PASO 5: Asignar roles por defecto del realm
            try {
                // Obtener todos los roles disponibles en el realm
                String realmRolesUrl = String.format("%s/admin/realms/%s/roles", keycloakUrl, realm);

                List<Map<String, Object>> allRoles = webClient.get()
                        .uri(realmRolesUrl)
                        .header("Authorization", "Bearer " + accessToken)
                        .retrieve()
                        .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .collectList()
                        .block();

                // Filtrar solo los roles por defecto
                List<Map<String, Object>> defaultRoles = allRoles.stream()
                        .filter(role -> {
                            String roleName = (String) role.get("name");
                            return "offline_access".equals(roleName) ||
                                    "uma_authorization".equals(roleName);
                        })
                        .toList();

                // Asignar roles de realm al usuario
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

                    System.out.println("✅ Roles de realm asignados: " + defaultRoles.size());
                }

                // Asignar roles de cliente 'account'
                String accountClientUrl = String.format("%s/admin/realms/%s/clients", keycloakUrl, realm);

                List<Map<String, Object>> clients = webClient.get()
                        .uri(accountClientUrl)
                        .header("Authorization", "Bearer " + accessToken)
                        .retrieve()
                        .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .collectList()
                        .block();

                // Buscar cliente 'account'
                Map<String, Object> accountClient = clients.stream()
                        .filter(client -> "account".equals(client.get("clientId")))
                        .findFirst()
                        .orElse(null);

                if (accountClient != null) {
                    String accountClientId = (String) accountClient.get("id");

                    // Obtener roles del cliente 'account'
                    String accountRolesUrl = String.format("%s/admin/realms/%s/clients/%s/roles",
                            keycloakUrl, realm, accountClientId);

                    List<Map<String, Object>> accountRoles = webClient.get()
                            .uri(accountRolesUrl)
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                            .collectList()
                            .block();

                    List<Map<String, Object>> accountDefaultRoles = accountRoles.stream()
                            .filter(role -> {
                                String roleName = (String) role.get("name");
                                return "manage-account".equals(roleName) ||
                                        "view-profile".equals(roleName);
                            })
                            .toList();

                    if (!accountDefaultRoles.isEmpty()) {
                        String clientRolesMappingUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/clients/%s",
                                keycloakUrl, realm, userId, accountClientId);

                        webClient.post()
                                .uri(clientRolesMappingUrl)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(accountDefaultRoles)
                                .retrieve()
                                .toBodilessEntity()
                                .block();

                        System.out.println("✅ Roles de cliente 'account' asignados: " + accountDefaultRoles.size());
                    }
                }

            } catch (Exception e) {
                System.out.println("⚠️ Error asignando roles por defecto: " + e.getMessage());
            }

            System.out.println("✅ Usuario creado exitosamente en Keycloak: " + email + " (ID: " + userId + ")");

        } catch (WebClientResponseException e) {
            throw new RuntimeException("Error creando usuario en Keycloak: " + e.getResponseBodyAsString(), e);
        }
    }

    public void actualizarPasswordUsuario(String email, String nuevaClave) {
        String accessToken = getAdminAccessToken();

        // Buscar usuario por email
        String searchUrl = String.format("%s/admin/realms/%s/users?email=%s", keycloakUrl, realm, email);
        List<Map<String, Object>> users = webClient.get()
                .uri(searchUrl)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList()
                .block();

        if (users == null || users.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado en Keycloak: " + email);
        }

        String userId = (String) users.get(0).get("id");

        // Reset password
        String resetUrl = String.format("%s/admin/realms/%s/users/%s/reset-password", keycloakUrl, realm, userId);

        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", nuevaClave);
        credential.put("temporary", false);

        try {
            webClient.put()
                    .uri(resetUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(credential)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            System.out.println("✅ Contraseña actualizada en Keycloak para: " + email);
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Error actualizando contraseña en Keycloak: " + e.getResponseBodyAsString(), e);
        }
    }
}