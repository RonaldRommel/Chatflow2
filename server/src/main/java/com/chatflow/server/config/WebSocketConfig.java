package com.chatflow.server.config;

import com.chatflow.server.handler.WebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configures WebSocket support for the chat server.
 * <p>
 * Registers the WebSocketHandler for the "/chat/**" endpoint and allows all origins.
 */

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;

    /**
     * Constructs the WebSocketConfig with the specified handler.
     *
     * @param webSocketHandler the handler managing WebSocket sessions
     */
    public WebSocketConfig(WebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * Registers WebSocket handlers and maps them to endpoint paths.
     *
     * @param registry the WebSocketHandlerRegistry to register handlers with
     */

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/chat/**")
                .setAllowedOrigins("*");
    }
}