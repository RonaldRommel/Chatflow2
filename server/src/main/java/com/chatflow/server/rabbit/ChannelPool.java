package com.chatflow.server.rabbit;

import com.rabbitmq.client.*;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.concurrent.*;

public class ChannelPool {

    private final BlockingQueue<Channel> pool;
    private final Connection connection;
    private final int poolSize;

    public ChannelPool(int poolSize) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");

        this.poolSize = poolSize;
        this.connection = factory.newConnection();
        this.pool = new ArrayBlockingQueue<>(poolSize);

        init();
    }

    private void init() throws IOException {
        for (int i = 0; i < poolSize; i++) {
            pool.offer(connection.createChannel());
        }
    }

    public Channel borrowChannel() throws InterruptedException {
        return pool.take();
    }

    public void returnChannel(Channel channel) {
        if (channel != null && channel.isOpen()) {
            pool.offer(channel);
        }
    }

    @PreDestroy
    public void close() throws IOException, TimeoutException {
        for (Channel channel : pool) {
            if (channel.isOpen()) channel.close();
        }
        if (connection.isOpen()) connection.close();
    }
}