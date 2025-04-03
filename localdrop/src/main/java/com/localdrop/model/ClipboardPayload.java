package com.localdrop.model;

/**
 * 📄 ClipboardPayload.java
 *
 * 🔍 Purpose:
 * This class represents the structure of a clipboard message payload.
 * It is typically used when exchanging structured JSON data (instead of plain text) for clipboard sharing.
 *
 * 🧠 When & Why to Use:
 * - Use this class when you want to send clipboard data in a structured JSON format
 * - Example JSON:
 *   {
 *     "content": "Hello from peer!"
 *   }
 * - This is helpful if you want to expand the payload later (e.g., add sender name, timestamp, etc.)
 *
 * 🔧 How Spring Uses It:
 * - When receiving JSON via WebSocket or REST, Spring can automatically convert it into this object using Jackson
 * - `@RequestBody ClipboardPayload payload` would map incoming JSON into this class
 *
 * 🛠️ Fields:
 * - `content`: holds the actual text that was copied/shared from one peer to another
 */
public class ClipboardPayload
{
    private String content;
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
