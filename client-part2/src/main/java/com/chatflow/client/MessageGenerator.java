package com.chatflow.client;

import com.chatflow.client.model.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;

@Component
public class MessageGenerator {

    private final Random random = new Random();
    private final List<String> messagePool = Arrays.asList(
            "Hello", "Hi", "How are you", "Great", "Thanks",
            "OK", "Yes", "No", "Maybe", "Sure"
    );

    public void generateMessages(BlockingQueue<ChatMessage> queue, int count) {
        for (int i = 0; i < count; i++) {
            try {
                int userId = random.nextInt(100_000) + 1;
                String username = "user" + userId;
                int roomId = random.nextInt(20) + 1;

                MessageType type;
                double p = random.nextDouble();
                if (p < 0.05) type = MessageType.JOIN;
                else if (p < 0.10) type = MessageType.LEAVE;
                else type = MessageType.TEXT;

                ChatMessage msg = new ChatMessage(
                        String.valueOf(userId),
                        username,
                        messagePool.get(random.nextInt(messagePool.size())),
                        "room" + roomId,
                        type,
                        Instant.now()
                );

                queue.put(msg);
            } catch (Exception e) {
                System.err.println("Generation error: " + e.getMessage());
            }
        }
    }
}