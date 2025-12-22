package com.skillmap.controller;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.repository.AssessmentSessionRepository;
import com.skillmap.service.analysis.CognitiveBiasAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cognitive")
@RequiredArgsConstructor
public class CognitiveAnalysisController {

    private final CognitiveBiasAnalysisService cognitiveBiasAnalysisService;
    private final AssessmentSessionRepository sessionRepository;

    @GetMapping("/bias-analysis/{sessionToken}")
    public ResponseEntity<List<CognitiveBiasAnalysisService.CognitiveBiasResult>> getCognitiveBiasAnalysis(
            @PathVariable String sessionToken) {
        Optional<AssessmentSession> sessionOpt = sessionRepository.findBySessionToken(sessionToken);
        if (sessionOpt.isPresent()) {
            List<CognitiveBiasAnalysisService.CognitiveBiasResult> biases = 
                cognitiveBiasAnalysisService.analyzeCognitiveBiases(sessionOpt.get());
            return ResponseEntity.ok(biases);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/bias-analysis/session/{sessionId}")
    public ResponseEntity<List<CognitiveBiasAnalysisService.CognitiveBiasResult>> getCognitiveBiasAnalysisBySessionId(
            @PathVariable Long sessionId) {
        Optional<AssessmentSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            List<CognitiveBiasAnalysisService.CognitiveBiasResult> biases = 
                cognitiveBiasAnalysisService.analyzeCognitiveBiases(sessionOpt.get());
            return ResponseEntity.ok(biases);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}