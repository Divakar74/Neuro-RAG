package com.skillmap.service;

import com.skillmap.model.entity.AIResponseCache;
import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.Response;
import com.skillmap.model.entity.ResumeData;
import com.skillmap.repository.AIResponseCacheRepository;
import com.skillmap.service.analysis.CognitiveBiasAnalysisService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OpenAIService {

    private final OpenAiService openAiService;

    @Autowired
    private AIResponseCacheRepository cacheRepository;

    @Value("${ai.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${ai.cache.ttl.hours:24}")
    private int cacheTtlHours;

    @Autowired
    private CognitiveBiasAnalysisService cognitiveBiasAnalysisService;

    public OpenAIService(@Value("${openai.api.key:sk-placeholder}") String apiKey) {
        String sanitized = apiKey == null ? "" : apiKey.replace("\r", "").replace("\n", "").trim();
        this.openAiService = new OpenAiService(sanitized, Duration.ofSeconds(30));
        log.info("OpenAI service initialized with caching {}", cacheEnabled ? "enabled" : "disabled");
    }

    public String generateSuggestion(String prompt) {
        return generateSuggestion(prompt, null, null, "suggestion");
    }

    public String generateSuggestion(String prompt, Long sessionId, Long userId, String requestType) {
        return generateSuggestionWithModel(prompt, sessionId, userId, requestType, "gpt-4", 1000, 0.7);
    }

    public String generateSuggestionWithModel(String prompt, Long sessionId, Long userId, String requestType,
                                            String model, int maxTokens, double temperature) {
        // Check cache first if enabled
        if (cacheEnabled) {
            String promptHash = generateHash(prompt);
            Optional<AIResponseCache> cachedResponse;
            if (userId != null) {
                cachedResponse = cacheRepository.findByUserIdAndPromptHashAndRequestType(userId, promptHash, requestType);
            } else {
                cachedResponse = cacheRepository.findByPromptHashAndRequestType(promptHash, requestType);
            }

            if (cachedResponse.isPresent() && !cachedResponse.get().isExpired()) {
                AIResponseCache cache = cachedResponse.get();
                cache.incrementHitCount();
                cacheRepository.save(cache);
                log.debug("Cache hit for prompt hash: {} (hit count: {})", promptHash, cache.getHitCount());
                return cache.getResponse();
            }
        }

        try {
            ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(),
                "You are an expert career counselor and skill development advisor. " +
                "Provide personalized, actionable suggestions based on user assessment responses. " +
                "Keep suggestions concise, practical, and encouraging.");

            ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), prompt);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(systemMessage, userMessage))
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();

            ChatCompletionResult result = openAiService.createChatCompletion(request);

            if (result.getChoices().isEmpty()) {
                log.warn("No choices returned from OpenAI API");
                return "Unable to generate suggestion at this time.";
            }

            String response = result.getChoices().get(0).getMessage().getContent();
            log.debug("Generated suggestion: {}", response);

            // Cache the response if caching is enabled
            if (cacheEnabled) {
                cacheResponse(prompt, response, sessionId, userId, requestType,
                    result.getUsage() != null ? result.getUsage().getTotalTokens() : null, model);
            }

            return response;

        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            return "Sorry, I couldn't generate a personalized suggestion right now. Please try again later.";
        }
    }

    public boolean testConnectivity() {
        try {
            ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(),
                "You are a health-check agent.");
            ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), "ping");
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(List.of(systemMessage, userMessage))
                .maxTokens(5)
                .temperature(0.0)
                .build();
            ChatCompletionResult result = openAiService.createChatCompletion(request);
            return result != null && !result.getChoices().isEmpty();
        } catch (Exception e) {
            log.error("OpenAI connectivity test failed", e);
            return false;
        }
    }

    public String generatePersonalizedSuggestions(String userResponsesSummary, String skillAnalysis, Long sessionId, Long userId) {
        String prompt = String.format(
            "Based on the following user assessment responses and skill analysis, " +
            "provide 3-5 personalized career development suggestions:\n\n" +
            "User Responses Summary:\n%s\n\n" +
            "Skill Analysis:\n%s\n\n" +
            "Suggestions should be specific, actionable, and focused on skill improvement and career growth.",
            userResponsesSummary, skillAnalysis
        );

        return generateSuggestion(prompt, sessionId, userId, "personalized_suggestions");
    }

    /**
     * Generates detailed suggestions with evidence from user responses, performance metrics, and cognitive context
     */
    public String generateDetailedSuggestionsWithEvidence(AssessmentSession session, List<Response> responses,
                                                        ResumeData resumeData, Map<String, Object> performanceMetrics) {
        log.info("Generating detailed suggestions with evidence for session: {}", session.getId());

        // Build comprehensive context
        String userProfile = buildUserProfileContext(session, resumeData);
        String responseEvidence = buildResponseEvidenceContext(responses);
        String performanceContext = buildPerformanceContext(performanceMetrics);
        String cognitiveContext = buildCognitiveContext(session);

        String prompt = String.format(
            "You are an expert career counselor using advanced Neuro-RAG analysis. " +
            "Generate highly detailed, evidence-based career development suggestions.\n\n" +
            "USER PROFILE:\n%s\n\n" +
            "RESPONSE EVIDENCE:\n%s\n\n" +
            "PERFORMANCE METRICS:\n%s\n\n" +
            "COGNITIVE CONTEXT:\n%s\n\n" +
            "TASK: Provide 4-6 specific, actionable suggestions with:\n" +
            "1. Evidence from user's actual responses\n" +
            "2. Performance data supporting the recommendation\n" +
            "3. Cognitive factors to consider\n" +
            "4. Concrete next steps with timelines\n" +
            "5. Expected outcomes and success metrics\n\n" +
            "Format each suggestion as:\n" +
            "**Suggestion X: [Title]**\n" +
            "*Evidence:* [Specific user response/performance data]\n" +
            "*Rationale:* [Why this helps based on cognitive/learning patterns]\n" +
            "*Action Plan:* [3-5 specific steps with timeline]\n" +
            "*Expected Impact:* [Measurable outcomes]\n\n" +
            "Ensure suggestions are tailored to the user's demonstrated strengths, weaknesses, and learning style.",
            userProfile, responseEvidence, performanceContext, cognitiveContext
        );

        return generateSuggestionWithModel(prompt, session.getId(), session.getUser().getId(),
                                         "detailed_evidence_suggestions", "gpt-4", 2000, 0.6);
    }

    /**
     * Builds comprehensive user profile context from session and resume data
     */
    private String buildUserProfileContext(AssessmentSession session, ResumeData resumeData) {
        StringBuilder profile = new StringBuilder();
        profile.append("Assessment Session: ").append(session.getId()).append("\n");
        profile.append("User ID: ").append(session.getUser().getId()).append("\n");

        if (resumeData != null) {
            profile.append("Experience Level: ");
            if (resumeData.getTotalYearsExperience() != null) {
                double years = resumeData.getTotalYearsExperience();
                if (years < 2) profile.append("Entry-level (").append(years).append(" years)");
                else if (years < 5) profile.append("Mid-level (").append(years).append(" years)");
                else profile.append("Senior-level (").append(years).append(" years)");
            } else {
                profile.append("Unknown");
            }
            profile.append("\n");

            profile.append("Skills: ");
            if (resumeData.getExtractedSkills() != null) {
                try {
                    @SuppressWarnings("unchecked")
                    List<String> skills = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(resumeData.getExtractedSkills(), List.class);
                    profile.append(String.join(", ", skills));
                } catch (Exception e) {
                    profile.append("Unable to parse skills");
                }
            } else {
                profile.append("Not available");
            }
            profile.append("\n");

            profile.append("Education: ");
            if (resumeData.getExtractedEducation() != null) {
                profile.append(resumeData.getExtractedEducation());
            } else {
                profile.append("Not available");
            }
        }

        return profile.toString();
    }

    /**
     * Builds detailed evidence context from user responses
     */
    private String buildResponseEvidenceContext(List<Response> responses) {
        return responses.stream()
            .map(response -> String.format(
                "Question: %s\n" +
                "User Response: %s\n" +
                "Correct: %s\n" +
                "Confidence: %.2f\n" +
                "Similarity Score: %.2f\n" +
                "Typing Speed: %.0f WPM\n" +
                "Think Time: %d seconds\n" +
                "Evidence: %s",
                response.getQuestion().getQuestionText(),
                response.getResponseText() != null ? response.getResponseText() : "N/A",
                response.getIsCorrect() != null ? response.getIsCorrect() : "N/A",
                response.getConfidenceLevel() != null ? response.getConfidenceLevel() : 0.0,
                response.getSimilarityScore() != null ? response.getSimilarityScore() : 0.0,
                response.getTypingSpeedWpm() != null ? response.getTypingSpeedWpm() : 0.0,
                response.getThinkTimeSeconds() != null ? response.getThinkTimeSeconds() : 0,
                extractKeyEvidence(response)
            ))
            .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * Extracts key evidence from a response for analysis
     */
    private String extractKeyEvidence(Response response) {
        StringBuilder evidence = new StringBuilder();

        if (response.getResponseText() != null && !response.getResponseText().trim().isEmpty()) {
            String text = response.getResponseText().toLowerCase();
            if (text.contains("don't know") || text.contains("unsure") || text.length() < 10) {
                evidence.append("Shows knowledge gap or uncertainty");
            } else if (response.getTypingSpeedWpm() != null && response.getTypingSpeedWpm() > 60) {
                evidence.append("Quick response suggests strong familiarity");
            } else if (response.getThinkTimeSeconds() != null && response.getThinkTimeSeconds() > 60) {
                evidence.append("Extended thinking time indicates deep consideration");
            } else {
                evidence.append("Standard response with moderate confidence");
            }
        }

        if (response.getEditCount() != null && response.getEditCount() > 3) {
            evidence.append("; Multiple edits suggest careful revision");
        }

        if (Boolean.TRUE.equals(response.getPasteDetected())) {
            evidence.append("; Paste detected - may indicate external reference use");
        }

        return evidence.toString();
    }

    /**
     * Builds performance context from metrics
     */
    private String buildPerformanceContext(Map<String, Object> metrics) {
        StringBuilder context = new StringBuilder();

        if (metrics.containsKey("averageSimilarity")) {
            double similarity = (Double) metrics.get("averageSimilarity");
            context.append(String.format("Average Similarity: %.2f (", similarity));
            if (similarity > 0.8) context.append("Excellent alignment with expected answers)");
            else if (similarity > 0.6) context.append("Good alignment with some room for improvement)");
            else context.append("Significant gaps in understanding)");
            context.append("\n");
        }

        if (metrics.containsKey("mcqAccuracy")) {
            double accuracy = (Double) metrics.get("mcqAccuracy");
            context.append(String.format("MCQ Accuracy: %.1f%%\n", accuracy * 100));
        }

        if (metrics.containsKey("averageConfidence")) {
            double confidence = (Double) metrics.get("averageConfidence");
            context.append(String.format("Average Confidence: %.2f\n", confidence));
        }

        if (metrics.containsKey("averageTypingSpeed")) {
            double speed = (Double) metrics.get("averageTypingSpeed");
            context.append(String.format("Average Typing Speed: %.0f WPM\n", speed));
        }

        if (metrics.containsKey("pasteRate")) {
            double pasteRate = (Double) metrics.get("pasteRate");
            context.append(String.format("Paste Detection Rate: %.1f%%\n", pasteRate * 100));
        }

        if (metrics.containsKey("performanceScore")) {
            double score = (Double) metrics.get("performanceScore");
            context.append(String.format("Overall Performance Score: %.2f\n", score));
        }

        return context.toString();
    }

    /**
     * Builds cognitive context from session analysis
     */
    private String buildCognitiveContext(AssessmentSession session) {
        try {
            List<CognitiveBiasAnalysisService.CognitiveBiasResult> biases =
                cognitiveBiasAnalysisService.analyzeCognitiveBiases(session);

            return biases.stream()
                .map(bias -> String.format(
                    "Bias: %s (Level: %s, Score: %.2f)\n" +
                    "Impact: %s\n" +
                    "Recommendations: %s",
                    bias.getType(), bias.getLevel(), bias.getScore(),
                    bias.getImpact(),
                    String.join(", ", bias.getRecommendations())
                ))
                .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            log.warn("Failed to build cognitive context", e);
            return "Cognitive analysis not available";
        }
    }

    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating hash", e);
            return String.valueOf(input.hashCode());
        }
    }

    private void cacheResponse(String prompt, String response, Long sessionId, Long userId, String requestType, Long tokensUsed, String model) {
        try {
            String promptHash = generateHash(prompt);
            String cacheKey = requestType + "_" + promptHash;

            AIResponseCache cache = new AIResponseCache();
            cache.setCacheKey(cacheKey);
            cache.setPromptHash(promptHash);
            cache.setPrompt(prompt);
            cache.setResponse(response);
            cache.setSessionId(sessionId);
            cache.setUserId(userId);
            cache.setRequestType(requestType);
            cache.setTokensUsed(tokensUsed != null ? tokensUsed.intValue() : null);
            cache.setModelUsed(model);
            cache.setExpiresAt(LocalDateTime.now().plusHours(cacheTtlHours));
            cache.setCreatedAt(LocalDateTime.now());
            cache.setHitCount(0);

            cacheRepository.save(cache);
            log.debug("Cached response for prompt hash: {} (tokens used: {})", promptHash, tokensUsed);
        } catch (Exception e) {
            log.error("Error caching response", e);
        }
    }

    /**
     * Generates tailored suggestions based on user learning preferences and cognitive patterns
     */
    public String generateTailoredSuggestionsWithPreferences(AssessmentSession session, List<Response> responses,
                                                           ResumeData resumeData, Map<String, Object> performanceMetrics,
                                                           Map<String, String> userPreferences) {
        log.info("Generating tailored suggestions with user preferences for session: {}", session.getId());

        // Build context with user preferences
        String userProfile = buildUserProfileContext(session, resumeData);
        String responseEvidence = buildResponseEvidenceContext(responses);
        String performanceContext = buildPerformanceContext(performanceMetrics);
        String cognitiveContext = buildCognitiveContext(session);
        String preferencesContext = buildPreferencesContext(userPreferences);

        String prompt = String.format(
            "You are an expert career counselor specializing in personalized learning strategies. " +
            "Generate suggestions that adapt to the user's learning preferences and cognitive patterns.\n\n" +
            "USER PROFILE:\n%s\n\n" +
            "RESPONSE EVIDENCE:\n%s\n\n" +
            "PERFORMANCE METRICS:\n%s\n\n" +
            "COGNITIVE CONTEXT:\n%s\n\n" +
            "USER PREFERENCES:\n%s\n\n" +
            "TASK: Create 4-6 highly personalized suggestions that:\n" +
            "1. Match the user's preferred learning style (%s)\n" +
            "2. Account for their cognitive biases and thinking patterns\n" +
            "3. Build on their demonstrated strengths\n" +
            "4. Address specific knowledge gaps shown in responses\n" +
            "5. Respect their time availability and preferred pace\n\n" +
            "Format each suggestion with:\n" +
            "**Suggestion X: [Title]**\n" +
            "*Why this matches you:* [Connection to preferences/cognitive style]\n" +
            "*Evidence from your assessment:* [Specific response/performance data]\n" +
            "*Personalized action plan:* [Steps tailored to your learning style]\n" +
            "*Timeline:* [Realistic schedule based on your availability]\n" +
            "*Success markers:* [How you'll know it's working]\n\n" +
            "Make suggestions feel like they were designed specifically for this individual.",
            userProfile, responseEvidence, performanceContext, cognitiveContext, preferencesContext,
            userPreferences.getOrDefault("learningStyle", "general")
        );

        return generateSuggestionWithModel(prompt, session.getId(), session.getUser().getId(),
                                         "tailored_preference_suggestions", "gpt-4", 2000, 0.5);
    }

    /**
     * Builds user preferences context
     */
    private String buildPreferencesContext(Map<String, String> preferences) {
        StringBuilder context = new StringBuilder();

        context.append("Learning Style: ").append(preferences.getOrDefault("learningStyle", "Not specified")).append("\n");
        context.append("Time Availability: ").append(preferences.getOrDefault("timeAvailability", "Not specified")).append("\n");
        context.append("Preferred Pace: ").append(preferences.getOrDefault("pace", "Not specified")).append("\n");
        context.append("Motivation Factors: ").append(preferences.getOrDefault("motivation", "Not specified")).append("\n");
        context.append("Preferred Resources: ").append(preferences.getOrDefault("resources", "Not specified")).append("\n");
        context.append("Goal Timeline: ").append(preferences.getOrDefault("timeline", "Not specified")).append("\n");

        return context.toString();
    }

    /**
     * Generates in-depth context engineering for complex analysis
     */
    public String generateInDepthContextAnalysis(AssessmentSession session, List<Response> responses,
                                               ResumeData resumeData, Map<String, Object> performanceMetrics,
                                               String analysisType) {
        log.info("Generating in-depth context analysis for session: {} (type: {})", session.getId(), analysisType);

        // Build multi-layered context
        String userProfile = buildUserProfileContext(session, resumeData);
        String responseEvidence = buildResponseEvidenceContext(responses);
        String performanceContext = buildPerformanceContext(performanceMetrics);
        String cognitiveContext = buildCognitiveContext(session);
        String skillGapContext = buildSkillGapContext(responses, resumeData);
        String learningTrajectoryContext = buildLearningTrajectoryContext(responses);

        String prompt = String.format(
            "You are a senior career development specialist conducting deep context engineering analysis. " +
            "Perform %s analysis using multiple evidence layers.\n\n" +
            "USER PROFILE:\n%s\n\n" +
            "RESPONSE EVIDENCE:\n%s\n\n" +
            "PERFORMANCE METRICS:\n%s\n\n" +
            "COGNITIVE CONTEXT:\n%s\n\n" +
            "SKILL GAP ANALYSIS:\n%s\n\n" +
            "LEARNING TRAJECTORY:\n%s\n\n" +
            "TASK: Conduct comprehensive %s analysis that:\n" +
            "1. Synthesizes evidence across all context layers\n" +
            "2. Identifies patterns and correlations between different data sources\n" +
            "3. Provides nuanced insights beyond surface-level observations\n" +
            "4. Generates actionable recommendations with specific evidence citations\n" +
            "5. Considers long-term development implications\n\n" +
            "Structure your analysis as:\n" +
            "**Key Findings:** [Synthesized insights with evidence citations]\n" +
            "**Pattern Analysis:** [Correlations between performance, cognition, and responses]\n" +
            "**Development Recommendations:** [Evidence-based suggestions with rationale]\n" +
            "**Risk Factors:** [Potential challenges based on identified patterns]\n" +
            "**Success Strategies:** [Tailored approaches for optimal growth]\n\n" +
            "Ensure all conclusions are directly supported by specific evidence from the provided context.",
            analysisType, userProfile, responseEvidence, performanceContext, cognitiveContext,
            skillGapContext, learningTrajectoryContext, analysisType
        );

        return generateSuggestionWithModel(prompt, session.getId(), session.getUser().getId(),
                                         "in_depth_" + analysisType.replace(" ", "_") + "_analysis", "gpt-4", 2500, 0.4);
    }

    /**
     * Builds skill gap context from responses and resume
     */
    private String buildSkillGapContext(List<Response> responses, ResumeData resumeData) {
        StringBuilder context = new StringBuilder();

        // Analyze incorrect responses for knowledge gaps
        List<Response> incorrectResponses = responses.stream()
            .filter(r -> Boolean.FALSE.equals(r.getIsCorrect()))
            .collect(Collectors.toList());

        context.append("Knowledge Gaps Identified:\n");
        for (Response response : incorrectResponses) {
            context.append(String.format("- %s: %s\n",
                response.getQuestion().getQuestionText(),
                response.getResponseText() != null ? response.getResponseText() : "No response"));
        }

        // Compare with resume skills
        if (resumeData != null && resumeData.getExtractedSkills() != null) {
            context.append("\nResume Skills vs Assessment Performance:\n");
            try {
                @SuppressWarnings("unchecked")
                List<String> resumeSkills = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(resumeData.getExtractedSkills(), List.class);

                // Simple analysis - could be enhanced with more sophisticated matching
                context.append("Resume lists ").append(resumeSkills.size()).append(" skills\n");
                context.append("Assessment shows ").append(incorrectResponses.size()).append(" knowledge gaps\n");

            } catch (Exception e) {
                context.append("Unable to analyze resume skills\n");
            }
        }

        return context.toString();
    }

    /**
     * Builds learning trajectory context from response patterns
     */
    private String buildLearningTrajectoryContext(List<Response> responses) {
        StringBuilder context = new StringBuilder();

        // Analyze response patterns over time (assuming responses are ordered)
        context.append("Response Pattern Analysis:\n");

        double avgConfidence = responses.stream()
            .mapToDouble(r -> r.getConfidenceLevel() != null ? r.getConfidenceLevel() : 0.0)
            .average().orElse(0.0);

        double avgSimilarity = responses.stream()
            .mapToDouble(r -> r.getSimilarityScore() != null ? r.getSimilarityScore() : 0.0)
            .average().orElse(0.0);

        long highConfidenceCount = responses.stream()
            .filter(r -> r.getConfidenceLevel() != null && r.getConfidenceLevel() > 0.8)
            .count();

        long lowConfidenceCount = responses.stream()
            .filter(r -> r.getConfidenceLevel() != null && r.getConfidenceLevel() < 0.4)
            .count();

        context.append(String.format("Average Confidence: %.2f\n", avgConfidence));
        context.append(String.format("Average Similarity: %.2f\n", avgSimilarity));
        context.append(String.format("High Confidence Responses: %d\n", highConfidenceCount));
        context.append(String.format("Low Confidence Responses: %d\n", lowConfidenceCount));

        // Learning progression analysis
        context.append("\nLearning Progression:\n");
        if (avgConfidence > 0.7 && avgSimilarity > 0.7) {
            context.append("Shows strong foundational knowledge with consistent performance\n");
        } else if (highConfidenceCount > lowConfidenceCount) {
            context.append("Demonstrates selective strength areas with some knowledge gaps\n");
        } else {
            context.append("Indicates broader learning needs with inconsistent understanding\n");
        }

        return context.toString();
    }

    /**
     * Generates tailored NeuroRAG feedback using cognitive assessment framework
     */
    public String generateNeuroRAGTailoredFeedback(AssessmentSession session, List<Response> responses,
                                                  Map<String, Object> skillGraphData,
                                                  Map<String, Object> behavioralInsights,
                                                  String previousFeedbackActions, String currentSessionGoal) {
        log.info("Generating tailored NeuroRAG feedback for session: {}", session.getId());

        // Build skill graph summary
        String skillGraphSummary = buildSkillGraphSummary(skillGraphData);

        // Build behavioral insights
        String behavioralInsightsStr = buildBehavioralInsightsString(behavioralInsights);

        String prompt = String.format(
            "You are a cognitive assessment and learning coach within the NeuroRAG framework. " +
            "You analyze the user's stored skill graph, past responses, and behavioral data to generate personalized, constructive feedback. " +
            "Your goal is to help the user understand their skill evolution, cognitive patterns, and provide actionable improvement strategies — not generic advice.\n\n" +
            "Use transparent reasoning and structured explanation. " +
            "Highlight what the user did well, what can be improved, and what exact micro-steps or resources they should follow next.\n\n" +
            "When relevant, include bias-awareness (e.g., overconfidence, anchoring, fatigue) and reference how their skill trajectory compares to previous sessions.\n\n" +
            "Always close with an encouraging, empathetic statement aligned with the user's growth.\n\n" +
            "SKILL GRAPH SUMMARY:\n%s\n\n" +
            "BEHAVIORAL INSIGHTS:\n%s\n\n" +
            "PREVIOUS FEEDBACK ACTIONS: %s\n\n" +
            "CURRENT SESSION GOAL: %s\n\n" +
            "TASK:\n" +
            "Generate a personalized suggestion paragraph that:\n\n" +
            "References the specific skill trends and behavioral data above.\n\n" +
            "Explains the underlying cause of changes (e.g., cognitive bias or practice effect).\n\n" +
            "Provides 2–3 actionable next steps (e.g., \"Try short timed logic exercises,\" \"Reflect on reasoning steps before confirming an answer\").\n\n" +
            "Concludes with an empathetic motivational statement that encourages sustained learning.\n\n" +
            "Format as a cohesive paragraph that flows naturally, not as bullet points.",
            skillGraphSummary, behavioralInsightsStr, previousFeedbackActions, currentSessionGoal
        );

        return generateSuggestionWithModel(prompt, session.getId(), session.getUser().getId(),
                                         "neurorag_tailored_feedback", "gpt-4", 1500, 0.7);
    }

    /**
     * Builds skill graph summary string from data
     */
    private String buildSkillGraphSummary(Map<String, Object> skillGraphData) {
        StringBuilder summary = new StringBuilder();

        for (Map.Entry<String, Object> entry : skillGraphData.entrySet()) {
            String skillName = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> skillData = (Map<String, Object>) value;
                Double current = (Double) skillData.get("current");
                Double previous = (Double) skillData.get("previous");
                String trend = (String) skillData.get("trend");

                if (previous != null) {
                    summary.append(String.format("- %s: %.2f → %.2f (%s)\n",
                        skillName, previous, current, trend));
                } else {
                    summary.append(String.format("- %s: %.2f (%s)\n",
                        skillName, current, trend));
                }
            } else if (value instanceof Double) {
                summary.append(String.format("- %s: %.2f\n", skillName, value));
            }
        }

        return summary.toString();
    }

    /**
     * Builds behavioral insights string from data
     */
    private String buildBehavioralInsightsString(Map<String, Object> behavioralInsights) {
        StringBuilder insights = new StringBuilder();

        for (Map.Entry<String, Object> entry : behavioralInsights.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Double && key.contains("score")) {
                insights.append(String.format("- %s: %.1f/%.0f\n",
                    key, value, key.contains("calmness") ? 1.0 : 5.0));
            } else if (value instanceof String) {
                insights.append(String.format("- %s: %s\n", key, value));
            } else if (value instanceof Integer) {
                insights.append(String.format("- %s: %d\n", key, value));
            } else {
                insights.append(String.format("- %s: %s\n", key, value.toString()));
            }
        }

        return insights.toString();
    }

    public String generateSuggestions(String prompt, String apiKey) {
        try {
            // Create a temporary OpenAI service with the provided API key
            OpenAiService tempService = new OpenAiService(apiKey.trim(), Duration.ofSeconds(30));

            ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(),
                "You are an AI career coach analyzing a user's skill assessment results. " +
                "Based on the information provided, give personalized career development suggestions. " +
                "Focus on skill gaps, learning resources, and career progression opportunities.");

            ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), prompt);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(List.of(systemMessage, userMessage))
                .maxTokens(1000)
                .temperature(0.7)
                .build();

            ChatCompletionResult result = tempService.createChatCompletion(request);

            if (result.getChoices().isEmpty()) {
                return "Unable to generate suggestions at this time.";
            }

            return result.getChoices().get(0).getMessage().getContent();

        } catch (Exception e) {
            log.error("Error generating suggestions with provided API key", e);
            throw new RuntimeException("Failed to generate suggestions: " + e.getMessage());
        }
    }

    public void clearExpiredCache() {
        try {
            cacheRepository.deleteExpiredEntries(LocalDateTime.now());
            log.info("Cleared expired cache entries");
        } catch (Exception e) {
            log.error("Error clearing expired cache", e);
        }
    }

    public long getCacheHitRate() {
        try {
            Object[] stats = cacheRepository.getCacheStatistics();
            if (stats != null && stats.length >= 2) {
                Long totalEntries = (Long) stats[0];
                Long totalHits = (Long) stats[1];
            return totalEntries > 0 ? (totalHits * 100 / totalEntries) : 0L;
            }
        } catch (Exception e) {
            log.error("Error calculating cache hit rate", e);
        }
        return 0;
    }
}
