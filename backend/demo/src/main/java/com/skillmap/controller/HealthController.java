package com.skillmap.controller;

import com.skillmap.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final OpenAIService openAIService;

    @GetMapping("/openai")
    public ResponseEntity<String> openAiHealth() {
        boolean ok = openAIService.testConnectivity();
        return ok ? ResponseEntity.ok("ok") : ResponseEntity.status(503).body("openai_unreachable");
    }
}



