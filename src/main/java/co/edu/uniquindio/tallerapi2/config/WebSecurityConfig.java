package co.edu.uniquindio.tallerapi2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@Configuration
public class WebSecurityConfig {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                // Swagger UI endpoints
                "/swagger-ui.html",
                "/swagger-ui/**",
                // OpenAPI docs
                "/v3/api-docs/**",
                "/v3/api-docs.yaml",
                // Recursos internos de Swagger
                "/swagger-resources/**",
                "/webjars/**"
        );
    }
}