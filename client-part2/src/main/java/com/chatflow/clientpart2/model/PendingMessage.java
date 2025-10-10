package com.chatflow.clientpart2.model;

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
    private volatile long lastAttemptTime;

    /** Delay in milliseconds before the next retry. */
    private volatile long nextRetryDelay = 200;

    /**
     * Constructs a PendingMessage wrapping a ChatMessage.
     * Initializes the lastAttemptTime to the current system time.
     *
     * @param message the chat message to track
     */

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

