package com.xagentstudy.checkin;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CheckinDayResponse(
        LocalDate date,
        boolean checkedIn,
        String summary,
        String mood,
        Integer studyMinutes,
        boolean canCheckIn,
        CheckinStatsResponse stats,
        OffsetDateTime updatedAt
) {
    public static CheckinDayResponse from(LocalDate date, DailyCheckin checkin, CheckinStatsResponse stats, boolean canCheckIn) {
        return new CheckinDayResponse(
                date,
                checkin != null,
                checkin == null ? "" : checkin.getSummary(),
                checkin == null ? "" : checkin.getMood(),
                checkin == null ? stats.onlineMinutes() : checkin.getStudyMinutes(),
                canCheckIn,
                stats,
                checkin == null ? null : checkin.getUpdatedAt()
        );
    }
}
