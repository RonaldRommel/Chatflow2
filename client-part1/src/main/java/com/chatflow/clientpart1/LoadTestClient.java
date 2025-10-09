package com.chatflow.clientpart1;

import java.util.UUID;
import com.chatflow.clientpart1.model.ChatMessage;
import com.chatflow.clientpart1.model.ChatResponse;
import com.chatflow.clientpart1.model.MessageType;
import com.chatflow.clientpart1.model.PendingMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LoadTestClient {

    private static  String SERVER_URL = "";
    private static  int TOTAL_MESSAGES = 32000;
    private static  int INITIAL_THREADS = 10;
    private static  int MESSAGES_PER_INITIAL_THREAD = TOTAL_MESSAGES / INITIAL_THREADS;
    private static ScheduledExecutorService scheduler;
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger connectionCount = new AtomicInteger(0);

    private final BlockingQueue<ChatMessage> messageQueue = new LinkedBlockingQueue<>(10_000);
    private final ConcurrentHashMap<String, PendingMessage> pendingMessages = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final String[] messagePool = new String[50];
    private final Random random = new Random();

    private final List<WebSocketClient> connectionPool = new ArrayList<>();
    private static final int POOL_SIZE = 10;
    private static AtomicInteger connectionCounter = new AtomicInteger(0);
    private static int brokenConnections=0;

    public LoadTestClient() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        initMessagePool();
    }

    private void initMessagePool() {
        for (int i = 0; i < 50; i++) {
            messagePool[i] = "Test message " + i + " - Hello from client!";
        }
    }

    public void runLoadTest(String ipAddress) {
        SERVER_URL="ws://"+ipAddress+":8080/chat/";
        System.out.println("\n=== Starting ChatFlow Load Test ===\n");
        long startTime = System.currentTimeMillis();
        try {
            System.out.println("Creating " + POOL_SIZE + " connections...");
            for (int i = 0; i < POOL_SIZE; i++) {
                String roomId = "room" + ((i % POOL_SIZE) + 1);
                connectionPool.add(createConnection(roomId));
            }
            System.out.println("Connections created!\n");
            System.out.println("Phase 1: Warmup");
            phaseSetup(32000,10);
            startMessageGenerator();
            runPhase(1,3);
            long endTime = System.currentTimeMillis();
            printResults(startTime, endTime);
            System.out.println("Phase 2: Main Phase");
            phaseSetup(500000,10);
            startMessageGenerator();
            runPhase(2,30);

//            Thread monitorThread = new Thread(() -> {
//                try {
//                    while (failureCount.get() + successCount.get() < TOTAL_MESSAGES) {
//                        Thread.sleep(500);
//                    }
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//            });
//            System.out.println("Running Pending requests");
//            monitorThread.start();
//            monitorThread.join();
//            scheduler.shutdownNow();
            endTime = System.currentTimeMillis();
            printResults(startTime, endTime);
            System.out.println("\nClosing connections...");
            for (WebSocketClient client : connectionPool) {
                if(!client.isOpen()) {
                    brokenConnections++;
                }
                client.closeBlocking();
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void phaseSetup(int totalMessages, int threads) {
        TOTAL_MESSAGES = totalMessages;
        INITIAL_THREADS = threads;
        MESSAGES_PER_INITIAL_THREAD = totalMessages / threads;
        failureCount.set(0);
        successCount.set(0);

    }

    private void runPhase(int phaseNumber,int initialWait) {
        ExecutorService executor = Executors.newFixedThreadPool(INITIAL_THREADS);
        CountDownLatch latch = new CountDownLatch(INITIAL_THREADS);

        for (int i = 0; i < INITIAL_THREADS; i++) {
            executor.submit(() -> {
                try {
                    sendMessages(MESSAGES_PER_INITIAL_THREAD);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
            if (!pendingMessages.isEmpty()) {
                startRetryScheduler(initialWait);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdown();
        System.out.println("Phase "+phaseNumber+" complete!");
    }

    private void sendMessages(int count) {
        try {
            WebSocketClient client;
            while(true){
                client = connectionPool.get(connectionCounter.getAndIncrement() % POOL_SIZE);
                if (client.isOpen()) {
                    break;
                }
            }
//            WebSocketClient client = connectionPool.get(ThreadLocalRandom.current().nextInt(POOL_SIZE));
            for (int i = 0; i < count; i++) {
                ChatMessage msg = messageQueue.poll(5, TimeUnit.SECONDS);
                if (msg == null) {
                    System.err.println("Message queue empty!");
                    break;
                }
                pendingMessages.put(msg.getMessageID(),new PendingMessage(msg));
                String json = objectMapper.writeValueAsString(msg);
                client.send(json);
            }
        } catch (Exception e) {

            System.err.println("Error: " + e.getMessage());
            failureCount.addAndGet(count);
        }
    }


    private WebSocketClient createConnection(String roomId) throws Exception {
        URI serverUri = URI.create(SERVER_URL + roomId);

        WebSocketClient client = new WebSocketClient(serverUri) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                connectionCount.incrementAndGet();
            }

            @Override
            public void onMessage(String message) {
                try {
//                    System.out.println("Message received: " + message);
                    ChatResponse response = objectMapper.readValue(message,ChatResponse.class);
                    PendingMessage pending  =pendingMessages.get(response.getMessage().getMessageID());
                    if (pending != null) {
                        pendingMessages.remove(response.getMessage().getMessageID());
                        successCount.incrementAndGet();
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if (remote) {
                    System.out.println("Connection closed (code " + code + ", remote=" + remote + "): " + reason);
                }
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("Error: " + ex.getMessage());
                failureCount.incrementAndGet();
            }
        };

        client.connectBlocking(10, TimeUnit.SECONDS);
        return client;
    }

    private void startRetryScheduler(int initialWait) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        CountDownLatch latch = new CountDownLatch(1);
        System.out.println("Initial Pending requests: "+pendingMessages.size());
        scheduler.scheduleWithFixedDelay(() -> {
            if (pendingMessages.isEmpty()) {
                System.out.println("Here");
                latch.countDown();
                scheduler.shutdownNow();
            }
            System.out.println("Pending requests: "+pendingMessages.size());
            long now = System.currentTimeMillis();

            pendingMessages.values().forEach(pending -> {
                if (pending.getAttempts().get()>= 5) {
                    pendingMessages.remove(pending.getMessage().getMessageID());
                    failureCount.incrementAndGet();
                    return;
                }

                // Check if it’s time to retry based on exponential backoff
                if (now >= pending.getNextRetryDelay() + pending.getLastAttemptTime()) {
                    int attempts = pending.getAttempts().getAndIncrement();
                    pending.setLastAttemptTime(now);
                    long initialDelay = 200;
                    pending.setNextRetryDelay(Math.min(initialDelay * (1L << (attempts)), 5000));
                    WebSocketClient client = connectionPool.get(ThreadLocalRandom.current().nextInt(POOL_SIZE));
                    String json = null;
                    try {
                        json = objectMapper.writeValueAsString(pending.getMessage());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    client.send(json);
                }
            });
        }, initialWait, 10, TimeUnit.SECONDS); // check every 100ms (fine granularity)
        try {
            latch.await();
        } catch (InterruptedException e) {}
    }

    private void startMessageGenerator() {
        Thread generator = new Thread(() -> {
            try {
                for (int i = 0; i < TOTAL_MESSAGES; i++) {
                    ChatMessage msg = createRandomMessage();
                    messageQueue.put(msg);
                }
                System.out.println("Message generation complete!");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        generator.setDaemon(true);
        generator.start();
    }

    private ChatMessage createRandomMessage() {
        String messageID = UUID.randomUUID().toString();
        int userId = random.nextInt(1, 100_001);
        String username = "user" + userId;
        String message = messagePool[random.nextInt(50)];
        MessageType type = selectRandomMessageType();

        ChatMessage msg = new ChatMessage();
        msg.setMessageID(messageID);
        msg.setUserID(String.valueOf(userId));
        msg.setUsername(username);
        msg.setMessage(message);
        msg.setTimestamp(LocalDateTime.now());
        msg.setMessageType(type);

        return msg;
    }

    private MessageType selectRandomMessageType() {
        double rand = random.nextDouble();
        if (rand < 0.90) return MessageType.TEXT;
        else if (rand < 0.95) return MessageType.JOIN;
        else return MessageType.LEAVE;
    }

    private void printResults(long startTime, long endTime) {
        long duration = endTime - startTime;
        double seconds = duration / 1000.0;
        double throughput = TOTAL_MESSAGES / seconds;
        System.out.println("\n=== Load Test Results ===");
        System.out.println("Total messages attempted: " + TOTAL_MESSAGES);
        System.out.println("Successful messages: " + successCount.get());
        System.out.println("Failed messages: " + failureCount.get());
        System.out.println("Total connections: " + connectionCount.get());
        System.out.println("Connection pool size: " + POOL_SIZE);
        System.out.println("Total runtime: " + duration + " ms (" + String.format("%.2f", seconds) + " seconds)");
        System.out.println("Overall throughput: " + String.format("%.2f", throughput) + " messages/second");
        System.out.println("========================\n");
    }
}