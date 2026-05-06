package com.xagentstudy.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String nickname;

    @Column(nullable = false, unique = true, length = 128)
    private String account;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false, length = 32)
    private String role;

    @Column(columnDefinition = "longtext")
    private String avatarUrl;

    private Boolean disabled;
    private OffsetDateTime disabledUntil;
    private Boolean emailVerified;
    private Integer failedLoginCount;
    private OffsetDateTime lockedUntil;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime createdAt;

    protected AppUser() {
    }

    public AppUser(String nickname, String account, String passwordHash, String role) {
        this.nickname = nickname;
        this.account = account;
        this.passwordHash = passwordHash;
        this.role = role;
        this.disabled = false;
        this.emailVerified = false;
        this.failedLoginCount = 0;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAccount() {
        return account;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public OffsetDateTime getDisabledUntil() {
        return disabledUntil;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public Integer getFailedLoginCount() {
        return failedLoginCount;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void recordLoginSuccess() {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.lastLoginAt = OffsetDateTime.now();
    }

    public void recordLoginFailure() {
        this.failedLoginCount = (this.failedLoginCount == null ? 0 : this.failedLoginCount) + 1;
        if (this.failedLoginCount >= 5) {
            this.lockedUntil = OffsetDateTime.now().plusMinutes(15);
        }
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public void setDisabled(Boolean disabled, OffsetDateTime disabledUntil) {
        this.disabled = disabled;
        this.disabledUntil = disabled ? disabledUntil : null;
    }

    public boolean isBanActive(OffsetDateTime now) {
        if (!Boolean.TRUE.equals(disabled)) {
            return false;
        }
        return disabledUntil == null || disabledUntil.isAfter(now);
    }

    public void clearExpiredBan(OffsetDateTime now) {
        if (Boolean.TRUE.equals(disabled) && disabledUntil != null && !disabledUntil.isAfter(now)) {
            this.disabled = false;
            this.disabledUntil = null;
        }
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
