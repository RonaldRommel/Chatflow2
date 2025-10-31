package com.chatflow.server.rabbit;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RabbitMQSenderIntegrationTest {

    @Autowired
    private RabbitMQSender sender;

    @Autowired
    @Qualifier("producerPool")
    private ChannelPool channelPool;

    @Test
    void testSendMessage_realRabbitMQ() throws Exception {
        String roomId = "1";
        String message = "Hello, RabbitMQ!";

        sender.sendMessage("1", message);
        sender.sendMessage("5", message);
        sender.sendMessage("10", message);

    }
}

