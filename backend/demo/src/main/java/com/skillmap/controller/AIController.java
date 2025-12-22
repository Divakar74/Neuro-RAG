package com.skillmap.controller;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.repository.AssessmentSessionRepository;
import com.skillmap.service.OpenAIService;
import com.skillmap.service.analysis.AISuggestionService;
import com.skillmap.service.analysis.NeuroRAGService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final AISuggestionService aiSuggestionService;
    private final NeuroRAGService neuroRAGService;
    private final OpenAIService openAIService;
    private final AssessmentSessionRepository sessionRepository;

    @Value("${openai.api.key:}")
    private String openAiKey;

    @GetMapping("/suggestions/{sessionId}")
    public ResponseEntity<String> getPersonalizedSuggestions(@PathVariable Long sessionId) {
        log.info("Received request for AI suggestions for session: {}", sessionId);
        String suggestions = aiSuggestionService.generatePersonalizedSuggestions(sessionId);
        return ResponseEntity.ok(suggestions);
    }

    // Health: check OPENAI_API_KEY presence and connectivity
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> out = new HashMap<>();
        boolean hasKey = openAiKey != null && !openAiKey.isBlank();
        boolean canConnect = false;
        if (hasKey) {
            try { canConnect = openAIService.testConnectivity(); } catch (Exception ignored) {}
        }
        out.put("openaiKeyPresent", hasKey);
        out.put("openaiConnectivity", canConnect);
        out.put("hint", hasKey ? "If connectivity is false, verify network/proxy and model availability" : "Set OPENAI_API_KEY in environment");
        return ResponseEntity.ok(out);
    }

    // Neuro-RAG suggestions: validates key, loads session, generates tailored plan
    @GetMapping("/neuro/{sessionId}")
    public ResponseEntity<?> generateNeuroRAG(@PathVariable Long sessionId) {
        if (openAiKey == null || openAiKey.isBlank()) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "OPENAI_API_KEY not configured");
            err.put("setup", "Set environment variable OPENAI_API_KEY and restart backend");
            return ResponseEntity.status(500).body(err);
        }
        Optional<AssessmentSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            String suggestion = neuroRAGService.generateValidatedFeedback(sessionOpt.get());
            Map<String, Object> out = new HashMap<>();
            out.put("sessionId", sessionId);
            out.put("suggestions", suggestion);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Failed to generate Neuro-RAG suggestions");
            err.put("message", e.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    @PostMapping("/generate-suggestions")
    public ResponseEntity<String> generateSuggestions(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        String apiKey = request.get("apiKey");

        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Prompt is required");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("API key is required");
        }

        try {
            String suggestions = openAIService.generateSuggestions(prompt, apiKey);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Error generating suggestions", e);
            return ResponseEntity.status(500).body("Failed to generate suggestions: " + e.getMessage());
        }
    }
}
