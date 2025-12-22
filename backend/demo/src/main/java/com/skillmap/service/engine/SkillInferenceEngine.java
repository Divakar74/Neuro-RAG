package com.skillmap.service.engine;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.Response;
import com.skillmap.model.entity.ResumeData;
import com.skillmap.model.entity.Skill;
import com.skillmap.model.entity.SkillAssessment;
import com.skillmap.repository.ResponseRepository;
import com.skillmap.repository.SkillAssessmentRepository;
import com.skillmap.repository.SkillRepository;
import com.skillmap.repository.ResumeDataRepository;
import com.skillmap.service.analysis.SymbolicValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillInferenceEngine {

    private final SkillRepository skillRepository;
    private final ResponseRepository responseRepository;
    private final SkillAssessmentRepository skillAssessmentRepository;
    private final SkillGraphService skillGraphService;
    private final ResumeDataRepository resumeDataRepository;
    private final SymbolicValidationService symbolicValidationService;

    // Bayesian belief propagation for skill inference
    private Map<String, Map<String, Double>> skillCorrelations = new HashMap<>();
    private volatile boolean correlationsBuilt = false;

    @PostConstruct
    public void initialize() {
        log.info("Initializing skill inference engine");
        // Build correlations lazily to avoid session issues
    }

    private synchronized void ensureSkillCorrelationsBuilt() {
        if (!correlationsBuilt) {
            buildSkillCorrelations();
            correlationsBuilt = true;
        }
    }

    private void buildSkillCorrelations() {
        log.info("Building skill correlations");

        skillRepository.findAll().forEach(skill -> {
            Map<String, Double> correlations = new HashMap<>();

            // Correlate with prerequisites and dependents
            List<String> prerequisites = skillGraphService.getPrerequisiteSkills(skill.getSkillCode());
            List<String> dependents = skillGraphService.getDependentSkills(skill.getSkillCode());

            prerequisites.forEach(prereq -> correlations.put(prereq, 0.7));
            dependents.forEach(dependent -> correlations.put(dependent, 0.6));

            // Add domain correlations (simplified)
            if (skill.getCategory() != null) {
                skillRepository.findByCategory(skill.getCategory()).forEach(relatedSkill -> {
                    if (!relatedSkill.getSkillCode().equals(skill.getSkillCode())) {
                        correlations.put(relatedSkill.getSkillCode(), 0.4);
                    }
                });
            }

            skillCorrelations.put(skill.getSkillCode(), correlations);
        });
    }

    public void updateBeliefsFromResponse(Response response) {
        log.info("Updating beliefs from response: {}", response.getId());

        // Ensure skill correlations are built before processing
        ensureSkillCorrelationsBuilt();

        AssessmentSession session = response.getSession();
        Skill skill = response.getQuestion().getSkill();
        double responseScore = calculateResponseScore(response);

        // Get or create skill assessment for this session and skill
        SkillAssessment assessment = skillAssessmentRepository
            .findBySessionAndSkill(session, skill)
            .orElse(new SkillAssessment());

        if (assessment.getId() == null) {
            // New assessment
            assessment.setSession(session);
            assessment.setSkill(skill);
            assessment.setAssessedLevel(responseScore);
            assessment.setConfidenceScore(0.5); // Initial confidence
        } else {
            // Update existing assessment with weighted average
            double currentLevel = assessment.getAssessedLevel();
            double currentConfidence = assessment.getConfidenceScore();
            double newLevel = (currentLevel * currentConfidence + responseScore * 0.5) / (currentConfidence + 0.5);
            double newConfidence = Math.min(currentConfidence + 0.1, 1.0); // Increase confidence

            assessment.setAssessedLevel(newLevel);
            assessment.setConfidenceScore(newConfidence);
        }

        // Update evidence
        String evidenceIds = assessment.getEvidenceResponseIds();
        if (evidenceIds == null) {
            evidenceIds = "[" + response.getId() + "]";
        } else {
            evidenceIds = evidenceIds.replace("]", "," + response.getId() + "]");
        }
        assessment.setEvidenceResponseIds(evidenceIds);

        skillAssessmentRepository.save(assessment);

        // Propagate to correlated skills
        propagateBeliefs(session, skill.getSkillCode(), responseScore);
    }

    private double calculateResponseScore(Response response) {
        // Calculate score based on response quality metrics
        double score = 0.0;

        // For MCQ questions, use correctness as primary score
        if ("mcq".equals(response.getQuestion().getQuestionType())) {
            if (response.getIsCorrect() != null && response.getIsCorrect()) {
                score = 1.0; // Full score for correct MCQ answer
            } else {
                score = 0.0; // No score for incorrect MCQ answer
            }
        } else {
            // For text questions, use traditional scoring
            // Base score from specificity and depth scores
            if (response.getSpecificityScore() != null) {
                score += response.getSpecificityScore() * 0.3;
            }

            // Score from response length and detail
            if (response.getResponseText() != null) {
                int length = response.getResponseText().length();
                score += Math.min(length / 500.0, 1.0) * 0.4; // Up to 0.4 for detailed responses
            }

            // Score from analysis metrics (if available)
            if (response.getDepthScore() != null) {
                score += response.getDepthScore() * 0.3;
            }
        }

        return Math.min(score, 1.0);
    }

    private void propagateBeliefs(AssessmentSession session, String sourceSkill, double evidence) {
        Map<String, Double> correlations = skillCorrelations.get(sourceSkill);

        if (correlations != null) {
            correlations.forEach((targetSkillCode, correlation) -> {
                Optional<Skill> targetSkillOpt = skillRepository.findBySkillCode(targetSkillCode);
                if (targetSkillOpt.isPresent()) {
                    Skill targetSkill = targetSkillOpt.get();
                    double propagatedEvidence = evidence * correlation * 0.5; // Reduce propagation strength

                    SkillAssessment assessment = skillAssessmentRepository
                        .findBySessionAndSkill(session, targetSkill)
                        .orElse(null);

                    if (assessment != null) {
                        // Only update existing assessments with propagated evidence
                        double currentLevel = assessment.getAssessedLevel();
                        double currentConfidence = assessment.getConfidenceScore();
                        double newLevel = (currentLevel * currentConfidence + propagatedEvidence * 0.3) /
                                        (currentConfidence + 0.3);
                        assessment.setAssessedLevel(newLevel);
                        skillAssessmentRepository.save(assessment);
                    }
                    // Do not create new assessments for propagated beliefs
                }
            });
        }
    }

    public Map<String, Double> getSkillBeliefs(AssessmentSession session) {
        List<SkillAssessment> assessments = skillAssessmentRepository.findBySession(session);
        Map<String, Double> beliefs = new HashMap<>();

        // Get resume-based priors
        Map<String, Double> resumePriors = getResumeSkillPriors(session);

        // Initialize all skills with resume priors or neutral belief
        skillRepository.findAll().forEach(skill -> {
            String skillCode = skill.getSkillCode();
            double prior = resumePriors.getOrDefault(skillCode, 0.5);
            beliefs.put(skillCode, prior);
        });

        // Update with assessment evidence using Bayesian inference
        assessments.forEach(assessment -> {
            String skillCode = assessment.getSkill().getSkillCode();
            double prior = resumePriors.getOrDefault(skillCode, 0.5);
            double evidence = assessment.getAssessedLevel();
            double confidence = assessment.getConfidenceScore();

            // Bayesian update: combine prior with evidence
            double posterior = (prior * (1 - confidence) + evidence * confidence) / (1 - confidence + confidence);
            beliefs.put(skillCode, posterior);
        });

        return beliefs;
    }

    public double getSkillBelief(AssessmentSession session, String skillCode) {
        Optional<Skill> skillOpt = skillRepository.findBySkillCode(skillCode);
        if (skillOpt.isPresent()) {
            return skillAssessmentRepository.findBySessionAndSkill(session, skillOpt.get())
                .map(SkillAssessment::getAssessedLevel)
                .orElse(0.5);
        }
        return 0.5;
    }

    public List<String> getHighConfidenceSkills(AssessmentSession session, double threshold) {
        return skillAssessmentRepository.findBySession(session).stream()
            .filter(assessment -> assessment.getAssessedLevel() >= threshold)
            .map(assessment -> assessment.getSkill().getSkillCode())
            .collect(Collectors.toList());
    }

    public List<String> getLowConfidenceSkills(AssessmentSession session, double threshold) {
        return skillAssessmentRepository.findBySession(session).stream()
            .filter(assessment -> assessment.getAssessedLevel() <= threshold)
            .map(assessment -> assessment.getSkill().getSkillCode())
            .collect(Collectors.toList());
    }

    public Map<String, Double> getSkillGaps(AssessmentSession session) {
        Map<String, Double> gaps = new HashMap<>();
        List<SkillAssessment> assessments = skillAssessmentRepository.findBySession(session);

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

    // Legacy methods for backward compatibility (return global neutral beliefs)
    @Deprecated
    public Map<String, Double> getSkillBeliefs() {
        Map<String, Double> beliefs = new HashMap<>();
        skillRepository.findAll().forEach(skill -> {
            beliefs.put(skill.getSkillCode(), 0.5);
        });
        return beliefs;
    }

    @Deprecated
    public double getSkillBelief(String skillCode) {
        return 0.5;
    }

    /**
     * Extracts skill priors from resume data for Bayesian inference
     * @param session the assessment session
     * @return map of skill codes to prior probabilities (0.8 for present skills, 0.2 for absent)
     */
    private Map<String, Double> getResumeSkillPriors(AssessmentSession session) {
        Map<String, Double> priors = new HashMap<>();

        // Get resume data for the session
        List<ResumeData> resumeDataList = resumeDataRepository.findBySessionId(session.getId());

        if (!resumeDataList.isEmpty()) {
            // Use the most recent resume data
            ResumeData resumeData = resumeDataList.get(0);

            // Extract verified skills from resume
            Set<String> resumeSkills = symbolicValidationService.extractVerifiedSkills(resumeData);

            // Get all system skills for comparison
            List<Skill> allSkills = skillRepository.findAll();

            // Set priors based on resume presence
            for (Skill skill : allSkills) {
                String skillName = skill.getSkillCode().toLowerCase();
                boolean skillPresent = resumeSkills.stream()
                    .anyMatch(resumeSkill -> resumeSkill.toLowerCase().contains(skillName) ||
                                             skillName.contains(resumeSkill.toLowerCase()));

                // High prior for skills present in resume, low prior for absent skills
                priors.put(skill.getSkillCode(), skillPresent ? 0.8 : 0.2);
            }

            log.info("Extracted {} resume skills as priors for session {}", resumeSkills.size(), session.getId());
        } else {
            log.debug("No resume data found for session {}, using neutral priors", session.getId());
        }

        return priors;
    }
}
