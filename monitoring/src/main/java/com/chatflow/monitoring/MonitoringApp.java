package com.chatflow.monitoring;

public class MonitoringApp {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0];

        switch (command) {
            case "queue-monitor":
                String rabbitHost = args.length > 1 ? args[1] : "localhost";
                new QueueMonitor(rabbitHost).start();
                break;

            case "server-health":
                String serverUrl = args.length > 1 ? args[1] : "http://localhost:8080";
                new ServerHealthMonitor(serverUrl).start();
                break;

            case "metrics-analyzer":
                String csvFile = args.length > 1 ? args[1] : "metrics.csv";
                new MetricsAnalyzer(csvFile).analyze();
                break;

            default:
                System.out.println("Unknown command: " + command);
                printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("ChatFlow Monitoring Tools");
        System.out.println("Usage: java -jar chatflow-monitoring.jar <command> [options]");
        System.out.println("");
        System.out.println("Commands:");
        System.out.println("  queue-monitor [rabbitmq-host]     Monitor RabbitMQ queues");
        System.out.println("  server-health [server-url]        Monitor server health");
        System.out.println("  metrics-analyzer [csv-file]       Analyze performance metrics");
        System.out.println("");
        System.out.println("Examples:");
        System.out.println("  java -jar chatflow-monitoring.jar queue-monitor localhost");
        System.out.println("  java -jar chatflow-monitoring.jar server-health http://localhost:8080");
        System.out.println("  java -jar chatflow-monitoring.jar metrics-analyzer metrics.csv");
    }
}