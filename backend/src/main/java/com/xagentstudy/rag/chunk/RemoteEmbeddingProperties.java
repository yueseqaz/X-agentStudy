package com.xagentstudy.rag.chunk;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.embedding")
public record RemoteEmbeddingProperties(
        String apiKey,
        String baseUrl,
        String model
) {
    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String safeBaseUrl() {
        return baseUrl == null || baseUrl.isBlank() ? "https://api.openai.com/v1" : baseUrl;
    }

    public String safeModel() {
        return model == null || model.isBlank() ? "text-embedding-3-small" : model;
    }
}
