package com.xagentstudy.practice;

import java.util.Map;

public record SubmitPracticeProjectRequest(
        String summary,
        String reflection,
        Map<String, String> taskNotes
) {
}
