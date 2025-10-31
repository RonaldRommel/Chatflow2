package com.chatflow.monitoring;

import java.io.*;
import java.util.*;

public class MetricsAnalyzer {

    private final String csvFile;

    public MetricsAnalyzer(String csvFile) {
        this.csvFile = csvFile;
    }

    public void analyze() {
        try {
            List<MetricRecord> records = readCSV();

            if (records.isEmpty()) {
                System.out.println("No data to analyze");
                return;
            }

            printAnalysis(records);

        } catch (Exception e) {
            System.err.println("Error analyzing metrics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<MetricRecord> readCSV() throws IOException {
        List<MetricRecord> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String header = reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    records.add(new MetricRecord(
                            parts[0],
                            parts[1],
                            Long.parseLong(parts[2]),
                            parts[3]
                    ));
                }
            }
        }

        return records;
    }

    private void printAnalysis(List<MetricRecord> records) {
        System.out.println("=".repeat(60));
        System.out.println("METRICS ANALYSIS");
        System.out.println("=".repeat(60));
        System.out.println("");

        List<Long> latencies = new ArrayList<>();
        Map<String, Integer> roomCounts = new HashMap<>();
        Map<String, Integer> typeCounts = new HashMap<>();

        for (MetricRecord record : records) {
            latencies.add(record.latencyMs);
            roomCounts.merge(record.roomId, 1, Integer::sum);
            typeCounts.merge(record.messageType, 1, Integer::sum);
        }

        Collections.sort(latencies);

        System.out.println("Latency Statistics (ms):");
        System.out.println("  Total Messages: " + records.size());
        System.out.printf("  Mean: %.2f%n", latencies.stream().mapToLong(Long::longValue).average().orElse(0));
        System.out.println("  Median: " + latencies.get(latencies.size() / 2));
        System.out.println("  P95: " + latencies.get((int)(latencies.size() * 0.95)));
        System.out.println("  P99: " + latencies.get((int)(latencies.size() * 0.99)));
        System.out.println("  Min: " + latencies.get(0));
        System.out.println("  Max: " + latencies.get(latencies.size() - 1));

        System.out.println("");
        System.out.println("Messages per Room:");
        roomCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("  %s: %d%n", e.getKey(), e.getValue()));

        System.out.println("");
        System.out.println("Message Type Distribution:");
        typeCounts.forEach((type, count) -> {
            double percent = (count * 100.0) / records.size();
            System.out.printf("  %s: %d (%.1f%%)%n", type, count, percent);
        });

        System.out.println("");
        System.out.println("=".repeat(60));
    }

    static class MetricRecord {
        String timestamp;
        String messageType;
        long latencyMs;
        String roomId;

        MetricRecord(String timestamp, String messageType, long latencyMs, String roomId) {
            this.timestamp = timestamp;
            this.messageType = messageType;
            this.latencyMs = latencyMs;
            this.roomId = roomId;
        }
    }
}