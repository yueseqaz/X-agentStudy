package com.xagentstudy.dashboard;

import com.xagentstudy.checkin.CheckinDayResponse;
import com.xagentstudy.checkin.DailyCheckinService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class DailyStudyWorkspaceService {
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final LearningCoachService coachService;
    private final DailyCheckinService checkinService;

    public DailyStudyWorkspaceService(LearningCoachService coachService, DailyCheckinService checkinService) {
        this.coachService = coachService;
        this.checkinService = checkinService;
    }

    @Transactional(readOnly = true)
    public DailyStudyWorkspaceResponse today() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LearningCoachResponse coach = coachService.today();
        CheckinDayResponse checkin = checkinService.day(today);
        return new DailyStudyWorkspaceResponse(
                today,
                coach,
                checkin,
                actionItems(coach, checkin),
                coach.estimatedMinutes(),
                checkin.checkedIn() ? "今日已打卡" : "今日还未打卡"
        );
    }

    private List<DailyStudyActionItemResponse> actionItems(LearningCoachResponse coach, CheckinDayResponse checkin) {
        List<DailyStudyActionItemResponse> items = new ArrayList<>();
        for (int index = 0; index < coach.nextSteps().size(); index++) {
            String step = coach.nextSteps().get(index);
            items.add(new DailyStudyActionItemResponse(
                    step,
                    index == 0 ? coach.reason() : "完成后回到首页刷新今日状态。",
                    coach.primaryActionPath(),
                    index == 0
            ));
        }
        if (!checkin.checkedIn()) {
            items.add(new DailyStudyActionItemResponse(
                    "完成今日打卡",
                    "记录今天学了什么，让学习成果持续沉淀。",
                    "/calendar",
                    false
            ));
        }
        return items;
    }
}
