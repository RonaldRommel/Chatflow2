package com.chatflow.monitoring;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class QueueMonitor {

    private final String rabbitHost;
    private Connection connection;
    private Channel channel;

    public QueueMonitor(String rabbitHost) {
        this.rabbitHost = rabbitHost;
    }

    public void start() {
        try {
            connect();
            monitor();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void connect() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitHost);
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");

        connection = factory.newConnection();
        channel = connection.createChannel();

        System.out.println("Connected to RabbitMQ at " + rabbitHost);
    }

    private void monitor() throws InterruptedException {
        System.out.println("RabbitMQ Queue Monitor");
        System.out.println("======================");
        System.out.println("Press Ctrl+C to stop");
        System.out.println("");

        while (true) {
            clearScreen();
            System.out.println("Queue Status - " + new java.util.Date());
            System.out.println("=".repeat(80));
            System.out.printf("%-40s %12s %12s %12s%n",
                    "Queue Name", "Total", "Ready", "Unacked");
            System.out.println("-".repeat(80));

            long totalMessages = 0;
            long totalReady = 0;
            long totalUnacked = 0;

            for (int i = 1; i <= 20; i++) {
                String queueName = "queue_" + getHostname() + "_room" + i;

                try {
                    AMQP.Queue.DeclareOk declareOk = channel.queueDeclarePassive(queueName);
                    long messageCount = declareOk.getMessageCount();
                    long consumerCount = declareOk.getConsumerCount();

                    System.out.printf("%-40s %12d %12s %12s%n",
                            queueName, messageCount, "-", "-");

                    totalMessages += messageCount;

                } catch (IOException e) {
                    System.out.printf("%-40s %12s %12s %12s%n",
                            queueName, "N/A", "N/A", "N/A");
                }
            }

            System.out.println("=".repeat(80));
            System.out.printf("%-40s %12d %12d %12d%n",
                    "TOTAL", totalMessages, totalReady, totalUnacked);
            System.out.println("");

            if (totalMessages > 10000) {
                System.out.println("WARNING: High queue depth detected!");
            }

            Thread.sleep(2000);
        }
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "localhost";
        }
    }

    private void cleanup() {
        try {
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
        } catch (Exception e) {}
    }
}