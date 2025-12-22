package com.skillmap.service.nlp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Calls Hugging Face Inference API for token classification using
 * model "yashpwr/resume-ner-bert-v2" to extract resume entities.
 */
@Service
@Slf4j
public class HuggingFaceNERService {

@Value("${huggingface.api.key:}")
    private String hfApiKey;

    @Value("${resume.ner.hf.enabled:false}")
    private boolean hfEnabled;

    @Value("${resume.ner.local.url:}")
    private String localUrl;

    private static final String MODEL = "yashpwr/resume-ner-bert-v2";
    private static final String ENDPOINT = "https://api-inference.huggingface.co/models/" + MODEL;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    /**
     * Extracts entities using HF Inference API. Returns a list of maps with keys: word, entity_group, score, start, end
     */
public List<Map<String, Object>> extractEntities(String text) {
        if (text == null) text = "";
        // Truncate text to avoid API limits (HF has token limits, approx 5000 chars for safety)
        if (text.length() > 5000) {
            text = text.substring(0, 5000);
            log.debug("Truncated text to 5000 chars for HF NER");
        }

        // Prefer local NER server if configured
        if (localUrl != null && !localUrl.isBlank()) {
            try {
                String payload = new com.fasterxml.jackson.databind.ObjectMapper()
                    .createObjectNode().put("text", text)
                    .toString();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(localUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return parseEntitiesJson(response.body());
                } else {
                    log.warn("Local NER returned status {}: {}", response.statusCode(), response.body());
                    return Collections.emptyList();
                }
            } catch (Exception e) {
                log.warn("Local NER call failed", e);
                return Collections.emptyList();
            }
        }

        // Fallback to HF cloud if enabled
        if (!hfEnabled || hfApiKey == null || hfApiKey.isBlank()) {
            log.debug("HF NER disabled or API key missing; skipping HF extraction");
            return Collections.emptyList();
        }
        try {
            String payload = "{\"inputs\": " + toJsonString(text) + ", \"options\": {\"wait_for_model\": true}}";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + hfApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseEntitiesJson(response.body());
            } else {
                log.warn("HF NER API returned status {}: {}", response.statusCode(), response.body());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.warn("HF NER extraction failed", e);
            return Collections.emptyList();
        }
    }

    // Minimal JSON string escaper
    private String toJsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    // Very lightweight JSON parser for expected HF output (list of list of entity maps)
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseEntitiesJson(String json) {
        // We avoid adding a JSON lib; use a tiny parser via java.util for this controlled structure
        // Format can be either a list or list-of-lists depending on pipeline; normalize to flat list
        try {
            Object parsed = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Object.class);
            List<Map<String, Object>> flat = new ArrayList<>();
            if (parsed instanceof List<?> root) {
                for (Object item : root) {
                    if (item instanceof List<?> inner) {
                        for (Object m : inner) if (m instanceof Map) flat.add((Map<String, Object>) m);
                    } else if (item instanceof Map) {
                        flat.add((Map<String, Object>) item);
                    }
                }
            }
            return flat;
        } catch (Exception e) {
            log.warn("Failed to parse HF JSON", e);
            return Collections.emptyList();
        }
    }

    /**
     * Derives simple sections from entities. Returns map with keys: skills, education, experience
     */
    public Map<String, List<String>> deriveSections(List<Map<String, Object>> entities) {
        Map<String, List<String>> sections = new HashMap<>();
        sections.put("skills", new ArrayList<>());
        sections.put("education", new ArrayList<>());
        sections.put("experience", new ArrayList<>());

        for (Map<String, Object> e : entities) {
            String group = String.valueOf(e.getOrDefault("entity_group", "")).toUpperCase(Locale.ROOT);
            String word = String.valueOf(e.getOrDefault("word", "")).replace("##", "").trim();
            if (word.isBlank()) continue;
            switch (group) {
                case "SKILL":
                case "TECH":
                case "TOOLS":
                    addUnique(sections.get("skills"), word);
                    break;
                case "EDUCATION":
                case "DEGREE":
                case "CERTIFICATION":
                    addUnique(sections.get("education"), word);
                    break;
                case "ORG":
                case "COMPANY":
                case "DESIGNATION":
                case "TITLE":
                    addUnique(sections.get("experience"), word);
                    break;
                default:
                    // ignore others like NAME/EMAIL/PHONE
                    break;
            }
        }
        // limit sizes for UI
        sections.replace("skills", sections.get("skills").stream().distinct().limit(50).toList());
        sections.replace("education", sections.get("education").stream().distinct().limit(20).toList());
        sections.replace("experience", sections.get("experience").stream().distinct().limit(40).toList());
        return sections;
    }

    private void addUnique(List<String> list, String value) {
        if (!list.contains(value)) list.add(value);
    }
}




