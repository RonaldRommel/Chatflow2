package com.chatflow.clientpart2;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Utility class for writing CSV files with buffered output.
 * <p>
 * Supports writing individual rows or batches of rows.
 * Automatically writes a CSV header on creation.
 */
public class BufferedCSVWriter implements AutoCloseable {
    private final BufferedWriter writer;

    /**
     * Constructs a BufferedCSVWriter for the specified file path.
     * Writes a header row automatically.
     *
     * @param filePath path to the CSV file
     * @throws IOException if the file cannot be opened for writing
     */

    public BufferedCSVWriter(String filePath) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(filePath));
        writer.write("timestamp,messageType,latencyMs,statusCode,roomId");
        writer.newLine();
    }

    /**
     * Writes a single row to the CSV file.
     *
     * @param timestamp  timestamp of the message
     * @param messageType type of the message
     * @param latency    message latency in milliseconds
     * @param statusCode HTTP-like status code or message status
     * @param roomId     ID of the chat room
     */

    public synchronized void writeRow(String timestamp, String messageType, String latency, String statusCode, String roomId) {
        try {
            writer.write(String.join(",", timestamp, messageType, latency, statusCode, roomId));
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        writer.flush();
        writer.close();
    }

}
