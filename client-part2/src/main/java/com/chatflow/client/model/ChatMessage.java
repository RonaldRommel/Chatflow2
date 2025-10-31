package com.chatflow.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public class ChatMessage {

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("userId")
    private String userId;

    private String username;
    private String message;

    @JsonProperty("roomId")
    private String roomId;

    private MessageType messageType;
    private Instant timestamp;

    public ChatMessage() {
        this.messageId = UUID.randomUUID().toString();
    }

    public ChatMessage(String userId, String username, String message, String roomId,
                       MessageType messageType, Instant timestamp) {
        this.messageId = UUID.randomUUID().toString();
        this.userId = userId;
        this.username = username;
        this.message = message;
        this.roomId = roomId;
        this.messageType = messageType;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}