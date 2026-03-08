package com.localdrop.service;

import com.localdrop.model.ChatMessage;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipboardServiceTest
{
    @Test
    void shouldTransitionStatusSentDeliveredReadWithIdempotentAcks()
    {
        ClipboardService service = new ClipboardService();
        WebSocketSession alice = session("alice-session", new AtomicBoolean(true), new ArrayList<>());
        WebSocketSession bob = session("bob-session", new AtomicBoolean(true), new ArrayList<>());

        service.registerSession(alice);
        service.registerSession(bob);

        service.handleSocketPayload(alice, syncRequest("alice", "Alice", 0));
        service.handleSocketPayload(bob, syncRequest("bob", "Bob", 0));
        service.handleSocketPayload(alice, chatMessage("msg-1", "alice", "Alice", "hello"));

        service.handleSocketPayload(bob, deliveryAck("msg-1", "bob", "Bob"));
        service.handleSocketPayload(bob, deliveryAck("msg-1", "bob", "Bob"));
        service.handleSocketPayload(bob, readAck("msg-1", "bob", "Bob"));
        service.handleSocketPayload(bob, readAck("msg-1", "bob", "Bob"));

        ChatMessage message = service.getMessageHistory().get(0);
        assertEquals("msg-1", message.getMessageId());
        assertEquals(1, message.getRecipientCount());
        assertEquals(1, message.getDeliveredCount());
        assertEquals(1, message.getReadCount());
        assertEquals("read", message.getStatus());
    }

    @Test
    void shouldReturnOnlyMissedMessagesForSyncCursor()
            throws InterruptedException
    {
        ClipboardService service = new ClipboardService();
        WebSocketSession alice = session("alice-session", new AtomicBoolean(true), new ArrayList<>());
        WebSocketSession bob = session("bob-session", new AtomicBoolean(true), new ArrayList<>());

        service.handleSocketPayload(alice, syncRequest("alice", "Alice", 0));
        service.handleSocketPayload(bob, syncRequest("bob", "Bob", 0));
        service.handleSocketPayload(alice, chatMessage("msg-1", "alice", "Alice", "first"));

        long firstTimestamp = service.getMessageHistory().get(0).getTimestamp();
        Thread.sleep(2);
        service.handleSocketPayload(alice, chatMessage("msg-2", "alice", "Alice", "second"));

        List<ChatMessage> missed = service.getMessagesSince(firstTimestamp);
        assertEquals(1, missed.size());
        assertEquals("msg-2", missed.get(0).getMessageId());
    }

    @Test
    void shouldStopTargetingDisconnectedSessions()
    {
        ClipboardService service = new ClipboardService();
        AtomicBoolean aliceOpen = new AtomicBoolean(true);
        AtomicBoolean bobOpen = new AtomicBoolean(true);
        WebSocketSession alice = session("alice-session", aliceOpen, new ArrayList<>());
        WebSocketSession bob = session("bob-session", bobOpen, new ArrayList<>());

        service.handleSocketPayload(alice, syncRequest("alice", "Alice", 0));
        service.handleSocketPayload(bob, syncRequest("bob", "Bob", 0));
        bobOpen.set(false);
        service.unregisterSession(bob);

        service.handleSocketPayload(alice, chatMessage("msg-3", "alice", "Alice", "ping"));
        ChatMessage message = service.getMessageHistory().get(0);

        assertEquals(1, message.getRecipientCount());
        assertEquals("sent", message.getStatus());
        assertTrue(message.getDeliveredCount() == 0 && message.getReadCount() == 0);
    }

    private WebSocketSession session(String id, AtomicBoolean isOpen, List<TextMessage> sentMessages)
    {
        return (WebSocketSession) Proxy.newProxyInstance(
                WebSocketSession.class.getClassLoader(),
                new Class[]{WebSocketSession.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getId".equals(name))
                    {
                        return id;
                    }
                    if ("isOpen".equals(name))
                    {
                        return isOpen.get();
                    }
                    if ("sendMessage".equals(name))
                    {
                        if (args != null && args.length == 1 && args[0] instanceof TextMessage textMessage)
                        {
                            sentMessages.add(textMessage);
                        }
                        return null;
                    }
                    if ("equals".equals(name))
                    {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(name))
                    {
                        return System.identityHashCode(proxy);
                    }
                    return null;
                }
        );
    }

    private String syncRequest(String senderId, String senderName, long since)
    {
        return "{\"type\":\"sync_request\",\"senderId\":\"" + senderId + "\",\"senderName\":\"" + senderName + "\",\"lastSyncTimestamp\":" + since + "}";
    }

    private String chatMessage(String messageId, String senderId, String senderName, String content)
    {
        return "{\"type\":\"chat_message\",\"messageId\":\"" + messageId + "\",\"senderId\":\"" + senderId + "\",\"senderName\":\"" + senderName + "\",\"content\":\"" + content + "\"}";
    }

    private String deliveryAck(String messageId, String senderId, String senderName)
    {
        return "{\"type\":\"delivery_ack\",\"messageId\":\"" + messageId + "\",\"senderId\":\"" + senderId + "\",\"senderName\":\"" + senderName + "\"}";
    }

    private String readAck(String messageId, String senderId, String senderName)
    {
        return "{\"type\":\"read_ack\",\"messageId\":\"" + messageId + "\",\"senderId\":\"" + senderId + "\",\"senderName\":\"" + senderName + "\"}";
    }
}
