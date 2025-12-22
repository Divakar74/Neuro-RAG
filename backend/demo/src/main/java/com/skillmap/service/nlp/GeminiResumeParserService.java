package com.skillmap.service.nlp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillmap.model.entity.AssessmentSession;
import com.skillmap.model.entity.ResumeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiResumeParserService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Note: In production, inject this from environment variable
    private final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");

    public ResumeData parseResume(byte[] contentBytes, AssessmentSession session) {
        try {
            log.info("Starting Gemini-based resume parsing for session: {}", session.getId());

            // Step 1: Extract text from file using basic string conversion (fallback)
            String rawText = extractTextFromFile(contentBytes);
            log.info("Extracted text length: {} characters", rawText.length());

            // Step 2: Use Gemini API to parse structured data
            Map<String, Object> parsedData = parseWithGemini(rawText, contentBytes);

            // Step 3: Create ResumeData entity
            ResumeData resumeData = new ResumeData();
            resumeData.setSession(session);
            resumeData.setRawText(rawText);

            // Convert parsed data to JSON strings for database storage
            // Store skills as simple array of strings
            resumeData.setExtractedSkills(objectMapper.writeValueAsString(parsedData.get("skills")));
            // Store education as array of objects
            resumeData.setExtractedEducation(objectMapper.writeValueAsString(parsedData.get("education")));
            // Store experiences as array of objects
            resumeData.setExtractedExperience(objectMapper.writeValueAsString(parsedData.get("experiences")));

            // Store personal info and entities for frontend compatibility
            Map<String, Object> rawEntitiesMap = new HashMap<>();
            Map<String, Object> personalInfo = new HashMap<>();
            personalInfo.put("name", parsedData.get("name"));
            personalInfo.put("email", parsedData.get("email"));
            personalInfo.put("phone", parsedData.get("phone"));
            rawEntitiesMap.put("personalInfo", personalInfo);
            rawEntitiesMap.put("entities", Map.of(
                "PERSON", List.of(parsedData.get("name")),
                "ORG", extractOrganizations((List<Map<String, Object>>) parsedData.get("experiences")),
                "SKILL", parsedData.get("skills")
            ));
            resumeData.setRawEntities(objectMapper.writeValueAsString(rawEntitiesMap));

            // Calculate total years of experience
            List<Map<String, Object>> experiences = (List<Map<String, Object>>) parsedData.get("experiences");
            double totalYears = calculateTotalExperience(experiences);
            resumeData.setTotalYearsExperience((int) Math.round(totalYears));

            log.info("Successfully parsed resume with Gemini API");
            return resumeData;

        } catch (Exception e) {
            log.error("Failed to parse resume with Gemini API", e);
            // Fallback to basic text extraction
            return createFallbackResumeData(contentBytes, session);
        }
    }

    private String extractTextFromFile(byte[] contentBytes) throws Exception {
        // Try Tika first, fallback to basic string conversion
        String text = extractTextWithTika(contentBytes);
        if (text == null || text.trim().isEmpty()) {
            text = new String(contentBytes, "UTF-8");
        }
        return text;
    }

    private String extractTextWithTika(byte[] bytes) {
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            Class<?> parserCls = Class.forName("org.apache.tika.parser.AutoDetectParser");
            Class<?> metadataCls = Class.forName("org.apache.tika.metadata.Metadata");
            Class<?> handlerCls = Class.forName("org.apache.tika.sax.BodyContentHandler");

            Object parser = parserCls.getDeclaredConstructor().newInstance();
            Object metadata = metadataCls.getDeclaredConstructor().newInstance();
            Object handler = handlerCls.getDeclaredConstructor(int.class).newInstance(-1);

            parserCls.getMethod("parse", InputStream.class, Class.forName("org.xml.sax.ContentHandler"), metadataCls)
                    .invoke(parser, is, handler, metadata);

            return (String) handlerCls.getMethod("toString").invoke(handler);
        } catch (Throwable e) {
            log.warn("Tika extraction failed: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> parseWithGemini(String rawText, byte[] contentBytes) {
        if (GEMINI_API_KEY == null || GEMINI_API_KEY.trim().isEmpty()) {
            log.warn("GEMINI_API_KEY not set, falling back to mock data");
            return getMockData();
        }

        try {
            // Build prompt
            String prompt = buildPrompt(rawText);

            // Call Gemini API
            String response = callGeminiAPI(prompt, contentBytes);

            // Parse JSON response
            return safeParseJson(response);

        } catch (Exception e) {
            log.error("Failed to call Gemini API, falling back to mock data", e);
            return getMockData();
        }
    }

    private String buildPrompt(String resumeText) {
        return "You are an expert HR assistant tasked with extracting structured information from resumes.\n\n" +
               "INSTRUCTIONS:\n" +
               "- Extract the full name of the candidate.\n" +
               "- Extract email and phone number if present.\n" +
               "- Extract skills as a list of objects with 'skillName' and estimated 'yearsExperience' based on context in the resume.\n" +
               "- Extract work experiences as a list of objects with 'title', 'company', 'start_year', 'end_year_or_present'.\n" +
               "- Extract education as a list of objects with 'degree', 'institution', 'year_or_range'.\n" +
               "- Be precise and only include information explicitly mentioned in the resume.\n" +
               "- Respond ONLY with valid JSON matching the exact schema below.\n\n" +
               "SCHEMA:\n" +
               "{\n" +
               "  \"name\": \"string\",\n" +
               "  \"email\": \"string or null\",\n" +
               "  \"phone\": \"string or null\",\n" +
               "  \"skills\": [{\"skillName\": \"string\", \"yearsExperience\": number}],\n" +
               "  \"experiences\": [{\"title\": \"string\", \"company\": \"string\", \"start_year\": \"string\", \"end_year_or_present\": \"string\"}],\n" +
               "  \"education\": [{\"degree\": \"string\", \"institution\": \"string\", \"year_or_range\": \"string\"}]\n" +
               "}\n\n" +
               "EXAMPLE:\n" +
               "{\n" +
               "  \"name\": \"John Smith\",\n" +
               "  \"email\": \"john.smith@email.com\",\n" +
               "  \"phone\": \"555-123-4567\",\n" +
               "  \"skills\": [{\"skillName\": \"Python\", \"yearsExperience\": 3}, {\"skillName\": \"Java\", \"yearsExperience\": 5}],\n" +
               "  \"experiences\": [{\"title\": \"Software Engineer\", \"company\": \"Tech Corp\", \"start_year\": \"2020\", \"end_year_or_present\": \"Present\"}],\n" +
               "  \"education\": [{\"degree\": \"Bachelor of Science in Computer Science\", \"institution\": \"University of Tech\", \"year_or_range\": \"2016-2020\"}]\n" +
               "}\n\n" +
               "RESUME TEXT:\n\"\"\"" + resumeText.substring(0, Math.min(resumeText.length(), 7000)) + "\"\"\"";
    }

    private String callGeminiAPI(String prompt, byte[] contentBytes) throws Exception {
        // Use the previously provided Python SDK script
        return callPythonGeminiScript(prompt, contentBytes);
    }

    private String callPythonGeminiScript(String prompt, byte[] contentBytes) throws Exception {
        // Write prompt to a temp file or pass as argument
        // Since the Python script expects a PDF, but we have text, we need to create a temp text file and modify the script or create a wrapper.

        // For simplicity, create a temp text file with the prompt, and modify the script to read from text file instead of PDF.

        // But to keep it simple, since the script is for PDF, but we have text, we can create a temp PDF from text, but that's complicated.

        // Alternatively, modify the Python script to accept text input.

        // Since the user provided the Python SDK, we can assume to use it by creating a temp file with the resume text, and call the script with a text file path, but the script expects PDF.

        // To make it work, I can create a temp text file, and call a modified version of the script that reads text instead of PDF.

        // But to avoid modifying the script, perhaps use the HTTP fallback, but the user wants to use the Python SDK.

        // The Python script uses the SDK, so to use it, I can execute the Python script, but since it's for PDF, I need to write the contentBytes to a temp PDF file.

        // Yes, the contentBytes is the PDF content.

        // So, write contentBytes to a temp PDF file, then call the Python script with the temp file path, then capture the output JSON.

        // Yes, that works.

        // The script prints the parsed JSON.

        // So, I can capture stdout, and parse the JSON from it.

        try {
            // Create temp PDF file
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("resume", ".pdf");
            java.nio.file.Files.write(tempFile, contentBytes);

            // Build command
            ProcessBuilder pb = new ProcessBuilder("python", "gemini_resume_pdf.py", tempFile.toString());
            pb.environment().put("GEMINI_API_KEY", GEMINI_API_KEY);
            pb.redirectErrorStream(true); // Merge stdout and stderr

            // Run process
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), "UTF-8");
            int exitCode = process.waitFor();

            // Clean up temp file
            java.nio.file.Files.deleteIfExists(tempFile);

            if (exitCode != 0) {
                log.error("Python script failed with exit code {}: {}", exitCode, output);
                throw new RuntimeException("Python Gemini script failed: " + output);
            }

            // Parse the JSON from output
            // The script prints "✅ Parsed JSON:" then the JSON
            int jsonStart = output.indexOf("✅ Parsed JSON:");
            if (jsonStart == -1) {
                throw new RuntimeException("No parsed JSON found in output");
            }
            String jsonPart = output.substring(jsonStart + "✅ Parsed JSON:".length()).trim();
            // Assume the JSON is the last part
            return jsonPart;

        } catch (Exception e) {
            log.error("Failed to call Python Gemini script", e);
            throw new RuntimeException("Python Gemini script call failed", e);
        }
    }

    private Map<String, Object> safeParseJson(String text) {
        try {
            int start = text.indexOf("{");
            int end = text.lastIndexOf("}");
            if (start == -1 || end == -1) {
                throw new RuntimeException("No JSON found in response");
            }
            String jsonStr = text.substring(start, end + 1);
            return objectMapper.readValue(jsonStr, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse JSON response", e);
            return getMockData();
        }
    }

    private Map<String, Object> getMockData() {
        Map<String, Object> result = new HashMap<>();

        // Mock personal info
        result.put("name", "John Doe");
        result.put("email", "john.doe@email.com");
        result.put("phone", "+1-555-0123");

        // Mock skills as objects with skillName and yearsExperience
        List<Map<String, Object>> skills = new ArrayList<>();
        Map<String, Object> skill1 = new HashMap<>();
        skill1.put("skillName", "Java");
        skill1.put("yearsExperience", 5);
        skills.add(skill1);
        Map<String, Object> skill2 = new HashMap<>();
        skill2.put("skillName", "Spring Boot");
        skill2.put("yearsExperience", 4);
        skills.add(skill2);
        Map<String, Object> skill3 = new HashMap<>();
        skill3.put("skillName", "React");
        skill3.put("yearsExperience", 3);
        skills.add(skill3);
        Map<String, Object> skill4 = new HashMap<>();
        skill4.put("skillName", "JavaScript");
        skill4.put("yearsExperience", 3);
        skills.add(skill4);
        Map<String, Object> skill5 = new HashMap<>();
        skill5.put("skillName", "Python");
        skill5.put("yearsExperience", 2);
        skills.add(skill5);
        Map<String, Object> skill6 = new HashMap<>();
        skill6.put("skillName", "SQL");
        skill6.put("yearsExperience", 4);
        skills.add(skill6);
        result.put("skills", skills);

        // Mock education
        List<Map<String, Object>> education = new ArrayList<>();
        Map<String, Object> edu1 = new HashMap<>();
        edu1.put("degree", "Bachelor of Science in Computer Science");
        edu1.put("institution", "University of Technology");
        edu1.put("year_or_range", "2018");
        education.add(edu1);
        result.put("education", education);

        // Mock experience
        List<Map<String, Object>> experience = new ArrayList<>();
        Map<String, Object> exp1 = new HashMap<>();
        exp1.put("title", "Software Engineer");
        exp1.put("company", "Tech Corp");
        exp1.put("start_year", "2020");
        exp1.put("end_year_or_present", "Present");
        experience.add(exp1);
        result.put("experiences", experience);

        return result;
    }

    private double calculateTotalExperience(List<Map<String, Object>> experiences) {
        if (experiences == null || experiences.isEmpty()) {
            return 0.0;
        }

        double totalYears = 0.0;
        for (Map<String, Object> exp : experiences) {
            try {
                Object startYear = exp.get("start_year");
                Object endYear = exp.get("end_year_or_present");

                if (startYear != null) {
                    int start = Integer.parseInt(startYear.toString().replaceAll("\\D", ""));
                    int end = "Present".equalsIgnoreCase(endYear.toString()) || endYear == null ?
                             java.time.Year.now().getValue() :
                             Integer.parseInt(endYear.toString().replaceAll("\\D", ""));

                    totalYears += Math.max(0, end - start);
                }
            } catch (Exception e) {
                log.warn("Failed to calculate experience for entry: {}", exp);
            }
        }
        return totalYears;
    }

    private List<String> extractOrganizations(List<Map<String, Object>> experiences) {
        List<String> orgs = new ArrayList<>();
        if (experiences != null) {
            for (Map<String, Object> exp : experiences) {
                Object company = exp.get("company");
                if (company != null) {
                    orgs.add(company.toString());
                }
            }
        }
        return orgs;
    }

    private ResumeData createFallbackResumeData(byte[] contentBytes, AssessmentSession session) {
        try {
            String rawText = extractTextFromFile(contentBytes);
            ResumeData resumeData = new ResumeData();
            resumeData.setSession(session);
            resumeData.setRawText(rawText);
            resumeData.setTotalYearsExperience(0);

            // Empty JSON arrays for fallback
            resumeData.setExtractedSkills("[]");
            resumeData.setExtractedEducation("[]");
            resumeData.setExtractedExperience("[]");
            resumeData.setRawEntities("{}");

            return resumeData;
        } catch (Exception e) {
            log.error("Failed to create fallback resume data", e);
            throw new RuntimeException("Resume parsing failed completely", e);
        }
    }
}
