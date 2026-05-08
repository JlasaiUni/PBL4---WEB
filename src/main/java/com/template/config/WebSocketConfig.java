package com.template.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.websocket.allowed-origins}")
    private String allowedOrigins;

    /**
     * Registra el endpoint WebSocket.
     * El cliente conecta con: new SockJS('/ws')
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();   // fallback para navegadores que no soporten WS
    }

    /**
     * Configura el broker de mensajes:
     *   - /topic  → broadcast (1 → todos los suscriptores)
     *   - /queue  → mensajes privados (1 → 1 usuario)
     *   - /app    → prefijo para @MessageMapping en controllers
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}