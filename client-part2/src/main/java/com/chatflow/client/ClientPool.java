package com.chatflow.client;

import com.chatflow.client.model.ChatMessage;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

@Component
public class ClientPool {

    private static final int CLIENT_COUNT = 100;
    private final List<ClientWorker> clients = new ArrayList<>();
    private ExecutorService executorService;

    public void startClients(BlockingQueue<ChatMessage> queue, String serverUrl,
                             MetricsCollector metrics) throws Exception {

        System.out.println("Starting " + CLIENT_COUNT + " clients");

        executorService = Executors.newFixedThreadPool(CLIENT_COUNT);

        for (int i = 0; i < CLIENT_COUNT; i++) {
            String userId = String.valueOf(1000 + i);
            String username = "user" + userId;

            ClientWorker worker = new ClientWorker(
                    new URI(serverUrl), userId, username, queue, metrics
            );

            clients.add(worker);
            executorService.submit(worker);
        }

        System.out.println("All clients started");
    }

    public void stopAll() {
        System.out.println("Stopping clients");
        clients.forEach(ClientWorker::stop);
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}