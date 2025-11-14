package co.edu.uniquindio.tallerapi2.service;

import co.edu.uniquindio.tallerapi2.config.AppRabbitMQProperties;
import co.edu.uniquindio.tallerapi2.dto.events.PasswordActualizadoEvent;
import co.edu.uniquindio.tallerapi2.dto.events.PasswordResetSolicitadoEvent;
import co.edu.uniquindio.tallerapi2.dto.events.SesionIniciadaEvent;
import co.edu.uniquindio.tallerapi2.dto.events.UsuarioCreadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import co.edu.uniquindio.tallerapi2.dto.events.UsuarioEliminadoEvent;

@Service
public class EventPublisherService {
    private static final Logger log = LoggerFactory.getLogger(EventPublisherService.class);

    private final RabbitTemplate rabbitTemplate;
    private final AppRabbitMQProperties properties;

    public EventPublisherService(RabbitTemplate rabbitTemplate, AppRabbitMQProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    /**
     * Publica evento de usuario creado
     */
    public void publishUsuarioCreado(UsuarioCreadoEvent event) {
        try {
            log.info("Publicando evento de usuario creado: {}", event);

            rabbitTemplate.convertAndSend(
                    properties.getExchange(),
                    properties.getUsuarios().getRoutingKey(),
                    event
            );

            log.info("Evento publicado exitosamente para usuario: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Error al publicar evento de usuario creado: {}", e.getMessage(), e);
        }
    }

    /**
     * Publica evento de usuario eliminado
     */
    public void publishUsuarioEliminado(UsuarioEliminadoEvent event) {
        try {
            log.info("Publicando evento de usuario eliminado: {}", event);
            rabbitTemplate.convertAndSend(
                    properties.getExchange(),
                    properties.getUsuarios().getEliminados().getRoutingKey(), // Usamos el nuevo routing key
                    event
            );
            log.info("Evento de eliminación publicado exitosamente para usuario: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Error al publicar evento de usuario eliminado: {}", e.getMessage(), e);
        }
    }


    /**
     * Publica evento de sesión iniciada
     */
    public void publishSesionIniciada(SesionIniciadaEvent event) {
        rabbitTemplate.convertAndSend(
                properties.getExchange(),
                properties.getSesiones().getRoutingKey(),
                event
        );
    }

    /**
     * Publica evento de password reset solicitado
     */
    public void publishPasswordResetSolicitado(PasswordResetSolicitadoEvent event) {
        rabbitTemplate.convertAndSend(
                properties.getExchange(),
                "password.reset.requested",
                event
        );
    }

    /**
     * Publica evento de password actualizado
     */
    public void publishPasswordActualizado(PasswordActualizadoEvent event) {
        rabbitTemplate.convertAndSend(
                properties.getExchange(),
                "password.updated",
                event
        );
    }
}