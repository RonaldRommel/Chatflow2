package com.chatflow.clientpart1.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class ChatMessage {

    @NotNull
    private String messageID;
    @NotNull
    @Size(min = 1, max = 6)
    @Pattern(regexp = "^[1-9][0-9]*$", message = "UserId must contain only digits and not start with 0")
    private String userID;

    @NotNull @Pattern(regexp = "^[a-zA-Z0-9]{3,20}$", message = "Username must be 3-20 alphanumeric characters")
    private String username;

    @NotNull @Size(min = 1, max = 500)
    private String message;

    @NotNull @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;

    @NotNull
    private MessageType messageType;

    public ChatMessage() {}

    public ChatMessage(String messageID,String userID, String username, String message, MessageType messageType,  LocalDateTime timestamp) {
        this.messageID = messageID;
        this.userID = userID;
        this.username = username;
        this.message = message;
        this.timestamp = timestamp;
        this.messageType = messageType;
    }

    public String getMessageID() {
        return messageID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
}
