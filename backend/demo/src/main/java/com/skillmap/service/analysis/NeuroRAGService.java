package com.skillmap.service.analysis;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.Response;
import com.skillmap.model.entity.Resource;
import com.skillmap.model.entity.ResumeData;
import com.skillmap.model.entity.Skill;
import com.skillmap.model.entity.SkillDependency;
import com.skillmap.repository.ResponseRepository;
import com.skillmap.repository.ResumeDataRepository;
import com.skillmap.repository.SkillDependencyRepository;
import com.skillmap.repository.SkillRepository;
import com.skillmap.repository.ResourceRepository;
import com.skillmap.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NeuroRAGService {

    private final ResponseRepository responseRepository;
    private final ResumeDataRepository resumeDataRepository;
    private final SkillRepository skillRepository;
    private final SkillDependencyRepository skillDependencyRepository;
    private final ResourceRepository resourceRepository;
    private final SymbolicValidationService symbolicValidationService;
    private final OpenAIService openAIService;
    private final com.skillmap.service.nlp.HuggingFaceNERService nerService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateValidatedFeedback(AssessmentSession session) {
        List<Response> responses = responseRepository.findBySessionId(session.getId());
        List<ResumeData> resumes = resumeDataRepository.findBySessionId(session.getId());
        ResumeData resumeData = resumes.isEmpty() ? null : resumes.get(0);

        // Log resume data for debugging
        if (resumeData != null) {
            log.info("Resume data found for session {}: skills={}, education={}, experience={}",
                session.getId(),
                resumeData.getExtractedSkills(),
                resumeData.getExtractedEducation(),
                resumeData.getExtractedExperience());
        }

        // 1) Neural retrieval: build candidates and rank via lightweight embeddings
        List<Candidate> candidates = buildCandidates();
        String userQuery = buildUserQuery(responses, resumeData);
        double[] q = embed(userQuery);
        List<Candidate> top = candidates.stream()
                .sorted((a, b) -> Double.compare(cos(q, embed(b.text)), cos(q, embed(a.text))))
                .limit(12)
                .collect(Collectors.toList());

        // 2) Symbolic layer: verify, filter, refine using skills graph
        List<Candidate> filtered = symbolicFilter(top);

        // 3) Compose grounded context
        String groundedContext = filtered.stream()
                .map(c -> String.format("[%s] %s", c.type, c.text))
                .collect(Collectors.joining("\n"));

        // 4) Build prompt with strict grounding instructions
        String prompt = buildPrompt(responses, resumeData, groundedContext);

        // 5) Generate with caching
        String raw = openAIService.generateSuggestion(prompt, session.getId(), session.getUser().getId(), "neurorag_feedback");

        // 6) Validate final output against symbolic layer (allow only verified skills/relations)
        Set<String> verifiedSkills = symbolicValidationService.extractVerifiedSkills(resumeData);
        String grounded = symbolicValidationService.enforceGrounding(raw, verifiedSkills);
        return grounded;
    }

    private static class Candidate {
        final String type; // SKILL | DEP | RESOURCE
        final String text;
        Candidate(String type, String text) { this.type = type; this.text = text; }
    }

    private List<Candidate> buildCandidates() {
        List<Candidate> list = new ArrayList<>();
        for (Skill s : skillRepository.findAll()) {
            String desc = s.getDescription() != null ? s.getDescription() : "";
            list.add(new Candidate("SKILL", s.getDisplayName() + ": " + desc));
        }
        for (SkillDependency d : skillDependencyRepository.findAll()) {
            String rel = String.format("%s requires %s (type=%s, weight=%.2f)",
                    d.getParentSkill().getDisplayName(), d.getChildSkill().getDisplayName(),
                    d.getDependencyType(), d.getWeight());
            list.add(new Candidate("DEP", rel));
        }
        for (Resource r : resourceRepository.findAll()) {
            String text = String.format("%s (%s): %s", r.getTitle(), r.getResourceType(),
                    r.getUrl() != null ? r.getUrl() : (r.getDescription() != null ? r.getDescription() : ""));
            list.add(new Candidate("RESOURCE", text));
        }
        return list;
    }

    private String buildUserQuery(List<Response> responses, ResumeData resume) {
        StringBuilder queryBuilder = new StringBuilder();

        // Extract entities from responses
        for (Response r : responses) {
            String text = r.getResponseText() != null ? r.getResponseText() : "";
            String qType = r.getQuestion().getQuestionType();

            if ("mcq".equalsIgnoreCase(qType)) {
                // For MCQ, include question, selected choice, correctness, and options if available
                queryBuilder.append("MCQ Question: ").append(r.getQuestion().getQuestionText()).append("\n");
                queryBuilder.append("Selected Choice: ").append(text).append("\n");
                boolean isCorrect = r.getIsCorrect() != null && r.getIsCorrect();
                queryBuilder.append("Is Correct: ").append(isCorrect).append("\n");
                // Append options for context if available in question
                if (r.getQuestion().getOptions() != null && !r.getQuestion().getOptions().isEmpty()) {
                    queryBuilder.append("Options: ").append(String.join(", ", r.getQuestion().getOptions())).append("\n");
                }
            } else {
                // For text responses, append full text
                queryBuilder.append("Text Response: ").append(text).append("\n");
            }

            // Extract skills, entities, dependencies using NER and derive sections
            var entities = nerService.extractEntities(text + " " + r.getQuestion().getQuestionText());
            var sections = nerService.deriveSections(entities);
            sections.forEach((key, values) -> {
                if (!values.isEmpty()) {
                    queryBuilder.append(key).append(": ").append(String.join(", ", values)).append("\n");
                }
            });

            // Additional skill extraction from response using lightweight OpenAI if needed
            if (!"mcq".equalsIgnoreCase(qType)) {
                try {
                    String skillPrompt = "Extract key skills and dependencies from this response: " + text;
                    String skillsJson = openAIService.generateSuggestion(skillPrompt, r.getSession().getId(), null, "skill_extraction");
                    if (skillsJson != null && !skillsJson.isEmpty()) {
                        queryBuilder.append("Extracted Skills/Dependencies: ").append(skillsJson).append("\n");
                    }
                } catch (Exception e) {
                    log.warn("Skill extraction failed for response: " + r.getId(), e);
                }
            }
        }

        String resumeText = resume != null && resume.getRawText() != null ? resume.getRawText() : "";
        queryBuilder.append("Resume Content: ").append(resumeText);
        return queryBuilder.toString().toLowerCase();
    }

    private List<Candidate> symbolicFilter(List<Candidate> top) {
        // Enforce relationship-based reasoning: if DEP mentions A requires B, ensure A and B exist as skills
        Set<String> allSkillNames = skillRepository.findAll().stream()
                .map(s -> s.getDisplayName().toLowerCase())
                .collect(Collectors.toSet());
        return top.stream().filter(c -> {
            if ("DEP".equals(c.type)) {
                String t = c.text.toLowerCase();
                // naive parse: "A requires B"
                String[] parts = t.split(" requires ");
                if (parts.length == 2) {
                    String a = parts[0].replaceAll("[^a-z0-9 +.#]", " ").trim();
                    String b = parts[1].replaceAll("[^a-z0-9 +.#]", " ").trim();
                    return allSkillNames.stream().anyMatch(a::contains) && allSkillNames.stream().anyMatch(b::contains);
                }
            }
            // SKILL/RESOURCE: accept if mentions a known skill token
            String canonical = c.text.toLowerCase();
            return allSkillNames.stream().anyMatch(canonical::contains);
        }).collect(Collectors.toList());
    }

    private String buildPrompt(List<Response> responses, ResumeData resume, String groundedContext) {
        String performance = summarizePerformance(responses);
        String userSignals = responses.stream()
                .map(r -> String.format("- Q: %s\n  A: %s\n  sim=%.2f wpm=%s corr=%s",
                        r.getQuestion().getQuestionText(),
                        truncate(r.getResponseText(), 160),
                        r.getSimilarityScore() != null ? r.getSimilarityScore() : 0.0,
                        r.getTypingSpeedWpm() != null ? String.format("%.0f", r.getTypingSpeedWpm()) : "",
                        String.valueOf(Boolean.TRUE.equals(r.getIsCorrect()))))
                .collect(Collectors.joining("\n"));
        String resumeSummary = resume != null ? truncate(resume.getRawText(), 800) : "";

        String userResponsesSection = responses.stream()
                .map(r -> String.format("Question: %s\nYour Answer: %s\nCorrect: %s\n",
                        r.getQuestion().getQuestionText(),
                        r.getResponseText() != null ? r.getResponseText() : "N/A",
                        r.getIsCorrect() != null ? r.getIsCorrect() : "N/A"))
                .collect(Collectors.joining("\n"));

        return "You are a Neuro-RAG system. Use the provided GROUND TRUTH only.\n" +
                "Pipeline:\n- Retrieve neural contexts\n- Symbolically verify dependencies/skills\n- Compose concise plan grounded only in verified facts.\n" +
                "If a claim is not in GROUND TRUTH, add: 'Based on available context…'\n\n" +
                "GROUND TRUTH (verified):\n" + groundedContext + "\n\n" +
                "USER SIGNALS:\n" + userSignals + "\n\n" +
                "USER RESPONSES:\n" + userResponsesSection + "\n\n" +
                "RESUME (truncated):\n" + resumeSummary + "\n\n" +
                "TASK: Produce four sections: User Responses Summary; Skill Strengths & Weaknesses; Consistency Report; Suggested Next Steps.\n" +
                "Include a summary of the user's actual responses in the first section. All suggestions must reference items from GROUND TRUTH by name. Avoid hallucinations.";
    }

    private String summarizePerformance(List<Response> responses) {
        if (responses.isEmpty()) return "No responses.";
        long correct = responses.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        double avgTime = responses.stream().map(Response::getThinkTimeSeconds).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).average().orElse(0);
        return String.format("%d/%d correct, avg think time ~%.1fs", correct, responses.size(), avgTime);
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    private static double[] embed(String input) {
        String clean = input == null ? "" : input.toLowerCase().replaceAll("[^a-z0-9 +.#]", " ");
        String[] tokens = clean.trim().split("\\s+");
        Map<String, Integer> counts = new HashMap<>();
        for (String t : tokens) {
            if (t.isEmpty()) continue;
            counts.put(t, counts.getOrDefault(t, 0) + 1);
        }
        int dim = 512;
        double[] vec = new double[dim];
        for (var e : counts.entrySet()) {
            int idx = Math.floorMod(e.getKey().hashCode(), dim);
            vec[idx] += e.getValue();
        }
        double norm = 0.0;
        for (double v : vec) norm += v * v;
        norm = Math.sqrt(norm) + 1e-9;
        for (int i = 0; i < dim; i++) vec[i] /= norm;
        return vec;
    }

    private static double cos(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return Math.max(0.0, Math.min(1.0, dot));
    }

    public Map<String, Object> calculatePerformance(AssessmentSession session) {
        Map<String, Object> result = new HashMap<>();
        List<Response> responses = responseRepository.findBySessionId(session.getId());
        if (responses.isEmpty()) {
            result.put("error", "No responses found for session");
            return result;
        }

        // Get resume data for enhanced performance calculation
        List<ResumeData> resumeDataList = resumeDataRepository.findBySessionId(session.getId());
        ResumeData resumeData = resumeDataList.isEmpty() ? null : resumeDataList.get(0);

        // Calculate average similarity score
        double totalSimilarity = 0.0;
        int count = 0;
        for (Response r : responses) {
            if (r.getSimilarityScore() != null) {
                totalSimilarity += r.getSimilarityScore();
                count++;
            }
        }
        double avgSimilarity = count > 0 ? totalSimilarity / count : 0.0;

        // Calculate average confidence
        double totalConfidence = 0.0;
        int confidenceCount = 0;
        for (Response r : responses) {
            if (r.getConfidenceLevel() != null) {
                totalConfidence += r.getConfidenceLevel();
                confidenceCount++;
            }
        }
        double avgConfidence = confidenceCount > 0 ? totalConfidence / confidenceCount : 0.0;

        // Calculate average typing speed
        double totalTypingSpeed = 0.0;
        int typingCount = 0;
        for (Response r : responses) {
            if (r.getTypingSpeedWpm() != null) {
                totalTypingSpeed += r.getTypingSpeedWpm();
                typingCount++;
            }
        }
        double avgTypingSpeed = typingCount > 0 ? totalTypingSpeed / typingCount : 0.0;

        // Calculate average think time
        double totalThinkTime = 0.0;
        int thinkCount = 0;
        for (Response r : responses) {
            if (r.getThinkTimeSeconds() != null) {
                totalThinkTime += r.getThinkTimeSeconds();
                thinkCount++;
            }
        }
        double avgThinkTime = thinkCount > 0 ? totalThinkTime / thinkCount : 0.0;

        // Calculate average total time
        double totalTime = 0.0;
        int timeCount = 0;
        for (Response r : responses) {
            if (r.getTotalTimeSeconds() != null) {
                totalTime += r.getTotalTimeSeconds();
                timeCount++;
            }
        }
        double avgTotalTime = timeCount > 0 ? totalTime / timeCount : 0.0;

        // Calculate average edit count
        double totalEdits = 0.0;
        int editCount = 0;
        for (Response r : responses) {
            if (r.getEditCount() != null) {
                totalEdits += r.getEditCount();
                editCount++;
            }
        }
        double avgEdits = editCount > 0 ? totalEdits / editCount : 0.0;

        // Calculate paste detection rate
        int pasteCount = 0;
        for (Response r : responses) {
            if (r.getPasteDetected() != null && r.getPasteDetected()) {
                pasteCount++;
            }
        }
        double pasteRate = responses.size() > 0 ? (double) pasteCount / responses.size() : 0.0;

        // Calculate MCQ accuracy
        int correctMCQ = 0;
        int totalMCQ = 0;
        for (Response r : responses) {
            if ("mcq".equals(r.getQuestion().getQuestionType())) {
                totalMCQ++;
                if (r.getIsCorrect() != null && r.getIsCorrect()) {
                    correctMCQ++;
                }
            }
        }
        double mcqAccuracy = totalMCQ > 0 ? (double) correctMCQ / totalMCQ : 0.0;

        // Enhanced performance score calculation with resume data
        double resumeBonus = 0.0;
        if (resumeData != null) {
            // Bonus for having resume data
            resumeBonus += 0.1;

            // Bonus for years of experience (up to 0.2)
            if (resumeData.getTotalYearsExperience() != null) {
                double expYears = resumeData.getTotalYearsExperience();
                resumeBonus += Math.min(expYears / 10.0, 0.2); // Max 0.2 bonus for 10+ years
            }

            // Bonus for skills count (up to 0.1)
            try {
                List<String> skills = objectMapper.readValue(resumeData.getExtractedSkills(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){});
                resumeBonus += Math.min(skills.size() / 20.0, 0.1); // Max 0.1 bonus for 20+ skills
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }

        // Calculate overall performance score (weighted average with resume bonus)
        double baseScore = (avgSimilarity * 0.3) + (avgConfidence * 0.2) + (mcqAccuracy * 0.3) + ((1.0 - pasteRate) * 0.2);
        double performanceScore = Math.min(baseScore + resumeBonus, 1.0); // Cap at 1.0

        result.put("averageSimilarity", avgSimilarity);
        result.put("averageConfidence", avgConfidence);
        result.put("averageTypingSpeed", avgTypingSpeed);
        result.put("averageThinkTime", avgThinkTime);
        result.put("averageTotalTime", avgTotalTime);
        result.put("averageEdits", avgEdits);
        result.put("pasteRate", pasteRate);
        result.put("mcqAccuracy", mcqAccuracy);
        result.put("performanceScore", performanceScore);
        result.put("resumeBonus", resumeBonus);
        result.put("responseCount", responses.size());

        return result;
    }
}



