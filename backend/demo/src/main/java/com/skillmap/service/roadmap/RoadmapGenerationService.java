package com.skillmap.service.roadmap;

import com.skillmap.model.entity.*;
import com.skillmap.repository.*;
import com.skillmap.service.engine.SkillGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapGenerationService {

    private final SkillRepository skillRepository;
    private final SkillDependencyRepository skillDependencyRepository;
    private final ResourceRepository resourceRepository;
    private final RoadmapRepository roadmapRepository;
    private final SkillGraphService skillGraphService;

    public Roadmap generatePersonalizedRoadmap(AssessmentSession session, Map<String, Double> skillLevels) {
        log.info("Generating personalized roadmap for session: {}", session.getId());

        Roadmap roadmap = new Roadmap();
        roadmap.setSession(session);

        // Calculate current overall level
        double currentOverallLevel = skillLevels.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        roadmap.setCurrentOverallLevel(currentOverallLevel);

        // Set target level (assume 80% proficiency)
        roadmap.setTargetLevel(80);
        roadmap.setLevelLabel("Advanced");

        // Analyze current skill gaps
        List<SkillGap> skillGaps = identifySkillGaps(skillLevels, session.getTargetRole());

        // Handle case when no skill gaps found (e.g., beginner or all skills proficient)
        if (skillGaps.isEmpty()) {
            roadmap.setGapAnalysis("[]");
            roadmap.setMilestones("[]");
            roadmap.setTotalEstimatedWeeks(0);
            roadmap.setMotivationalMessage("Great job! You have no significant skill gaps. Keep up the good work!");
            Roadmap savedRoadmap = roadmapRepository.save(roadmap);
            log.info("Generated roadmap with no skill gaps for session: {}", session.getId());
            return savedRoadmap;
        }

        // Generate gap analysis
        roadmap.setGapAnalysis(generateGapAnalysisJson(skillGaps));

        // Generate milestones
        List<LearningPhase> learningPhases = createLearningPhases(skillGaps, skillLevels);
        roadmap.setMilestones(convertPhasesToJson(learningPhases));

        // Set total estimated weeks
        roadmap.setTotalEstimatedWeeks(calculateTotalDuration(learningPhases));

        // Generate motivational message
        roadmap.setMotivationalMessage(generateMotivationalMessage(skillGaps.size(), roadmap.getTotalEstimatedWeeks()));

        // Save roadmap
        Roadmap savedRoadmap = roadmapRepository.save(roadmap);

        log.info("Generated roadmap with {} skill gaps and {} weeks duration",
                skillGaps.size(), roadmap.getTotalEstimatedWeeks());

        return savedRoadmap;
    }

    private List<SkillGap> identifySkillGaps(Map<String, Double> skillLevels, String targetRole) {
        List<SkillGap> gaps = new ArrayList<>();

        // Get all skills for the target role category
        List<Skill> allSkills = skillRepository.findAll();
        List<Skill> relevantSkills;

        if (targetRole != null && !targetRole.isEmpty()) {
            relevantSkills = allSkills.stream()
                .filter(skill -> skill.getCategory() != null &&
                       skill.getCategory().name().toLowerCase().contains(targetRole.toLowerCase()))
                .collect(Collectors.toList());
        } else {
            // If no target role or beginner user, consider all skills as relevant
            relevantSkills = allSkills;
        }

        for (Skill skill : relevantSkills) {
            double currentLevel = skillLevels.getOrDefault(skill.getSkillCode(), 0.0);
            double requiredLevel = 0.8; // Assume 80% proficiency is required

            if (currentLevel < requiredLevel) {
                SkillGap gap = new SkillGap();
                gap.setSkill(skill);
                gap.setCurrentLevel(currentLevel);
                gap.setRequiredLevel(requiredLevel);
                gap.setGapSize(requiredLevel - currentLevel);
                gap.setPriority(calculatePriority(skill, currentLevel));
                gaps.add(gap);
            }
        }

        // Sort by priority (highest first)
        gaps.sort((a, b) -> Double.compare(b.getPriority(), a.getPriority()));

        return gaps;
    }

    private double calculatePriority(Skill skill, double currentLevel) {
        double priority = 0.0;

        // Base priority on importance weight
        priority += skill.getImportanceWeight() * 0.4;

        // Higher priority for foundational skills
        int skillLevel = skillGraphService.getSkillLevel(skill.getSkillCode());
        priority += (1.0 / skillLevel) * 0.3;

        // Higher priority for larger gaps
        priority += (1.0 - currentLevel) * 0.3;

        return Math.min(priority, 1.0);
    }

    private List<LearningPhase> createLearningPhases(List<SkillGap> skillGaps, Map<String, Double> skillLevels) {
        List<LearningPhase> phases = new ArrayList<>();

        // Group skills by difficulty level
        Map<Integer, List<SkillGap>> skillsByDifficulty = skillGaps.stream()
            .collect(Collectors.groupingBy(gap -> skillGraphService.getSkillLevel(gap.getSkill().getSkillCode())));

        // Create phases from foundational to advanced
        for (int level = 1; level <= skillsByDifficulty.keySet().stream().mapToInt(Integer::intValue).max().orElse(1); level++) {
            List<SkillGap> levelSkills = skillsByDifficulty.get(level);
            if (levelSkills != null && !levelSkills.isEmpty()) {
                LearningPhase phase = createPhaseForLevel(level, levelSkills);
                phases.add(phase);
            }
        }

        return phases;
    }

    private LearningPhase createPhaseForLevel(int level, List<SkillGap> skillGaps) {
        LearningPhase phase = new LearningPhase();
        phase.setPhaseNumber(level);
        phase.setPhaseName(getPhaseName(level));
        phase.setSkills(skillGaps.stream().map(SkillGap::getSkill).collect(Collectors.toList()));

        // Calculate duration based on skill gaps
        int durationWeeks = calculatePhaseDuration(skillGaps);
        phase.setDurationWeeks(durationWeeks);

        // Get resources for this phase
        List<Resource> resources = getResourcesForSkills(skillGaps.stream()
            .map(gap -> gap.getSkill())
            .collect(Collectors.toList()));
        phase.setResources(resources);

        // Set learning objectives
        phase.setObjectives(generateObjectives(skillGaps));

        return phase;
    }

    private String getPhaseName(int level) {
        switch (level) {
            case 1: return "Foundations";
            case 2: return "Core Concepts";
            case 3: return "Intermediate Skills";
            case 4: return "Advanced Topics";
            case 5: return "Expert Level";
            default: return "Phase " + level;
        }
    }

    private int calculatePhaseDuration(List<SkillGap> skillGaps) {
        // Base duration per skill: 2 weeks
        int baseDuration = skillGaps.size() * 2;

        // Adjust based on gap sizes
        double averageGap = skillGaps.stream()
            .mapToDouble(SkillGap::getGapSize)
            .average()
            .orElse(0.5);

        // Larger gaps need more time
        int adjustedDuration = (int) Math.ceil(baseDuration * (1 + averageGap));

        return Math.max(adjustedDuration, 2); // Minimum 2 weeks
    }

    private List<Resource> getResourcesForSkills(List<Skill> skills) {
        List<Resource> allResources = new ArrayList<>();

        for (Skill skill : skills) {
            List<Resource> skillResources = resourceRepository.findBySkillId(skill.getId());
            allResources.addAll(skillResources);
        }

        // Limit to top 5 resources per skill to avoid overload
        return allResources.stream()
            .limit(skills.size() * 5)
            .collect(Collectors.toList());
    }

    private List<String> generateObjectives(List<SkillGap> skillGaps) {
        List<String> objectives = new ArrayList<>();

        for (SkillGap gap : skillGaps) {
            String objective = String.format("Achieve %.1f proficiency in %s",
                gap.getRequiredLevel(), gap.getSkill().getDisplayName());
            objectives.add(objective);
        }

        return objectives;
    }

    private String convertPhasesToJson(List<LearningPhase> phases) {
        // Simple JSON conversion - in real implementation, use Jackson ObjectMapper
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < phases.size(); i++) {
            LearningPhase phase = phases.get(i);
            json.append("{")
                .append("\"phaseNumber\":").append(phase.getPhaseNumber()).append(",")
                .append("\"phaseName\":\"").append(phase.getPhaseName()).append("\",")
                .append("\"durationWeeks\":").append(phase.getDurationWeeks()).append(",")
                .append("\"skills\":[");

            List<Skill> skills = phase.getSkills();
            for (int j = 0; j < skills.size(); j++) {
                Skill skill = skills.get(j);
                json.append("\"").append(skill.getDisplayName()).append("\"");
                if (j < skills.size() - 1) json.append(",");
            }

            json.append("],\"objectives\":[");
            List<String> objectives = phase.getObjectives();
            for (int j = 0; j < objectives.size(); j++) {
                json.append("\"").append(objectives.get(j)).append("\"");
                if (j < objectives.size() - 1) json.append(",");
            }

            json.append("]}");
            if (i < phases.size() - 1) json.append(",");
        }
        json.append("]");

        return json.toString();
    }

    private int calculateTotalDuration(List<LearningPhase> phases) {
        return phases.stream().mapToInt(LearningPhase::getDurationWeeks).sum();
    }

    private int calculateTotalResources(List<LearningPhase> phases) {
        return phases.stream().mapToInt(phase -> phase.getResources().size()).sum();
    }

    private String generateGapAnalysisJson(List<SkillGap> skillGaps) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < skillGaps.size(); i++) {
            SkillGap gap = skillGaps.get(i);
            json.append("{")
                .append("\"skill\":\"").append(gap.getSkill().getDisplayName()).append("\",")
                .append("\"currentLevel\":").append(gap.getCurrentLevel()).append(",")
                .append("\"requiredLevel\":").append(gap.getRequiredLevel()).append(",")
                .append("\"gapSize\":").append(gap.getGapSize()).append(",")
                .append("\"priority\":").append(gap.getPriority())
                .append("}");
            if (i < skillGaps.size() - 1) json.append(",");
        }
        json.append("]");
        return json.toString();
    }

    private String generateMotivationalMessage(int skillGapCount, Integer totalWeeks) {
        if (skillGapCount <= 3) {
            return "You're almost there! With just a few key skills to develop, you'll be ready for your target role in no time.";
        } else if (skillGapCount <= 7) {
            return "Great progress! Focus on these core skills and you'll be well-prepared for your career goals.";
        } else {
            return "Every expert was once a beginner. Stay committed to this learning journey - your dedication will pay off!";
        }
    }

    public Roadmap updateRoadmapProgress(Roadmap roadmap, Map<String, Double> updatedSkillLevels) {
        log.info("Updating roadmap progress for roadmap: {}", roadmap.getId());

        // Recalculate phases based on new skill levels
        List<SkillGap> updatedGaps = identifySkillGaps(updatedSkillLevels, roadmap.getSession().getTargetRole());
        List<LearningPhase> updatedPhases = createLearningPhases(updatedGaps, updatedSkillLevels);

        // Update roadmap
        roadmap.setGapAnalysis(generateGapAnalysisJson(updatedGaps));
        roadmap.setMilestones(convertPhasesToJson(updatedPhases));
        roadmap.setTotalEstimatedWeeks(calculateTotalDuration(updatedPhases));

        // Update current level
        double updatedOverallLevel = updatedSkillLevels.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(roadmap.getCurrentOverallLevel());
        roadmap.setCurrentOverallLevel(updatedOverallLevel);

        return roadmapRepository.save(roadmap);
    }

    // Inner classes for roadmap structure
    private static class SkillGap {
        private Skill skill;
        private double currentLevel;
        private double requiredLevel;
        private double gapSize;
        private double priority;

        // Getters and setters
        public Skill getSkill() { return skill; }
        public void setSkill(Skill skill) { this.skill = skill; }
        public double getCurrentLevel() { return currentLevel; }
        public void setCurrentLevel(double currentLevel) { this.currentLevel = currentLevel; }
        public double getRequiredLevel() { return requiredLevel; }
        public void setRequiredLevel(double requiredLevel) { this.requiredLevel = requiredLevel; }
        public double getGapSize() { return gapSize; }
        public void setGapSize(double gapSize) { this.gapSize = gapSize; }
        public double getPriority() { return priority; }
        public void setPriority(double priority) { this.priority = priority; }
    }

    private static class LearningPhase {
        private int phaseNumber;
        private String phaseName;
        private List<Skill> skills;
        private int durationWeeks;
        private List<Resource> resources;
        private List<String> objectives;

        // Getters and setters
        public int getPhaseNumber() { return phaseNumber; }
        public void setPhaseNumber(int phaseNumber) { this.phaseNumber = phaseNumber; }
        public String getPhaseName() { return phaseName; }
        public void setPhaseName(String phaseName) { this.phaseName = phaseName; }
        public List<Skill> getSkills() { return skills; }
        public void setSkills(List<Skill> skills) { this.skills = skills; }
        public int getDurationWeeks() { return durationWeeks; }
        public void setDurationWeeks(int durationWeeks) { this.durationWeeks = durationWeeks; }
        public List<Resource> getResources() { return resources; }
        public void setResources(List<Resource> resources) { this.resources = resources; }
        public List<String> getObjectives() { return objectives; }
        public void setObjectives(List<String> objectives) { this.objectives = objectives; }
    }
}
