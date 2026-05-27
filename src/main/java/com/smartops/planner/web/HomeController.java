package com.smartops.planner.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "SmartOps Planner backend funcionando";
    }

    @GetMapping("/api/health")
    public String health() {
        return "OK";
    }
}
