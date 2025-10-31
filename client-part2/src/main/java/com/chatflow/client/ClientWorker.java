package com.chatflow.client;

import com.chatflow.client.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class ClientWorker implements Runnable {

    private final URI serverUri;
    private final String userId;
    private final String username;
    private final BlockingQueue<ChatMessage> messageQueue;
    private final MetricsCollector metrics;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    private volatile boolean running = true;
    private WebSocketClient client;
    private final Map<String, Instant> pendingMessages = new ConcurrentHashMap<>();
    private final String assignedRoomId;

    public ClientWorker(URI serverUri, String userId, String username,
                        BlockingQueue<ChatMessage> messageQueue, MetricsCollector metrics) {
        this.serverUri = serverUri;
        this.userId = userId;
        this.username = username;
        this.messageQueue = messageQueue;
        this.metrics = metrics;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.assignedRoomId = "room" + random.nextInt(1, 21);
    }

    @Override
    public void run() {
        try {
            connect();
            register();
            Thread.sleep(100);
            sendJoin();

            while (running) {
                ChatMessage msg = messageQueue.poll(200, TimeUnit.MILLISECONDS);
                if (msg != null) {
                    sendMessage(msg);
                }
            }
        } catch (Exception e) {
        } finally {
            close();
        }
    }

    private void connect() throws Exception {
        client = new WebSocketClient(serverUri) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                metrics.incrementConnections();
            }

            @Override
            public void onMessage(String message) {
                handleBroadcast(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {}

            @Override
            public void onError(Exception ex) {}
        };
        client.connectBlocking(5, TimeUnit.SECONDS);
    }

    private void register() {
        try {
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("userId", userId);
            userInfo.put("username", username);
            userInfo.put("roomId", assignedRoomId);

            client.send(objectMapper.writeValueAsString(userInfo));
        } catch (Exception e) {}
    }

    private void handleBroadcast(String message) {
        try {
            Map<String, Object> msg = objectMapper.readValue(message, Map.class);

            if ("REGISTERED".equals(msg.get("status"))) return;

            String messageId = (String) msg.get("messageId");
            if (messageId != null && pendingMessages.containsKey(messageId)) {
                Instant sendTime = pendingMessages.remove(messageId);
                Instant ackTime = Instant.now();

                String messageType = (String) msg.getOrDefault("messageType", "TEXT");
                String roomId = (String) msg.getOrDefault("roomId", "unknown");

                metrics.recordMessageSent(sendTime, ackTime, messageType, roomId, 200, true);
            }
        } catch (Exception e) {}
    }

    private void sendMessage(ChatMessage chatMessage) {
        try {
            if (client != null && client.isOpen()) {
                Instant sendTime = Instant.now();
                chatMessage.setRoomId(assignedRoomId);

                pendingMessages.put(chatMessage.getMessageId(), sendTime);
                client.send(objectMapper.writeValueAsString(chatMessage));
            }
        } catch (Exception e) {}
    }

    private void sendJoin() {
        try {
            ChatMessage join = new ChatMessage();
            join.setMessageId(UUID.randomUUID().toString());
            join.setUserId(userId);
            join.setUsername(username);
            join.setMessage(username + " joined");
            join.setMessageType(MessageType.JOIN);
            join.setRoomId(assignedRoomId);
            join.setTimestamp(Instant.now());

            sendMessage(join);
        } catch (Exception e) {}
    }

    private void close() {
        try {
            if (client != null && client.isOpen()) {
                client.closeBlocking();
            }
        } catch (Exception e) {}
    }

    public void stop() {
        running = false;
        close();
    }
}