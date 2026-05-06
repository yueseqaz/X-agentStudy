package com.xagentstudy.auth;

import com.xagentstudy.user.AppUser;

public record UserSession(
        Long id,
        String nickname,
        String account,
        String role,
        String avatarUrl,
        Boolean emailVerified,
        Boolean disabled,
        java.time.OffsetDateTime disabledUntil
) {
    public static UserSession from(AppUser user) {
        return new UserSession(
                user.getId(),
                user.getNickname(),
                user.getAccount(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getEmailVerified(),
                user.getDisabled(),
                user.getDisabledUntil()
        );
    }
}
