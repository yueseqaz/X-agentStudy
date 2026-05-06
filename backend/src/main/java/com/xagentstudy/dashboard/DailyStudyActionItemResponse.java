package com.xagentstudy.dashboard;

public record DailyStudyActionItemResponse(
        String title,
        String detail,
        String path,
        boolean primary
) {
}
