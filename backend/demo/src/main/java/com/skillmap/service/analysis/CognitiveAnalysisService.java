package com.skillmap.service.analysis;

import com.skillmap.model.entity.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class CognitiveAnalysisService {

    /**
     * Analyze a list of responses for cognitive biases and update their scores.
     * This method calculates confidence, consistency, time pressure, answer stability,
     * and overall cognitive bias score for each response.
     *
     * @param responses List of user responses in the current session
     */
    public void analyzeResponses(List<Response> responses) {
        if (responses == null || responses.isEmpty()) {
            return;
        }

        // Example: Calculate average think time for normalization
        double avgThinkTime = responses.stream()
                .filter(r -> r.getThinkTimeSeconds() != null)
                .mapToInt(Response::getThinkTimeSeconds)
                .average()
                .orElse(30.0); // default average think time 30 seconds

        for (Response response : responses) {
            // Confidence: Use user reported confidence if available, else estimate
            Double confidence = response.getConfidenceLevel();
            if (confidence == null) {
                confidence = estimateConfidence(response);
                response.setConfidenceLevel(confidence);
            }

            // Consistency: Compare with previous answers (simplified example)
            Double consistency = calculateConsistency(response, responses);
            response.setConsistencyScore(consistency);

            // Time pressure: ratio of average think time to user's think time
            Integer thinkTime = response.getThinkTimeSeconds();
            double timePressure = (thinkTime != null && thinkTime > 0) ? avgThinkTime / thinkTime : 1.0;
            response.setTimePressureScore(timePressure);

            // Answer stability: based on edit count and paste detection
            double stability = calculateAnswerStability(response);
            response.setAnswerStability(stability);

            // Cognitive bias score: weighted sum of above factors
            double biasScore = 0.4 * confidence + 0.3 * consistency + 0.2 * timePressure + 0.1 * stability;
            response.setCognitiveBiasScore(biasScore);

            log.debug("Analyzed response id {}: confidence={}, consistency={}, timePressure={}, stability={}, biasScore={}",
                    response.getId(), confidence, consistency, timePressure, stability, biasScore);
        }
    }

    private double estimateConfidence(Response response) {
        // Placeholder: estimate confidence based on response length and specificity
        if (response.getResponseText() == null) {
            return 0.5;
        }
        int length = response.getResponseText().length();
        if (length > 100) {
            return 0.9;
        } else if (length > 50) {
            return 0.7;
        } else {
            return 0.5;
        }
    }

    private double calculateConsistency(Response current, List<Response> allResponses) {
        // Placeholder: simplistic consistency calculation
        // In real case, compare current answer with previous answers to same or similar questions
        return 0.8; // fixed value for example
    }

    private double calculateAnswerStability(Response response) {
        int edits = response.getEditCount() != null ? response.getEditCount() : 0;
        boolean pasted = response.getPasteDetected() != null ? response.getPasteDetected() : false;
        double stability = 1.0 - (edits * 0.1);
        if (pasted) {
            stability -= 0.2;
        }
        if (stability < 0) {
            stability = 0;
        }
        return stability;
    }
}
