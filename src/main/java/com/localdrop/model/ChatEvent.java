package com.localdrop.model;

import java.util.List;

public class ChatEvent
{
    private String type;
    private String messageId;
    private String senderId;
    private String senderName;
    private String content;
    private long timestamp;
    private Long lastSyncTimestamp;
    private String status;
    private Integer recipientCount;
    private Integer deliveredCount;
    private Integer readCount;
    private List<ChatMessage> messages;
    private Integer onlineCount;
    private List<String> onlineUsers;
    private String error;

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getMessageId()
    {
        return messageId;
    }

    public void setMessageId(String messageId)
    {
        this.messageId = messageId;
    }

    public String getSenderId()
    {
        return senderId;
    }

    public void setSenderId(String senderId)
    {
        this.senderId = senderId;
    }

    public String getSenderName()
    {
        return senderName;
    }

    public void setSenderName(String senderName)
    {
        this.senderName = senderName;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }

    public Long getLastSyncTimestamp()
    {
        return lastSyncTimestamp;
    }

    public void setLastSyncTimestamp(Long lastSyncTimestamp)
    {
        this.lastSyncTimestamp = lastSyncTimestamp;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getRecipientCount()
    {
        return recipientCount;
    }

    public void setRecipientCount(Integer recipientCount)
    {
        this.recipientCount = recipientCount;
    }

    public Integer getDeliveredCount()
    {
        return deliveredCount;
    }

    public void setDeliveredCount(Integer deliveredCount)
    {
        this.deliveredCount = deliveredCount;
    }

    public Integer getReadCount()
    {
        return readCount;
    }

    public void setReadCount(Integer readCount)
    {
        this.readCount = readCount;
    }

    public List<ChatMessage> getMessages()
    {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages)
    {
        this.messages = messages;
    }

    public Integer getOnlineCount()
    {
        return onlineCount;
    }

    public void setOnlineCount(Integer onlineCount)
    {
        this.onlineCount = onlineCount;
    }

    public List<String> getOnlineUsers()
    {
        return onlineUsers;
    }

    public void setOnlineUsers(List<String> onlineUsers)
    {
        this.onlineUsers = onlineUsers;
    }

    public String getError()
    {
        return error;
    }

    public void setError(String error)
    {
        this.error = error;
    }
}
