package com.skillmap.service.analysis;

import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.Response;
import com.skillmap.model.entity.ResumeData;
import com.skillmap.repository.ResponseRepository;
import com.skillmap.repository.ResumeDataRepository;
import com.skillmap.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RAGFeedbackService {

    private final ResponseRepository responseRepository;
    private final ResumeDataRepository resumeDataRepository;
    private final SymbolicValidationService symbolicValidationService;
    private final OpenAIService openAIService;

    public String generateFeedback(AssessmentSession session) {
        List<Response> responses = responseRepository.findBySessionId(session.getId());
        List<ResumeData> resumes = resumeDataRepository.findBySessionId(session.getId());
        ResumeData resumeData = resumes.isEmpty() ? null : resumes.get(0);

        // Build concise context
        String performance = summarizePerformance(responses);
        String skillAlignment = summarizeSkillAlignment(responses);
        String resumeSummary = summarizeResume(resumeData);

        String prompt = "You are an expert assessor. Create factual, concise feedback with the following sections:" +
                "\n- Skill Strengths & Weaknesses" +
                "\n- Consistency Report (timing + correctness trends)" +
                "\n- Tailored Feedback (tie suggestions to the user's target role and resume where possible)" +
                "\n- Evidence (bullet points quoting short snippets from the user's responses and listing specific skills/resources referenced)" +
                "\n- Suggested Next Steps (3–5 short, actionable tasks with concrete resources)" +
                "\nWrite in a professional, encouraging tone. Base EVERY claim ONLY on the provided data. If unsure, explicitly say 'Based on available context…'." +
                "\n\nResume Summary:\n" + resumeSummary +
                "\n\nPerformance Summary:\n" + performance +
                "\n\nSkill Alignment:\n" + skillAlignment +
                "\n\nFormat ALL sections as clear markdown with bullets and short sentences.";

        String raw = openAIService.generateSuggestion(prompt);
        Set<String> verifiedSkills = symbolicValidationService.extractVerifiedSkills(resumeData);
        String grounded = symbolicValidationService.enforceGrounding(raw, verifiedSkills);
        return grounded;
    }

    private String summarizePerformance(List<Response> responses) {
        if (responses.isEmpty()) return "No responses.";
        long correct = responses.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        double avgTime = responses.stream().map(Response::getThinkTimeSeconds).filter(v -> v != null)
                .mapToInt(Integer::intValue).average().orElse(0);
        return String.format("%d/%d correct, avg think time ~%.1fs", correct, responses.size(), avgTime);
    }

    private String summarizeSkillAlignment(List<Response> responses) {
        if (responses.isEmpty()) return "No data.";
        Map<String, Long> skillCorrect = responses.stream()
                .collect(Collectors.groupingBy(r -> r.getQuestion().getSkill().getDisplayName() +
                                (Boolean.TRUE.equals(r.getIsCorrect()) ? "_correct" : "_incorrect"), Collectors.counting()));
        return skillCorrect.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("; "));
    }

    private String summarizeResume(ResumeData resume) {
        if (resume == null) return "No resume uploaded.";
        String skills = resume.getExtractedSkills() != null ? resume.getExtractedSkills() : "[]";
        Integer years = resume.getTotalYearsExperience();
        return "yearsExperience=" + (years != null ? years : 0) + ", skills=" + skills;
    }
}


