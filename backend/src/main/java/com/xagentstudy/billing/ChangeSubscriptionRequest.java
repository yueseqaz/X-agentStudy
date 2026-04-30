package com.xagentstudy.billing;

import jakarta.validation.constraints.NotBlank;

public record ChangeSubscriptionRequest(
        @NotBlank String planCode
) {
}
