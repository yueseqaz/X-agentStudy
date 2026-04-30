package com.xagentstudy.report;

public record WeakPointSourceResponse(
        String knowledgePoint,
        String sourceScope,
        String sourceLabel,
        int wrongCount
) {
}
