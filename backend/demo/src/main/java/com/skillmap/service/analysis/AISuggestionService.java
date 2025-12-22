package com.skillmap.service.analysis;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.Response;
import com.skillmap.repository.AssessmentSessionRepository;
import com.skillmap.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AISuggestionService {

    private final ResponseRepository responseRepository;
    private final AssessmentSessionRepository sessionRepository;
    private final ResponseAnalysisService responseAnalysisService;
    private final RAGService ragService;
    private final CognitiveBiasAnalysisService cognitiveBiasAnalysisService;

    public String generatePersonalizedSuggestions(Long sessionId) {
        log.info("Generating AI-powered suggestions for session: {}", sessionId);

        // Fetch all responses for the session
        List<Response> responses = responseRepository.findBySessionId(sessionId);

        if (responses.isEmpty()) {
            log.warn("No responses found for session: {}", sessionId);
            return "No assessment data available to generate suggestions.";
        }

        // Analyze responses
        for (Response response : responses) {
            responseAnalysisService.analyzeResponse(response);
        }

        // Get session for cognitive bias analysis
        AssessmentSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return "Session not found.";
        }

        // Analyze cognitive biases
        List<CognitiveBiasAnalysisService.CognitiveBiasResult> biases = cognitiveBiasAnalysisService.analyzeCognitiveBiases(session);

        // Generate AI suggestions using RAG with cognitive context
        String suggestions = ragService.generateSuggestionsWithRAGAndBiases(sessionId, responses, biases);

        log.info("Generated AI suggestions for session: {}", sessionId);
        return suggestions;
    }


}
