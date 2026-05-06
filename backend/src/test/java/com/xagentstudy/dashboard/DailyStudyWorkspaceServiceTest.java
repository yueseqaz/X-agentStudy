package com.xagentstudy.dashboard;

import com.xagentstudy.checkin.CheckinDayResponse;
import com.xagentstudy.checkin.CheckinStatsResponse;
import com.xagentstudy.checkin.DailyCheckinService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DailyStudyWorkspaceServiceTest {
    private final LearningCoachService coachService = mock(LearningCoachService.class);
    private final DailyCheckinService checkinService = mock(DailyCheckinService.class);
    private final DailyStudyWorkspaceService service = new DailyStudyWorkspaceService(coachService, checkinService);

    @Test
    void buildsTodayWorkspaceFromCoachAndCheckin() {
        LearningCoachResponse coach = new LearningCoachResponse(
                16L,
                "Redis 系统学习计划",
                "Redis",
                "先清掉今天的待复习",
                "你已经有待复习知识点。先处理复习，再推进新内容，学习会更稳。",
                18,
                "去复习",
                "/plans/16?tab=review",
                List.of("完成待复习条目", "重做相关错题", "刷新学习报告看薄弱点变化"),
                List.of("资料 2 份", "题目 3 道", "待复习 11 项", "任务完成 0/66")
        );
        CheckinDayResponse checkin = new CheckinDayResponse(
                LocalDate.now(DailyStudyWorkspaceService.BUSINESS_ZONE),
                false,
                "",
                "",
                12,
                true,
                new CheckinStatsResponse(1, 2, 1, 0, 0, 0, 12, 17),
                null
        );
        when(coachService.today()).thenReturn(coach);
        when(checkinService.day(LocalDate.now(DailyStudyWorkspaceService.BUSINESS_ZONE))).thenReturn(checkin);

        DailyStudyWorkspaceResponse response = service.today();

        assertThat(response.coach()).isEqualTo(coach);
        assertThat(response.checkin()).isEqualTo(checkin);
        assertThat(response.date()).isEqualTo(LocalDate.now(DailyStudyWorkspaceService.BUSINESS_ZONE));
        assertThat(response.totalEstimatedMinutes()).isEqualTo(18);
        assertThat(response.actionItems()).hasSize(4);
        assertThat(response.actionItems().get(0).title()).isEqualTo("完成待复习条目");
        assertThat(response.actionItems().get(0).path()).isEqualTo("/plans/16?tab=review");
        assertThat(response.actionItems().get(0).primary()).isTrue();
        assertThat(response.actionItems().get(3).title()).isEqualTo("完成今日打卡");
        assertThat(response.actionItems().get(3).path()).isEqualTo("/calendar");
        assertThat(response.completionText()).isEqualTo("今日还未打卡");
    }
}
