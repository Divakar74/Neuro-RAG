package com.skillmap.controller;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.SessionFeedback;
import com.skillmap.repository.AssessmentSessionRepository;
import com.skillmap.repository.SessionFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user-data")
@RequiredArgsConstructor
public class UserDataController {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final SessionFeedbackRepository sessionFeedbackRepository;

    @GetMapping("/session/{sessionId}/feedback")
    public ResponseEntity<List<Map<String, Object>>> getSessionFeedback(@PathVariable Long sessionId) {
        Optional<SessionFeedback> feedbackOpt = sessionFeedbackRepository.findBySessionId(sessionId);

        if (feedbackOpt.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        SessionFeedback feedback = feedbackOpt.get();
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", feedback.getId());
        summary.put("feedbackText", feedback.getFeedbackText());
        summary.put("userResponsesSummary", feedback.getUserResponsesSummary());
        summary.put("aiModelUsed", feedback.getAiModelUsed());
        summary.put("tokensUsed", feedback.getTokensUsed());
        summary.put("skillGraphData", feedback.getSkillGraphData());
        summary.put("behavioralInsights", feedback.getBehavioralInsights());
        summary.put("previousFeedbackActions", feedback.getPreviousFeedbackActions());
        summary.put("currentSessionGoal", feedback.getCurrentSessionGoal());
        summary.put("generatedAt", feedback.getGeneratedAt());

        return ResponseEntity.ok(List.of(summary));
    }

    @GetMapping("/session/{sessionId}/cognitive-analysis")
    public ResponseEntity<Map<String, Object>> getCognitiveAnalysis(@PathVariable Long sessionId) {
        AssessmentSession session = assessmentSessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));

        // Extract cognitive analysis from session responses
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("sessionId", sessionId);
        analysis.put("totalResponses", session.getResponses() != null ? session.getResponses().size() : 0);

        // Calculate basic cognitive metrics
        if (session.getResponses() != null && !session.getResponses().isEmpty()) {
            double avgConfidence = session.getResponses().stream()
                .filter(r -> r.getConfidenceLevel() != null)
                .mapToDouble(r -> r.getConfidenceLevel())
                .average()
                .orElse(0.0);

            double avgTime = session.getResponses().stream()
                .filter(r -> r.getThinkTimeSeconds() != null)
                .mapToDouble(r -> r.getThinkTimeSeconds())
                .average()
                .orElse(0.0);

            analysis.put("averageConfidence", avgConfidence);
            analysis.put("averageThinkTime", avgTime);
        }

        return ResponseEntity.ok(analysis);
    }
}
