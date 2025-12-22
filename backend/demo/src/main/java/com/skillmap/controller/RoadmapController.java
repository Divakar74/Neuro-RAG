package com.skillmap.controller;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.Roadmap;
import com.skillmap.repository.AssessmentSessionRepository;
import com.skillmap.repository.RoadmapRepository;
import com.skillmap.service.engine.SkillInferenceEngine;
import com.skillmap.service.roadmap.RoadmapGenerationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/roadmap")
public class RoadmapController {

    @Autowired
    private RoadmapRepository roadmapRepository;

    @Autowired
    private AssessmentSessionRepository sessionRepository;

    @Autowired
    private SkillInferenceEngine skillInferenceEngine;

    @Autowired
    private RoadmapGenerationService roadmapGenerationService;

    @Autowired
    private com.skillmap.service.analysis.RoadmapGenerationService analysisRoadmapGenerationService;

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<Roadmap>> getRoadmapsBySession(@PathVariable Long sessionId) {
        List<Roadmap> roadmaps = roadmapRepository.findBySessionId(sessionId);
        return ResponseEntity.ok(roadmaps);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Roadmap>> getRoadmapsByUser(@PathVariable Long userId) {
        List<Roadmap> roadmaps = roadmapRepository.findByUserId(userId);
        return ResponseEntity.ok(roadmaps);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Roadmap> getRoadmapById(@PathVariable Long id) {
        Optional<Roadmap> roadmapOpt = roadmapRepository.findById(id);
        if (roadmapOpt.isPresent()) {
            return ResponseEntity.ok(roadmapOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<Roadmap> generateRoadmap(@RequestBody GenerateRoadmapRequest request) {
        Optional<AssessmentSession> sessionOpt = sessionRepository.findById(request.getSessionId());
        if (!sessionOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        AssessmentSession session = sessionOpt.get();
        // Get skill levels from the inference engine
        var skillLevels = skillInferenceEngine.getSkillBeliefs(session);

        // Generate personalized roadmap
        Roadmap roadmap = roadmapGenerationService.generatePersonalizedRoadmap(session, skillLevels);

        return ResponseEntity.ok(roadmap);
    }

    @PostMapping("/generate-analysis")
    public ResponseEntity<java.util.Map<String, Object>> generateAnalysisRoadmap(@RequestBody GenerateRoadmapRequest request) {
        Optional<AssessmentSession> sessionOpt = sessionRepository.findById(request.getSessionId());
        if (!sessionOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        AssessmentSession session = sessionOpt.get();

        // Generate personalized roadmap using analysis service
        java.util.Map<String, Object> roadmap = analysisRoadmapGenerationService.generatePersonalizedRoadmap(session);

        return ResponseEntity.ok(roadmap);
    }

    @PostMapping
    public ResponseEntity<Roadmap> createRoadmap(@RequestBody Roadmap roadmap) {
        Roadmap savedRoadmap = roadmapRepository.save(roadmap);
        return ResponseEntity.ok(savedRoadmap);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Roadmap> updateRoadmap(@PathVariable Long id, @RequestBody Roadmap roadmapDetails) {
        Optional<Roadmap> roadmapOpt = roadmapRepository.findById(id);
        if (roadmapOpt.isPresent()) {
            Roadmap roadmap = roadmapOpt.get();
            roadmap.setCurrentOverallLevel(roadmapDetails.getCurrentOverallLevel());
            roadmap.setTargetLevel(roadmapDetails.getTargetLevel());
            roadmap.setLevelLabel(roadmapDetails.getLevelLabel());
            roadmap.setGapAnalysis(roadmapDetails.getGapAnalysis());
            roadmap.setMilestones(roadmapDetails.getMilestones());
            roadmap.setTotalEstimatedWeeks(roadmapDetails.getTotalEstimatedWeeks());
            roadmap.setMotivationalMessage(roadmapDetails.getMotivationalMessage());
            Roadmap updatedRoadmap = roadmapRepository.save(roadmap);
            return ResponseEntity.ok(updatedRoadmap);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoadmap(@PathVariable Long id) {
        if (roadmapRepository.existsById(id)) {
            roadmapRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public static class GenerateRoadmapRequest {
        private Long sessionId;

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }
    }
}
