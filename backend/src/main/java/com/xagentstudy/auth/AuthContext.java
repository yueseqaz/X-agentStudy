package com.xagentstudy.auth;

import com.xagentstudy.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthContext {
    private AuthContext() {
    }

    public static Long currentUserId() {
        return currentPrincipal().userId();
    }

    public static String currentRole() {
        return currentPrincipal().role();
    }

    private static AuthPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new BusinessException("UNAUTHORIZED", "Login required");
        }
        return principal;
    }
}
