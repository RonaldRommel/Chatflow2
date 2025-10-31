package com.chatflow.server.rabbit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Configuration
public class RabbitMQConfig {

    @Bean
    @Qualifier("producerPool")
    public ChannelPool producerChannelPool() throws IOException, TimeoutException {
        return new ChannelPool(50);
    }

    @Bean
    @Qualifier("consumerPool")
    public ChannelPool consumerChannelPool() throws IOException, TimeoutException {
        return new ChannelPool(150);
    }
}