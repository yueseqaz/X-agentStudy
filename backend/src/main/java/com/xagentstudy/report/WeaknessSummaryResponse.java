package com.xagentstudy.report;

public record WeaknessSummaryResponse(
        int openWeaknessCount,
        int repairingCount,
        int masteredCount,
        int totalWrongCount
) {
}
