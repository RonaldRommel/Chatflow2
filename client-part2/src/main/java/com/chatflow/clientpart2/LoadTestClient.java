package com.chatflow.clientpart2;

import com.chatflow.clientpart2.model.ChatMessage;
import com.chatflow.clientpart2.model.ChatResponse;
import com.chatflow.clientpart2.model.MessageType;
import com.chatflow.clientpart2.model.PendingMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A load testing client for ChatFlow server.
 * <p>
 * Simulates multiple WebSocket clients sending messages concurrently,
 * tracks latency and success/failure rates, and writes metrics to CSV.
 * Provides retry logic for pending messages and generates throughput and latency statistics.
 */
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
    private static final int POOL_SIZE = 20;
    private static AtomicInteger connectionCounter = new AtomicInteger(0);
    private static int brokenConnections=0;
    private BufferedCSVWriter csv;

    public LoadTestClient() throws IOException {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        initMessagePool();
        csv= new BufferedCSVWriter("./results/latency_metrics.csv");
    }

    private void initMessagePool() {
        for (int i = 0; i < 50; i++) {
            messagePool[i] = "Test message " + i + " - Hello from client!";
        }
    }

    /**
     * Runs the load test against the specified server IP address.
     * Creates connections, sends messages in phases, retries pending messages,
     * and generates statistics and charts.
     *
     * @param ipAddress the server IP address to connect to
     */

    public void runLoadTest(String ipAddress) {
        SERVER_URL="ws://"+ipAddress+":8080/chat/";
        System.out.println("\n=== Starting ChatFlow Load Test ===\n");
        long startTime = System.currentTimeMillis();
        long endTime;
        try {
            System.out.println("Creating " + POOL_SIZE + " connections...");
            for (int i = 0; i < POOL_SIZE; i++) {
                String roomId = "room" + ((i % POOL_SIZE) + 1);
                connectionPool.add(createConnection(roomId));
            }
            System.out.println("Connections created!\n");
//            System.out.println("Phase 1: Warmup");
//            phaseSetup(32000,32);
//            startMessageGenerator();
//            runPhase(1,4);
//            endTime = System.currentTimeMillis();
//            printResults(startTime, endTime,"Warmup Phase");
            System.out.println("Phase 2: Main Phase");
            phaseSetup(500000,32);
            startMessageGenerator();
            runPhase(2,30);
            endTime = System.currentTimeMillis();
            printResults(startTime, endTime,"Main Phase");
            System.out.println("\nClosing connections...");
            csv.close();
            for (WebSocketClient client : connectionPool) {
                if(!client.isOpen()) {
                    brokenConnections++;
                }
                client.closeBlocking();
            }
            StreamingLatency.displayStatistics();
            ThroughputChart.createThroughputChart("./results/latency_metrics.csv","./results/throughput_chart.png");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sets up a testing phase with specified total messages and thread count.
     *
     * @param totalMessages number of messages to send in this phase
     * @param threads       number of concurrent threads to use
     */
    private void phaseSetup(int totalMessages, int threads) {
        TOTAL_MESSAGES = totalMessages;
        INITIAL_THREADS = threads;
        MESSAGES_PER_INITIAL_THREAD = totalMessages / threads;
        failureCount.set(0);
        successCount.set(0);

    }


    /**
     * Runs a testing phase, sending messages concurrently and retrying pending messages.
     *
     * @param phaseNumber  the phase number for reporting
     * @param initialWait  initial delay before retry scheduler starts
     */
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

    /**
     * Sends a specified number of messages from the message queue using available WebSocket connections.
     *
     * @param count number of messages to send
     */

    private void sendMessages(int count) {
        try {
            WebSocketClient client;
            while(true){
                client = connectionPool.get(connectionCounter.getAndIncrement() % POOL_SIZE);
                if (client.isOpen()) {
                    break;
                }
            }
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


    /**
     * Creates a WebSocket connection to a given room and handles message responses.
     *
     * @param roomId the chat room ID
     * @return a connected WebSocketClient
     * @throws Exception if connection fails
     */

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
                    ChatResponse response = objectMapper.readValue(message,ChatResponse.class);
                    PendingMessage pending  =pendingMessages.get(response.getMessage().getMessageID());
                    if (pending != null) {
                        writeToCSV(response,roomId);
                        pendingMessages.remove(response.getMessage().getMessageID());
                        successCount.incrementAndGet();
                    }
                } catch (IOException e) {
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


    /**
     * Writes a received ChatResponse to the CSV metrics file, computing latency.
     *
     * @param response the server response
     * @param roomId   the chat room ID
     * @throws IOException if writing to CSV fails
     */

    private void writeToCSV(ChatResponse response,String roomId) throws IOException {
        String clientTimestamp = response.getMessage().getTimestamp().toString();
        String serverTimestamp = response.getServerTimestamp().toString();
        String messageType = String.valueOf(response.getMessage().getMessageType());
        String status = response.getStatus().toString();


        Instant serverInstant = response.getServerTimestamp();
        Instant clientInstant = response.getMessage().getTimestamp();
//         Difference in milliseconds
        long diffMillis = serverInstant.toEpochMilli() - clientInstant.toEpochMilli();
        String latency = String.valueOf(diffMillis);
        csv.writeRow(clientTimestamp,messageType,latency,status,roomId);
    }


    /**
     * Starts a scheduler to retry pending messages with exponential backoff.
     *
     * @param initialWait initial delay before starting retries (in seconds)
     */
    private void startRetryScheduler(int initialWait) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        CountDownLatch latch = new CountDownLatch(1);
//        System.out.println("Initial Pending requests: "+pendingMessages.size());
        scheduler.scheduleWithFixedDelay(() -> {
            if (pendingMessages.isEmpty()) {
                latch.countDown();
                scheduler.shutdownNow();
            }
//            System.out.println("Pending requests: "+pendingMessages.size());
            long now = System.currentTimeMillis();

            pendingMessages.values().forEach(pending -> {
                if (pending.getAttempts().get()>= 5) {
                    pendingMessages.remove(pending.getMessage().getMessageID());
                    failureCount.incrementAndGet();
                    return;
                }

                if (now >= pending.getNextRetryDelay() + pending.getLastAttemptTime()) {
                    int attempts = pending.getAttempts().getAndIncrement();
                    pending.setLastAttemptTime(now);
                    ChatMessage msg = pending.getMessage();
                    msg.setTimestamp(Instant.now());
                    pending.setMessage(msg);
                    long initialDelay = 200;
                    pending.setNextRetryDelay(Math.min(initialDelay * (1L << (attempts)), 5000));
                    WebSocketClient client = connectionPool.get(ThreadLocalRandom.current().nextInt(POOL_SIZE));
                    String json = null;
                    try {
                        json = objectMapper.writeValueAsString(msg);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    client.send(json);
                }
            });
        }, initialWait, 10, TimeUnit.SECONDS);
        try {
            latch.await();
        } catch (InterruptedException e) {}
    }


    /**
     * Starts a background thread to generate random messages and enqueue them for sending.
     */

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


    /**
     * Creates a random ChatMessage with random user, room, content, and type.
     *
     * @return a ChatMessage object
     */

    private ChatMessage createRandomMessage() {
        String messageID = UUID.randomUUID().toString();
        int userId = random.nextInt(1, 100_001);
        String username = "user" + userId;
        String message = messagePool[random.nextInt(50)];
        MessageType type = selectRandomMessageType();
        String roomID = String.valueOf(random.nextInt(1,21));
        ChatMessage msg = new ChatMessage();
        msg.setMessageID(messageID);
        msg.setUserID(String.valueOf(userId));
        msg.setUsername(username);
        msg.setMessage(message);
        msg.setTimestamp(Instant.now());
        msg.setMessageType(type);
        msg.setRoomID(roomID);
        return msg;
    }

    /**
     * Selects a random message type based on predefined probabilities.
     *
     * @return a MessageType enum
     */
    private MessageType selectRandomMessageType() {
        double rand = random.nextDouble();
        if (rand < 0.90) return MessageType.TEXT;
        else if (rand < 0.95) return MessageType.JOIN;
        else return MessageType.LEAVE;
    }


    /**
     * Prints summarized load test results for a given phase.
     *
     * @param startTime start timestamp in milliseconds
     * @param endTime   end timestamp in milliseconds
     * @param phase     phase name for reporting
     */
    private void printResults(long startTime, long endTime, String phase) {
        long duration = endTime - startTime;
        double seconds = duration / 1000.0;
        double throughput = TOTAL_MESSAGES / seconds;
        System.out.println("\n=== Load Test Results: "+phase+"  ===");
        System.out.println("Total messages attempted: " + TOTAL_MESSAGES);
        System.out.println("Successful messages: " + successCount.get());
        System.out.println("Failed messages: " + failureCount.get());
        System.out.println("Total connections: " + connectionCount.get());
        System.out.println("Total runtime: " + duration + " ms (" + String.format("%.2f", seconds) + " seconds)");
        System.out.println("Overall throughput: " + String.format("%.2f", throughput) + " messages/second");
        System.out.println("========================\n");
    }
}