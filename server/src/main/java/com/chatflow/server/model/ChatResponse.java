package com.chatflow.server.model;

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
    private ResponseType responseType;

    /** Timestamp when the server processed the message. */
    @NotNull
    private Instant serverTimestamp;

    /** The original chat message associated with this response. */
    @NotNull
    private ChatMessage message;

    /** Default constructor. */
    public ChatResponse() {}

    /**
     * Constructs a ChatResponse with all required fields.
     *
     * @param responseType          status of the message
     * @param serverTimestamp server timestamp when message was processed
     * @param message         the associated ChatMessage
     */
    public ChatResponse(ResponseType responseType, Instant serverTimestamp, ChatMessage message) {
        this.responseType = responseType;
        this.serverTimestamp = serverTimestamp;
        this.message = message;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
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
