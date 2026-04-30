package com.xagentstudy.checkin;

public record CheckinStatsResponse(
        int qaCount,
        int answeredQuestionCount,
        int correctAnswerCount,
        int completedTaskCount,
        int uploadedDocumentCount,
        int generatedDocumentCount,
        int onlineMinutes,
        int activeScore
) {
}
