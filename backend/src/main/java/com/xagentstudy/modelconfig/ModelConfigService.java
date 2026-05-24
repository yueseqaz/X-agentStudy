package com.xagentstudy.modelconfig;

import com.xagentstudy.agent.model.SpringAiModelGateway;
import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.common.security.SecretCipherService;
import com.xagentstudy.user.AppUser;
import com.xagentstudy.user.AppUserRepository;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ModelConfigService {
    private final ModelConfigRepository repository;
    private final SecretCipherService cipherService;
    private final AppUserRepository userRepository;
    private final SpringAiModelGateway.ChatModelFactory chatModelFactory;

    public ModelConfigService(
            ModelConfigRepository repository,
            SecretCipherService cipherService,
            AppUserRepository userRepository,
            SpringAiModelGateway.ChatModelFactory chatModelFactory
    ) {
        this.repository = repository;
        this.cipherService = cipherService;
        this.userRepository = userRepository;
        this.chatModelFactory = chatModelFactory;
    }

    @Transactional(readOnly = true)
    public List<ModelConfigResponse> list() {
        ensureAdmin();
        return repository.findByUserIdOrderByUpdatedAtDesc(AuthContext.currentUserId())
                .stream()
                .map(ModelConfigResponse::from)
                .toList();
    }

    @Transactional
    public ModelConfigResponse save(SaveModelConfigRequest request) {
        ensureAdmin();
        if (request.apiKey() == null || request.apiKey().isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "API Key is required");
        }
        validateModelConfig(request, request.apiKey());
        ModelConfig saved = repository.save(new ModelConfig(
                AuthContext.currentUserId(),
                request.provider().trim(),
                request.modelName().trim(),
                blankToNull(request.baseUrl()),
                mask(request.apiKey()),
                cipherService.encrypt(request.apiKey()),
                true
        ));
        return ModelConfigResponse.from(saved);
    }

    @Transactional
    public ModelConfigResponse update(Long id, SaveModelConfigRequest request) {
        ensureAdmin();
        ModelConfig config = ownedConfig(id);
        String apiKey = request.apiKey() == null || request.apiKey().isBlank()
                ? cipherService.decrypt(config.getApiKeyCipher())
                : request.apiKey().trim();
        validateModelConfig(request, apiKey);
        config.update(
                request.provider().trim(),
                request.modelName().trim(),
                blankToNull(request.baseUrl()),
                request.apiKey() == null || request.apiKey().isBlank() ? null : mask(request.apiKey()),
                request.apiKey() == null || request.apiKey().isBlank() ? null : cipherService.encrypt(request.apiKey())
        );
        return ModelConfigResponse.from(config);
    }

    @Transactional
    public void delete(Long id) {
        ensureAdmin();
        repository.delete(ownedConfig(id));
    }

    private String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        String trimmed = apiKey.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateModelConfig(SaveModelConfigRequest request, String apiKeyValue) {
        String baseUrl = requiredValue(request.baseUrl(), "Base URL is required").replaceAll("/+$", "");
        String modelName = requiredValue(request.modelName(), "Model name is required");
        String apiKey = apiKeyValue.trim();
        try {
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(modelName)
                    .temperature(0.0)
                    .maxTokens(32)
                    .build();
            ChatModel chatModel = chatModelFactory.create(baseUrl, apiKey, options);
            ChatResponse response = chatModel.call(new Prompt(List.of(
                    new SystemMessage("You are a model connectivity checker."),
                    new UserMessage("Reply with OK.")
            )));
            String content = response == null || response.getResult() == null || response.getResult().getOutput() == null
                    ? ""
                    : response.getResult().getOutput().getContent();
            if (content == null || content.isBlank()) {
                throw new BusinessException("MODEL_CONFIG_INVALID", "模型已响应但未返回有效内容");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("MODEL_CONFIG_INVALID", "模型连通性验证失败，请检查网络、Base URL、模型名称或 API Key");
        }
    }

    private String requiredValue(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", message);
        }
        return value.trim();
    }

    private void ensureAdmin() {
        AppUser user = userRepository.findById(AuthContext.currentUserId())
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", "Login required"));
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BusinessException("FORBIDDEN", "Only administrators can manage model configs");
        }
    }

    private ModelConfig ownedConfig(Long id) {
        ModelConfig config = repository.findById(id)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "Model config not found"));
        if (!AuthContext.currentUserId().equals(config.getUserId())) {
            throw new BusinessException("FORBIDDEN", "No access to this model config");
        }
        return config;
    }
}
