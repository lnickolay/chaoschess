package com.chaoschess.backend;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The HTTP endpoint used to initiate the WebSocket connection (ws://localhost:8080/ws)
        registry.addEndpoint("/ws")
                // Enables CORS for the frontend
                .setAllowedOriginPatterns("http://localhost:5173")
                // Adds fallback support for older browsers
                .withSockJS();
    }
}
