package co.edu.uniquindio.tallerapi2.config;

import co.edu.uniquindio.tallerapi2.dto.events.UsuarioCreadoEvent;
import co.edu.uniquindio.tallerapi2.dto.events.SesionIniciadaEvent;
import co.edu.uniquindio.tallerapi2.dto.events.PasswordResetSolicitadoEvent;
import co.edu.uniquindio.tallerapi2.dto.events.PasswordActualizadoEvent;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import co.edu.uniquindio.tallerapi2.dto.events.UsuarioEliminadoEvent;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    private final AppRabbitMQProperties properties;

    public RabbitMQConfig(AppRabbitMQProperties properties) {
        this.properties = properties;
    }

    // ========== EXCHANGE ==========
    @Bean
    public TopicExchange appExchange() {
        return new TopicExchange(properties.getExchange());
    }

    // ========== QUEUES ==========
    @Bean
    public Queue usuariosQueue() {
        return new Queue(properties.getUsuarios().getQueue(), true);
    }

    @Bean
    public Queue sesionesQueue() {
        return new Queue(properties.getSesiones().getQueue(), true);
    }

    @Bean
    public Queue passwordQueue() {
        return new Queue(properties.getPassword().getQueue(), true);
    }

    @Bean
    public Queue usuariosEliminadosQueue() {return new Queue(properties.getUsuarios().getEliminados().getQueue(), true);
    }

    // ========== BINDINGS ==========
    @Bean
    public Binding usuariosBinding(Queue usuariosQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(usuariosQueue)
                .to(appExchange)
                .with(properties.getUsuarios().getRoutingKey());
    }

    @Bean
    public Binding sesionesBinding(Queue sesionesQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(sesionesQueue)
                .to(appExchange)
                .with(properties.getSesiones().getRoutingKey());
    }

    @Bean
    public Binding passwordBinding(Queue passwordQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(passwordQueue)
                .to(appExchange)
                .with(properties.getPassword().getRoutingKey());
    }

    @Bean
    public Binding usuariosEliminadosBinding(Queue usuariosEliminadosQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(usuariosEliminadosQueue)
                .to(appExchange)
                .with(properties.getUsuarios().getEliminados().getRoutingKey());
    }

    // ========== MESSAGE CONVERTER ==========
    /**
     * Converter que serializa los objetos en JSON con mapeo de clases
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

        // 🔥 Configurar mapeo de clases para deserialización automática
        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("co.edu.uniquindio.tallerapi2.dto.events.UsuarioCreadoEvent", UsuarioCreadoEvent.class);
        idClassMapping.put("co.edu.uniquindio.tallerapi2.dto.events.SesionIniciadaEvent", SesionIniciadaEvent.class);
        idClassMapping.put("co.edu.uniquindio.tallerapi2.dto.events.PasswordResetSolicitadoEvent", PasswordResetSolicitadoEvent.class);
        idClassMapping.put("co.edu.uniquindio.tallerapi2.dto.events.PasswordActualizadoEvent", PasswordActualizadoEvent.class);
        idClassMapping.put("co.edu.uniquindio.tallerapi2.dto.events.UsuarioEliminadoEvent", UsuarioEliminadoEvent.class);

        classMapper.setIdClassMapping(idClassMapping);
        converter.setClassMapper(classMapper);

        return converter;
    }

    // ========== RABBIT TEMPLATE ==========
    /**
     * RabbitTemplate configurado con el converter JSON
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    // ========== LISTENER FACTORY ==========
    /**
     * Factory para listeners con converter JSON
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                               MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}