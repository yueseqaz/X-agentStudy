package com.xagentstudy.checkin;

public record SaveCheckinRequest(
        String summary,
        String mood,
        Integer studyMinutes
) {
}
