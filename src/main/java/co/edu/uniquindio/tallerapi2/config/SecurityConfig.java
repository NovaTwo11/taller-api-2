package co.edu.uniquindio.tallerapi2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://keycloak:8080/realms/taller}")
    private String issuerUri;
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://keycloak:8080/realms/taller/protocol/openid-connect/certs}")
    private String jwkSetUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // ===================================================================
                        // ⚠️ CORRECCIÓN: REGLAS ESPECÍFICAS PRIMERO
                        // ===================================================================

                        // Endpoint de cambio de password - cualquier usuario autenticado
                        // Esta regla DEBE ir ANTES que la regla general de PUT /api/usuarios/**
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/password").hasAnyRole("admin", "user")

                        // ===================================================================
                        // REGLAS GENERALES DESPUÉS
                        // ===================================================================

                        // Endpoints de usuarios - solo admin puede crear/actualizar/eliminar
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").hasRole("admin")
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/**").hasRole("admin") // Ahora solo captura lo que no sea /password
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/**").hasRole("admin")
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/**").hasAnyRole("admin", "user")

                        // Endpoints de perfiles - cualquier usuario autenticado
                        .requestMatchers("/api/perfiles/**").hasAnyRole("admin", "user")

                        // Cualquier otra petición requiere autenticación
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter())
                        )
                );
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}