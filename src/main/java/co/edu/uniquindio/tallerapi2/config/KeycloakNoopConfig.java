package co.edu.uniquindio.tallerapi2.config;

import co.edu.uniquindio.tallerapi2.service.KeycloakAdminService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra un KeycloakAdminService "no-op" cuando app.keycloak.enabled=false.
 * De este modo, la API no intenta conectarse a Keycloak durante la creación de usuarios
 * y se evitan 500 por integraciones caídas, sin tocar controladores ni servicios.
 */
@Configuration
public class KeycloakNoopConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.keycloak", name = "enabled", havingValue = "false", matchIfMissing = true)
    public KeycloakAdminService keycloakAdminServiceNoop() {
        return new KeycloakAdminService() {
            @Override
            public void crearUsuarioEnKeycloak(String nombre, String email, String password) {
                // No-Op
            }

            @Override
            public void actualizarPasswordUsuario(String email, String nuevaClave) {
                // No-Op
            }
        };
    }
}