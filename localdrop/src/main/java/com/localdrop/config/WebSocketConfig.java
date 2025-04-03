package com.localdrop.config;

import com.localdrop.controller.ClipboardController;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 📄 WebSocketConfig.java
 *
 * 🔍 Purpose:
 * This class configures WebSocket support in the Spring Boot application.
 * It tells Spring to treat certain endpoints as WebSocket endpoints and route incoming messages to handlers like ClipboardController.
 *
 * 🧠 How it works:
 * - WebSocket connections are persistent, bi-directional communication channels between client and server
 * - Spring needs to know which classes handle which WebSocket URLs — that's what this class defines
 * - Any client that connects to `/clipboard` via WebSocket is now managed by ClipboardController
 *
 * 🔧 Key Concepts:
 * - @Configuration: Marks this class as a Spring configuration class (like a settings module)
 * - @EnableWebSocket: Enables WebSocket functionality in the app
 * - WebSocketConfigurer: Interface to implement custom WebSocket handler registration
 * - WebSocketHandlerRegistry: Used to define endpoint mappings for WebSocket handlers
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer
{
    private final ClipboardController clipboardController;

    /**
     * 💡 Constructor injection of ClipboardController
     * Spring automatically injects the handler used to process messages at /clipboard
     */
    public WebSocketConfig(ClipboardController clipboardController)
    {
        this.clipboardController = clipboardController;
    }

    /**
     * 🧭 Registers WebSocket endpoint and maps it to our handler
     * @param registry the WebSocket registry where endpoints are added
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry)
    {
        registry.addHandler(clipboardController, "/clipboard")
                .setAllowedOrigins("*"); // Allow connections from any LAN origin
    }
}

