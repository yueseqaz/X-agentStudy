package com.xagentstudy.review;

public record CompleteReviewRequest(
        Integer quality
) {
    public int safeQuality() {
        if (quality == null) {
            return 4;
        }
        return Math.max(1, Math.min(5, quality));
    }
}
