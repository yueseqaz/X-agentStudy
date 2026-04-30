package com.xagentstudy.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String nickname,
        @NotBlank String account,
        @NotBlank @Size(min = 6) String password
) {
}
