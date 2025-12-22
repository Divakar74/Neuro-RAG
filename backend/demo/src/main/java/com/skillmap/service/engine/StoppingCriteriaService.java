package com.skillmap.service.engine;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.Response;
import com.skillmap.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoppingCriteriaService {

    private final ResponseRepository responseRepository;
    private final SkillInferenceEngine skillInferenceEngine;

    // Configuration constants
    private static final int MIN_QUESTIONS = 10;
    private static final int MAX_QUESTIONS = 15;
    private static final int MAX_TIME_MINUTES = 45;
    private static final double CONFIDENCE_THRESHOLD = 0.7;
    private static final double SKILL_COVERAGE_THRESHOLD = 0.6;

    public boolean shouldStopAssessment(AssessmentSession session) {
        log.debug("Evaluating stopping criteria for session: {}", session.getId());

        List<Response> responses = responseRepository.findBySessionId(session.getId());

        // Always continue if minimum questions not reached
        if (responses.size() < MIN_QUESTIONS) {
            log.debug("Continuing: minimum questions not reached ({}/{})", responses.size(), MIN_QUESTIONS);
            return false;
        }

        // Stop if maximum questions reached
        if (responses.size() >= MAX_QUESTIONS) {
            log.debug("Stopping: maximum questions reached ({})", responses.size());
            return true;
        }

        // Stop if time limit exceeded
        if (hasExceededTimeLimit(responses)) {
            log.debug("Stopping: time limit exceeded");
            return true;
        }

        // Stop if sufficient confidence achieved
        if (hasSufficientConfidence()) {
            log.debug("Stopping: sufficient confidence achieved");
            return true;
        }

        // Stop if good skill coverage achieved
        if (hasGoodSkillCoverage(session)) {
            log.debug("Stopping: good skill coverage achieved");
            return true;
        }

        // Continue assessment
        log.debug("Continuing assessment - no stopping criteria met");
        return false;
    }

    private boolean hasExceededTimeLimit(List<Response> responses) {
        int totalTimeSeconds = responses.stream()
            .mapToInt(response -> response.getTotalTimeSeconds() != null ? response.getTotalTimeSeconds() : 0)
            .sum();

        int maxTimeSeconds = MAX_TIME_MINUTES * 60;
        return totalTimeSeconds > maxTimeSeconds;
    }

    private boolean hasSufficientConfidence() {
        // Note: This method needs session context to work properly
        // For now, return false to avoid using deprecated method
        return false;
    }

    private boolean hasGoodSkillCoverage(AssessmentSession session) {
        Map<String, Double> skillBeliefs = skillInferenceEngine.getSkillBeliefs(session);

        // For targeted assessments, check coverage of relevant skills
        if (session.getTargetRole() != null) {
            long relevantSkills = skillBeliefs.size(); // Simplified - all skills are relevant
            long assessedSkills = skillBeliefs.values().stream()
                .filter(belief -> belief != 0.5) // Not neutral
                .count();

            double coverageRatio = (double) assessedSkills / relevantSkills;
            return coverageRatio >= SKILL_COVERAGE_THRESHOLD;
        }

        return false;
    }

    public AssessmentStopReason getStopReason(AssessmentSession session) {
        List<Response> responses = responseRepository.findBySessionId(session.getId());

        if (responses.size() >= MAX_QUESTIONS) {
            return AssessmentStopReason.MAX_QUESTIONS_REACHED;
        }

        if (hasExceededTimeLimit(responses)) {
            return AssessmentStopReason.TIME_LIMIT_EXCEEDED;
        }

        if (hasSufficientConfidence()) {
            return AssessmentStopReason.SUFFICIENT_CONFIDENCE;
        }

        if (hasGoodSkillCoverage(session)) {
            return AssessmentStopReason.GOOD_COVERAGE;
        }

        return AssessmentStopReason.UNKNOWN;
    }

    public Map<String, Object> getStoppingCriteriaStatus(AssessmentSession session) {
        List<Response> responses = responseRepository.findBySessionId(session.getId());
        Map<String, Double> skillBeliefs = skillInferenceEngine.getSkillBeliefs(session);

        Map<String, Object> status = new HashMap<>();
        status.put("questionsAnswered", responses.size());
        status.put("minQuestions", MIN_QUESTIONS);
        status.put("maxQuestions", MAX_QUESTIONS);

        int totalTimeSeconds = responses.stream()
            .mapToInt(response -> response.getTotalTimeSeconds() != null ? response.getTotalTimeSeconds() : 0)
            .sum();
        status.put("totalTimeMinutes", totalTimeSeconds / 60.0);
        status.put("maxTimeMinutes", MAX_TIME_MINUTES);

        // Confidence metrics
        long confidentSkills = skillBeliefs.values().stream()
            .filter(belief -> belief >= CONFIDENCE_THRESHOLD || belief <= (1 - CONFIDENCE_THRESHOLD))
            .count();
        double confidenceRatio = skillBeliefs.isEmpty() ? 0 : (double) confidentSkills / skillBeliefs.size();
        status.put("confidenceRatio", confidenceRatio);
        status.put("confidenceThreshold", SKILL_COVERAGE_THRESHOLD);

        // Coverage metrics
        long assessedSkills = skillBeliefs.values().stream()
            .filter(belief -> belief != 0.5)
            .count();
        double coverageRatio = skillBeliefs.isEmpty() ? 0 : (double) assessedSkills / skillBeliefs.size();
        status.put("coverageRatio", coverageRatio);
        status.put("coverageThreshold", SKILL_COVERAGE_THRESHOLD);

        status.put("shouldStop", shouldStopAssessment(session));

        return status;
    }

    public enum AssessmentStopReason {
        MAX_QUESTIONS_REACHED,
        TIME_LIMIT_EXCEEDED,
        SUFFICIENT_CONFIDENCE,
        GOOD_COVERAGE,
        UNKNOWN
    }
}
