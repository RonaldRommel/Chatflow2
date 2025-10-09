package com.chatflow.server.model;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class ChatResponse {
    @NotNull
    private ResponseType responseType;
    @NotNull
    private LocalDateTime serverTimestamp;
    @NotNull
    private ChatMessage message;
    public ChatResponse(ResponseType responseType, LocalDateTime serverTimestamp, ChatMessage message) {
        this.responseType = responseType;
        this.serverTimestamp = serverTimestamp;
        this.message = message;
    }
    public ChatResponse(){}

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }

    public LocalDateTime getServerTimestamp() {
        return serverTimestamp;
    }

    public void setServerTimestamp(LocalDateTime serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public void setMessage(ChatMessage message) {
        this.message = message;
    }
}
