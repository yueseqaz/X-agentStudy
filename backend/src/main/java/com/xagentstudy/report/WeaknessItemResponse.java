package com.xagentstudy.report;

import java.util.List;

public record WeaknessItemResponse(
        String knowledgePoint,
        String status,
        int wrongCount,
        int masteryScore,
        String reason,
        List<String> sources,
        List<Long> questionIds,
        List<String> actions
) {
}
