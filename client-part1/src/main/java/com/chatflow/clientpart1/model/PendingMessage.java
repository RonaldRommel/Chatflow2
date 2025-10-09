package com.chatflow.clientpart1.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class PendingMessage {
    private ChatMessage message;
    private AtomicInteger attempts = new AtomicInteger(1);
    private volatile long lastAttemptTime;
    private volatile long nextRetryDelay=200;

    public PendingMessage(ChatMessage message) {
        this.message = message;
        this.lastAttemptTime = System.currentTimeMillis();
    }

    public ChatMessage getMessage() {
        return message;
    }

    public void setMessage(ChatMessage message) {
        this.message = message;
    }

    public AtomicInteger getAttempts() {
        return attempts;
    }

    public void setAttempts(AtomicInteger attempts) {
        this.attempts = attempts;
    }

    public long getLastAttemptTime() {
        return lastAttemptTime;
    }

    public void setLastAttemptTime(long lastAttemptTime) {
        this.lastAttemptTime = lastAttemptTime;
    }

    public long getNextRetryDelay() {
        return nextRetryDelay;
    }

    public void setNextRetryDelay(long nextRetryDelay) {
        this.nextRetryDelay = nextRetryDelay;
    }
}

