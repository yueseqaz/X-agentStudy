package com.xagentstudy.agent.model;

import com.xagentstudy.common.security.SecretCipherService;
import com.xagentstudy.modelconfig.ModelConfig;
import com.xagentstudy.modelconfig.ModelConfigRepository;
import com.xagentstudy.user.AppUser;
import com.xagentstudy.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiModelGatewayTest {
    @Test
    void generateJsonUsesAdminRuntimeConfigAndJsonResponseFormat() {
        CapturingChatModelFactory factory = new CapturingChatModelFactory();
        ModelConfigRepository modelConfigRepository = mock(ModelConfigRepository.class);
        SecretCipherService cipherService = mock(SecretCipherService.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        OpenAiCompatibleProperties properties = new OpenAiCompatibleProperties("env-key", "https://env.example/v1", "env-model");
        SpringAiModelGateway gateway = new SpringAiModelGateway(
                properties,
                factory,
                modelConfigRepository,
                cipherService,
                userRepository
        );

        AppUser admin = new AppUser("Admin", "admin@example.com", "hash", "ADMIN");
        ReflectionTestUtils.setField(admin, "id", 1L);
        ModelConfig config = new ModelConfig(1L, "DeepSeek", "deepseek-chat", "https://api.deepseek.com", "****", "cipher", true);
        when(userRepository.findAll()).thenReturn(List.of(admin));
        when(modelConfigRepository.findFirstByUserIdInAndEnabledTrueOrderByUpdatedAtDesc(List.of(1L)))
                .thenReturn(Optional.of(config));
        when(cipherService.decrypt("cipher")).thenReturn("db-key");
        factory.nextContent = "{\"answer\":\"OK\"}";

        Optional<String> result = gateway.generateJson("system prompt", "user prompt");

        assertThat(result).contains("{\"answer\":\"OK\"}");
        assertThat(factory.baseUrl).isEqualTo("https://api.deepseek.com");
        assertThat(factory.apiKey).isEqualTo("db-key");
        assertThat(factory.options.getModel()).isEqualTo("deepseek-chat");
        assertThat(factory.options.getTemperature()).isEqualTo(0.2);
        assertThat(factory.options.getResponseFormat().getType()).isEqualTo(ResponseFormat.Type.JSON_OBJECT);
        assertThat(factory.prompt.getInstructions()).hasSize(2);
        assertThat(factory.prompt.getInstructions().get(0)).isInstanceOf(SystemMessage.class);
        assertThat(factory.prompt.getInstructions().get(1)).isInstanceOf(UserMessage.class);
    }

    @Test
    void generateTextDoesNotForceJsonResponseFormat() {
        CapturingChatModelFactory factory = new CapturingChatModelFactory();
        ModelConfigRepository modelConfigRepository = mock(ModelConfigRepository.class);
        SecretCipherService cipherService = mock(SecretCipherService.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        SpringAiModelGateway gateway = new SpringAiModelGateway(
                new OpenAiCompatibleProperties("env-key", "https://env.example/v1", "env-model"),
                factory,
                modelConfigRepository,
                cipherService,
                userRepository
        );

        when(userRepository.findAll()).thenReturn(List.of());
        factory.nextContent = "Markdown body";

        Optional<String> result = gateway.generateText("system prompt", "user prompt");

        assertThat(result).contains("Markdown body");
        assertThat(factory.baseUrl).isEqualTo("https://env.example/v1");
        assertThat(factory.apiKey).isEqualTo("env-key");
        assertThat(factory.options.getModel()).isEqualTo("env-model");
        assertThat(factory.options.getResponseFormat()).isNull();
    }

    private static final class CapturingChatModelFactory implements SpringAiModelGateway.ChatModelFactory {
        private String baseUrl;
        private String apiKey;
        private OpenAiChatOptions options;
        private Prompt prompt;
        private String nextContent;

        @Override
        public ChatModel create(String baseUrl, String apiKey, OpenAiChatOptions options) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.options = options;
            return prompt -> {
                this.prompt = prompt;
                return new ChatResponse(List.of(new Generation(new AssistantMessage(nextContent))));
            };
        }
    }
}
