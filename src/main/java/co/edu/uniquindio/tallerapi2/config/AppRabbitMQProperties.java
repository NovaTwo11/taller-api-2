package co.edu.uniquindio.tallerapi2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.rabbitmq")
public class AppRabbitMQProperties {

    private String exchange;
    private Usuarios usuarios = new Usuarios();
    private Sesiones sesiones = new Sesiones();
    private Password password = new Password();

    public static class Usuarios {
        private String queue;
        private String routingKey;

        // --- INICIO DE LA CORRECCIÓN ---
        // 1. Campo para la nueva configuración
        private Eliminados eliminados = new Eliminados();

        // 2. Clase interna para la nueva configuración
        public static class Eliminados {
            private String queue;
            private String routingKey;

            public String getQueue() { return queue; }
            public void setQueue(String queue) { this.queue = queue; }
            public String getRoutingKey() { return routingKey; }
            public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
        }

        // 3. Getters y Setters para el nuevo campo (ESTO ES LO QUE FALTABA)
        public Eliminados getEliminados() { return eliminados; }
        public void setEliminados(Eliminados eliminados) { this.eliminados = eliminados; }
        // --- FIN DE LA CORRECCIÓN ---

        // (Getters y Setters originales de Usuarios)
        public String getQueue() {
            return queue;
        }
        public void setQueue(String queue) {
            this.queue = queue;
        }
        public String getRoutingKey() {
            return routingKey;
        }
        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }
    }


    public static class Sesiones {
        private String queue;
        private String routingKey;
        public String getQueue() { return queue; }
        public void setQueue(String queue) { this.queue = queue; }
        public String getRoutingKey() { return routingKey; }
        public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
    }

    public static class Password {
        private String queue;
        private String routingKey;
        public String getQueue() { return queue; }
        public void setQueue(String queue) { this.queue = queue; }
        public String getRoutingKey() { return routingKey; }
        public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
    }

    // (Getters y setters principales sin cambios)
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public Usuarios getUsuarios() { return usuarios; }
    public void setUsuarios(Usuarios usuarios) { this.usuarios = usuarios; }
    public Sesiones getSesiones() { return sesiones; }
    public void setSesiones(Sesiones sesiones) { this.sesiones = sesiones; }
    public Password getPassword() { return password; }
    public void setPassword(Password password) { this.password = password; }
}