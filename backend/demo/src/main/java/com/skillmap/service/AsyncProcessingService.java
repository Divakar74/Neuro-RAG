package com.skillmap.service;

import com.skillmap.model.entity.Response;
import com.skillmap.service.embedding.EmbeddingService;
import com.skillmap.service.engine.SkillInferenceEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncProcessingService {

    private final EmbeddingService embeddingService;
    private final SkillInferenceEngine skillInferenceEngine;

    /**
     * Asynchronously compute similarity scores for text responses
     */
    @Async
    public void computeSimilarityScoreAsync(Response response) {
        try {
            if ("text".equalsIgnoreCase(response.getQuestion().getQuestionType())) {
                String expected = response.getQuestion().getContextHint();
                if (expected == null || expected.isBlank()) {
                    expected = response.getQuestion().getQuestionText();
                }

                Double sim = embeddingService.computeCosineSimilarity(expected, response.getResponseText());
                if (sim != null) {
                    response.setSimilarityScore(sim);
                    // Note: In a real implementation, you'd save this back to the database
                    log.debug("Computed similarity score {} for response {}", sim, response.getId());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to compute similarity score for response {}: {}", response.getId(), e.getMessage());
        }
    }

    /**
     * Asynchronously update Bayesian beliefs from response
     */
    @Async
    public void updateBeliefsAsync(Response response) {
        try {
            skillInferenceEngine.updateBeliefsFromResponse(response);
            log.debug("Updated beliefs for response {}", response.getId());
        } catch (Exception e) {
            log.warn("Failed to update beliefs for response {}: {}", response.getId(), e.getMessage());
        }
    }
}
