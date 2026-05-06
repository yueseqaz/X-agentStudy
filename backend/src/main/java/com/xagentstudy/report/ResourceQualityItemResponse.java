package com.xagentstudy.report;

import java.util.List;

public record ResourceQualityItemResponse(
        String name,
        int score,
        String status,
        String reason,
        List<String> evidence
) {
}
