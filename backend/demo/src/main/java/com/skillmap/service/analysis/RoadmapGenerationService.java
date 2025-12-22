package com.skillmap.service.analysis;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.Response;
import com.skillmap.model.entity.Skill;
import com.skillmap.model.entity.SkillAssessment;
import com.skillmap.model.entity.SkillDependency;
import com.skillmap.model.entity.Resource;
import com.skillmap.model.entity.ResumeData;
import com.skillmap.repository.SkillRepository;
import com.skillmap.repository.SkillDependencyRepository;
import com.skillmap.repository.ResourceRepository;
import com.skillmap.repository.SkillAssessmentRepository;
import com.skillmap.repository.ResumeDataRepository;
import com.skillmap.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to generate personalized learning roadmap data based on user responses,
 * skill gaps, AI suggestions, and skill dependencies.
 */
@Service("analysisRoadmapGenerationService")
@RequiredArgsConstructor
@Slf4j
public class RoadmapGenerationService {

    private final SkillRepository skillRepository;
    private final SkillDependencyRepository skillDependencyRepository;
    private final ResourceRepository resourceRepository;
    private final SkillAssessmentRepository skillAssessmentRepository;
    private final ResumeDataRepository resumeDataRepository;
    private final OpenAIService openAIService;

    /**
     * Generates a personalized roadmap data structure for frontend visualization.
     * @param session The assessment session
     * @return Map representing roadmap phases, milestones, and resources
     */
    public Map<String, Object> generatePersonalizedRoadmap(AssessmentSession session) {
        log.info("Generating personalized roadmap for session: {}", session.getId());

        // Step 1: Get skill gaps from skill assessments and resume data
        Map<String, Double> skillGaps = getSkillGaps(session);

        // Step 2: Get resume skills to adjust gaps
        Map<String, Double> resumeSkillLevels = getResumeSkillLevels(session);
        skillGaps = adjustSkillGapsWithResume(skillGaps, resumeSkillLevels);

        // Step 3: Identify top skill gaps to focus on
        List<String> topGaps = skillGaps.entrySet().stream()
            .filter(e -> e.getValue() > 0.3) // threshold for gap
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // Step 4: Build roadmap phases and milestones based on skill dependencies and resources
        List<Map<String, Object>> phases = new ArrayList<>();
        int phaseIndex = 1;

        for (String skillCode : topGaps) {
            Optional<Skill> skillOpt = skillRepository.findBySkillCode(skillCode);
            if (skillOpt.isEmpty()) continue;
            Skill skill = skillOpt.get();

            Map<String, Object> phase = new HashMap<>();
            phase.put("title", "Develop " + skill.getDisplayName());
            phase.put("description", skill.getDescription() != null ? skill.getDescription() : "Improve your skills in " + skill.getDisplayName());
            phase.put("duration", 4); // default 4 weeks per skill phase

            // Milestones: skill dependencies and resources
            List<Map<String, Object>> milestones = new ArrayList<>();

            // Add dependencies as milestones
            List<SkillDependency> dependencies = skillDependencyRepository.findByParentSkillId(skill.getId());
            for (SkillDependency dep : dependencies) {
                Map<String, Object> milestone = new HashMap<>();
                milestone.put("title", "Master prerequisite: " + dep.getChildSkill().getDisplayName());
                milestone.put("description", "Understand and practice " + dep.getChildSkill().getDisplayName());
                milestone.put("resources", getResourcesForSkill(dep.getChildSkill().getId()));
                milestones.add(milestone);
            }

            // Add main skill milestone
            Map<String, Object> mainMilestone = new HashMap<>();
            mainMilestone.put("title", "Master skill: " + skill.getDisplayName());
            mainMilestone.put("description", "Focus on mastering " + skill.getDisplayName());
            mainMilestone.put("resources", getResourcesForSkill(skill.getId()));
            milestones.add(mainMilestone);

            phase.put("milestones", milestones);
            phases.add(phase);
            phaseIndex++;
        }

        // Step 5: Generate AI tailored suggestions for roadmap summary (removed to avoid token waste)
        String userResponsesSummary = summarizeUserResponses(session);
        String skillAnalysis = analyzeSkillGaps(skillGaps);

        Map<String, Object> roadmap = new HashMap<>();
        roadmap.put("phases", phases);
        roadmap.put("totalDuration", phases.size() * 4);
        roadmap.put("totalMilestones", phases.stream().mapToInt(p -> ((List<?>)p.get("milestones")).size()).sum());
        // Removed aiSuggestions to avoid token waste - roadmap is now purely data-driven

        log.info("Generated personalized roadmap for session: {}", session.getId());
        return roadmap;
    }

    private List<Map<String, Object>> getResourcesForSkill(Long skillId) {
        List<Resource> resources = resourceRepository.findBySkillId(skillId);
        return resources.stream().map(resource -> {
            Map<String, Object> resMap = new HashMap<>();
            resMap.put("title", resource.getTitle());
            resMap.put("resourceType", resource.getResourceType());
            resMap.put("url", resource.getUrl());
            resMap.put("description", resource.getDescription());
            return resMap;
        }).collect(Collectors.toList());
    }

