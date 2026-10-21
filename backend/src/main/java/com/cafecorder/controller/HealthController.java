package com.cafecorder.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// For the uptime pinger that keeps the free Render instance awake. Deliberately skips the database.
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public String health() {
        return "ok";
    }
}
