package com.xagentstudy.admin;

import com.xagentstudy.user.AppUser;

import java.time.OffsetDateTime;

public record AdminUserResponse(
        Long id,
        String nickname,
        String account,
        String role,
        String avatarUrl,
        Boolean disabled,
        OffsetDateTime disabledUntil,
        Boolean emailVerified,
        Integer failedLoginCount,
        OffsetDateTime lockedUntil,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt
) {
    public static AdminUserResponse from(AppUser user) {
        return new AdminUserResponse(
                user.getId(),
                user.getNickname(),
                user.getAccount(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getDisabled(),
                user.getDisabledUntil(),
                user.getEmailVerified(),
                user.getFailedLoginCount(),
                user.getLockedUntil(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
