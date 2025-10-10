package com.chatflow.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple REST controller for server health checks.
 */

@RestController
public class ServerController {


    /**
     * Health check endpoint.
     * <p>
     * Returns "OK" if the server is running.
     *
     * @return the string "OK"
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }

}