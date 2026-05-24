package com.xagentstudy.modelconfig;

import com.xagentstudy.agent.model.SpringAiModelGateway;
import com.xagentstudy.auth.AuthPrincipal;
import com.xagentstudy.common.security.SecretCipherService;
import com.xagentstudy.user.AppUser;
import com.xagentstudy.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModelConfigServiceTest {
    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveValidatesModelConfigThroughSpringAi() {
        ModelConfigRepository repository = mock(ModelConfigRepository.class);
        SecretCipherService cipherService = mock(SecretCipherService.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        CapturingChatModelFactory factory = new CapturingChatModelFactory();
        ModelConfigService service = new ModelConfigService(repository, cipherService, userRepository, factory);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(7L, "admin@example.com", "ADMIN"),
                null
        ));
        AppUser admin = new AppUser("Admin", "admin@example.com", "hash", "ADMIN");
        ReflectionTestUtils.setField(admin, "id", 7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));
        when(cipherService.encrypt("sk-test")).thenReturn("cipher");
        when(repository.save(any(ModelConfig.class))).thenAnswer(invocation -> {
            ModelConfig config = invocation.getArgument(0);
            ReflectionTestUtils.setField(config, "id", 31L);
            return config;
        });
        factory.nextContent = "OK";

        ModelConfigResponse response = service.save(new SaveModelConfigRequest(
                "DeepSeek",
                "deepseek-chat",
                "https://api.deepseek.com",
                "sk-test"
        ));

        assertThat(response.id()).isEqualTo(31L);
        assertThat(response.apiKeyMask()).isEqualTo("****");
        assertThat(factory.baseUrl).isEqualTo("https://api.deepseek.com");
        assertThat(factory.apiKey).isEqualTo("sk-test");
        assertThat(factory.options.getModel()).isEqualTo("deepseek-chat");
        assertThat(factory.options.getTemperature()).isZero();
        assertThat(factory.options.getMaxTokens()).isEqualTo(32);
    }

    private static final class CapturingChatModelFactory implements SpringAiModelGateway.ChatModelFactory {
        private String baseUrl;
        private String apiKey;
        private OpenAiChatOptions options;
        private String nextContent;

        @Override
        public ChatModel create(String baseUrl, String apiKey, OpenAiChatOptions options) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.options = options;
            return prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage(nextContent))));
        }
    }
}
