package com.localdrop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.localdrop.model.ChatEvent;
import com.localdrop.model.ChatMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ClipboardService
{
    private static final String TYPE_CHAT_MESSAGE = "chat_message";
    private static final String TYPE_DELIVERY_ACK = "delivery_ack";
    private static final String TYPE_READ_ACK = "read_ack";
    private static final String TYPE_SYNC_REQUEST = "sync_request";
    private static final String TYPE_SYNC_RESPONSE = "sync_response";
    private static final String TYPE_PRESENCE_UPDATE = "presence_update";
    private static final String TYPE_SYSTEM_REFRESH_FILES = "system_refresh_files";
    private static final String TYPE_ERROR = "error";
    private static final int MAX_HISTORY_SIZE = 300;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<WebSocketSession, String> sessionToClientId = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> clientIdToSession = new ConcurrentHashMap<>();
    private final Map<String, String> clientIdToName = new ConcurrentHashMap<>();
    private final Set<String> knownClientIds = ConcurrentHashMap.newKeySet();
    private final LinkedList<StoredMessage> messageHistory = new LinkedList<>();
    private final Map<String, StoredMessage> messageIndex = new ConcurrentHashMap<>();

    public void registerSession(WebSocketSession session)
    {
        if (session != null)
        {
            sessions.add(session);
        }
    }

    public void unregisterSession(WebSocketSession session)
    {
        if (session == null)
        {
            return;
        }
        sessions.remove(session);
        String clientId = sessionToClientId.remove(session);
        if (clientId != null)
        {
            WebSocketSession mapped = clientIdToSession.get(clientId);
            if (mapped == session)
            {
                clientIdToSession.remove(clientId);
            }
        }
        broadcastPresenceUpdate();
    }

    public void handleSocketPayload(WebSocketSession session, String payload)
    {
        if (payload == null || payload.isBlank())
        {
            return;
        }
        registerSession(session);

        ChatEvent event = parseEvent(payload);
        if (event == null)
        {
            handleLegacyChatMessage(session, payload.trim());
            return;
        }

        String type = event.getType();
        if (TYPE_SYNC_REQUEST.equals(type))
        {
            handleSyncRequest(session, event);
            return;
        }
        if (TYPE_CHAT_MESSAGE.equals(type))
        {
            handleChatMessage(session, event);
            return;
        }
        if (TYPE_DELIVERY_ACK.equals(type))
        {
            handleDeliveryAck(session, event);
            return;
        }
        if (TYPE_READ_ACK.equals(type))
        {
            handleReadAck(session, event);
            return;
        }
        sendError(session, "Unsupported event type: " + type);
    }

    public List<ChatMessage> getMessageHistory()
    {
        synchronized (messageHistory)
        {
            List<ChatMessage> snapshots = new ArrayList<>(messageHistory.size());
            for (StoredMessage message : messageHistory)
            {
                snapshots.add(toChatMessage(message));
            }
            return snapshots;
        }
    }

    public List<ChatMessage> getMessagesSince(long sinceTimestamp)
    {
        synchronized (messageHistory)
        {
            List<ChatMessage> snapshots = new ArrayList<>();
            for (StoredMessage message : messageHistory)
            {
                if (message.timestamp > sinceTimestamp)
                {
                    snapshots.add(toChatMessage(message));
                }
            }
            return snapshots;
        }
    }

    public void broadcastFileRefresh()
    {
        ChatEvent event = new ChatEvent();
        event.setType(TYPE_SYSTEM_REFRESH_FILES);
        event.setTimestamp(Instant.now().toEpochMilli());
        broadcastEvent(event, null);
    }

    private void handleSyncRequest(WebSocketSession session, ChatEvent event)
    {
        String clientId = resolveClientId(session, event);
        String senderName = resolveSenderName(clientId, event.getSenderName());
        registerIdentity(session, clientId, senderName);

        long since = event.getLastSyncTimestamp() == null ? 0L : event.getLastSyncTimestamp();
        ChatEvent response = new ChatEvent();
        response.setType(TYPE_SYNC_RESPONSE);
        response.setTimestamp(Instant.now().toEpochMilli());
        response.setMessages(getMessagesSince(since));
        sendEvent(session, response);

        broadcastPresenceUpdate();
    }

    private void handleChatMessage(WebSocketSession session, ChatEvent event)
    {
        String content = event.getContent() == null ? "" : event.getContent().trim();
        if (content.isBlank())
        {
            sendError(session, "Message content is empty.");
            return;
        }

        String senderId = resolveClientId(session, event);
        String senderName = resolveSenderName(senderId, event.getSenderName());
        registerIdentity(session, senderId, senderName);

        StoredMessage message = new StoredMessage();
        message.messageId = isBlank(event.getMessageId()) ? UUID.randomUUID().toString() : event.getMessageId().trim();
        message.senderId = senderId;
        message.senderName = senderName;
        message.content = content;
        message.timestamp = Instant.now().toEpochMilli();
        message.recipientIds.addAll(getTrackedRecipientIds(senderId));

        addMessage(message);

        ChatEvent outgoing = new ChatEvent();
        outgoing.setType(TYPE_CHAT_MESSAGE);
        outgoing.setMessageId(message.messageId);
        outgoing.setSenderId(message.senderId);
        outgoing.setSenderName(message.senderName);
        outgoing.setContent(message.content);
        outgoing.setTimestamp(message.timestamp);
        outgoing.setRecipientCount(message.recipientIds.size());
        outgoing.setDeliveredCount(message.deliveredBy.size());
        outgoing.setReadCount(message.readBy.size());
        outgoing.setStatus(computeStatus(message));

        for (String recipientId : message.recipientIds)
        {
            WebSocketSession recipientSession = clientIdToSession.get(recipientId);
            sendEvent(recipientSession, outgoing);
        }

        sendStatusToSender(message, TYPE_DELIVERY_ACK);
    }

    private void handleDeliveryAck(WebSocketSession session, ChatEvent event)
    {
        String messageId = normalize(event.getMessageId());
        if (messageId == null)
        {
            return;
        }

        StoredMessage message = messageIndex.get(messageId);
        if (message == null)
        {
            return;
        }

        String clientId = resolveClientId(session, event);
        registerIdentity(session, clientId, resolveSenderName(clientId, event.getSenderName()));

        if (!message.recipientIds.contains(clientId))
        {
            return;
        }

        if (message.deliveredBy.add(clientId))
        {
            sendStatusToSender(message, TYPE_DELIVERY_ACK);
        }
    }

    private void handleReadAck(WebSocketSession session, ChatEvent event)
    {
        String messageId = normalize(event.getMessageId());
        if (messageId == null)
        {
            return;
        }

        StoredMessage message = messageIndex.get(messageId);
        if (message == null)
        {
            return;
        }

        String clientId = resolveClientId(session, event);
        registerIdentity(session, clientId, resolveSenderName(clientId, event.getSenderName()));

        if (!message.recipientIds.contains(clientId))
        {
            return;
        }

        boolean changed = message.deliveredBy.add(clientId);
        if (message.readBy.add(clientId))
        {
            changed = true;
        }

        if (changed)
        {
            sendStatusToSender(message, TYPE_READ_ACK);
        }
    }

    private void sendStatusToSender(StoredMessage message, String type)
    {
        WebSocketSession senderSession = clientIdToSession.get(message.senderId);
        if (senderSession == null)
        {
            return;
        }

        ChatEvent ack = new ChatEvent();
        ack.setType(type);
        ack.setMessageId(message.messageId);
        ack.setTimestamp(Instant.now().toEpochMilli());
        ack.setRecipientCount(message.recipientIds.size());
        ack.setDeliveredCount(message.deliveredBy.size());
        ack.setReadCount(message.readBy.size());
        ack.setStatus(computeStatus(message));
        sendEvent(senderSession, ack);
    }

    private void addMessage(StoredMessage message)
    {
        synchronized (messageHistory)
        {
            messageHistory.add(message);
            messageIndex.put(message.messageId, message);
            while (messageHistory.size() > MAX_HISTORY_SIZE)
            {
                StoredMessage removed = messageHistory.removeFirst();
                messageIndex.remove(removed.messageId);
            }
        }
    }

    private void handleLegacyChatMessage(WebSocketSession session, String payload)
    {
        ChatEvent legacy = new ChatEvent();
        legacy.setType(TYPE_CHAT_MESSAGE);
        legacy.setSenderId(sessionToClientId.get(session));
        legacy.setSenderName(resolveSenderName(legacy.getSenderId(), null));
        legacy.setContent(payload);
        handleChatMessage(session, legacy);
    }

    private ChatEvent parseEvent(String payload)
    {
        try
        {
            return objectMapper.readValue(payload, ChatEvent.class);
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private void registerIdentity(WebSocketSession session, String clientId, String senderName)
    {
        registerSession(session);
        sessionToClientId.put(session, clientId);
        clientIdToSession.put(clientId, session);
        clientIdToName.put(clientId, senderName);
        knownClientIds.add(clientId);
    }

    private String resolveClientId(WebSocketSession session, ChatEvent event)
    {
        if (event != null && !isBlank(event.getSenderId()))
        {
            return event.getSenderId().trim();
        }
        String current = sessionToClientId.get(session);
        if (!isBlank(current))
        {
            return current;
        }
        return "client-" + session.getId();
    }

    private String resolveSenderName(String clientId, String requestedName)
    {
        if (!isBlank(requestedName))
        {
            return requestedName.trim();
        }
        if (!isBlank(clientId) && clientIdToName.containsKey(clientId))
        {
            return clientIdToName.get(clientId);
        }
        if (isBlank(clientId))
        {
            return "User";
        }
        String suffix = clientId.length() <= 4 ? clientId : clientId.substring(clientId.length() - 4);
        return "User-" + suffix;
    }

    private List<String> getTrackedRecipientIds(String senderId)
    {
        List<String> recipients = new ArrayList<>();
        for (String clientId : knownClientIds)
        {
            if (!clientId.equals(senderId))
            {
                recipients.add(clientId);
            }
        }
        return recipients;
    }

    private void broadcastPresenceUpdate()
    {
        List<String> online = new ArrayList<>();
        for (Map.Entry<String, WebSocketSession> entry : clientIdToSession.entrySet())
        {
            WebSocketSession session = entry.getValue();
            if (session != null && session.isOpen())
            {
                online.add(clientIdToName.getOrDefault(entry.getKey(), entry.getKey()));
            }
        }
        Collections.sort(online);

        ChatEvent event = new ChatEvent();
        event.setType(TYPE_PRESENCE_UPDATE);
        event.setTimestamp(Instant.now().toEpochMilli());
        event.setOnlineCount(online.size());
        event.setOnlineUsers(online);
        broadcastEvent(event, null);
    }

    private void sendError(WebSocketSession session, String message)
    {
        ChatEvent event = new ChatEvent();
        event.setType(TYPE_ERROR);
        event.setTimestamp(Instant.now().toEpochMilli());
        event.setError(message);
        sendEvent(session, event);
    }

    private void broadcastEvent(ChatEvent event, WebSocketSession exclude)
    {
        for (WebSocketSession session : sessions)
        {
            if (session != exclude)
            {
                sendEvent(session, event);
            }
        }
    }

    private void sendEvent(WebSocketSession session, ChatEvent event)
    {
        if (session == null || !session.isOpen())
        {
            return;
        }
        try
        {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
        }
        catch (Exception ignored)
        {
            unregisterSession(session);
        }
    }

    private ChatMessage toChatMessage(StoredMessage message)
    {
        ChatMessage snapshot = new ChatMessage();
        snapshot.setMessageId(message.messageId);
        snapshot.setSenderId(message.senderId);
        snapshot.setSenderName(message.senderName);
        snapshot.setContent(message.content);
        snapshot.setTimestamp(message.timestamp);
        snapshot.setRecipientCount(message.recipientIds.size());
        snapshot.setDeliveredCount(message.deliveredBy.size());
        snapshot.setReadCount(message.readBy.size());
        snapshot.setStatus(computeStatus(message));
        return snapshot;
    }

    private String computeStatus(StoredMessage message)
    {
        int recipients = message.recipientIds.size();
        if (recipients == 0)
        {
            return "sent";
        }
        if (message.readBy.size() >= recipients)
        {
            return "read";
        }
        if (message.deliveredBy.size() >= recipients)
        {
            return "delivered";
        }
        return "sent";
    }

    private String normalize(String value)
    {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value)
    {
        return value == null || value.isBlank();
    }

    private static class StoredMessage
    {
        private String messageId;
        private String senderId;
        private String senderName;
        private String content;
        private long timestamp;
        private final Set<String> recipientIds = ConcurrentHashMap.newKeySet();
        private final Set<String> deliveredBy = ConcurrentHashMap.newKeySet();
        private final Set<String> readBy = ConcurrentHashMap.newKeySet();
    }
}
