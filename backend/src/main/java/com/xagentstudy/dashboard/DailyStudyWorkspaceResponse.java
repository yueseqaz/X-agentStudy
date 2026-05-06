package com.xagentstudy.dashboard;

import com.xagentstudy.checkin.CheckinDayResponse;

import java.time.LocalDate;
import java.util.List;

public record DailyStudyWorkspaceResponse(
        LocalDate date,
        LearningCoachResponse coach,
        CheckinDayResponse checkin,
        List<DailyStudyActionItemResponse> actionItems,
        int totalEstimatedMinutes,
        String completionText
) {
}
