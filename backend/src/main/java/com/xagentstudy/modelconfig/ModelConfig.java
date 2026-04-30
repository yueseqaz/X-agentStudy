package com.xagentstudy.modelconfig;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "model_configs")
public class ModelConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String provider;
    private String modelName;
    private String baseUrl;
    private String apiKeyMask;
    private String apiKeyCipher;
    private Boolean enabled;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    protected ModelConfig() {
    }

    public ModelConfig(Long userId, String provider, String modelName, String baseUrl, String apiKeyMask, String apiKeyCipher, Boolean enabled) {
        this.userId = userId;
        this.provider = provider;
        this.modelName = modelName;
        this.baseUrl = baseUrl;
        this.apiKeyMask = apiKeyMask;
        this.apiKeyCipher = apiKeyCipher;
        this.enabled = enabled;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getProvider() {
        return provider;
    }

    public String getModelName() {
        return modelName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKeyMask() {
        return apiKeyMask;
    }

    public String getApiKeyCipher() {
        return apiKeyCipher;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(String provider, String modelName, String baseUrl, String apiKeyMask, String apiKeyCipher) {
        this.provider = provider;
        this.modelName = modelName;
        this.baseUrl = baseUrl;
        if (apiKeyMask != null) {
            this.apiKeyMask = apiKeyMask;
        }
        if (apiKeyCipher != null) {
            this.apiKeyCipher = apiKeyCipher;
        }
        this.updatedAt = OffsetDateTime.now();
    }
}
