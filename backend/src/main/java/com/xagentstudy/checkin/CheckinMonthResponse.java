package com.xagentstudy.checkin;

import java.time.LocalDate;
import java.util.List;

public record CheckinMonthResponse(
        String month,
        LocalDate today,
        int checkedDays,
        int currentStreak,
        int longestStreak,
        List<CheckinDayResponse> days
) {
}
