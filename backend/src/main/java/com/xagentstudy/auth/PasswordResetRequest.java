package com.xagentstudy.auth;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(@NotBlank String account) {
}
