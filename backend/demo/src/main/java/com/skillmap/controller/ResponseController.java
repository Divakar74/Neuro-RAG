package com.skillmap.controller;

import com.skillmap.model.dto.ResponseDto;
import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.Question;
import com.skillmap.model.entity.Response;
import com.skillmap.repository.AssessmentSessionRepository;
import com.skillmap.repository.QuestionRepository;
import com.skillmap.repository.ResponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/responses")
public class ResponseController {

    @Autowired
    private ResponseRepository responseRepository;

    @Autowired
    private AssessmentSessionRepository sessionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private com.skillmap.repository.UserRepository userRepository;

    @Autowired
    private com.skillmap.service.engine.SkillInferenceEngine skillInferenceEngine;

    @Autowired
    private com.skillmap.service.embedding.EmbeddingService embeddingService;

    @Autowired
    private com.skillmap.service.AsyncProcessingService asyncProcessingService;

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<Response>> getResponsesBySession(@PathVariable Long sessionId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = (authentication == null) || !authentication.isAuthenticated() ||
                "anonymousUser".equalsIgnoreCase(String.valueOf(authentication.getPrincipal()));
        String username = authentication != null ? authentication.getName() : null;

        Optional<AssessmentSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        AssessmentSession session = sessionOpt.get();
        if (!isAnonymous) {
            if (session.getUser() == null || username == null || !session.getUser().getEmail().equals(username)) {
                return ResponseEntity.status(403).build();
            }
        }

        List<Response> responses = responseRepository.findBySessionId(session.getId());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Response>> getResponsesByUser(@PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = (authentication == null) || !authentication.isAuthenticated() ||
                "anonymousUser".equalsIgnoreCase(String.valueOf(authentication.getPrincipal()));
        String username = authentication != null ? authentication.getName() : null;

        // Check if the requesting user matches the userId or is admin
        if (!isAnonymous) {
            Optional<com.skillmap.model.entity.User> requestingUserOpt = userRepository.findByEmail(username);
            if (requestingUserOpt.isEmpty() || !requestingUserOpt.get().getId().equals(userId)) {
                return ResponseEntity.status(403).build();
            }
        }

        List<Response> responses = responseRepository.findByUserId(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/session/token/{sessionToken}")
    public ResponseEntity<List<Response>> getResponsesBySessionToken(@PathVariable String sessionToken) {
        Optional<AssessmentSession> sessionOpt = sessionRepository.findBySessionToken(sessionToken);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        AssessmentSession session = sessionOpt.get();

        List<Response> responses = responseRepository.findBySessionId(session.getId());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getResponseById(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = (authentication == null) || !authentication.isAuthenticated() ||
                "anonymousUser".equalsIgnoreCase(String.valueOf(authentication.getPrincipal()));
        String username = authentication != null ? authentication.getName() : null;

        Optional<Response> responseOpt = responseRepository.findById(id);
        if (responseOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Response r = responseOpt.get();
        if (!isAnonymous) {
            if (r.getSession() == null || r.getSession().getUser() == null ||
                    username == null || !r.getSession().getUser().getEmail().equals(username)) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(r);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> createResponse(
        @RequestBody ResponseDto dto,
        @RequestParam(required = false) String sessionToken,
        @RequestHeader("X-User-Id") Long userId
    ) {
        Optional<AssessmentSession> sessionOpt;
        if (sessionToken != null && !sessionToken.isEmpty()) {
            sessionOpt = sessionRepository.findBySessionToken(sessionToken);
        } else {
            sessionOpt = sessionRepository.findById(dto.getSessionId());
        }
        Optional<Question> questionOpt = questionRepository.findById(dto.getQuestionId());
        if (sessionOpt.isEmpty() || questionOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Validate user exists
        Optional<com.skillmap.model.entity.User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        AssessmentSession session = sessionOpt.get();

        Question question = questionOpt.get();
        Response entity = new Response();
        entity.setSession(session);
        entity.setQuestion(question);
        entity.setUser(userOpt.get()); // Always set user

        String text = dto.getResponseText();
        if (text == null) {
            if (dto.getResponseChoice() != null) {
                text = dto.getResponseChoice();
            } else if (dto.getResponseScale() != null) {
                text = String.valueOf(dto.getResponseScale());
            } else {
                text = "";
            }
        }
        entity.setResponseText(text);
        entity.setWordCount(dto.getWordCount());
        entity.setCharCount(dto.getCharCount());
        entity.setTotalTimeSeconds(dto.getTotalTimeSeconds());
        entity.setThinkTimeSeconds(dto.getThinkTimeSeconds());
        entity.setEditCount(dto.getEditCount());
        entity.setPasteDetected(dto.getPasteDetected());
        if (dto.getConfidenceLevel() != null) {
            entity.setConfidenceLevel(dto.getConfidenceLevel());
        }
        if (dto.getTypingSpeedWpm() != null) {
            entity.setTypingSpeedWpm(dto.getTypingSpeedWpm());
        }
        if (dto.getSimilarityScore() != null) {
            entity.setSimilarityScore(dto.getSimilarityScore());
        }

        // For MCQ/Choice questions, check if the answer is correct
        if (("mcq".equals(question.getQuestionType()) || "choice".equals(question.getQuestionType())) && dto.getResponseChoice() != null) {
            String correctAnswer = question.getCorrectAnswer();
            boolean isCorrect = correctAnswer != null && correctAnswer.equals(dto.getResponseChoice());
            entity.setIsCorrect(isCorrect);
        } else if ("text".equalsIgnoreCase(question.getQuestionType())) {
            // Skip similarity computation during response creation for performance
            // Will be computed asynchronously in background
            if (entity.getSimilarityScore() == null) {
                // Use lightweight fallback immediately, async computation will update later
                try {
                    String expected = question.getContextHint();
                    if (expected == null || expected.isBlank()) {
                        expected = question.getQuestionText();
                    }
                    double sim = cosineSimilarity(simpleEmbed(expected), simpleEmbed(text));
                    entity.setSimilarityScore(sim);
                } catch (Exception ignored) {
                    entity.setSimilarityScore(0.5); // Neutral score
                }
            }
        }

        Response saved = responseRepository.save(entity);

        // Trigger async processing for heavy computations
        if ("text".equalsIgnoreCase(question.getQuestionType())) {
            asyncProcessingService.computeSimilarityScoreAsync(saved);
        }
        asyncProcessingService.updateBeliefsAsync(saved);

        return ResponseEntity.ok(saved);
    }

    private static double[] simpleEmbed(String input) {
        String clean = input == null ? "" : input.toLowerCase().replaceAll("[^a-z0-9 ]", " ");
        String[] tokens = clean.trim().split("\\s+");
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (String t : tokens) {
            if (t.isEmpty()) continue;
            counts.put(t, counts.getOrDefault(t, 0) + 1);
        }
        // Project into fixed small vocab by hashing for lightweight cosine
        int dim = 512;
        double[] vec = new double[dim];
        for (var e : counts.entrySet()) {
            int idx = Math.floorMod(e.getKey().hashCode(), dim);
            vec[idx] += e.getValue();
        }
        // L2 normalize
        double norm = 0.0;
        for (double v : vec) norm += v*v;
        norm = Math.sqrt(norm) + 1e-9;
        for (int i = 0; i < dim; i++) vec[i] /= norm;
        return vec;
    }

    private static double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return Math.max(0.0, Math.min(1.0, dot));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Response> updateResponse(@PathVariable Long id, @RequestBody Response responseDetails) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = (authentication == null) || !authentication.isAuthenticated() ||
                "anonymousUser".equalsIgnoreCase(String.valueOf(authentication.getPrincipal()));
        String username = authentication != null ? authentication.getName() : null;

        Optional<Response> responseOpt = responseRepository.findById(id);
        if (responseOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Response response = responseOpt.get();
        if (!isAnonymous) {
            if (response.getSession() == null || response.getSession().getUser() == null ||
                    username == null || !response.getSession().getUser().getEmail().equals(username)) {
                return ResponseEntity.status(403).build();
            }
        }
        response.setResponseText(responseDetails.getResponseText());
        response.setWordCount(responseDetails.getWordCount());
        response.setCharCount(responseDetails.getCharCount());
        response.setKeywordMatches(responseDetails.getKeywordMatches());
        response.setThinkTimeSeconds(responseDetails.getThinkTimeSeconds());
        response.setTotalTimeSeconds(responseDetails.getTotalTimeSeconds());
        response.setEditCount(responseDetails.getEditCount());
        response.setPasteDetected(responseDetails.getPasteDetected());
        response.setSpecificityScore(responseDetails.getSpecificityScore());
        response.setDepthScore(responseDetails.getDepthScore());
        Response updatedResponse = responseRepository.save(response);
        return ResponseEntity.ok(updatedResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResponse(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = (authentication == null) || !authentication.isAuthenticated() ||
                "anonymousUser".equalsIgnoreCase(String.valueOf(authentication.getPrincipal()));
        String username = authentication != null ? authentication.getName() : null;

        Optional<Response> responseOpt = responseRepository.findById(id);
        if (responseOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Response response = responseOpt.get();
        if (!isAnonymous) {
            if (response.getSession() == null || response.getSession().getUser() == null ||
                    username == null || !response.getSession().getUser().getEmail().equals(username)) {
                return ResponseEntity.status(403).build();
            }
        }
        responseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
