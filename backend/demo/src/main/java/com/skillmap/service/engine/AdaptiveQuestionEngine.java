package com.skillmap.service.engine;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.Question;
import com.skillmap.model.entity.Response;
import com.skillmap.repository.AssessmentSessionRepository;
import com.skillmap.repository.QuestionRepository;
import com.skillmap.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdaptiveQuestionEngine {

    private final QuestionRepository questionRepository;
    private final ResponseRepository responseRepository;
    private final AssessmentSessionRepository sessionRepository;
    private final SkillInferenceEngine skillInferenceEngine;
    private final SkillGraphService skillGraphService;

    // Cache for questions to avoid repeated database calls
    private volatile List<Question> cachedQuestions = null;
    private volatile long lastCacheUpdate = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes TTL

    /**
     * Get cached questions, refreshing if necessary
     */
    private List<Question> getCachedQuestions() {
        long now = System.currentTimeMillis();
        if (cachedQuestions == null || (now - lastCacheUpdate) > CACHE_TTL_MS) {
            synchronized (this) {
                if (cachedQuestions == null || (now - lastCacheUpdate) > CACHE_TTL_MS) {
                    log.debug("Refreshing questions cache");
                    cachedQuestions = questionRepository.findAll();
                    lastCacheUpdate = now;
                }
            }
        }
        return cachedQuestions;
    }

    /**
     * Force refresh of questions cache (useful if questions are updated)
     */
    public void refreshQuestionsCache() {
        synchronized (this) {
            cachedQuestions = null;
            lastCacheUpdate = 0;
        }
    }

    public Question selectNextQuestion(AssessmentSession session) {
        log.info("Selecting next question for session: {}", session.getId());

        // Get all questions from cache
        List<Question> availableQuestions = getCachedQuestions();

        // Filter by target role if specified; be forgiving and fall back on empty
        if (session.getTargetRole() != null) {
            String role = session.getTargetRole().toLowerCase();
            List<Question> roleFiltered = availableQuestions.stream()
                .filter(q -> {
                    boolean match = false;
                    if (q.getSkill().getCategory() != null) {
                        match = q.getSkill().getCategory().name().toLowerCase().contains(role);
                    }
                    if (!match && q.getSkill().getSkillCode() != null) {
                        match = q.getSkill().getSkillCode().toLowerCase().contains(role);
                    }
                    if (!match && q.getSkill().getDisplayName() != null) {
                        match = q.getSkill().getDisplayName().toLowerCase().contains(role);
                    }
                    // Common mapping: software engineer/developer -> Programming category
                    if (!match && (role.contains("engineer") || role.contains("developer"))) {
                        match = q.getSkill().getCategory() != null && q.getSkill().getCategory().name().equalsIgnoreCase("Programming");
                    }
                    return match;
                })
                .collect(Collectors.toList());

            if (!roleFiltered.isEmpty()) {
                availableQuestions = roleFiltered;
            }
            // else keep all questions as fallback to avoid empty sets
        }

        // Filter out already answered questions
        List<Long> answeredQuestionIds = responseRepository.findAnsweredQuestionIds(session.getId());

        List<Question> unansweredQuestions = availableQuestions.stream()
            .filter(q -> !answeredQuestionIds.contains(q.getId()))
            .collect(Collectors.toList());

        if (unansweredQuestions.isEmpty()) {
            log.info("No more questions available for session: {}", session.getId());
            return null;
        }

        // For performance, use simpler selection for first few questions, then adaptive
        int questionsAnswered = answeredQuestionIds.size();
        if (questionsAnswered < 3) {
            // Simple random selection for first 3 questions to get started quickly
            return unansweredQuestions.get(new Random().nextInt(Math.min(unansweredQuestions.size(), 10)));
        }

        // Use adaptive selection algorithm for later questions
        try {
            return selectAdaptiveQuestion(unansweredQuestions, session);
        } catch (Exception e) {
            log.warn("Adaptive selection failed for session {}: {}, falling back to MCQ questions", session.getId(), e.getMessage());
            // Fallback to MCQ questions if adaptive selection fails
            List<Question> mcqQuestions = unansweredQuestions.stream()
                .filter(q -> "mcq".equalsIgnoreCase(q.getQuestionType()) || "choice".equalsIgnoreCase(q.getQuestionType()))
                .collect(Collectors.toList());

            if (!mcqQuestions.isEmpty()) {
                return mcqQuestions.get(new Random().nextInt(mcqQuestions.size()));
            }

            // If no MCQs available, return any random question
            return unansweredQuestions.get(new Random().nextInt(unansweredQuestions.size()));
        }
    }

    private Question selectAdaptiveQuestion(List<Question> questions, AssessmentSession session) {
        // Calculate information gain for each question
        Map<Question, Double> questionScores = new HashMap<>();

        for (Question question : questions) {
            double score = calculateQuestionScore(question, session);
            questionScores.put(question, score);
        }

        // Select question with highest information gain
        return questionScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(questions.get(0)); // Fallback to first question
    }

    private double calculateQuestionScore(Question question, AssessmentSession session) {
        double score = 0.0;

        try {
            if (question.getSkill() == null) {
                log.warn("Question {} has null skill, returning neutral score", question.getId());
                return 0.5;
            }

            String skillCode = question.getSkill().getSkillCode();
            if (skillCode == null || skillCode.trim().isEmpty()) {
                log.warn("Question {} has null or empty skillCode, returning neutral score", question.getId());
                return 0.5;
            }

            double currentBelief = skillInferenceEngine.getSkillBelief(session, skillCode);

            // Score based on uncertainty (questions about uncertain skills are more valuable)
            double uncertainty = Math.abs(currentBelief - 0.5) * 2; // 0 to 1 scale
            score += uncertainty * 0.4;

            // Score based on skill importance (prerequisites are more important)
            int skillLevel = skillGraphService.getSkillLevel(skillCode);
            if (skillLevel > 0) {
                score += (1.0 / skillLevel) * 0.3; // Higher level skills get lower priority initially
            } else {
                score += 0.3; // Default score if skill level is invalid
            }

            // Score based on question difficulty (prefer medium difficulty)
            double difficulty = question.getDifficultyLevel() != null ? question.getDifficultyLevel() : 0.5;
            double difficultyScore = 1.0 - Math.abs(difficulty - 0.5) * 2; // Peak at 0.5
            score += difficultyScore * 0.2;

            // Score based on question type diversity (prefer variety)
            double typeDiversity = calculateTypeDiversity(question, session);
            score += typeDiversity * 0.1;
        } catch (Exception e) {
            // If any calculation fails, return a neutral score to avoid breaking the selection
            log.warn("Error calculating question score for question {}: {}", question.getId(), e.getMessage());
            score = 0.5;
        }

        return score;
    }

    private double calculateTypeDiversity(Question question, AssessmentSession session) {
        // Get recently asked question types
        List<Response> recentResponses = responseRepository.findBySessionId(session.getId());

        if (recentResponses.size() < 3) {
            return 0.5; // Neutral score for early questions
        }

        List<String> recentTypes = recentResponses.subList(Math.max(0, recentResponses.size() - 3), recentResponses.size())
            .stream()
            .map(response -> response.getQuestion().getQuestionText()) // Using questionText as proxy for type
            .collect(Collectors.toList());

        String currentType = question.getQuestionText();

        // Penalize if same type as last 2 questions
        long sameTypeCount = recentTypes.stream()
            .filter(type -> type.equals(currentType))
            .count();

        return sameTypeCount > 1 ? 0.0 : 0.5;
    }

    public boolean shouldContinueAssessment(AssessmentSession session) {
        // Check stopping criteria
        List<Response> responses = responseRepository.findBySessionId(session.getId());

        // Minimum number of questions
        if (responses.size() < 5) {
            return true;
        }

        // Maximum number of questions
        if (responses.size() >= 20) {
            return false;
        }

        // Check confidence levels using session-specific beliefs
        Map<String, Double> skillBeliefs = skillInferenceEngine.getSkillBeliefs(session);
        long highConfidenceSkills = skillBeliefs.values().stream()
            .filter(belief -> belief > 0.8 || belief < 0.2)
            .count();

        // Continue if we have enough high-confidence assessments
        double confidenceRatio = (double) highConfidenceSkills / skillBeliefs.size();
        if (confidenceRatio < 0.6) {
            return true;
        }

        // Check time spent
        int totalTime = responses.stream()
            .mapToInt(response -> response.getTotalTimeSeconds() != null ? response.getTotalTimeSeconds() : 0)
            .sum();

        // Stop if total time exceeds reasonable limit (30 minutes)
        if (totalTime > 1800) {
            return false;
        }

        return false; // Default to stopping
    }

    public List<Question> getRecommendedQuestions(AssessmentSession session, int count) {
        List<Question> availableQuestions = getCachedQuestions();

        // Filter by target role if specified
        if (session.getTargetRole() != null) {
            availableQuestions = availableQuestions.stream()
                .filter(q -> q.getSkill().getCategory() != null &&
                           q.getSkill().getCategory().name().toLowerCase().contains(session.getTargetRole().toLowerCase()))
                .collect(Collectors.toList());
        }

        // Sort by adaptive score
        return availableQuestions.stream()
            .sorted((q1, q2) -> Double.compare(
                calculateQuestionScore(q2, session),
                calculateQuestionScore(q1, session)
            ))
            .limit(count)
            .collect(Collectors.toList());
    }

    public Map<String, Object> getAssessmentProgress(AssessmentSession session) {
        List<Response> responses = responseRepository.findBySessionId(session.getId());
        Map<String, Double> skillBeliefs = skillInferenceEngine.getSkillBeliefs(session);

        Map<String, Object> progress = new HashMap<>();
        progress.put("questionsAnswered", responses.size());

        // Count total questions for the target role (fallback if none)
        List<Question> totalQuestions = getCachedQuestions();
        if (session.getTargetRole() != null) {
            String role = session.getTargetRole().toLowerCase();
            List<Question> roleFiltered = totalQuestions.stream()
                .filter(q -> {
                    boolean match = false;
                    if (q.getSkill().getCategory() != null) {
                        match = q.getSkill().getCategory().name().toLowerCase().contains(role);
                    }
                    if (!match && q.getSkill().getSkillCode() != null) {
                        match = q.getSkill().getSkillCode().toLowerCase().contains(role);
                    }
                    if (!match && q.getSkill().getDisplayName() != null) {
                        match = q.getSkill().getDisplayName().toLowerCase().contains(role);
                    }
                    if (!match && (role.contains("engineer") || role.contains("developer"))) {
                        match = q.getSkill().getCategory() != null && q.getSkill().getCategory().name().equalsIgnoreCase("Programming");
                    }
                    return match;
                })
                .collect(Collectors.toList());
            if (!roleFiltered.isEmpty()) {
                totalQuestions = roleFiltered;
            }
        }
        progress.put("totalQuestions", totalQuestions.size());

        progress.put("skillConfidenceLevels", skillBeliefs);
        progress.put("shouldContinue", shouldContinueAssessment(session));

        // Calculate overall progress
        double averageConfidence = skillBeliefs.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.5);

        progress.put("overallProgress", averageConfidence);

        return progress;
    }
}
