package com.chatflow.client;

import com.chatflow.client.model.ChatMessage;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.*;

@SpringBootApplication
public class ClientApplication implements CommandLineRunner {

    private final ClientPool clientPool;
    private final MessageGenerator messageGenerator;

    public ClientApplication(ClientPool clientPool, MessageGenerator messageGenerator) {
        this.clientPool = clientPool;
        this.messageGenerator = messageGenerator;
    }

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        MetricsCollector metrics = new MetricsCollector();
        BlockingQueue<ChatMessage> queue = new LinkedBlockingQueue<>();

        System.out.println("ChatFlow Load Test");
        System.out.println("=".repeat(60));

        metrics.startTimer();

        Thread generator = new Thread(() -> {
            System.out.println("Generating 500K messages");
            messageGenerator.generateMessages(queue, 500_000);
            System.out.println("Generation complete");
        });
        generator.start();

        Thread.sleep(3000);

        clientPool.startClients(queue, "ws://localhost:8080/chat", metrics);

        generator.join();

        System.out.println("Processing");
        while (!queue.isEmpty()) {
            System.out.printf("  Queue: %,d%n", queue.size());
            Thread.sleep(3000);
        }

        System.out.println("Waiting for ACKs");
        int lastCount = 0;
        int stuckCount = 0;

        while (metrics.getSuccessCount() < 500_000 && stuckCount < 30) {
            Thread.sleep(2000);
            int currentCount = metrics.getSuccessCount();

            System.out.printf("  ACKs: %,d / 500,000 (%.1f%%)%n",
                    currentCount, (currentCount / 500_000.0) * 100);

            if (currentCount == lastCount) {
                stuckCount++;
            } else {
                stuckCount = 0;
            }
            lastCount = currentCount;
        }

        int finalCount = metrics.getSuccessCount();
        if (finalCount >= 500_000) {
            System.out.println("All messages received");
        } else {
            System.out.println("Received " + finalCount + " / 500,000");
            System.out.println("Missing: " + (500_000 - finalCount));
        }

        metrics.stopTimer();
        clientPool.stopAll();

        metrics.printDetailedSummary();
        metrics.writeMetricsToCSV("metrics.csv");
        ThroughputVisualizer.generateThroughputData(metrics.getMessageMetrics(), "throughput.csv");

        System.out.println("\nComplete");
        System.exit(0);
    }
}