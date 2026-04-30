package com.xagentstudy.auth;

public record AuthPrincipal(
        Long userId,
        String account,
        String role
) {
}
