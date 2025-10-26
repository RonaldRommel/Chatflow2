package com.chatflow.clientpart2;

import com.chatflow.clientpart2.model.ChatMessage;
import com.chatflow.clientpart2.model.ChatResponse;
import com.chatflow.clientpart2.model.MessageType;
import com.chatflow.clientpart2.model.PendingMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
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
    private static  int INITIAL_THREADS = 32;
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger connectionCount = new AtomicInteger(0);
    private final ScheduledExecutorService retryScheduler = Executors.newScheduledThreadPool(10);
    private final ExecutorService csvWriterExecutor = Executors.newFixedThreadPool(20);
    private final BlockingQueue<PendingMessage> messageQueue = new LinkedBlockingQueue<>(50_000);
    private final ConcurrentHashMap<String, PendingMessage> pendingMessages = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final String[] messagePool = new String[50];
    private final Random random = new Random();
    private final ConcurrentHashMap<String,WebSocketClient> connections = new ConcurrentHashMap<>();
    private static final int POOL_SIZE = 20;
    private final BufferedCSVWriter csv;

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
        long endTime;
        try {
            System.out.println("Creating " + POOL_SIZE + " connections...");
            for (int i = 0; i < POOL_SIZE; i++) {
                String roomId = "room" + ((i % POOL_SIZE) + 1);
                connections.put(roomId, createConnection(roomId));
            }
            System.out.println("Connections created!\n");
            long startTime = System.currentTimeMillis();
            System.out.println("Phase 2: Main Phase");
            phaseSetup(500000,50);
            startMessageGenerator();
            runPhase(2);
            endTime = System.currentTimeMillis();
            printResults(startTime, endTime,"Main Phase");
            System.out.println("\nClosing connections...");
            retryScheduler.shutdownNow();
            csv.close();
            csvWriterExecutor.shutdown();
            csvWriterExecutor.awaitTermination(5, TimeUnit.SECONDS);
            for (WebSocketClient client : connections.values()) {
                client.closeBlocking();
            }
            System.out.println("Connections closed!\n");
            System.out.println("\n Success: "+successCount.get()+" Failure:"+failureCount.get());
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
        failureCount.set(0);
        successCount.set(0);

    }

    /**
     * Runs a testing phase, sending messages concurrently and retrying pending messages.
     *
     * @param phaseNumber  the phase number for reporting
     */
    private void runPhase(int phaseNumber) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(INITIAL_THREADS);
        for (int i = 0; i < INITIAL_THREADS; i++) {
            executor.submit(() -> {
                while(true) {
                    if (messageQueue.isEmpty() && pendingMessages.isEmpty()) {
                        break;
                    }
                    sendMessage();
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        System.out.println("Phase "+phaseNumber+" complete!");
    }

    /**
     * Sends a specified number of messages from the message queue using available WebSocket connections.
     *
     */

    private void sendMessage() {
        PendingMessage msg = null;
        try {
            msg = messageQueue.poll(2, TimeUnit.SECONDS);
            if (msg == null) {
//                System.err.println("Message queue empty!");
                return;
            }
            WebSocketClient client = connections.get(msg.getChatMessage().getRoomID());
            ChatMessage chatMessage = msg.getChatMessage();
            String json = objectMapper.writeValueAsString(chatMessage);
            synchronized (client) {
                client.send(json);
            }
            pendingMessages.put(msg.getChatMessage().getMessageID(), msg);
            PendingMessage finalMsg = msg;
//            long delay = (long) Math.pow(3, finalMsg.getAttempts());
            retryScheduler.schedule(()->handleTimeout(finalMsg),60,TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (msg != null) {
                handleTimeout(msg);
            }

        }
    }



    private void handleTimeout(PendingMessage msg) {
        if (pendingMessages.remove(msg.getChatMessage().getMessageID()) != null) {
            System.out.println("Timeout: " + msg.getChatMessage().getMessageID());
            if (msg.incrementAttempts() < 5) {
                messageQueue.add(msg);
            } else {
                System.out.println("Failed after retries: " + msg.getChatMessage().getMessageID());
                failureCount.incrementAndGet();
            }
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
                    if (pendingMessages.remove(response.getMessage().getMessageID())!=null){
                        successCount.incrementAndGet();
                        csvWriterExecutor.submit(() -> {
                            synchronized (csv) {
                                try {
                                    writeToCSV(response, response.getMessage().getRoomID());
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }
                } catch (IOException e) {
//                    throw new RuntimeException(e);
                    System.err.println("Object Error: "+e.getMessage());
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
     * Starts a background thread to generate random messages and enqueue them for sending.
     */
    private void startMessageGenerator() {
        Thread generator = new Thread(() -> {
            try {
                for (int i = 0; i < TOTAL_MESSAGES; i++) {
                    ChatMessage msg = createRandomMessage();
                    PendingMessage pending = new PendingMessage(msg);
                    messageQueue.put(pending);
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
        String roomID = "room"+ random.nextInt(1, 21);
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