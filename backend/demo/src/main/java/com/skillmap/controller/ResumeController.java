package com.skillmap.controller;

import com.skillmap.model.dto.ResumeUpdateDTO;
import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.ResumeData;
import com.skillmap.model.entity.User;
import com.skillmap.repository.AssessmentSessionRepository;
import com.skillmap.repository.ResumeDataRepository;
import com.skillmap.repository.UserRepository;
import com.skillmap.service.nlp.GeminiResumeParserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Autowired
    private ResumeDataRepository resumeDataRepository;

    @Autowired
    private AssessmentSessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GeminiResumeParserService resumeParserService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadResume(@RequestParam("file") MultipartFile file,
                                                            @RequestParam(value = "role", required = false) String role,
                                                            @RequestParam("userId") Long userId) {
        try {
            // Get user by ID (required)
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "User not found with ID: " + userId);
                return ResponseEntity.status(404).body(error);
            }

            // Create a new assessment session for the resume
            AssessmentSession session = new AssessmentSession();
            session.setSessionToken(java.util.UUID.randomUUID().toString());
            session.setTargetRole(role);
            session.setStatus(AssessmentSession.Status.in_progress);
            session.setResumeUploaded(true);
            session.setQuestionsAsked(0);
            session.setUser(userOpt.get()); // Always set user
            AssessmentSession savedSession = sessionRepository.save(session);

            byte[] contentBytes = file.getBytes();
            // Parse the resume content using NLP service (supports PDF/DOC via Tika)
            ResumeData savedResume = resumeParserService.parseResume(contentBytes, savedSession);

            Map<String, Object> response = new HashMap<>();
            // Build DTO-only map to avoid lazy-loading or serializer recursion
            Map<String, Object> resumeDto = new HashMap<>();
            resumeDto.put("id", savedResume.getId());
            resumeDto.put("rawText", savedResume.getRawText());
            resumeDto.put("extractedSkills", savedResume.getExtractedSkills());
            resumeDto.put("extractedEducation", savedResume.getExtractedEducation());
            resumeDto.put("extractedExperience", savedResume.getExtractedExperience());
            resumeDto.put("totalYearsExperience", savedResume.getTotalYearsExperience());
            resumeDto.put("rawEntities", savedResume.getRawEntities());

            Map<String, Object> sessionDto = new HashMap<>();
            sessionDto.put("id", savedSession.getId());
            sessionDto.put("sessionToken", savedSession.getSessionToken());
            sessionDto.put("targetRole", savedSession.getTargetRole());
            sessionDto.put("status", savedSession.getStatus());
            sessionDto.put("questionsAsked", savedSession.getQuestionsAsked());
            sessionDto.put("userId", savedSession.getUser().getId());

            response.put("resumeData", resumeDto);
            response.put("session", sessionDto);

            return ResponseEntity.ok().body(response);
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to read uploaded file");
            return ResponseEntity.status(400).body(error);
        } catch (Exception e) {
            log.error("Failed to parse resume", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to parse resume: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<ResumeData>> getResumesBySession(@PathVariable Long sessionId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = (authentication == null) || !authentication.isAuthenticated() ||
                "anonymousUser".equalsIgnoreCase(String.valueOf(authentication.getPrincipal()));
        String username = authentication != null ? authentication.getName() : null;
        Optional<User> userOpt = (username != null) ? userRepository.findByEmail(username) : Optional.empty();

        Optional<AssessmentSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!isAnonymous) {
            if (userOpt.isEmpty() || sessionOpt.get().getUser() == null ||
                    !sessionOpt.get().getUser().getId().equals(userOpt.get().getId())) {
                return ResponseEntity.status(403).build();
            }
        }

        List<ResumeData> resumes = resumeDataRepository.findBySessionId(sessionId);
        return ResponseEntity.ok(resumes);
    }

    // Normalized structured response for dashboard (arrays instead of JSON strings)
    @GetMapping("/normalized/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getNormalizedBySession(@PathVariable Long sessionId) {
        Optional<AssessmentSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) return ResponseEntity.notFound().build();
        List<ResumeData> list = resumeDataRepository.findBySessionId(sessionId);
        if (list.isEmpty()) return ResponseEntity.notFound().build();
        ResumeData rd = list.get(0);
        Map<String, Object> out = new HashMap<>();
        out.put("sessionId", sessionId);
        out.put("totalYearsExperience", rd.getTotalYearsExperience());
        out.put("rawTextPreview", rd.getRawText() != null && rd.getRawText().length() > 800 ? rd.getRawText().substring(0, 800) + "…" : rd.getRawText());
        out.put("skills", parseJsonListOfObjects(rd.getExtractedSkills()));
        out.put("education", parseJsonListOfStrings(rd.getExtractedEducation()));
        out.put("experience", parseJsonListOfStrings(rd.getExtractedExperience()));
        return ResponseEntity.ok(out);
    }

    @GetMapping("/session/token/{sessionToken}")
    public ResponseEntity<ResumeData> getResumeBySessionToken(@PathVariable String sessionToken) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = (authentication == null) || !authentication.isAuthenticated() ||
                "anonymousUser".equalsIgnoreCase(String.valueOf(authentication.getPrincipal()));
        String username = authentication != null ? authentication.getName() : null;
        Optional<User> userOpt = (username != null) ? userRepository.findByEmail(username) : Optional.empty();

        Optional<AssessmentSession> sessionOpt = sessionRepository.findBySessionToken(sessionToken);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!isAnonymous) {
            if (userOpt.isEmpty() || sessionOpt.get().getUser() == null ||
                    !sessionOpt.get().getUser().getId().equals(userOpt.get().getId())) {
                return ResponseEntity.status(403).build();
            }
        }

        List<ResumeData> resumes = resumeDataRepository.findBySessionId(sessionOpt.get().getId());
        if (!resumes.isEmpty()) {
            return ResponseEntity.ok(resumes.get(0));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ResumeData>> getResumesByUserId(@PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = (authentication == null) || !authentication.isAuthenticated() ||
                "anonymousUser".equalsIgnoreCase(String.valueOf(authentication.getPrincipal()));
        String username = authentication != null ? authentication.getName() : null;
        Optional<User> userOpt = (username != null) ? userRepository.findByEmail(username) : Optional.empty();

        // Check if the requesting user matches the userId or is admin
        if (!isAnonymous) {
            if (userOpt.isEmpty() || !userOpt.get().getId().equals(userId)) {
                return ResponseEntity.status(403).build();
            }
        }

        Optional<User> targetUserOpt = userRepository.findById(userId);
        if (targetUserOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<ResumeData> resumes = resumeDataRepository.findByUserId(userId);
        return ResponseEntity.ok(resumes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeData> getResumeById(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = (authentication == null) || !authentication.isAuthenticated() ||
                "anonymousUser".equalsIgnoreCase(String.valueOf(authentication.getPrincipal()));
        String username = authentication != null ? authentication.getName() : null;
        Optional<User> userOpt = (username != null) ? userRepository.findByEmail(username) : Optional.empty();

        Optional<ResumeData> resumeOpt = resumeDataRepository.findById(id);
        if (resumeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ResumeData resume = resumeOpt.get();
        if (!isAnonymous) {
            if (userOpt.isEmpty() || resume.getSession() == null || resume.getSession().getUser() == null ||
                    !resume.getSession().getUser().getId().equals(userOpt.get().getId())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(resume);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = (authentication == null) || !authentication.isAuthenticated() ||
                "anonymousUser".equalsIgnoreCase(String.valueOf(authentication.getPrincipal()));
        String username = authentication != null ? authentication.getName() : null;
        Optional<User> userOpt = (username != null) ? userRepository.findByEmail(username) : Optional.empty();

        Optional<ResumeData> resumeOpt = resumeDataRepository.findById(id);
        if (resumeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ResumeData resume = resumeOpt.get();
        if (!isAnonymous) {
            if (userOpt.isEmpty() || resume.getSession() == null || resume.getSession().getUser() == null ||
                    !resume.getSession().getUser().getId().equals(userOpt.get().getId())) {
                return ResponseEntity.status(403).build();
            }
        }
        resumeDataRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResumeData> updateResume(@PathVariable Long id, @RequestBody ResumeUpdateDTO updated) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = (authentication == null) || !authentication.isAuthenticated() ||
                "anonymousUser".equalsIgnoreCase(String.valueOf(authentication.getPrincipal()));
        String username = authentication != null ? authentication.getName() : null;
        Optional<User> userOpt = (username != null) ? userRepository.findByEmail(username) : Optional.empty();

        return resumeDataRepository.findById(id)
            .map(existing -> {
                if (!isAnonymous) {
                    if (userOpt.isEmpty() || existing.getSession() == null || existing.getSession().getUser() == null ||
                            !existing.getSession().getUser().getId().equals(userOpt.get().getId())) {
                        return ResponseEntity.status(403).<ResumeData>build();
                    }
                }
                existing.setExtractedSkills(updated.getExtractedSkills());
                existing.setExtractedEducation(updated.getExtractedEducation());
                existing.setExtractedExperience(updated.getExtractedExperience());
                existing.setTotalYearsExperience(updated.getTotalYearsExperience());
                ResumeData saved = resumeDataRepository.save(existing);
                return ResponseEntity.ok(saved);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // Test endpoint for evaluation (accepts text directly, no auth required)
    @PostMapping("/parse")
    public ResponseEntity<Map<String, Object>> parseResumeText(@RequestBody Map<String, String> request) {
        try {
            String resumeText = request.get("resumeText");
            String fileName = request.get("fileName");

            if (resumeText == null || resumeText.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "resumeText is required");
                return ResponseEntity.status(400).body(error);
            }

            // Create a mock session for testing
            AssessmentSession session = new AssessmentSession();
            session.setSessionToken("test_" + java.util.UUID.randomUUID().toString());
            session.setTargetRole("Test Role");
            session.setStatus(AssessmentSession.Status.in_progress);
            session.setResumeUploaded(true);
            session.setQuestionsAsked(0);

            // Convert text to bytes (simple UTF-8 encoding)
            byte[] contentBytes = resumeText.getBytes("UTF-8");

            // Parse the resume
            ResumeData savedResume = resumeParserService.parseResume(contentBytes, session);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedResume.getId());
            response.put("rawText", savedResume.getRawText());
            response.put("extractedSkills", parseJsonListOfObjects(savedResume.getExtractedSkills()));
            response.put("extractedEducation", parseJsonListOfStrings(savedResume.getExtractedEducation()));
            response.put("extractedExperience", parseJsonListOfStrings(savedResume.getExtractedExperience()));
            response.put("totalYearsExperience", savedResume.getTotalYearsExperience());
            response.put("processingTimeMs", 1000L); // Mock processing time

            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            log.error("Failed to parse resume text", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to parse resume: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // Helpers to parse stored JSON strings safely
    private java.util.List<java.util.Map<String, Object>> parseJsonListOfObjects(String json) {
        try {
            if (json == null || json.isBlank()) return java.util.List.of();
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>(){});
        } catch (Exception e) { return java.util.List.of(); }
    }

    private java.util.List<String> parseJsonListOfStrings(String json) {
        try {
            if (json == null || json.isBlank()) return java.util.List.of();
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>(){});
        } catch (Exception e) { return java.util.List.of(); }
    }
}
