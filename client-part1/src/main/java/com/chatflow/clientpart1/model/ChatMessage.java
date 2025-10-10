package com.chatflow.clientpart1.model;

import jakarta.validation.constraints.*;
import java.time.Instant;

/**
 * Represents a chat message in the system.
 * <p>
 * Contains information about the sender, content, timestamp, message type, and room.
 * Includes validation constraints on fields.
 */
public class ChatMessage {

    /** Unique identifier for the message. */
    @NotNull
    private String messageID;

    /** ID of the user sending the message. Must be numeric and 1-6 digits. */
    @NotNull
    @Size(min = 1, max = 6)
    @Pattern(regexp = "^[1-9][0-9]*$", message = "UserId must contain only digits and not start with 0")
    private String userID;

    /** Username of the sender. 3-20 alphanumeric characters. */
    @NotNull
    @Pattern(regexp = "^[a-zA-Z0-9]{3,20}$", message = "Username must be 3-20 alphanumeric characters")
    private String username;

    /** Content of the message. 1-500 characters. */
    @NotNull
    @Size(min = 1, max = 500)
    private String message;

    /** Timestamp when the message was created. */
    @NotNull
    private Instant timestamp;

    /** Type of the message (e.g., TEXT, JOIN, LEAVE). */
    @NotNull
    private MessageType messageType;

    /** Room ID where the message was sent. 1-20. */
    @NotNull
    @Min(1)
    @Max(20)
    private String roomID;


    /**
     * Default constructor
     */
    public ChatMessage() {}


    /**
     * Constructs a ChatMessage with all required fields.
     *
     * @param messageID   unique message ID
     * @param userID      sender's user ID
     * @param username    sender's username
     * @param message     message content
     * @param messageType type of message
     * @param timestamp   message timestamp
     * @param roomID      room ID
     */
    public ChatMessage(String messageID,String userID, String username, String message, MessageType messageType,  Instant timestamp, String roomID) {
        this.messageID = messageID;
        this.userID = userID;
        this.username = username;
        this.message = message;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.roomID = roomID;
    }

    public String getRoomID() {
        return roomID;
    }

    public void setRoomID(String roomID) {
        this.roomID = roomID;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
}
