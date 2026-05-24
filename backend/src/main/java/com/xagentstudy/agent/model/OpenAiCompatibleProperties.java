package com.xagentstudy.agent.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.openai-compatible")
public record OpenAiCompatibleProperties(
        String apiKey,
        String baseUrl,
        String model
) {
    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
