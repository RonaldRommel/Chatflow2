package com.chatflow.monitoring;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

public class ServerHealthMonitor {

    private final String serverUrl;

    public ServerHealthMonitor(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void start() {
        System.out.println("Server Health Monitor");
        System.out.println("=====================");
        System.out.println("Monitoring: " + serverUrl);
        System.out.println("Press Ctrl+C to stop");
        System.out.println("");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            while (true) {
                checkHealth(httpClient);
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            System.out.println("Monitoring stopped");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void checkHealth(CloseableHttpClient httpClient) {
        long startTime = System.currentTimeMillis();

        try {
            HttpGet request = new HttpGet(serverUrl + "/health");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                long responseTime = System.currentTimeMillis() - startTime;
                int statusCode = response.getCode();
                String body = EntityUtils.toString(response.getEntity());

                String status = statusCode == 200 ? "HEALTHY" : "UNHEALTHY";

                System.out.printf("[%s] %s | Status: %d | Response: %dms | Body: %s%n",
                        new java.util.Date(), status, statusCode, responseTime, body.trim());

            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            System.out.printf("[%s] DOWN | Error: %s | Time: %dms%n",
                    new java.util.Date(), e.getMessage(), responseTime);
        }
    }
}