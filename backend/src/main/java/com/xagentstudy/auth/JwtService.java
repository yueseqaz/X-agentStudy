package com.xagentstudy.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public JwtService(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String issue(AuthPrincipal principal) {
        try {
            String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
            String payload = encodeJson(Map.of(
                    "sub", principal.userId(),
                    "account", principal.account(),
                    "role", principal.role(),
                    "exp", Instant.now().plusSeconds(properties.ttlSeconds()).getEpochSecond()
            ));
            String signingInput = header + "." + payload;
            return signingInput + "." + sign(signingInput);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to issue token", ex);
        }
    }

    public Optional<AuthPrincipal> verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }
            String signingInput = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(signingInput), parts[2])) {
                return Optional.empty();
            }
            Map<String, Object> payload = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]),
                    new TypeReference<>() {
                    }
            );
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                return Optional.empty();
            }
            return Optional.of(new AuthPrincipal(
                    ((Number) payload.get("sub")).longValue(),
                    String.valueOf(payload.get("account")),
                    String.valueOf(payload.get("role"))
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
