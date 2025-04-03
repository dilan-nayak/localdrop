package com.localdrop.controller;

import com.localdrop.service.ClipboardService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 📄 ClipboardController.java
 *
 * 🔍 Purpose:
 * This class handles real-time clipboard (text message) sharing via WebSocket.
 * It receives messages from connected peers and broadcasts them to others using the ClipboardService.
 *
 * 🧠 How it works:
 * - Each connected browser/client opens a WebSocket connection to `/clipboard`
 * - When a user sends text, it’s captured by `handleTextMessage(...)`
 * - The message is forwarded to `ClipboardService`, which broadcasts it to all connected sessions (peers)
 *
 * 🔧 Key Concepts:
 * - @Component: Marks this class as a Spring-managed bean so it can be registered in the WebSocket config
 * - WebSocketSession: Represents an open WebSocket connection
 * - TextMessage: Wrapper for a text message sent via WebSocket
 * - TextWebSocketHandler: A Spring class that allows handling WebSocket messages using simple overrides
 */
@Component
public class ClipboardController extends TextWebSocketHandler
{
    private final ClipboardService clipboardService;

    /**
     * 📦 Constructor injection of ClipboardService
     * Spring will automatically provide the ClipboardService instance
     */
    public ClipboardController(ClipboardService clipboardService) {
        this.clipboardService = clipboardService;
    }

    /**
     * 📩 Triggered when a WebSocket message is received from any connected client
     * @param session the client session that sent the message
     * @param message the text message to be forwarded to peers
     */
    @Override
    public void handleTextMessage( WebSocketSession session, TextMessage message )
    {
        clipboardService.broadcastClipboard( session, message );
    }
}
