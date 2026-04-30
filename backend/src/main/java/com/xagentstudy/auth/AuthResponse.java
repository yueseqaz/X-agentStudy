package com.xagentstudy.auth;

public record AuthResponse(
        String token,
        String refreshToken,
        UserSession user
) {
}
