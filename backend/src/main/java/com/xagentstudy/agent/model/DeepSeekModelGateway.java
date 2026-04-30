package com.xagentstudy.agent.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.common.security.SecretCipherService;
import com.xagentstudy.modelconfig.ModelConfigRepository;
import com.xagentstudy.user.AppUser;
import com.xagentstudy.user.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DeepSeekModelGateway implements ModelGateway {
    private static final Logger log = LoggerFactory.getLogger(DeepSeekModelGateway.class);

    private final DeepSeekProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;
    private final ModelConfigRepository modelConfigRepository;
    private final SecretCipherService cipherService;
    private final AppUserRepository userRepository;

    public DeepSeekModelGateway(
            DeepSeekProperties properties,
            ObjectMapper objectMapper,
            RestClient.Builder builder,
            ModelConfigRepository modelConfigRepository,
            SecretCipherService cipherService,
            AppUserRepository userRepository
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClientBuilder = builder;
        this.modelConfigRepository = modelConfigRepository;
        this.cipherService = cipherService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean hasConfiguredModel() {
        return runtimeModel().configured();
    }

    @Override
    public Optional<String> generateJson(String systemPrompt, String userPrompt) {
        return generate(systemPrompt, userPrompt, true);
    }

    @Override
    public Optional<String> generateText(String systemPrompt, String userPrompt) {
        return generate(systemPrompt, userPrompt, false);
    }

    private Optional<String> generate(String systemPrompt, String userPrompt, boolean jsonMode) {
        RuntimeModel runtimeModel = runtimeModel();
        if (!runtimeModel.configured()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("model", runtimeModel.model());
            body.put("temperature", 0.2);
            if (jsonMode) {
                body.put("response_format", Map.of("type", "json_object"));
            }
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .build();
            String endpoint = runtimeModel.baseUrl().replaceAll("/+$", "") + "/chat/completions";
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + runtimeModel.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = client
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .orTimeout(35, java.util.concurrent.TimeUnit.SECONDS)
                    .join();
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null || response.body().isBlank()) {
                log.warn("DeepSeek generation returned non-success status: {}", response.statusCode());
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(content.asText());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("DeepSeek generation failed, falling back to local generator: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private RuntimeModel runtimeModel() {
        try {
            List<Long> adminUserIds = userRepository.findAll()
                    .stream()
                    .filter(user -> "ADMIN".equalsIgnoreCase(user.getRole()))
                    .map(AppUser::getId)
                    .toList();
            if (adminUserIds.isEmpty()) {
                return new RuntimeModel(properties.baseUrl(), properties.model(), properties.apiKey());
            }
            return modelConfigRepository.findFirstByUserIdInAndEnabledTrueOrderByUpdatedAtDesc(adminUserIds)
                    .map(config -> new RuntimeModel(
                            blankToDefault(config.getBaseUrl(), properties.baseUrl()),
                            blankToDefault(config.getModelName(), properties.model()),
                            cipherService.decrypt(config.getApiKeyCipher())
                    ))
                    .filter(RuntimeModel::configured)
                    .orElseGet(() -> new RuntimeModel(properties.baseUrl(), properties.model(), properties.apiKey()));
        } catch (Exception ignored) {
            return new RuntimeModel(properties.baseUrl(), properties.model(), properties.apiKey());
        }
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record RuntimeModel(String baseUrl, String model, String apiKey) {
        boolean configured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
