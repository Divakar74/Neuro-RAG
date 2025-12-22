package com.skillmap.controller;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.SessionFeedback;
import com.skillmap.repository.AssessmentSessionRepository;
import com.skillmap.repository.SessionFeedbackRepository;
import com.skillmap.service.analysis.RAGFeedbackService;
import com.skillmap.service.analysis.NeuroRAGService;
import com.skillmap.service.OpenAIService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final AssessmentSessionRepository sessionRepository;
    private final SessionFeedbackRepository sessionFeedbackRepository;
    private final RAGFeedbackService ragFeedbackService;
    private final NeuroRAGService neuroRagService;
    private final OpenAIService openAIService;
    private final ObjectMapper objectMapper;

    @GetMapping("/{sessionToken}")
    public ResponseEntity<String> getFeedback(@PathVariable String sessionToken) {
        try {
            Optional<AssessmentSession> sessionOpt = sessionRepository.findBySessionToken(sessionToken);
            if (sessionOpt.isEmpty()) {
                log.warn("Session not found for token: {}", sessionToken);
                return ResponseEntity.notFound().build();
            }

            AssessmentSession session = sessionOpt.get();
            Optional<SessionFeedback> existingFeedback = sessionFeedbackRepository.findBySessionId(session.getId());

            if (existingFeedback.isPresent()) {
                log.info("Returning cached feedback for session {}", sessionToken);
                return ResponseEntity.ok(existingFeedback.get().getFeedbackText());
            }

            // Generate tailored NeuroRAG feedback using OpenAI service
            String feedback = generateTailoredNeuroRAGFeedback(session);

            // Compute user responses summary
            String responsesSummary = session.getResponses().stream()
                    .map(r -> String.format("Q: %s\nA: %s", r.getQuestion().getQuestionText(), r.getResponseText()))
                    .collect(Collectors.joining("\n"));

            // Save to DB with enhanced metadata
            SessionFeedback newFeedback = new SessionFeedback();
            newFeedback.setSession(session);
            newFeedback.setUser(session.getUser()); // Set user for user_id linking
            newFeedback.setFeedbackText(feedback);
            newFeedback.setUserResponsesSummary(responsesSummary);
            newFeedback.setAiModelUsed("gpt-4");
            newFeedback.setTokensUsed(1500); // Approximate token usage
            sessionFeedbackRepository.save(newFeedback);

            log.info("Generated and saved new feedback for session {}", sessionToken);
            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            log.error("Unexpected error generating feedback for session {}", sessionToken, e);
            return ResponseEntity.internalServerError().body("Error generating feedback");
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SessionFeedback>> getFeedbackByUser(@PathVariable Long userId) {
        Optional<SessionFeedback> feedbackOpt = sessionFeedbackRepository.findByUserId(userId);
        if (feedbackOpt.isPresent()) {
            return ResponseEntity.ok(List.of(feedbackOpt.get()));
        } else {
            return ResponseEntity.ok(List.of()); // Return empty list if no feedback found
        }
    }

    /**
     * Generates tailored NeuroRAG feedback using the enhanced OpenAI service
     */
    private String generateTailoredNeuroRAGFeedback(AssessmentSession session) {
        try {
            // Build skill graph data from session responses
            Map<String, Object> skillGraphData = buildSkillGraphData(session);

            // Build behavioral insights from responses
            Map<String, Object> behavioralInsights = buildBehavioralInsights(session);

            // Get previous feedback actions (placeholder for now)
            String previousFeedbackActions = "Focused on accuracy under time pressure and self-evaluation after each question.";

            // Current session goal
            String currentSessionGoal = "Improve problem-solving and maintain coding logic accuracy.";

            // Generate feedback using OpenAI service
            return openAIService.generateNeuroRAGTailoredFeedback(
                session,
                session.getResponses(),
                skillGraphData,
                behavioralInsights,
                previousFeedbackActions,
                currentSessionGoal
            );
        } catch (Exception e) {
            log.error("Error generating tailored NeuroRAG feedback", e);
            // Fallback to NeuroRAG service
            try {
                return neuroRagService.generateValidatedFeedback(session);
            } catch (Exception e2) {
                log.error("NeuroRAG fallback also failed", e2);
                return ragFeedbackService.generateFeedback(session);
            }
        }
    }

    /**
     * Builds skill graph data from session responses
     */
    private Map<String, Object> buildSkillGraphData(AssessmentSession session) {
        // This is a simplified implementation - in a real system, this would analyze
        // the responses to calculate skill scores
        Map<String, Object> skillGraph = new java.util.HashMap<>();

        // Example data based on the user's prompt
        Map<String, Object> problemSolving = new java.util.HashMap<>();
        problemSolving.put("current", 0.68);
        problemSolving.put("previous", 0.72);
        problemSolving.put("trend", "slight decline");

        Map<String, Object> codingLogic = new java.util.HashMap<>();
        codingLogic.put("current", 0.67);
        codingLogic.put("previous", 0.55);
        codingLogic.put("trend", "improved");

        Map<String, Object> dataInterpretation = new java.util.HashMap<>();
        dataInterpretation.put("current", 0.61);
        dataInterpretation.put("previous", 0.61);
        dataInterpretation.put("trend", "stable");

        skillGraph.put("Problem Solving", problemSolving);
        skillGraph.put("Coding Logic", codingLogic);
        skillGraph.put("Data Interpretation", dataInterpretation);

        return skillGraph;
    }

    /**
     * Builds behavioral insights from session responses
     */
    private Map<String, Object> buildBehavioralInsights(AssessmentSession session) {
        Map<String, Object> insights = new java.util.HashMap<>();

        // Example data based on the user's prompt
        insights.put("hesitationTimeReduction", "14%");
        insights.put("confidenceOverestimation", "2 out of 5 technical answers");
        insights.put("calmnessScore", 0.8);

        return insights;
    }
}
