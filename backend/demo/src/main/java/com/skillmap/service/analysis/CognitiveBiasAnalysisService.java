package com.skillmap.service.analysis;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.Response;
import com.skillmap.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CognitiveBiasAnalysisService {

    private final ResponseRepository responseRepository;

    public List<CognitiveBiasResult> analyzeCognitiveBiases(AssessmentSession session) {
        List<Response> responses = responseRepository.findBySessionId(session.getId());
        if (responses.isEmpty()) {
            return Collections.emptyList();
        }

        List<CognitiveBiasResult> biases = new ArrayList<>();

        // Analyze overconfidence bias
        CognitiveBiasResult overconfidence = analyzeOverconfidenceBias(responses);
        if (overconfidence != null) {
            biases.add(overconfidence);
        }

        // Analyze confirmation bias
        CognitiveBiasResult confirmation = analyzeConfirmationBias(responses);
        if (confirmation != null) {
            biases.add(confirmation);
        }

        // Analyze anchoring bias
        CognitiveBiasResult anchoring = analyzeAnchoringBias(responses);
        if (anchoring != null) {
            biases.add(anchoring);
        }

        // Analyze availability heuristic
        CognitiveBiasResult availability = analyzeAvailabilityHeuristic(responses);
        if (availability != null) {
            biases.add(availability);
        }

        // Analyze consistency bias
        CognitiveBiasResult consistency = analyzeConsistencyBias(responses);
        if (consistency != null) {
            biases.add(consistency);
        }

        return biases;
    }

    private CognitiveBiasResult analyzeOverconfidenceBias(List<Response> responses) {
        List<Response> mcqResponses = responses.stream()
            .filter(r -> r.getIsCorrect() != null)
            .collect(Collectors.toList());

        if (mcqResponses.size() < 3) return null;

        int correctAnswers = (int) mcqResponses.stream()
            .filter(Response::getIsCorrect)
            .count();

        double actualAccuracy = (double) correctAnswers / mcqResponses.size();
        double averageConfidence = mcqResponses.stream()
            .filter(r -> r.getConfidenceLevel() != null)
            .mapToDouble(Response::getConfidenceLevel)
            .average()
            .orElse(0.5);

        double overconfidenceScore = averageConfidence - actualAccuracy;
        
        if (overconfidenceScore > 0.2) {
            return CognitiveBiasResult.builder()
                .type("Overconfidence")
                .level(overconfidenceScore > 0.4 ? "High" : "Medium")
                .score(overconfidenceScore)
                .evidence(String.format("Average confidence: %.1f%%, Actual accuracy: %.1f%%", 
                    averageConfidence * 100, actualAccuracy * 100))
                .impact("May underestimate skill gaps and overestimate abilities")
                .recommendations(Arrays.asList(
                    "Practice with more challenging questions",
                    "Review incorrect answers to understand knowledge gaps",
                    "Seek feedback from peers or mentors"
                ))
                .build();
        }

        return null;
    }

    private CognitiveBiasResult analyzeConfirmationBias(List<Response> responses) {
        // Analyze response patterns for confirmation bias
        List<Response> textResponses = responses.stream()
            .filter(r -> r.getResponseText() != null && r.getResponseText().length() > 50)
            .collect(Collectors.toList());

        if (textResponses.size() < 3) return null;

        // Look for patterns in response structure and content
        double averageLength = textResponses.stream()
            .mapToInt(r -> r.getResponseText().length())
            .average()
            .orElse(0);

        double lengthVariance = textResponses.stream()
            .mapToDouble(r -> Math.pow(r.getResponseText().length() - averageLength, 2))
            .average()
            .orElse(0);

        double coefficientOfVariation = Math.sqrt(lengthVariance) / averageLength;

        if (coefficientOfVariation < 0.2) {
            return CognitiveBiasResult.builder()
                .type("Confirmation Bias")
                .level("Low")
                .score(0.3)
                .evidence("Consistent response patterns across different topics")
                .impact("Good consistency in reasoning approach")
                .recommendations(Arrays.asList(
                    "Continue maintaining consistent analytical approach",
                    "Consider exploring different problem-solving methods"
                ))
                .build();
        }

        return null;
    }

    private CognitiveBiasResult analyzeAnchoringBias(List<Response> responses) {
        // Analyze if responses are influenced by first impressions or initial information
        List<Response> orderedResponses = responses.stream()
            .sorted(Comparator.comparing(Response::getAnsweredAt))
            .collect(Collectors.toList());

        if (orderedResponses.size() < 5) return null;

        // Check if confidence levels show anchoring pattern
        List<Double> confidenceLevels = orderedResponses.stream()
            .filter(r -> r.getConfidenceLevel() != null)
            .map(Response::getConfidenceLevel)
            .collect(Collectors.toList());

        if (confidenceLevels.size() < 3) return null;

        double firstConfidence = confidenceLevels.get(0);
        double lastConfidence = confidenceLevels.get(confidenceLevels.size() - 1);
        double confidenceChange = Math.abs(lastConfidence - firstConfidence);

        if (confidenceChange < 0.1 && firstConfidence > 0.7) {
            return CognitiveBiasResult.builder()
                .type("Anchoring Bias")
                .level("Medium")
                .score(0.4)
                .evidence(String.format("Confidence remained high (%.1f%%) throughout assessment", firstConfidence * 100))
                .impact("May be anchored to initial confidence level")
                .recommendations(Arrays.asList(
                    "Question initial assumptions more critically",
                    "Consider alternative approaches to problems",
                    "Practice with diverse question types"
                ))
                .build();
        }

        return null;
    }

    private CognitiveBiasResult analyzeAvailabilityHeuristic(List<Response> responses) {
        // Analyze if responses rely too heavily on easily recalled information
        List<Response> textResponses = responses.stream()
            .filter(r -> r.getResponseText() != null && r.getResponseText().length() > 20)
            .collect(Collectors.toList());

        if (textResponses.size() < 3) return null;

        // Look for patterns in response depth and specificity
        double averageSpecificity = textResponses.stream()
            .filter(r -> r.getSpecificityScore() != null)
            .mapToDouble(Response::getSpecificityScore)
            .average()
            .orElse(0.5);

        double averageDepth = textResponses.stream()
            .filter(r -> r.getDepthScore() != null)
            .mapToDouble(Response::getDepthScore)
            .average()
            .orElse(0.5);

        if (averageSpecificity < 0.3 && averageDepth < 0.3) {
            return CognitiveBiasResult.builder()
                .type("Availability Heuristic")
                .level("Medium")
                .score(0.5)
                .evidence("Responses show low specificity and depth, suggesting reliance on surface-level information")
                .impact("May miss important details and nuances")
                .recommendations(Arrays.asList(
                    "Practice providing more detailed explanations",
                    "Focus on understanding underlying concepts",
                    "Take time to think through complex problems"
                ))
                .build();
        }

        return null;
    }

    private CognitiveBiasResult analyzeConsistencyBias(List<Response> responses) {
        // Analyze response consistency over time
        List<Response> orderedResponses = responses.stream()
            .sorted(Comparator.comparing(Response::getAnsweredAt))
            .collect(Collectors.toList());

        if (orderedResponses.size() < 4) return null;

        // Calculate consistency score based on response patterns
        double consistencyScore = orderedResponses.stream()
            .filter(r -> r.getConsistencyScore() != null)
            .mapToDouble(Response::getConsistencyScore)
            .average()
            .orElse(0.5);

        if (consistencyScore < 0.3) {
            return CognitiveBiasResult.builder()
                .type("Inconsistency")
                .level("High")
                .score(1.0 - consistencyScore)
                .evidence(String.format("Low consistency score: %.1f", consistencyScore))
                .impact("Inconsistent responses may indicate uncertainty or lack of clear understanding")
                .recommendations(Arrays.asList(
                    "Take more time to think through responses",
                    "Review and verify answers before submitting",
                    "Practice with similar questions to build confidence"
                ))
                .build();
        } else if (consistencyScore > 0.8) {
            return CognitiveBiasResult.builder()
                .type("Consistency")
                .level("Low")
                .score(0.2)
                .evidence(String.format("High consistency score: %.1f", consistencyScore))
                .impact("Good consistency in responses and reasoning")
                .recommendations(Arrays.asList(
                    "Continue maintaining consistent approach",
                    "Consider exploring different problem-solving strategies"
                ))
                .build();
        }

        return null;
    }

    @lombok.Builder
    @lombok.Data
    public static class CognitiveBiasResult {
        private String type;
        private String level;
        private Double score;
        private String evidence;
        private String impact;
        private List<String> recommendations;
    }
}










