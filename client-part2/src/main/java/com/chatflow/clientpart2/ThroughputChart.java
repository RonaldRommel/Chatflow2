package com.chatflow.clientpart2;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class for generating throughput charts from CSV latency data.
 * Aggregates messages into 10-second buckets and plots messages per second over time.
 */

public class ThroughputChart {

    /**
     * Reads timestamped message data from a CSV file, calculates throughput in 10-second buckets,
     * and saves an XY line chart as a PNG.
     *
     * @param csvPath    path to the input CSV file with timestamps
     * @param outputPath path where the chart PNG will be saved
     * @throws Exception if the CSV cannot be read or chart cannot be saved
     */
    public static void createThroughputChart(String csvPath, String outputPath) throws Exception {
        Map<Long, Integer> bucketCounts = new TreeMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line = br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 5) continue;

                try {
                    Instant ts = Instant.parse(parts[0].trim());
                    long epochSeconds = ts.getEpochSecond();

                    // Group timestamps into 10-second buckets
                    long bucket = (epochSeconds / 10) * 10;
                    bucketCounts.put(bucket, bucketCounts.getOrDefault(bucket, 0) + 1);
                } catch (DateTimeParseException e) {
                    System.err.println("Skipping invalid timestamp: " + parts[0]);
                }
            }
        }

        if (bucketCounts.isEmpty()) {
            System.out.println("No valid data found in CSV for chart generation.");
            return;
        }

        // Convert bucket counts to throughput (messages/sec)
        XYSeries series = new XYSeries("Throughput (messages/sec)");
        for (Map.Entry<Long, Integer> entry : bucketCounts.entrySet()) {
            double timeSec = entry.getKey();
            double throughput = entry.getValue() / 10.0; // since each bucket = 10 seconds
            series.add(timeSec, throughput);
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Throughput Over Time",
                "Time (UTC, epoch seconds)",
                "Messages per second",
                dataset
        );

        // Save as PNG
        ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1000, 600);
        System.out.println("Throughput chart saved to: " + outputPath);
    }

    public static void main(String[] args) throws Exception {
        String csvPath = "./results/latency_metrics.csv";
        String outputPath = "./results/throughput_chart.png";
        createThroughputChart(csvPath, outputPath);
    }
}
