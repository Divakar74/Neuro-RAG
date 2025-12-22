package com.skillmap.controller;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.SkillAssessment;
import com.skillmap.model.entity.User;
import com.skillmap.repository.AssessmentSessionRepository;
import com.skillmap.repository.SkillAssessmentRepository;
import com.skillmap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assessment")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final SkillAssessmentRepository skillAssessmentRepository;
    private final UserRepository userRepository;

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startAssessment(@RequestParam(value = "targetRole", required = false) String targetRole) {
        try {
            // Get authenticated user from SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByEmail(username).orElseThrow(() -> new RuntimeException("User not found"));

            AssessmentSession session = new AssessmentSession();
            session.setSessionToken(java.util.UUID.randomUUID().toString());
            session.setTargetRole(targetRole);
            session.setStatus(AssessmentSession.Status.in_progress);
            session.setResumeUploaded(false);
            session.setQuestionsAsked(0);
            session.setUser(user); // Always associate with authenticated user
            AssessmentSession savedSession = assessmentSessionRepository.save(session);

            Map<String, Object> response = new HashMap<>();
            response.put("sessionToken", savedSession.getSessionToken());
            response.put("id", savedSession.getId());
            response.put("targetRole", savedSession.getTargetRole());
            response.put("status", savedSession.getStatus());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to start assessment: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/user/sessions")
    public ResponseEntity<List<Map<String, Object>>> getUserSessions() {
        // Get authenticated user from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByEmail(username).orElseThrow(() -> new RuntimeException("User not found"));
        Long userId = user.getId();

        List<AssessmentSession> sessions = assessmentSessionRepository.findByUserId(userId);

        // Convert to enhanced response including skill assessments for user data persistence
        // Limit to last 10 sessions but include skill assessments for dashboard restoration
        List<Map<String, Object>> sessionSummaries = sessions.stream()
            .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt())) // Most recent first
            .limit(10) // Limit to prevent large responses
            .map(session -> {
                Map<String, Object> summary = new HashMap<>();
                summary.put("id", session.getId());
                summary.put("sessionToken", session.getSessionToken());
                summary.put("targetRole", session.getTargetRole());
                summary.put("status", session.getStatus().toString());
                summary.put("questionsAsked", session.getQuestionsAsked() != null ? session.getQuestionsAsked() : 0);
                summary.put("startedAt", session.getStartedAt());
                summary.put("completedAt", session.getCompletedAt());
                summary.put("resumeUploaded", session.getResumeUploaded());

                // Include skill assessments for user data restoration
                List<SkillAssessment> skillAssessments = skillAssessmentRepository.findBySessionId(session.getId());
                List<Map<String, Object>> skillAssessmentSummaries = skillAssessments.stream()
                    .map(sa -> {
                        Map<String, Object> skillSummary = new HashMap<>();
                        skillSummary.put("id", sa.getId());
                        skillSummary.put("skillCode", sa.getSkill().getSkillCode());
                        skillSummary.put("skillName", sa.getSkill().getDisplayName());
                        skillSummary.put("assessedLevel", sa.getAssessedLevel());
                        skillSummary.put("confidenceScore", sa.getConfidenceScore());
                        skillSummary.put("depthRating", sa.getDepthRating() != null ? sa.getDepthRating().toString() : null);
                        return skillSummary;
                    })
                    .collect(Collectors.toList());
                summary.put("skillAssessments", skillAssessmentSummaries);

                // Include response count for dashboard display
                summary.put("responseCount", session.getResponses() != null ? session.getResponses().size() : 0);

                // Include AI feedback link for persistence
                summary.put("hasAIFeedback", false); // TODO: Implement proper feedback check

                // Include roadmap data link
                summary.put("hasRoadmap", !session.getRoadmaps().isEmpty());

                return summary;
            })
            .toList();

        return ResponseEntity.ok(sessionSummaries);
    }
}
