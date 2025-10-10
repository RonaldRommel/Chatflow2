package com.chatflow.clientpart2;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for analyzing streaming message latency metrics from a CSV file.
 * Computes statistics such as mean, median, percentiles, and message distribution.
 */

public class StreamingLatency {
    /**
     * Reads latency data from a CSV file and prints statistics including:
     * <ul>
     *     <li>Total count, mean, median, min, max, 95th and 99th percentiles</li>
     *     <li>Message throughput per room</li>
     *     <li>Distribution of message types</li>
     * </ul>
     *
     * @throws Exception if the CSV file cannot be read or parsed
     */
    public static void displayStatistics() throws Exception {
        String file = "./results/latency_metrics.csv";

        List<Long> latencies = new ArrayList<>();
        Map<String, Long> roomCounts = new HashMap<>();
        Map<String, Long> typeCounts = new HashMap<>();

        long count = 0;
        double sum = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 5) continue;

                long latency = Long.parseLong(parts[2]);
                String messageType = parts[1];
                String roomId = parts[4];

                latencies.add(latency);
                sum += latency;
                count++;

                if (latency < min) min = latency;
                if (latency > max) max = latency;
                roomCounts.put(roomId, roomCounts.getOrDefault(roomId, 0L) + 1);
                typeCounts.put(messageType, typeCounts.getOrDefault(messageType, 0L) + 1);
            }
        }

        if (latencies.isEmpty()) {
            System.out.println("No data found in CSV.");
            return;
        }

        Collections.sort(latencies);

        double mean = sum / count;
        long median = latencies.get(latencies.size() / 2);
        long p95 = latencies.get((int) (latencies.size() * 0.95));
        long p99 = latencies.get((int) (latencies.size() * 0.99));

        // === Print Results ===
        System.out.println("=== Full Latency Statistics (ms) ===");
        System.out.println("Count: " + count);
        System.out.printf("Mean response time: %.3f%nms", mean);
        System.out.println("Median response time: " + median);
        System.out.println("Min response time: " + min+" ms");
        System.out.println("Max response time: " + max+" ms");
        System.out.println("95th percentile response time: " + p95);
        System.out.println("99th percentile response time: " + p99);

        // Throughput per room
        System.out.println("\n=== Throughput per Room ===");
        roomCounts.forEach((room, c) -> System.out.println(room + ": " + c));

        // Message type distribution
        System.out.println("\n=== Message Type Distribution ===");
        AtomicLong countType= new AtomicLong();
        typeCounts.forEach((type, c) -> countType.addAndGet(c));

        typeCounts.forEach((type, c) -> {
            double percentage = (c * 100.0) / countType.get();
            System.out.printf("%s: %d (%.2f%%)%n", type, c, percentage);
        });

    }
    public static void main(String[] args) throws Exception {
        StreamingLatency.displayStatistics();
    }

}
