package com.chatflow.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.time.Instant;

public class ChatMessage {

    @NotNull
    @JsonProperty("messageId")
    private String messageId;

    @NotNull
    @JsonProperty("userId")
    private String userId;

    @NotNull
    @Pattern(regexp = "^[a-zA-Z0-9]{3,20}$")
    private String username;

    @NotNull
    @Size(min = 1, max = 500)
    private String message;

    @NotNull
    private Instant timestamp;

    @NotNull
    private MessageType messageType;

    @NotNull
    @JsonProperty("roomId")
    private String roomId;

    // Constructors
    public ChatMessage() {}

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
}