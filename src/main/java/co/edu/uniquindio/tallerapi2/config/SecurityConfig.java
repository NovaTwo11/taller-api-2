package co.edu.uniquindio.tallerapi2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos - Sesiones
                        .requestMatchers("/api/sesiones/**").permitAll()

                        // Endpoints públicos - Usuarios
                        .requestMatchers(
                                "/api/usuarios",
                                "/api/usuarios/recovery",
                                "/api/usuarios/reset-password",
                                "/api/usuarios/verify-token/**"
                        ).permitAll()

                        // Endpoints públicos - Password reset (nuevo!)
                        .requestMatchers("/api/password/**").permitAll()

                        // Endpoints legacy
                        .requestMatchers("/api/auth/**").permitAll()

                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}