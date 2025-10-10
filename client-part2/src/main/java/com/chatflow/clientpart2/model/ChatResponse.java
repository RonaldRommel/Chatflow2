package com.chatflow.clientpart2.model;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Represents a response from the server for a chat message.
 * <p>
 * Contains the message, server timestamp, and status of the request.
 */
public class ChatResponse {

    /** Status of the message processing. */
    @NotNull
    private Status status;

    /** Timestamp when the server processed the message. */
    @NotNull
    private Instant serverTimestamp;

    /** The original chat message associated with this response. */
    @NotNull
    private ChatMessage message;

    /**
     * Constructs a ChatResponse with all required fields.
     *
     * @param status          status of the message
     * @param serverTimestamp server timestamp when message was processed
     * @param message         the associated ChatMessage
     */
    public ChatResponse(Status status, Instant serverTimestamp, ChatMessage message) {
        this.status = status;
        this.serverTimestamp = serverTimestamp;
        this.message = message;
    }
    /** Default constructor. */
    public ChatResponse(){}

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getServerTimestamp() {
        return serverTimestamp;
    }

    public void setServerTimestamp(Instant serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public void setMessage(ChatMessage message) {
        this.message = message;
    }
}
