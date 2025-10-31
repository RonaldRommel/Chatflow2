package com.chatflow.client;

import java.io.*;
import java.util.*;

public class ThroughputVisualizer {

    public static void generateThroughputData(List<MetricsCollector.MessageMetric> metrics,
                                              String outputFile) {
        if (metrics == null || metrics.isEmpty()) return;

        Map<Long, Long> throughputBuckets = new TreeMap<>();
        long startEpoch = metrics.get(0).sendTime.getEpochSecond();

        for (MetricsCollector.MessageMetric metric : metrics) {
            long bucket = (metric.sendTime.getEpochSecond() - startEpoch) / 10;
            throughputBuckets.merge(bucket, 1L, Long::sum);
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            writer.println("timeSeconds,messagesPerSecond");

            throughputBuckets.forEach((bucket, count) -> {
                writer.printf("%d,%.2f%n", bucket * 10, count / 10.0);
            });

            System.out.println("Throughput data written to " + outputFile);
        } catch (Exception e) {
            System.err.println("Failed to write throughput data: " + e.getMessage());
        }
    }
}