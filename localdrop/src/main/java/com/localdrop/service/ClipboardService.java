package com.localdrop.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 📄 ClipboardService.java
 *
 * 🔍 Purpose:
 * This service handles real-time message broadcasting for clipboard sharing.
 * When one user sends a text message (clipboard content), this service delivers it to all other connected peers.
 *
 * 🧠 How it works:
 * - Maintains a synchronized set of all connected WebSocket sessions
 * - Adds the sender's session if it's not already tracked
 * - Sends the message to all other open sessions
 *
 * 🔧 Key Concepts:
 * - WebSocketSession: Represents each open connection (browser tab or peer device)
 * - TextMessage: A message sent over the WebSocket, containing plain text
 * - SynchronizedSet: Ensures thread-safe access to the session list
 * - Spring @Service: Marks this class as a service bean for Spring to manage
 *
 * 🔐 Bonus:
 * This service also supports system messages like "__REFRESH_FILES__"
 * which trigger file list updates on all peers, not just chat messages.
 */
@Service
public class ClipboardService
{
    // 🧠 Set of active WebSocket connections (thread-safe)
    private final Set< WebSocketSession > sessions = Collections.synchronizedSet( new HashSet<>() );

    /**
     * 📡 Broadcasts a clipboard message to all connected peers (except the sender)
     *
     * @param sender  The WebSocket session that sent the message (can be null for system broadcasts)
     * @param message The message to be sent to all peers
     */
    public void broadcastClipboard( WebSocketSession sender, TextMessage message )
    {
        if (sender != null)
        {
            sessions.add(sender);
        }
        synchronized ( sessions )
        {
            for ( WebSocketSession session : sessions )
            {
                if ( session != sender && session.isOpen() )
                {
                    try {
                        // 💬 Send to all open sessions, except sender (if sender is not null)
                        if ( session.isOpen() && ( sender == null || !session.getId().equals( sender.getId() ) ) )
                        {
                            session.sendMessage(message);
                        }
                    }
                    catch ( Exception ignored )
                    {
                        // Optionally log error — ignored here to prevent broadcast interruption
                    }
                }
            }
        }
    }
}
