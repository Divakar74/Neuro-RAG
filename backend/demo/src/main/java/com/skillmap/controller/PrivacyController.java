package com.skillmap.controller;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.Response;
import com.skillmap.model.entity.ResumeData;
import com.skillmap.model.entity.User;
import com.skillmap.repository.AssessmentSessionRepository;
import com.skillmap.repository.ResponseRepository;
import com.skillmap.repository.ResumeDataRepository;
import com.skillmap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/privacy")
@RequiredArgsConstructor
public class PrivacyController {

    private final UserRepository userRepository;
    private final AssessmentSessionRepository sessionRepository;
    private final ResponseRepository responseRepository;
    private final ResumeDataRepository resumeDataRepository;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportMyData() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();
        List<AssessmentSession> sessions = sessionRepository.findByUser(user);

        Map<String, Object> payload = new HashMap<>();
        payload.put("user", Map.of("email", user.getEmail(), "createdAt", user.getCreatedAt()));

        payload.put("sessions", sessions.stream().map(s -> {
            Map<String, Object> sMap = new HashMap<>();
            sMap.put("id", s.getId());
            sMap.put("token", s.getSessionToken());
            sMap.put("targetRole", s.getTargetRole());
            sMap.put("status", s.getStatus());
            List<Response> responses = responseRepository.findBySessionId(s.getId());
            sMap.put("responses", responses);
            List<ResumeData> resumes = resumeDataRepository.findBySessionId(s.getId());
            sMap.put("resumes", resumes);
            return sMap;
        }).toList());

        String json;
        try {
            json = new com.fasterxml.jackson.databind.ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(payload);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            json = "{\"error\":\"export_failed\"}";
        }
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=neuro-rag-export.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bytes);
    }
}


