package com.skillmap.service.analysis;

import com.skillmap.model.entity.Response;
import com.skillmap.model.entity.Skill;
import com.skillmap.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResponseAnalysisService {

    private final ResponseRepository responseRepository;

    // NLP patterns for analysis
    private static final Pattern TECHNICAL_TERMS_PATTERN = Pattern.compile(
        "\\b(java|python|javascript|react|spring|hibernate|docker|kubernetes|aws|azure|git|sql|nosql|api|rest|graphql|microservices|agile|scrum)\\b",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EXPERIENCE_INDICATORS = Pattern.compile(
        "\\b(years?|months?|experience|worked|developed|built|implemented|managed|led|team)\\b",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CONFIDENCE_INDICATORS = Pattern.compile(
        "\\b(confident|expert|proficient|experienced|knowledgeable|comfortable|familiar|beginner|novice|learning)\\b",
        Pattern.CASE_INSENSITIVE
    );

    public void analyzeResponse(Response response) {
        log.info("Analyzing response: {}", response.getId());

        String responseText = response.getResponseText();
        if (responseText == null || responseText.trim().isEmpty()) {
            log.warn("Empty response text for response: {}", response.getId());
            return;
        }

        // Analyze various aspects
        int wordCount = calculateWordCount(responseText);
        int charCount = responseText.length();
        int thinkTime = response.getThinkTimeSeconds() != null ? response.getThinkTimeSeconds() : 0;
        int totalTime = response.getTotalTimeSeconds() != null ? response.getTotalTimeSeconds() : 0;

        // Calculate scores
        double specificityScore = calculateSpecificityScore(responseText, wordCount);
        double depthScore = calculateDepthScore(responseText, wordCount);

        // Update response with analysis results
        response.setWordCount(wordCount);
        response.setCharCount(charCount);
        response.setSpecificityScore(specificityScore);
        response.setDepthScore(depthScore);

        // Extract keywords and patterns
        String keywordMatches = extractKeywordMatches(responseText);
        response.setKeywordMatches(keywordMatches);

        log.debug("Analysis complete for response {}: specificity={}, depth={}",
                 response.getId(), specificityScore, depthScore);
    }

    private int calculateWordCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private double calculateSpecificityScore(String text, int wordCount) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }

        double score = 0.0;
        String lowerText = text.toLowerCase();

        // Technical terms (0-0.4 points)
        long technicalTerms = TECHNICAL_TERMS_PATTERN.matcher(lowerText).results().count();
        score += Math.min(technicalTerms * 0.1, 0.4);

        // Experience indicators (0-0.3 points)
        long experienceIndicators = EXPERIENCE_INDICATORS.matcher(lowerText).results().count();
        score += Math.min(experienceIndicators * 0.1, 0.3);

        // Specific examples or details (0-0.3 points)
        boolean hasExamples = lowerText.contains("for example") ||
                             lowerText.contains("such as") ||
                             lowerText.contains("like") ||
                             lowerText.contains("specifically");
        if (hasExamples) score += 0.2;

        boolean hasDetails = wordCount > 50; // Longer responses tend to be more specific
        if (hasDetails) score += 0.1;

        return Math.min(score, 1.0);
    }

    private double calculateDepthScore(String text, int wordCount) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }

        double score = 0.0;
        String lowerText = text.toLowerCase();

        // Length-based depth (0-0.3 points)
        if (wordCount > 100) score += 0.3;
        else if (wordCount > 50) score += 0.2;
        else if (wordCount > 20) score += 0.1;

        // Complexity indicators (0-0.4 points)
        boolean hasMultipleConcepts = TECHNICAL_TERMS_PATTERN.matcher(lowerText).results().count() > 2;
        if (hasMultipleConcepts) score += 0.2;

        boolean hasProcessDescription = lowerText.contains("process") ||
                                       lowerText.contains("approach") ||
                                       lowerText.contains("method") ||
                                       lowerText.contains("strategy");
        if (hasProcessDescription) score += 0.2;

        // Confidence indicators (0-0.3 points)
        long confidenceIndicators = CONFIDENCE_INDICATORS.matcher(lowerText).results().count();
        score += Math.min(confidenceIndicators * 0.1, 0.3);

        return Math.min(score, 1.0);
    }

    private String extractKeywordMatches(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "[]";
        }

        Set<String> keywords = new HashSet<>();

        // Extract technical terms
        var technicalMatcher = TECHNICAL_TERMS_PATTERN.matcher(text.toLowerCase());
        while (technicalMatcher.find()) {
            keywords.add(technicalMatcher.group());
        }

        // Extract experience indicators
        var experienceMatcher = EXPERIENCE_INDICATORS.matcher(text.toLowerCase());
        while (experienceMatcher.find()) {
            keywords.add(experienceMatcher.group());
        }

        // Extract confidence indicators
        var confidenceMatcher = CONFIDENCE_INDICATORS.matcher(text.toLowerCase());
        while (confidenceMatcher.find()) {
            keywords.add(confidenceMatcher.group());
        }

        return keywords.toString();
    }

    public Map<String, Double> analyzeResponseQuality(Response response) {
        Map<String, Double> qualityMetrics = new HashMap<>();

        qualityMetrics.put("specificityScore", response.getSpecificityScore() != null ? response.getSpecificityScore() : 0.0);
        qualityMetrics.put("depthScore", response.getDepthScore() != null ? response.getDepthScore() : 0.0);
        qualityMetrics.put("wordCount", response.getWordCount() != null ? response.getWordCount().doubleValue() : 0.0);
        qualityMetrics.put("charCount", response.getCharCount() != null ? response.getCharCount().doubleValue() : 0.0);

        // Calculate response efficiency
        if (response.getTotalTimeSeconds() != null && response.getTotalTimeSeconds() > 0) {
            double wordsPerMinute = (response.getWordCount() != null ? response.getWordCount() : 0) /
                                   (response.getTotalTimeSeconds() / 60.0);
            qualityMetrics.put("wordsPerMinute", wordsPerMinute);
        }

        // Calculate edit efficiency
        if (response.getEditCount() != null && response.getWordCount() != null && response.getWordCount() > 0) {
            double editsPerWord = response.getEditCount().doubleValue() / response.getWordCount();
            qualityMetrics.put("editsPerWord", editsPerWord);
        }

        return qualityMetrics;
    }

    public List<String> getResponseInsights(Response response) {
        List<String> insights = new ArrayList<>();

        if (response.getSpecificityScore() != null) {
            if (response.getSpecificityScore() > 0.7) {
                insights.add("High specificity - response contains detailed technical information");
            } else if (response.getSpecificityScore() < 0.3) {
                insights.add("Low specificity - response could benefit from more concrete examples");
            }
        }

        if (response.getDepthScore() != null) {
            if (response.getDepthScore() > 0.7) {
                insights.add("Deep understanding demonstrated through comprehensive explanation");
            } else if (response.getDepthScore() < 0.3) {
                insights.add("Response depth could be improved with more detailed explanations");
            }
        }

        if (response.getWordCount() != null) {
            if (response.getWordCount() < 20) {
                insights.add("Response is quite brief - consider providing more detail");
            } else if (response.getWordCount() > 200) {
                insights.add("Very detailed response showing strong knowledge");
            }
        }

        if (response.getPasteDetected() != null && response.getPasteDetected()) {
            insights.add("Potential copy-paste detected - ensure responses are original");
        }

        return insights;
    }

    public double calculateOverallResponseScore(Response response) {
        double specificity = response.getSpecificityScore() != null ? response.getSpecificityScore() : 0.0;
        double depth = response.getDepthScore() != null ? response.getDepthScore() : 0.0;

        // Weighted average: 40% specificity, 60% depth
        return (specificity * 0.4) + (depth * 0.6);
    }

    /**
     * Determines cognitive level based on specificity and depth scores and generates evidence.
     * Uses Bloom's taxonomy levels: Remembering, Understanding, Applying, Analyzing, Evaluating, Creating.
     *
     * @param response The response to analyze
     * @return Map containing "level" (String) and "evidence" (List<String>)
     */
    public Map<String, Object> getCognitiveLevelAndEvidence(Response response) {
        Map<String, Object> result = new HashMap<>();

        double specificity = response.getSpecificityScore() != null ? response.getSpecificityScore() : 0.0;
        double depth = response.getDepthScore() != null ? response.getDepthScore() : 0.0;
        String responseText = response.getResponseText() != null ? response.getResponseText() : "";
        int wordCount = response.getWordCount() != null ? response.getWordCount() : 0;

        List<String> evidence = new ArrayList<>();

        String level;
        if (specificity < 0.3 && depth < 0.3) {
            level = "Remembering";
            evidence.add("Response shows basic recall with limited technical details");
            if (specificity < 0.3) evidence.add("Low specificity score indicates few technical terms or examples");
            if (depth < 0.3) evidence.add("Low depth score suggests minimal explanation or complexity");
        } else if (specificity < 0.5 && depth < 0.4) {
            level = "Understanding";
            evidence.add("Response demonstrates comprehension with moderate detail");
            if (specificity >= 0.3) evidence.add("Moderate specificity from some technical terms or experience indicators");
            if (depth < 0.4) evidence.add("Limited depth in explanation and process description");
        } else if (specificity >= 0.5 && depth < 0.6) {
            level = "Applying";
            evidence.add("Response shows practical application with concrete examples");
            if (specificity >= 0.5) evidence.add("High specificity from technical terms, examples, and experience details");
            if (depth >= 0.4) evidence.add("Moderate depth with some process or approach description");
        } else if (specificity >= 0.4 && depth >= 0.6) {
            level = "Analyzing";
            evidence.add("Response demonstrates analytical thinking with detailed breakdown");
            if (specificity >= 0.4) evidence.add("Good specificity with technical content and examples");
            if (depth >= 0.6) evidence.add("High depth from complex explanations and multiple concepts");
        } else if (specificity >= 0.7 && depth >= 0.7) {
            level = "Evaluating";
            evidence.add("Response shows evaluation skills with comprehensive analysis");
            if (specificity >= 0.7) evidence.add("Very high specificity with extensive technical details");
            if (depth >= 0.7) evidence.add("Deep analysis with confidence indicators and complex reasoning");
        } else {
            level = "Creating";
            evidence.add("Response demonstrates creative synthesis and innovative thinking");
            if (specificity >= 0.8) evidence.add("Exceptional specificity with advanced technical content");
            if (depth >= 0.8) evidence.add("Outstanding depth with sophisticated explanations and strategies");
        }

        // Add specific evidence based on response characteristics
        if (wordCount > 100) {
            evidence.add("Response length (" + wordCount + " words) indicates substantial content");
        } else if (wordCount < 20) {
            evidence.add("Brief response (" + wordCount + " words) suggests concise but limited detail");
        }

        // Check for specific patterns that influenced scoring
        if (responseText.toLowerCase().contains("for example") || responseText.toLowerCase().contains("such as")) {
            evidence.add("Includes concrete examples, contributing to specificity score");
        }

        if (responseText.toLowerCase().contains("process") || responseText.toLowerCase().contains("approach") ||
            responseText.toLowerCase().contains("method") || responseText.toLowerCase().contains("strategy")) {
            evidence.add("Describes processes or approaches, contributing to depth score");
        }

        long technicalTerms = TECHNICAL_TERMS_PATTERN.matcher(responseText.toLowerCase()).results().count();
        if (technicalTerms > 0) {
            evidence.add("Contains " + technicalTerms + " technical term(s), supporting specificity analysis");
        }

        long experienceIndicators = EXPERIENCE_INDICATORS.matcher(responseText.toLowerCase()).results().count();
        if (experienceIndicators > 0) {
            evidence.add("Includes " + experienceIndicators + " experience indicator(s), enhancing specificity");
        }

        result.put("level", level);
        result.put("evidence", evidence);
        return result;
    }
}
