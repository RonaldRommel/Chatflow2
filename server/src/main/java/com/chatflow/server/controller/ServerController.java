package com.chatflow.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ServerController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/server-info")
    public Map<String, String> serverInfo() {
        Map<String, String> info = new HashMap<>();
        try {
            info.put("hostname", InetAddress.getLocalHost().getHostName());
            info.put("ip", InetAddress.getLocalHost().getHostAddress());
            info.put("status", "running");
        } catch (Exception e) {
            info.put("error", e.getMessage());
        }
        return info;
    }
}