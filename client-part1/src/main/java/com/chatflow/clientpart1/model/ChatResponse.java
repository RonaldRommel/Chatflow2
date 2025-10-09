package com.chatflow.clientpart1.model;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class ChatResponse {
    @NotNull
    private Status status;
    @NotNull
    private LocalDateTime serverTimestamp;
    @NotNull
    private ChatMessage message;
    public ChatResponse(Status status, LocalDateTime serverTimestamp, ChatMessage message) {
        this.status = status;
        this.serverTimestamp = serverTimestamp;
        this.message = message;
    }
    public ChatResponse(){}

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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
