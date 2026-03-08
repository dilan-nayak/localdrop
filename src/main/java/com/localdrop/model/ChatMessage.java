package com.localdrop.model;

public class ChatMessage
{
    private String messageId;
    private String senderId;
    private String senderName;
    private String content;
    private long timestamp;
    private int recipientCount;
    private int deliveredCount;
    private int readCount;
    private String status;

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

    public int getRecipientCount()
    {
        return recipientCount;
    }

    public void setRecipientCount(int recipientCount)
    {
        this.recipientCount = recipientCount;
    }

    public int getDeliveredCount()
    {
        return deliveredCount;
    }

    public void setDeliveredCount(int deliveredCount)
    {
        this.deliveredCount = deliveredCount;
    }

    public int getReadCount()
    {
        return readCount;
    }

    public void setReadCount(int readCount)
    {
        this.readCount = readCount;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
