package com.xagentstudy.report;

import java.util.List;

public record DynamicProfileDimensionResponse(
        String name,
        String value,
        int confidence,
        String status,
        List<String> evidence
) {
}