    private Map<String, Double> getSkillGaps(AssessmentSession session) {
        List<SkillAssessment> assessments = skillAssessmentRepository.findBySession(session);
        Map<String, Double> gaps = new HashMap<>();

        skillRepository.findAll().forEach(skill -> {
            double level = assessments.stream()
                .filter(assessment -> assessment.getSkill().equals(skill))
                .map(SkillAssessment::getAssessedLevel)
                .findFirst()
                .orElse(0.5);
            gaps.put(skill.getSkillCode(), 1.0 - level);
        });

        return gaps;
    }

    private Map<String, Double> getResumeSkillLevels(AssessmentSession session) {
        Map<String, Double> resumeSkills = new HashMap<>();

        // Get resume data for this session
        List<ResumeData> resumeDataList = resumeDataRepository.findBySessionId(session.getId());
        if (!resumeDataList.isEmpty()) {
            ResumeData resumeData = resumeDataList.get(0); // Use the most recent resume

            // Parse extracted skills from resume
            if (resumeData.getExtractedSkills() != null) {
                try {
                    List<String> skills = parseJsonArray(resumeData.getExtractedSkills());
                    // Assume resume skills indicate some proficiency (e.g., 0.7 level)
                    skills.forEach(skill -> resumeSkills.put(skill.toLowerCase(), 0.7));
                } catch (Exception e) {
                    log.warn("Failed to parse extracted skills from resume: {}", e.getMessage());
                }
            }
        }

        return resumeSkills;
    }

    private Map<String, Double> adjustSkillGapsWithResume(Map<String, Double> skillGaps, Map<String, Double> resumeSkillLevels) {
        Map<String, Double> adjustedGaps = new HashMap<>(skillGaps);

        // Adjust gaps based on resume skills - reduce gap if skill is mentioned in resume
        resumeSkillLevels.forEach((skillName, resumeLevel) -> {
            // Find matching skill codes (simple string matching)
            adjustedGaps.entrySet().forEach(entry -> {
                String skillCode = entry.getKey();
                if (skillCode.toLowerCase().contains(skillName) || skillName.contains(skillCode.toLowerCase())) {
                    double currentGap = entry.getValue();
                    // Reduce gap by resume level (resume indicates some existing knowledge)
                    double adjustedGap = Math.max(0, currentGap - resumeLevel);
                    entry.setValue(adjustedGap);
                }
            });
        });

        return adjustedGaps;
    }

    private List<String> parseJsonArray(String jsonString) {
        // Simple JSON array parser for strings
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Remove brackets and quotes, split by comma
            String cleaned = jsonString.replaceAll("^\\[|\\]$", "").replaceAll("\"", "");
            return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to parse JSON array: {}", jsonString);
            return new ArrayList<>();
        }
    }

    private String summarizeUserResponses(AssessmentSession session) {
        // Summarize user responses with evidence-based insights
        StringBuilder summary = new StringBuilder();
        summary.append("User Assessment Summary:\n");

        // Analyze response patterns for insights
        List<Response> responses = session.getResponses();
        if (responses != null && !responses.isEmpty()) {
            int totalResponses = responses.size();
            int shortResponses = (int) responses.stream()
                .filter(r -> r.getResponseText() != null && r.getResponseText().length() < 50)
                .count();
            int lowConfidenceResponses = (int) responses.stream()
                .filter(r -> r.getConfidenceLevel() != null && r.getConfidenceLevel() < 0.6)
                .count();
            int quickResponses = (int) responses.stream()
                .filter(r -> r.getThinkTimeSeconds() != null && r.getThinkTimeSeconds() < 30)
                .count();

            summary.append(String.format("- Total responses: %d\n", totalResponses));
            if (shortResponses > totalResponses * 0.3) {
                summary.append("- Many brief responses suggest areas needing deeper understanding\n");
            }
            if (lowConfidenceResponses > totalResponses * 0.4) {
                summary.append("- Multiple uncertain responses indicate knowledge gaps\n");
            }
            if (quickResponses > totalResponses * 0.3) {
                summary.append("- Quick responses may indicate surface-level knowledge in some areas\n");
            }

            // Add specific response examples for context
            summary.append("\nKey Response Patterns:\n");
            responses.stream()
                .filter(r -> r.getResponseText() != null && r.getResponseText().length() > 10)
                .limit(3)
                .forEach(r -> {
                    summary.append(String.format("- Question: %s\n",
                        r.getQuestion() != null ? r.getQuestion().getQuestionText() : "N/A"));
                    summary.append(String.format("  Response: %s\n",
                        r.getResponseText().substring(0, Math.min(100, r.getResponseText().length()))));
                });
        }

        return summary.toString();
    }

    private String analyzeSkillGaps(Map<String, Double> skillGaps) {
        // Placeholder: create textual skill gap analysis for AI prompt
        StringBuilder sb = new StringBuilder();
        skillGaps.forEach((skill, gap) -> {
            if (gap > 0.3) {
                sb.append(String.format("Skill %s has a gap of %.2f. ", skill, gap));
            }
        });
        return sb.toString();
    }
}
