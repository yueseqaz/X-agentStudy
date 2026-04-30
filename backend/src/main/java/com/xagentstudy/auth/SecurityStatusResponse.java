package com.xagentstudy.auth;

import com.xagentstudy.user.AppUser;

import java.time.OffsetDateTime;

public record SecurityStatusResponse(
        Long userId,
        String account,
        String role,
        String avatarUrl,
        Boolean disabled,
        OffsetDateTime disabledUntil,
        Boolean emailVerified,
        Integer failedLoginCount,
        OffsetDateTime lockedUntil,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        Boolean refreshTokenStored
) {
    public static SecurityStatusResponse from(AppUser user, boolean refreshTokenStored) {
        return new SecurityStatusResponse(
                user.getId(),
                user.getAccount(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getDisabled(),
                user.getDisabledUntil(),
                user.getEmailVerified(),
                user.getFailedLoginCount(),
                user.getLockedUntil(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                refreshTokenStored
        );
    }
}
