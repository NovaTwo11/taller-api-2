package co.edu.uniquindio.tallerapi2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> auth
                // Endpoints que tu test-automation usa sin token
                .requestMatchers(HttpMethod.POST, "/api/usuarios", "/api/usuarios/").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/usuarios", "/api/usuarios/").permitAll()
                .requestMatchers("/api/usuarios/recovery", "/api/usuarios/reset-password").permitAll()
                .requestMatchers("/api/usuarios/verify-token/**").permitAll()
                .requestMatchers("/api/password/**").permitAll()
                .requestMatchers("/api/sesiones/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().permitAll()
        );

        // Importante: NO activar oauth2ResourceServer() por defecto
        return http.build();
    }
}