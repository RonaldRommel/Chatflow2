package com.chatflow.client;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsCollector {

    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger connections = new AtomicInteger(0);
    private final List<MessageMetric> metrics = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong startTime = new AtomicLong(0);
    private final AtomicLong endTime = new AtomicLong(0);

    public static class MessageMetric {
        public final Instant sendTime;
        public final Instant ackTime;
        public final long latencyMs;
        public final String messageType;
        public final String roomId;

        public MessageMetric(Instant sendTime, Instant ackTime, String messageType, String roomId) {
            this.sendTime = sendTime;
            this.ackTime = ackTime;
            this.latencyMs = ackTime.toEpochMilli() - sendTime.toEpochMilli();
            this.messageType = messageType;
            this.roomId = roomId;
        }
    }

    public void startTimer() {
        startTime.set(System.currentTimeMillis());
    }

    public void stopTimer() {
        endTime.set(System.currentTimeMillis());
    }

    public void recordMessageSent(Instant sendTime, Instant ackTime, String messageType,
                                  String roomId, int statusCode, boolean success) {
        if (success) {
            successCount.incrementAndGet();
            metrics.add(new MessageMetric(sendTime, ackTime, messageType, roomId));
        } else {
            failureCount.incrementAndGet();
        }
    }

    public void incrementConnections() {
        connections.incrementAndGet();
    }

    public int getSuccessCount() {
        return successCount.get();
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    public void printDetailedSummary() {
        long totalMs = endTime.get() - startTime.get();
        double totalSec = totalMs / 1000.0;

        System.out.println("\n" + "=".repeat(60));
        System.out.println("PERFORMANCE METRICS");
        System.out.println("=".repeat(60));
        System.out.println("\nMessages:");
        System.out.println("  Success: " + successCount.get());
        System.out.println("  Failed: " + failureCount.get());
        System.out.println("  Connections: " + connections.get());

        System.out.println("\nPerformance:");
        System.out.printf("  Runtime: %.2f seconds%n", totalSec);
        System.out.printf("  Throughput: %.2f msg/s%n", successCount.get() / totalSec);

        if (!metrics.isEmpty()) {
            List<Long> latencies = new ArrayList<>();
            for (MessageMetric m : metrics) {
                latencies.add(m.latencyMs);
            }
            Collections.sort(latencies);

            long sum = latencies.stream().mapToLong(Long::longValue).sum();
            double mean = sum / (double) latencies.size();
            long median = latencies.get(latencies.size() / 2);
            long p95 = latencies.get((int) (latencies.size() * 0.95));
            long p99 = latencies.get((int) (latencies.size() * 0.99));
            long min = latencies.get(0);
            long max = latencies.get(latencies.size() - 1);

            System.out.println("\nResponse Time (ms):");
            System.out.printf("  Mean: %.0f ms%n", mean);
            System.out.printf("  Median: %d ms%n", median);
            System.out.printf("  P95: %d ms%n", p95);
            System.out.printf("  P99: %d ms%n", p99);
            System.out.printf("  Min: %d ms%n", min);
            System.out.printf("  Max: %d ms%n", max);
        }

        System.out.println("\n" + "=".repeat(60));
    }

    public void writeMetricsToCSV(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("timestamp,messageType,latencyMs,roomId");
            for (MessageMetric m : metrics) {
                writer.printf("%s,%s,%d,%s%n", m.sendTime, m.messageType, m.latencyMs, m.roomId);
            }
            System.out.println("Metrics written to " + filename);
        } catch (Exception e) {
            System.err.println("Failed to write CSV: " + e.getMessage());
        }
    }

    public List<MessageMetric> getMessageMetrics() {
        return new ArrayList<>(metrics);
    }
}