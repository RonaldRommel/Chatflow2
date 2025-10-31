package com.chatflow.server.rabbit;

import com.rabbitmq.client.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQInitializer {

    private static final String EXCHANGE_PREFIX = "chat.exchange.";
    private static final int ROOM_COUNT = 20;

    public RabbitMQInitializer(@Qualifier("producerPool") ChannelPool channelPool) {
        try {
            Channel channel = channelPool.borrowChannel();

            for (int i = 1; i <= ROOM_COUNT; i++) {
                String exchangeName = EXCHANGE_PREFIX + "room" + i;
                channel.exchangeDeclare(exchangeName, BuiltinExchangeType.FANOUT, true);
            }

            channelPool.returnChannel(channel);
            System.out.println("RabbitMQ initialized: 20 exchanges created");

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize RabbitMQ", e);
        }
    }
}