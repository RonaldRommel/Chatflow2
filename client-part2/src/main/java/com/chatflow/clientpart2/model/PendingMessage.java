package com.chatflow.clientpart2.model;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a message pending acknowledgment from the server.
 * <p>
 * Tracks the message itself, number of send attempts, last attempt time,
 * and the delay before the next retry (for exponential backoff).
 */
public class PendingMessage {

    /** The chat message awaiting server response. */
    private ChatMessage message;

    /** Number of attempts to send this message. */
    private AtomicInteger attempts = new AtomicInteger(1);

    /** Timestamp of the last send attempt (milliseconds since epoch). */
    private volatile Instant lastAttemptTime;

    private CompletableFuture<Boolean> ack;

    /**
     * Constructs a PendingMessage wrapping a ChatMessage.
     * Initializes the lastAttemptTime to the current system time.
     *
     * @param message the chat message to track
     */

    public PendingMessage(ChatMessage message) {
        this.message = message;
        this.lastAttemptTime = Instant.now();
        this.ack = new CompletableFuture<>();
    }

    public ChatMessage getChatMessage() {
        return message;
    }

    public void setChatMessage(ChatMessage message) {
        this.message = message;
    }

    public double getAttempts() {
        return attempts.get();
    }

    public void setAttempts(AtomicInteger attempts) {
        this.attempts = attempts;
    }

    public Instant getLastAttemptTime() {
        return lastAttemptTime;
    }

    public void setLastAttemptTime(Instant lastAttemptTime) {
        this.lastAttemptTime = lastAttemptTime;
    }

    public int incrementAttempts(){
        return attempts.incrementAndGet();
    }
}

