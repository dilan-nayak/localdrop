package com.localdrop.model;

/**
 * 📄 FileMetadata.java
 *
 * 🔍 Purpose:
 * Represents a simple data structure holding metadata about uploaded/encrypted files.
 * Used to send file information (name, size, timestamp) to the frontend without exposing actual file contents.
 *
 * 🧠 Why this is useful:
 * - Keeps API responses lightweight and focused
 * - Prevents exposing internal details (like file paths or keys)
 * - Separates metadata logic from actual file encryption/decryption
 *
 * 🧰 Fields:
 * - name: the original name of the uploaded file (without .data or .key extensions)
 * - size: file size in bytes
 * - uploadTime: when the file was saved, in milliseconds since epoch (can be formatted to human-readable date)
 *
 * 💡 This class is commonly used in REST API responses (like `GET /api/files/list`)
 * Spring automatically converts lists of this object to JSON.
 */
public class FileMetadata
{
    private String name;
    private long size;
    private long uploadTime;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public long getUploadTime() { return uploadTime; }
    public void setUploadTime(long uploadTime) { this.uploadTime = uploadTime; }
}
