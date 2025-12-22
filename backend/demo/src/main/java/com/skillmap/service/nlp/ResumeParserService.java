package com.skillmap.service.nlp;

import com.skillmap.model.dto.ExtractedSkill;
import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.ResumeData;
import com.skillmap.repository.ResumeDataRepository;
import com.skillmap.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeParserService {

    private final SkillRepository skillRepository;
    private final ResumeDataRepository resumeDataRepository;
    private final HuggingFaceNERService huggingFaceNERService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern YEARS_PATTERN = Pattern.compile(
        "(\\d+)\\+?\\s*(?:years?|yrs?)",
        Pattern.CASE_INSENSITIVE
    );

    public ResumeData parseResume(String resumeText, AssessmentSession session) {
        log.info("Parsing resume for session: {}", session.getId());

        // Preprocess text
        String cleanText = preprocessText(resumeText);

        // Extract sections
        Map<String, String> sections = extractSections(cleanText);

        // Optionally enrich using Hugging Face NER
        java.util.List<java.util.Map<String, Object>> rawEntities = java.util.Collections.emptyList();
        try {
            rawEntities = huggingFaceNERService.extractEntities(cleanText);
            if (!rawEntities.isEmpty()) {
                Map<String, java.util.List<String>> hf = huggingFaceNERService.deriveSections(rawEntities);
                if (!hf.getOrDefault("skills", java.util.List.of()).isEmpty()) {
                    sections.put("skills", String.join("\n", hf.get("skills")));
                }
                if (!hf.getOrDefault("education", java.util.List.of()).isEmpty()) {
                    sections.put("education", String.join("\n", hf.get("education")));
                }
                if (!hf.getOrDefault("experience", java.util.List.of()).isEmpty()) {
                    sections.put("experience", String.join("\n", hf.get("experience")));
                }
            }
        } catch (Exception e) {
            log.debug("HF enrichment skipped: {}", e.toString());
        }

        // Extract skills
        List<ExtractedSkill> skills = extractSkills(sections);

        // Extract education
        List<String> education = extractEducation(sections.getOrDefault("education", ""));

        // Calculate total experience
        int totalYears = calculateTotalExperience(sections.getOrDefault("experience", ""));

        // Derive experience items (bulleted list) from HF or sections
        List<String> experienceItems = new ArrayList<>();
        try {
            Map<String, List<String>> hfSections = huggingFaceNERService.deriveSections(rawEntities);
            if (!hfSections.getOrDefault("experience", List.of()).isEmpty()) {
                experienceItems.addAll(hfSections.get("experience"));
            }
        } catch (Exception ignored) {}
        if (experienceItems.isEmpty()) {
            String expBlock = sections.getOrDefault("experience", "");
            // Split on lines and bullets; filter out too-short lines
            for (String line : expBlock.split("\n")) {
                String trimmed = line.replaceAll("^[•·●◦\\-\\*]+\\s*", "").trim();
                if (trimmed.length() >= 12) {
                    experienceItems.add(trimmed);
                }
                if (experienceItems.size() >= 40) break;
            }
        }

        // Save to database
        ResumeData resumeData = new ResumeData();
        resumeData.setSession(session);
        resumeData.setRawText(resumeText);
        // Convert extracted skills list to JSON string
        resumeData.setExtractedSkills(convertSkillsToJson(skills));
        // Convert education list to JSON string
        resumeData.setExtractedEducation(convertListToJson(education));
        // Persist structured experience list
        resumeData.setExtractedExperience(convertListToJson(experienceItems));
        resumeData.setTotalYearsExperience(totalYears);
        // Save raw entities from NER
        resumeData.setRawEntities(convertEntitiesToJson(rawEntities));

        try {
            ResumeData saved = resumeDataRepository.save(resumeData);
            return saved;
        } catch (Exception e) {
            log.error("Error saving ResumeData to database", e);
            throw e;
        }
    }

    // Overload to accept bytes (e.g., PDF/DOC uploads) and extract text via Apache Tika
    public ResumeData parseResume(byte[] fileBytes, AssessmentSession session) {
        log.info("Extracting text from uploaded file for session {}", session.getId());
        String extracted = extractTextWithTika(fileBytes);
        log.info("Extracted text length: {}", extracted != null ? extracted.length() : 0);
        return parseResume(extracted != null ? extracted : "", session);
    }

    private String extractTextWithTika(byte[] bytes) {
        // Use reflection to avoid hard dependency errors if Tika is not on classpath during lint
        try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
            Class<?> parserCls = Class.forName("org.apache.tika.parser.AutoDetectParser");
            Class<?> metadataCls = Class.forName("org.apache.tika.metadata.Metadata");
            Class<?> handlerCls = Class.forName("org.apache.tika.sax.BodyContentHandler");

            Object parser = parserCls.getDeclaredConstructor().newInstance();
            Object metadata = metadataCls.getDeclaredConstructor().newInstance();
            Object handler = handlerCls.getDeclaredConstructor(int.class).newInstance(-1);

            parserCls.getMethod("parse", InputStream.class, Class.forName("org.xml.sax.ContentHandler"), metadataCls)
                    .invoke(parser, is, handler, metadata);

            String extractedText = (String) handlerCls.getMethod("toString").invoke(handler);
            if (extractedText.trim().isEmpty()) {
                log.warn("Tika extracted empty text, falling back to raw bytes as string");
                return new String(bytes);
            }
            // Normalize and return
            return preprocessText(extractedText.trim());
        } catch (Throwable e) {
            log.warn("Tika extraction unavailable, falling back to raw bytes as string");
            try { return preprocessText(new String(bytes)); } catch (Exception ignored) { return ""; }
        }
    }

    private String convertSkillsToJson(List<ExtractedSkill> skills) {
        try {
            return objectMapper.writeValueAsString(skills);
        } catch (Exception e) {
            log.error("Error converting skills to JSON", e);
            return "[]";
        }
    }

    private String convertListToJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("Error converting list to JSON", e);
            return "[]";
        }
    }

    private String convertEntitiesToJson(List<Map<String, Object>> entities) {
        try {
            return objectMapper.writeValueAsString(entities);
        } catch (Exception e) {
            log.error("Error converting entities to JSON", e);
            return "[]";
        }
    }

    private String preprocessText(String text) {
        if (text == null) {
            return "";
        }

        // Normalize Unicode to reduce weird symbols/ligatures from PDFs
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKC);

        // Standardize newlines first
        normalized = normalized.replace("\r\n", "\n").replace('\r', '\n');

        // Remove control characters except tab/newline
        normalized = normalized.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]", "");

        // Remove soft hyphens and fix common ligatures/quotes
        normalized = normalized
            .replace("\u00AD", "")           // soft hyphen
            .replace("ﬁ", "fi")
            .replace("ﬂ", "fl")
            .replace("“", "\"")
            .replace("”", "\"")
            .replace("‘", "'")
            .replace("’", "'")
            .replace("\u00A0", " ");        // nbsp

        // De-hyphenate words broken across line breaks (e.g., exper-
        // ience -> experience)
        normalized = normalized.replaceAll("([A-Za-z])-(?:\n|\r?\n)\\s*([A-Za-z])", "$1$2");

        // Collapse excessive blank lines to at most one empty line between paragraphs
        normalized = normalized.replaceAll("\n{3,}", "\n\n");

        // Trim trailing spaces on each line and collapse internal multiple spaces
        StringBuilder cleaned = new StringBuilder();
        for (String line : normalized.split("\n", -1)) {
            String trimmedRight = line.replaceAll("[\\t ]+$", "");
            // keep single spaces but collapse large runs; preserve tabs
            trimmedRight = trimmedRight.replaceAll(" {2,}", " ");
            cleaned.append(trimmedRight).append('\n');
        }

        String result = cleaned.toString().trim();

        // Ensure bullet markers are spaced consistently
        result = result.replaceAll("(\n)[•·●◦](\\s*)", "$1• ");

        return result;
    }

    private Map<String, String> extractSections(String text) {
        Map<String, String> sections = new HashMap<>();

        // Enhanced section extraction with multiple variations
        Map<String, String[]> sectionPatterns = new HashMap<>();
        sectionPatterns.put("experience", new String[]{
            "experience", "work experience", "professional experience", "employment",
            "work history", "career history", "job experience"
        });
        sectionPatterns.put("education", new String[]{
            "education", "academic background", "educational background", "qualifications",
            "academic qualifications", "degrees", "certifications"
        });
        sectionPatterns.put("skills", new String[]{
            "skills", "technical skills", "core competencies", "competencies",
            "expertise", "technologies", "tools"
        });
        sectionPatterns.put("projects", new String[]{
            "projects", "project experience", "key projects", "notable projects"
        });

        for (Map.Entry<String, String[]> entry : sectionPatterns.entrySet()) {
            String sectionKey = entry.getKey();
            String[] headers = entry.getValue();

            for (String header : headers) {
                Pattern pattern = Pattern.compile(
                    "(?i)" + Pattern.quote(header) + "[^\\n]*\\n(.*?)(\\n[A-Z][^\\n]*:|$|\\n\\n|$)",
                    Pattern.DOTALL
                );
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String content = matcher.group(1).trim();
                    if (!content.isEmpty() && content.length() > 10) { // Minimum content length
                        sections.put(sectionKey, content);
                        break; // Use first match found
                    }
                }
            }
        }

        // If no sections found, try to extract from the entire text
        if (sections.isEmpty()) {
            log.warn("No sections found in resume, using entire text for skill extraction");
            sections.put("experience", text);
            sections.put("skills", text);
        }

        return sections;
    }

    private List<ExtractedSkill> extractSkills(Map<String, String> sections) {
        List<ExtractedSkill> extracted = new ArrayList<>();

        // Combine skills and experience sections for extraction
        String combinedText = sections.getOrDefault("skills", "") + " " +
                             sections.getOrDefault("experience", "") + " " +
                             sections.getOrDefault("projects", "");

        // Convert to lowercase for case-insensitive matching
        String lowerText = combinedText.toLowerCase();

        // Get all skills from database
        skillRepository.findAll().forEach(skill -> {
            try {
                if (skill.getKeywords() == null) {
                    log.debug("Skipping skill {} due to null keywords", skill.getSkillCode());
                    return; // Skip this skill
                }
                List<String> keywords = objectMapper.readValue(skill.getKeywords(), new TypeReference<List<String>>() {});
                double maxConfidence = 0.0;
                int maxYears = 0;
                // Track best match per skill (confidence and years)

                for (String keyword : keywords) {
                    String lowerKeyword = keyword.toLowerCase();

                    // Check for exact match first
                    if (lowerText.contains(lowerKeyword)) {
                        int years = extractYearsForSkill(combinedText, keyword);
                        double confidence = calculateConfidence(keyword, combinedText);

                        if (confidence > maxConfidence) {
                            maxConfidence = confidence;
                            maxYears = years;
                            // best match updated
                        }
                    }
                    // Check for partial matches (e.g., "spring" matches "spring boot")
                    else {
                        // Split keyword into words and check if all words are present
                        String[] keywordWords = lowerKeyword.split("\\s+");
                        boolean allWordsPresent = true;
                        for (String word : keywordWords) {
                            if (word.length() > 2 && !lowerText.contains(word)) { // Ignore very short words
                                allWordsPresent = false;
                                break;
                            }
                        }

                        if (allWordsPresent && keywordWords.length > 1) {
                            int years = extractYearsForSkill(combinedText, keyword);
                            double confidence = calculateConfidence(keyword, combinedText) * 0.8; // Slightly lower confidence for partial matches

                            if (confidence > maxConfidence) {
                                maxConfidence = confidence;
                                maxYears = years;
                                // best match updated
                            }
                        }
                    }
                }

                // Add skill if we found a match with sufficient confidence
                if (maxConfidence > 0.1) { // Minimum confidence threshold
                    extracted.add(ExtractedSkill.builder()
                        .skillCode(skill.getSkillCode())
                        .skillName(skill.getDisplayName())
                        .yearsExperience(maxYears)
                        .confidence(maxConfidence)
                        .build());

                    log.debug("Extracted skill: {} with confidence: {} and years: {}",
                             skill.getDisplayName(), maxConfidence, maxYears);
                }

            } catch (Exception e) {
                log.error("Error parsing keywords JSON for skill: " + skill.getSkillCode(), e);
            }
        });

        // Sort by confidence descending
        extracted.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));

        // Limit to top skills to avoid too many false positives
        return extracted.size() > 20 ? extracted.subList(0, 20) : extracted;
    }

    private int extractYearsForSkill(String text, String skillKeyword) {
        // Look for patterns like "Java (3 years)", "3 years Java", etc.
        String context = extractContextAroundSkill(text, skillKeyword, 50);

        Matcher matcher = YEARS_PATTERN.matcher(context);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return 0;
    }

    private String extractContextAroundSkill(String text, String skill, int contextSize) {
        Pattern pattern = Pattern.compile(
            "(?i).{0," + contextSize + "}\\b" + Pattern.quote(skill) + "\\b.{0," + contextSize + "}"
        );
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group() : "";
    }

    private double calculateConfidence(String keyword, String text) {
        // Simple confidence based on keyword frequency and context
        long occurrences = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b", Pattern.CASE_INSENSITIVE)
            .matcher(text).results().count();

        return Math.min(1.0, occurrences * 0.3);
    }

    private List<String> extractEducation(String educationSection) {
        List<String> degrees = new ArrayList<>();

        String[] degreePatterns = {
            "Bachelor.*?Science", "Bachelor.*?Arts", "Bachelor.*?Engineering",
            "Master.*?Science", "Master.*?Arts", "Master.*?Engineering",
            "PhD", "B\\.S\\.", "M\\.S\\.", "B\\.E\\.", "M\\.E\\."
        };

        for (String pattern : degreePatterns) {
            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(educationSection);
            while (m.find()) {
                degrees.add(m.group());
            }
        }

        return degrees;
    }

    private int calculateTotalExperience(String experienceSection) {
        Matcher matcher = YEARS_PATTERN.matcher(experienceSection);
        int maxYears = 0;

        while (matcher.find()) {
            maxYears = Math.max(maxYears, Integer.parseInt(matcher.group(1)));
        }

        return maxYears;
    }
}
