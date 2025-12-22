package com.skillmap.service;

import com.skillmap.model.entity.*;
import com.skillmap.repository.AssessmentSessionRepository;
import com.skillmap.repository.ResponseRepository;
import com.skillmap.repository.UserProfileRepository;
import com.skillmap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserHistoryAnalysisService {

    private final UserRepository userRepository;
    private final AssessmentSessionRepository sessionRepository;
    private final ResponseRepository responseRepository;
    private final UserProfileRepository userProfileRepository;

    /**
     * Analyzes user history and returns comprehensive analytics
     */
    public UserHistoryAnalytics analyzeUserHistory(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<AssessmentSession> sessions = sessionRepository.findByUserId(userId);
        List<Response> responses = responseRepository.findByUserId(userId);

        UserHistoryAnalytics analytics = new UserHistoryAnalytics();
        analytics.setUserId(userId);
        analytics.setTotalSessions(sessions.size());
        analytics.setTotalResponses(responses.size());

        if (!sessions.isEmpty()) {
            analytics.setFirstAssessmentDate(sessions.get(0).getStartedAt());
            analytics.setLastAssessmentDate(sessions.get(sessions.size() - 1).getStartedAt());
            analytics.setAverageSessionDuration(calculateAverageSessionDuration(sessions));
        }

        analytics.setSkillProgression(analyzeSkillProgression(sessions));
        analytics.setCognitiveBiasTrends(analyzeCognitiveBiasTrends(sessions));
        analytics.setPerformancePatterns(analyzePerformancePatterns(responses));
        analytics.setLearningPreferences(inferLearningPreferences(responses));

        return analytics;
    }

    /**
     * Updates user profile based on recent session data
     */
    @Transactional
    public void updateUserProfile(Long userId, AssessmentSession latestSession) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElse(new UserProfile());

        if (profile.getId() == null) {
            profile.setUser(user);
        }

        // Update session statistics
        List<AssessmentSession> userSessions = sessionRepository.findByUserId(userId);
        profile.setTotalSessionsCompleted(userSessions.size());
        profile.setLastAssessmentDate(latestSession.getCompletedAt() != null ?
            latestSession.getCompletedAt() : LocalDateTime.now());

        // Calculate average session duration
        if (!userSessions.isEmpty()) {
            double avgDuration = userSessions.stream()
                .filter(s -> s.getTotalTimeSeconds() != null)
                .mapToInt(AssessmentSession::getTotalTimeSeconds)
                .average()
                .orElse(0.0);
            profile.setAverageSessionDurationMinutes(avgDuration / 60.0);
        }

        // Update skill progression data
        UserHistoryAnalytics analytics = analyzeUserHistory(userId);
        profile.setSkillProgressionData(serializeToJson(analytics.getSkillProgression()));
        profile.setCognitiveBiasTrends(serializeToJson(analytics.getCognitiveBiasTrends()));
        profile.setPerformancePatterns(serializeToJson(analytics.getPerformancePatterns()));

        // Infer learning preferences
        profile.setPreferredQuestionTypes(serializeToJson(analytics.getLearningPreferences().getPreferredQuestionTypes()));
        profile.setLearningStyle(analytics.getLearningPreferences().getLearningStyle());

        userProfileRepository.save(profile);
    }

    /**
     * Gets personalized question recommendations based on user history
     */
    public AdaptiveQuestionRecommendation getAdaptiveRecommendations(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        UserHistoryAnalytics analytics = analyzeUserHistory(userId);

        AdaptiveQuestionRecommendation recommendation = new AdaptiveQuestionRecommendation();

        if (profile != null && profile.getAdaptiveEnabled()) {
            // Use profile data for recommendations
            recommendation.setRecommendedDifficultyRange(profile.getPreferredDifficultyRange());
            recommendation.setQuestionTypeWeights(deserializeFromJson(profile.getQuestionTypeWeights(), Map.class));
            recommendation.setDifficultyAdjustmentRate(profile.getDifficultyAdjustmentRate());
        } else {
            // Fallback to analytics-based recommendations
            recommendation.setRecommendedDifficultyRange(calculateOptimalDifficultyRange(analytics));
            recommendation.setQuestionTypeWeights(analytics.getLearningPreferences().getPreferredQuestionTypes());
        }

        recommendation.setAvoidedQuestionTypes(analytics.getLearningPreferences().getStruggledQuestionTypes());
        recommendation.setRecommendedSessionLengthMinutes(calculateOptimalSessionLength(analytics));

        return recommendation;
    }

    private Double calculateAverageSessionDuration(List<AssessmentSession> sessions) {
        return sessions.stream()
            .filter(s -> s.getTotalTimeSeconds() != null)
            .mapToInt(AssessmentSession::getTotalTimeSeconds)
            .average()
            .orElse(0.0);
    }

    private Map<String, Object> analyzeSkillProgression(List<AssessmentSession> sessions) {
        Map<String, Object> progression = new HashMap<>();

        // Group sessions by target role
        Map<String, List<AssessmentSession>> sessionsByRole = sessions.stream()
            .filter(s -> s.getTargetRole() != null)
            .collect(Collectors.groupingBy(AssessmentSession::getTargetRole));

        Map<String, List<Double>> skillTrends = new HashMap<>();

        for (Map.Entry<String, List<AssessmentSession>> entry : sessionsByRole.entrySet()) {
            List<AssessmentSession> roleSessions = entry.getValue()
                .stream()
                .sorted(Comparator.comparing(AssessmentSession::getStartedAt))
                .collect(Collectors.toList());

            List<Double> averageLevels = new ArrayList<>();
            for (AssessmentSession session : roleSessions) {
                double avgLevel = session.getSkillAssessments().stream()
                    .filter(sa -> sa.getAssessedLevel() != null)
                    .mapToDouble(SkillAssessment::getAssessedLevel)
                    .average()
                    .orElse(0.0);
                averageLevels.add(avgLevel);
            }

            skillTrends.put(entry.getKey(), averageLevels);
        }

        progression.put("skillTrends", skillTrends);
        progression.put("overallImprovement", calculateOverallImprovement(sessions));

        return progression;
    }

    private Map<String, Object> analyzeCognitiveBiasTrends(List<AssessmentSession> sessions) {
        Map<String, Object> trends = new HashMap<>();

        // This would integrate with cognitive bias analysis service
        // For now, return placeholder structure
        trends.put("biasEvolution", new ArrayList<>());
        trends.put("improvementAreas", new ArrayList<>());
        trends.put("consistentStrengths", new ArrayList<>());

        return trends;
    }

    private Map<String, Object> analyzePerformancePatterns(List<Response> responses) {
        Map<String, Object> patterns = new HashMap<>();

        // Analyze response times, accuracy patterns, etc.
        Map<String, Double> averageResponseTimes = responses.stream()
            .filter(r -> r.getTotalTimeSeconds() != null)
            .collect(Collectors.groupingBy(
                r -> r.getQuestion().getQuestionType(),
                Collectors.averagingInt(Response::getTotalTimeSeconds)
            ));

        patterns.put("averageResponseTimesByType", averageResponseTimes);
        patterns.put("consistencyScore", calculateConsistencyScore(responses));
        patterns.put("learningCurve", analyzeLearningCurve(responses));

        return patterns;
    }

    private LearningPreferences inferLearningPreferences(List<Response> responses) {
        LearningPreferences preferences = new LearningPreferences();

        // Analyze which question types user performs best/worst
        Map<String, Double> accuracyByType = responses.stream()
            .filter(r -> r.getIsCorrect() != null)
            .collect(Collectors.groupingBy(
                r -> r.getQuestion().getQuestionType(),
                Collectors.averagingDouble(r -> r.getIsCorrect() ? 1.0 : 0.0)
            ));

        // Sort by accuracy to find preferences
        List<Map.Entry<String, Double>> sortedTypes = accuracyByType.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .collect(Collectors.toList());

        preferences.setPreferredQuestionTypes(sortedTypes.stream()
            .limit(3)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        preferences.setStruggledQuestionTypes(sortedTypes.stream()
            .skip(Math.max(0, sortedTypes.size() - 3))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        // Infer learning style based on response patterns
        preferences.setLearningStyle(inferLearningStyle(responses));

        return preferences;
    }

    private String inferLearningStyle(List<Response> responses) {
        // Simple heuristic based on response patterns
        long textResponses = responses.stream()
            .filter(r -> r.getResponseText() != null && r.getResponseText().length() > 50)
            .count();

        if (textResponses > responses.size() * 0.7) {
            return "reading";
        } else if (responses.stream().anyMatch(r -> r.getConfidenceLevel() != null)) {
            return "kinesthetic"; // Interactive engagement
        } else {
            return "visual"; // Default assumption
        }
    }

    private Double calculateOverallImprovement(List<AssessmentSession> sessions) {
        if (sessions.size() < 2) return 0.0;

        AssessmentSession first = sessions.get(0);
        AssessmentSession last = sessions.get(sessions.size() - 1);

        double firstAvg = first.getSkillAssessments().stream()
            .filter(sa -> sa.getAssessedLevel() != null)
            .mapToDouble(SkillAssessment::getAssessedLevel)
            .average()
            .orElse(0.0);

        double lastAvg = last.getSkillAssessments().stream()
            .filter(sa -> sa.getAssessedLevel() != null)
            .mapToDouble(SkillAssessment::getAssessedLevel)
            .average()
            .orElse(0.0);

        return lastAvg - firstAvg;
    }

    private Double calculateConsistencyScore(List<Response> responses) {
        if (responses.isEmpty()) return 0.0;

        List<Double> accuracies = responses.stream()
            .filter(r -> r.getIsCorrect() != null)
            .map(r -> r.getIsCorrect() ? 1.0 : 0.0)
            .collect(Collectors.toList());

        if (accuracies.isEmpty()) return 0.0;

        double mean = accuracies.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = accuracies.stream()
            .mapToDouble(acc -> Math.pow(acc - mean, 2))
            .average()
            .orElse(0.0);

        // Return 1 - normalized variance (higher consistency = lower variance)
        return Math.max(0.0, 1.0 - variance);
    }

    private List<Double> analyzeLearningCurve(List<Response> responses) {
        // Group responses by session and calculate rolling averages
        Map<Long, List<Response>> responsesBySession = responses.stream()
            .collect(Collectors.groupingBy(r -> r.getSession().getId()));

        List<Double> learningCurve = new ArrayList<>();
        for (List<Response> sessionResponses : responsesBySession.values()) {
            sessionResponses.sort(Comparator.comparing(r -> r.getAnsweredAt()));

            // Calculate rolling accuracy
            for (int i = 0; i < sessionResponses.size(); i++) {
                int windowSize = Math.min(5, i + 1);
                double rollingAccuracy = sessionResponses.subList(i - windowSize + 1, i + 1).stream()
                    .filter(r -> r.getIsCorrect() != null)
                    .mapToDouble(r -> r.getIsCorrect() ? 1.0 : 0.0)
                    .average()
                    .orElse(0.0);
                learningCurve.add(rollingAccuracy);
            }
        }

        return learningCurve;
    }

    private String calculateOptimalDifficultyRange(UserHistoryAnalytics analytics) {
        double avgLevel = analytics.getSkillProgression().values().stream()
            .filter(v -> v instanceof Number)
            .mapToDouble(v -> ((Number) v).doubleValue())
            .average()
            .orElse(0.5);

        double range = 0.2; // ±0.2 range
        double min = Math.max(0.1, avgLevel - range);
        double max = Math.min(0.9, avgLevel + range);

        return String.format("%.1f-%.1f", min, max);
    }

    private Integer calculateOptimalSessionLength(UserHistoryAnalytics analytics) {
        if (analytics.getAverageSessionDuration() == null) return 30;

        // Suggest slightly longer than average, but cap at 60 minutes
        return Math.min(60, (int) Math.ceil(analytics.getAverageSessionDuration() * 1.2));
    }

    private String serializeToJson(Object object) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(object);
        } catch (Exception e) {
            log.error("Failed to serialize object to JSON", e);
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T deserializeFromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, clazz);
        } catch (Exception e) {
            log.error("Failed to deserialize JSON to {}", clazz.getSimpleName(), e);
            return null;
        }
    }

    // DTO Classes
    public static class UserHistoryAnalytics {
        private Long userId;
        private Integer totalSessions;
        private Integer totalResponses;
        private LocalDateTime firstAssessmentDate;
        private LocalDateTime lastAssessmentDate;
        private Double averageSessionDuration;
        private Map<String, Object> skillProgression;
        private Map<String, Object> cognitiveBiasTrends;
        private Map<String, Object> performancePatterns;
        private LearningPreferences learningPreferences;

        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Integer getTotalSessions() { return totalSessions; }
        public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions; }

        public Integer getTotalResponses() { return totalResponses; }
        public void setTotalResponses(Integer totalResponses) { this.totalResponses = totalResponses; }

        public LocalDateTime getFirstAssessmentDate() { return firstAssessmentDate; }
        public void setFirstAssessmentDate(LocalDateTime firstAssessmentDate) { this.firstAssessmentDate = firstAssessmentDate; }

        public LocalDateTime getLastAssessmentDate() { return lastAssessmentDate; }
        public void setLastAssessmentDate(LocalDateTime lastAssessmentDate) { this.lastAssessmentDate = lastAssessmentDate; }

        public Double getAverageSessionDuration() { return averageSessionDuration; }
        public void setAverageSessionDuration(Double averageSessionDuration) { this.averageSessionDuration = averageSessionDuration; }

        public Map<String, Object> getSkillProgression() { return skillProgression; }
        public void setSkillProgression(Map<String, Object> skillProgression) { this.skillProgression = skillProgression; }

        public Map<String, Object> getCognitiveBiasTrends() { return cognitiveBiasTrends; }
        public void setCognitiveBiasTrends(Map<String, Object> cognitiveBiasTrends) { this.cognitiveBiasTrends = cognitiveBiasTrends; }

        public Map<String, Object> getPerformancePatterns() { return performancePatterns; }
        public void setPerformancePatterns(Map<String, Object> performancePatterns) { this.performancePatterns = performancePatterns; }

        public LearningPreferences getLearningPreferences() { return learningPreferences; }
        public void setLearningPreferences(LearningPreferences learningPreferences) { this.learningPreferences = learningPreferences; }
    }

    public static class LearningPreferences {
        private Map<String, Double> preferredQuestionTypes;
        private Map<String, Double> struggledQuestionTypes;
        private String learningStyle;

        public Map<String, Double> getPreferredQuestionTypes() { return preferredQuestionTypes; }
        public void setPreferredQuestionTypes(Map<String, Double> preferredQuestionTypes) { this.preferredQuestionTypes = preferredQuestionTypes; }

        public Map<String, Double> getStruggledQuestionTypes() { return struggledQuestionTypes; }
        public void setStruggledQuestionTypes(Map<String, Double> struggledQuestionTypes) { this.struggledQuestionTypes = struggledQuestionTypes; }

        public String getLearningStyle() { return learningStyle; }
        public void setLearningStyle(String learningStyle) { this.learningStyle = learningStyle; }
    }

    public static class AdaptiveQuestionRecommendation {
        private String recommendedDifficultyRange;
        private Map<String, Double> questionTypeWeights;
        private Double difficultyAdjustmentRate;
        private Map<String, Double> avoidedQuestionTypes;
        private Integer recommendedSessionLengthMinutes;

        public String getRecommendedDifficultyRange() { return recommendedDifficultyRange; }
        public void setRecommendedDifficultyRange(String recommendedDifficultyRange) { this.recommendedDifficultyRange = recommendedDifficultyRange; }

        public Map<String, Double> getQuestionTypeWeights() { return questionTypeWeights; }
        public void setQuestionTypeWeights(Map<String, Double> questionTypeWeights) { this.questionTypeWeights = questionTypeWeights; }

        public Double getDifficultyAdjustmentRate() { return difficultyAdjustmentRate; }
        public void setDifficultyAdjustmentRate(Double difficultyAdjustmentRate) { this.difficultyAdjustmentRate = difficultyAdjustmentRate; }

        public Map<String, Double> getAvoidedQuestionTypes() { return avoidedQuestionTypes; }
        public void setAvoidedQuestionTypes(Map<String, Double> avoidedQuestionTypes) { this.avoidedQuestionTypes = avoidedQuestionTypes; }

        public Integer getRecommendedSessionLengthMinutes() { return recommendedSessionLengthMinutes; }
        public void setRecommendedSessionLengthMinutes(Integer recommendedSessionLengthMinutes) { this.recommendedSessionLengthMinutes = recommendedSessionLengthMinutes; }
    }
}
