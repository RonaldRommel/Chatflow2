package com.chatflow.clientpart2;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClientApplication2 implements CommandLineRunner {

    private final LoadTestClient loadTestClient;

    public ClientApplication2(LoadTestClient loadTestClient) {
        this.loadTestClient = loadTestClient;
    }

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication2.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String ipAddress = "";
        for (String arg : args) {
            if (arg.startsWith("--server=")) {
                String ip = arg.split("=", 2)[1];
                System.out.println("Server IP: " + ip);
                ipAddress = ip;
            }
        }
        loadTestClient.runLoadTest(ipAddress);
    }
}