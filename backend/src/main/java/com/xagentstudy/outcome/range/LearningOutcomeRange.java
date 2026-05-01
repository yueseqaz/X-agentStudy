package com.xagentstudy.outcome.range;

import com.xagentstudy.common.exception.BusinessException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;

public record LearningOutcomeRange(
        String code,
        String label,
        LocalDate startDate,
        LocalDate endDate
) {
    private static final LocalDate HISTORY_START = LocalDate.of(2000, 1, 1);

    public static LearningOutcomeRange resolve(String range, LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (startDate != null || endDate != null) {
            if (startDate == null || endDate == null) {
                throw new BusinessException("INVALID_RANGE", "startDate and endDate must be provided together");
            }
            if (startDate.isAfter(endDate)) {
                throw new BusinessException("INVALID_RANGE", "startDate must be on or before endDate");
            }
            return new LearningOutcomeRange("CUSTOM", "自定义", startDate, endDate);
        }

        String normalized = range == null || range.isBlank()
                ? "last_30_days"
                : range.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "7d", "last_7_days" -> fixedRange("LAST_7_DAYS", "最近 7 天", today.minusDays(6), today);
            case "30d", "last_30_days" -> fixedRange("LAST_30_DAYS", "最近 30 天", today.minusDays(29), today);
            case "90d", "last_90_days" -> fixedRange("LAST_90_DAYS", "最近 90 天", today.minusDays(89), today);
            case "all" -> fixedRange("ALL", "全部记录", HISTORY_START, today);
            case "custom" -> throw new BusinessException("INVALID_RANGE", "custom range requires startDate and endDate");
            default -> throw new BusinessException("INVALID_RANGE", "Unsupported range: " + normalized);
        };
    }

    public OffsetDateTime startAt(ZoneId zoneId) {
        return startDate.atStartOfDay(zoneId).toOffsetDateTime();
    }

    public OffsetDateTime endExclusiveAt(ZoneId zoneId) {
        return endDate.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();
    }

    private static LearningOutcomeRange fixedRange(String code, String label, LocalDate startDate, LocalDate endDate) {
        return new LearningOutcomeRange(code, label, startDate, endDate);
    }
}
