package com.xagentstudy.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "account_action_tokens")
public class AccountActionToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String tokenHash;
    private String actionType;
    private OffsetDateTime expiresAt;
    private Boolean used;
    private OffsetDateTime createdAt;

    protected AccountActionToken() {
    }

    public AccountActionToken(Long userId, String tokenHash, String actionType, OffsetDateTime expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.actionType = actionType;
        this.expiresAt = expiresAt;
        this.used = false;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getUserId() {
        return userId;
    }

    public String getActionType() {
        return actionType;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public Boolean getUsed() {
        return used;
    }

    public void markUsed() {
        this.used = true;
    }
}
