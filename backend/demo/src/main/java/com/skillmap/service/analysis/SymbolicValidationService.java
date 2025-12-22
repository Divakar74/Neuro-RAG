package com.skillmap.service.analysis;

import com.skillmap.model.entity.ResumeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SymbolicValidationService {

    public Set<String> extractVerifiedSkills(ResumeData resumeData) {
        if (resumeData == null) {
            return Collections.emptySet();
        }

        Set<String> skills = new HashSet<>();

        // Extract from extractedSkills (JSON array of strings from Gemini)
        if (resumeData.getExtractedSkills() != null) {
            try {
                // Parse as JSON array of strings
                @SuppressWarnings("unchecked")
                List<String> skillList = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(resumeData.getExtractedSkills(), List.class);
                for (String skill : skillList) {
                    if (skill != null && !skill.trim().isEmpty()) {
                        skills.add(skill.trim().toLowerCase());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse extractedSkills JSON", e);
                // Fallback to regex parsing
                Pattern namePattern = Pattern.compile("\\\"(.*?)\\\"");
                var matcher = namePattern.matcher(resumeData.getExtractedSkills());
                while (matcher.find()) {
                    String skill = matcher.group(1).trim();
                    if (!skill.isEmpty()) skills.add(skill.toLowerCase());
                }
            }
        }

        // Also extract from raw text as backup
        if (resumeData.getRawText() != null) {
            String text = resumeData.getRawText().toLowerCase();
            // Common tech skills to look for
            String[] commonSkills = {"java", "python", "javascript", "react", "spring", "sql", "git", "docker", "aws", "kubernetes"};
            for (String skill : commonSkills) {
                if (text.contains(skill)) {
                    skills.add(skill);
                }
            }
        }

        return skills;
    }

    public String enforceGrounding(String generated, Set<String> verifiedSkillsInput) {
        if (generated == null || generated.isBlank()) return generated;
        final Set<String> verifiedSkills = (verifiedSkillsInput == null)
                ? Collections.emptySet()
                : verifiedSkillsInput;
        List<String> lines = Arrays.asList(generated.split("\\r?\\n"));

        // filter out lines that assert unverified skills explicitly
        List<String> filtered = lines.stream().filter(line -> {
            // if line mentions a capitalized token that looks like a skill, ensure it's verified
            String[] tokens = line.replaceAll("[^A-Za-z0-9+.# ]"," ").split("\\s+");
            for (String token : tokens) {
                if (token.length() >= 3 && Character.isUpperCase(token.charAt(0))) {
                    if (!verifiedSkills.contains(token.toLowerCase())) {
                        // allow generic terms
                        if (!isGenericTerm(token)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }).collect(Collectors.toList());

        return String.join("\n", filtered);
    }

    private boolean isGenericTerm(String token) {
        String t = token.toLowerCase();
        return t.equals("api") || t.equals("rest") || t.equals("sql") || t.equals("cloud") || t.equals("testing");
    }
}


