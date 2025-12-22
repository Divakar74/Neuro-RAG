package com.skillmap.service.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class EmbeddingService {

    private final String openAiApiKey;
    private final String huggingFaceApiKey;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public EmbeddingService(
            @Value("${openai.api.key:}") String openAiApiKey,
            @Value("${huggingface.api.key:}") String huggingFaceApiKey
    ) {
        this.openAiApiKey = openAiApiKey == null ? "" : openAiApiKey.trim();
        this.huggingFaceApiKey = huggingFaceApiKey == null ? "" : huggingFaceApiKey.trim();
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Compute cosine similarity between two texts using available embedding providers.
     * Returns null if remote providers are unavailable; caller should fallback.
     */
    public Double computeCosineSimilarity(String a, String b) {
        try {
            double[] ea = embed(a);
            double[] eb = embed(b);
            if (ea == null || eb == null || ea.length != eb.length) return null;
            double dot = 0.0, na = 0.0, nb = 0.0;
            for (int i = 0; i < ea.length; i++) {
                dot += ea[i] * eb[i];
                na += ea[i] * ea[i];
                nb += eb[i] * eb[i];
            }
            double denom = Math.sqrt(na) * Math.sqrt(nb) + 1e-9;
            double sim = dot / denom;
            // clamp to [0,1]
            return Math.max(0.0, Math.min(1.0, sim));
        } catch (Exception e) {
            log.warn("Cosine similarity failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Try OpenAI first, then Hugging Face. Return null if neither available.
     */
    public double[] embed(String text) {
        if (text == null || text.isBlank()) return null;
        // Prefer OpenAI embeddings if key present
        if (!openAiApiKey.isEmpty()) {
            try {
                double[] v = embedOpenAI(text);
                if (v != null) return v;
            } catch (Exception e) {
                log.warn("OpenAI embedding failed: {}", e.getMessage());
            }
        }
        // Fallback to Hugging Face feature extraction
        if (!huggingFaceApiKey.isEmpty()) {
            try {
                double[] v = embedHuggingFace(text);
                if (v != null) return v;
            } catch (Exception e) {
                log.warn("HF embedding failed: {}", e.getMessage());
            }
        }
        return null;
    }

    private double[] embedOpenAI(String text) throws Exception {
        // OpenAI embeddings endpoint
        String body = mapper.createObjectNode()
                .put("model", "text-embedding-3-small")
                .putArray("input").add(text)
                .toString();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/embeddings"))
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            JsonNode root = mapper.readTree(resp.body());
            JsonNode arr = root.path("data").get(0).path("embedding");
            if (arr.isArray()) {
                double[] vec = new double[arr.size()];
                for (int i = 0; i < arr.size(); i++) vec[i] = arr.get(i).asDouble();
                return l2Normalize(vec);
            }
        } else {
            log.warn("OpenAI embeddings HTTP {}: {}", resp.statusCode(), resp.body());
        }
        return null;
    }

    private double[] embedHuggingFace(String text) throws Exception {
        // Use sentence-transformers/all-MiniLM-L6-v2 via HF inference API (feature-extraction)
        String model = "sentence-transformers/all-MiniLM-L6-v2";
        String url = "https://api-inference.huggingface.co/pipeline/feature-extraction/" + model;
        String body = mapper.createObjectNode().put("inputs", text).toString();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + huggingFaceApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            // HF returns nested arrays [[...]]
            JsonNode root = mapper.readTree(resp.body());
            JsonNode arr = root.isArray() && root.get(0).isArray() ? root.get(0) : root;
            List<Double> vals = new ArrayList<>();
            for (JsonNode n : arr) vals.add(n.asDouble());
            double[] vec = new double[vals.size()];
            for (int i = 0; i < vals.size(); i++) vec[i] = vals.get(i);
            return l2Normalize(vec);
        } else {
            log.warn("HF embeddings HTTP {}: {}", resp.statusCode(), resp.body());
        }
        return null;
    }

    private static double[] l2Normalize(double[] v) {
        double n = 0.0;
        for (double x : v) n += x * x;
        n = Math.sqrt(n) + 1e-9;
        for (int i = 0; i < v.length; i++) v[i] /= n;
        return v;
    }
}
