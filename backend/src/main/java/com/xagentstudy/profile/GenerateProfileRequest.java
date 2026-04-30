package com.xagentstudy.profile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GenerateProfileRequest(
        @NotEmpty List<@Valid ProfileAnswer> answers
) {
}
