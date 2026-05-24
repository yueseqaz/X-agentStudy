package com.xagentstudy.agent.model;

import com.xagentstudy.common.security.SecretCipherService;
import com.xagentstudy.modelconfig.ModelConfigRepository;
import com.xagentstudy.user.AppUser;
import com.xagentstudy.user.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SpringAiModelGateway implements ModelGateway {
    private static final Logger log = LoggerFactory.getLogger(SpringAiModelGateway.class);

    private final OpenAiCompatibleProperties properties;
    private final ChatModelFactory chatModelFactory;
    private final ModelConfigRepository modelConfigRepository;
    private final SecretCipherService cipherService;
    private final AppUserRepository userRepository;

    public SpringAiModelGateway(
            OpenAiCompatibleProperties properties,
            ChatModelFactory chatModelFactory,
            ModelConfigRepository modelConfigRepository,
            SecretCipherService cipherService,
            AppUserRepository userRepository
    ) {
        this.properties = properties;
        this.chatModelFactory = chatModelFactory;
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
            OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                    .model(runtimeModel.model())
                    .temperature(0.2);
            if (jsonMode) {
                optionsBuilder.responseFormat(ResponseFormat.builder()
                        .type(ResponseFormat.Type.JSON_OBJECT)
                        .build());
            }
            ChatModel chatModel = chatModelFactory.create(
                    runtimeModel.baseUrl().replaceAll("/+$", ""),
                    runtimeModel.apiKey(),
                    optionsBuilder.build()
            );
            ChatResponse response = chatModel.call(new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userPrompt)
            )));
            String content = response == null || response.getResult() == null || response.getResult().getOutput() == null
                    ? ""
                    : response.getResult().getOutput().getContent();
            return content == null || content.isBlank() ? Optional.empty() : Optional.of(content);
        } catch (Exception ex) {
            log.warn("Spring AI generation failed, falling back to local generator: {}", ex.getMessage());
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
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public interface ChatModelFactory {
        ChatModel create(String baseUrl, String apiKey, OpenAiChatOptions options);
    }

    @Component
    static class OpenAiChatModelFactory implements ChatModelFactory {
        @Override
        public ChatModel create(String baseUrl, String apiKey, OpenAiChatOptions options) {
            return new OpenAiChatModel(new OpenAiApi(baseUrl, apiKey), options);
        }
    }

    private record RuntimeModel(String baseUrl, String model, String apiKey) {
        boolean configured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
