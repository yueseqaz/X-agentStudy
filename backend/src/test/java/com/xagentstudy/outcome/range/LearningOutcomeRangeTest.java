package com.xagentstudy.outcome.range;

import com.xagentstudy.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LearningOutcomeRangeTest {
    @Test
    void defaultsToLastThirtyDaysWhenNoParamIsProvided() {
        LearningOutcomeRange range = LearningOutcomeRange.resolve(null, null, null, LocalDate.of(2026, 4, 30));

        assertThat(range.code()).isEqualTo("LAST_30_DAYS");
        assertThat(range.label()).isEqualTo("最近 30 天");
        assertThat(range.startDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(range.endDate()).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    void usesCustomDateWindowWhenStartAndEndDateAreProvided() {
        LearningOutcomeRange range = LearningOutcomeRange.resolve(
                "7d",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 30)
        );

        assertThat(range.code()).isEqualTo("CUSTOM");
        assertThat(range.label()).isEqualTo("自定义");
        assertThat(range.startDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(range.endDate()).isEqualTo(LocalDate.of(2026, 4, 20));
    }

    @Test
    void supportsAllRange() {
        LearningOutcomeRange range = LearningOutcomeRange.resolve("ALL", null, null, LocalDate.of(2026, 4, 30));

        assertThat(range.code()).isEqualTo("ALL");
        assertThat(range.label()).isEqualTo("全部记录");
        assertThat(range.startDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(range.endDate()).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    void rejectsPartialCustomDateInput() {
        assertThatThrownBy(() -> LearningOutcomeRange.resolve(
                null,
                LocalDate.of(2026, 4, 10),
                null,
                LocalDate.of(2026, 4, 30)
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("startDate and endDate must be provided together");
    }
}
