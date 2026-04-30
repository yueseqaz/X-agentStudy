package com.xagentstudy.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        Long expiresInSeconds
) {
    public long ttlSeconds() {
        return expiresInSeconds == null ? 604800L : expiresInSeconds;
    }
}
