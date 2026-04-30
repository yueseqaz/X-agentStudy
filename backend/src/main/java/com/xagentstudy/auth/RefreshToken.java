package com.xagentstudy.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String tokenHash;
    private OffsetDateTime expiresAt;
    private Boolean revoked;
    private OffsetDateTime createdAt;

    protected RefreshToken() {
    }

    public RefreshToken(Long userId, String tokenHash, OffsetDateTime expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = false;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getUserId() {
        return userId;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public Boolean getRevoked() {
        return revoked;
    }

    public void revoke() {
        this.revoked = true;
    }
}
