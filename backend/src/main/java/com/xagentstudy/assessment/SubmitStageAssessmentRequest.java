package com.xagentstudy.assessment;

import java.util.Map;

public record SubmitStageAssessmentRequest(
        Map<String, String> answers
) {
}
