package com.xagentstudy.rag.chunk;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Primary
public class RemoteFirstEmbeddingProvider implements EmbeddingProvider {
    private final RemoteEmbeddingProperties properties;
    private final LocalEmbeddingService fallback;
    private final RestClient restClient;

    public RemoteFirstEmbeddingProvider(RemoteEmbeddingProperties properties, LocalEmbeddingService fallback) {
        this.properties = properties;
        this.fallback = fallback;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public List<Double> embed(String text) {
        if (!properties.enabled()) {
            return fallback.embed(text);
        }
        try {
            JsonNode response = restClient.post()
                    .uri(properties.safeBaseUrl() + "/embeddings")
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .body(Map.of(
                            "model", properties.safeModel(),
                            "input", text == null ? "" : text
                    ))
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode embedding = response == null ? null : response.path("data").path(0).path("embedding");
            if (embedding == null || !embedding.isArray()) {
                return fallback.embed(text);
            }
            List<Double> vector = new ArrayList<>(embedding.size());
            embedding.forEach(value -> vector.add(value.asDouble()));
            return normalize(vector);
        } catch (Exception ignored) {
            return fallback.embed(text);
        }
    }

    private List<Double> normalize(List<Double> vector) {
        double norm = 0.0d;
        for (double value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        if (norm == 0.0d) {
            return vector;
        }
        List<Double> result = new ArrayList<>(vector.size());
        for (double value : vector) {
            result.add(value / norm);
        }
        return result;
    }
}
