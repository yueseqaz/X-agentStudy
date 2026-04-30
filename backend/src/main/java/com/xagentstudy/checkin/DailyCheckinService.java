package com.xagentstudy.checkin;

import com.xagentstudy.auth.AuthContext;
import com.xagentstudy.common.exception.BusinessException;
import com.xagentstudy.direction.LearningDirection;
import com.xagentstudy.direction.LearningDirectionRepository;
import com.xagentstudy.knowledge.KnowledgeDocumentRepository;
import com.xagentstudy.plan.LearningPlan;
import com.xagentstudy.plan.LearningPlanRepository;
import com.xagentstudy.plan.task.PlanTaskRecordRepository;
import com.xagentstudy.qa.QARecordRepository;
import com.xagentstudy.quiz.AnswerRecordRepository;
import com.xagentstudy.user.AppUser;
import com.xagentstudy.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DailyCheckinService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String GENERATED_DOCUMENT_PREFIX = "在线生成 - ";

    private final DailyCheckinRepository checkinRepository;
    private final LearningDirectionRepository directionRepository;
    private final LearningPlanRepository planRepository;
    private final QARecordRepository qaRecordRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final PlanTaskRecordRepository taskRecordRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final AppUserRepository userRepository;

    public DailyCheckinService(
            DailyCheckinRepository checkinRepository,
            LearningDirectionRepository directionRepository,
            LearningPlanRepository planRepository,
            QARecordRepository qaRecordRepository,
            AnswerRecordRepository answerRecordRepository,
            PlanTaskRecordRepository taskRecordRepository,
            KnowledgeDocumentRepository documentRepository,
            AppUserRepository userRepository
    ) {
        this.checkinRepository = checkinRepository;
        this.directionRepository = directionRepository;
        this.planRepository = planRepository;
        this.qaRecordRepository = qaRecordRepository;
        this.answerRecordRepository = answerRecordRepository;
        this.taskRecordRepository = taskRecordRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CheckinMonthResponse month(String month) {
        Long userId = AuthContext.currentUserId();
        YearMonth yearMonth = parseMonth(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        Map<LocalDate, DailyCheckin> checkins = mapCheckins(userId, start, end);
        List<Long> planIds = userPlanIds(userId);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        AppUser user = currentUser(userId);
        List<CheckinDayResponse> days = start.datesUntil(end.plusDays(1))
                .map(date -> CheckinDayResponse.from(date, checkins.get(date), stats(userId, planIds, user, date), date.equals(today)))
                .toList();
        Set<LocalDate> streakDates = checkinRepository
                .findByUserIdAndCheckinDateBetweenOrderByCheckinDateAsc(userId, LocalDate.now(BUSINESS_ZONE).minusYears(1), LocalDate.now(BUSINESS_ZONE))
                .stream()
                .map(DailyCheckin::getCheckinDate)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
        return new CheckinMonthResponse(
                yearMonth.toString(),
                today,
                checkins.size(),
                currentStreak(streakDates),
                longestStreak(streakDates),
                days
        );
    }

    @Transactional(readOnly = true)
    public CheckinDayResponse day(LocalDate date) {
        Long userId = AuthContext.currentUserId();
        List<Long> planIds = userPlanIds(userId);
        AppUser user = currentUser(userId);
        DailyCheckin checkin = checkinRepository.findByUserIdAndCheckinDate(userId, date).orElse(null);
        return CheckinDayResponse.from(date, checkin, stats(userId, planIds, user, date), date.equals(LocalDate.now(BUSINESS_ZONE)));
    }

    @Transactional
    public CheckinDayResponse save(LocalDate date, SaveCheckinRequest request) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (!date.equals(today)) {
            throw new BusinessException("CHECKIN_DATE_NOT_ALLOWED", "只能为今天打卡，历史日期和未来日期仅支持查看");
        }
        Long userId = AuthContext.currentUserId();
        AppUser user = currentUser(userId);
        CheckinStatsResponse stats = stats(userId, userPlanIds(userId), user, date);
        DailyCheckin checkin = checkinRepository
                .findByUserIdAndCheckinDate(userId, date)
                .orElseGet(() -> new DailyCheckin(userId, date));
        checkin.update(
                clean(request == null ? "" : request.summary(), 1000),
                clean(request == null ? "" : request.mood(), 32),
                stats.onlineMinutes()
        );
        DailyCheckin saved = checkinRepository.save(checkin);
        return CheckinDayResponse.from(date, saved, stats, true);
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException ex) {
            return YearMonth.from(LocalDate.now(BUSINESS_ZONE));
        }
    }

    private Map<LocalDate, DailyCheckin> mapCheckins(Long userId, LocalDate start, LocalDate end) {
        Map<LocalDate, DailyCheckin> result = new HashMap<>();
        for (DailyCheckin checkin : checkinRepository.findByUserIdAndCheckinDateBetweenOrderByCheckinDateAsc(userId, start, end)) {
            result.put(checkin.getCheckinDate(), checkin);
        }
        return result;
    }

    private List<Long> userPlanIds(Long userId) {
        List<Long> directionIds = directionRepository.findByUserIdAndDeletedAtIsNullOrderByLastActiveAtDesc(userId)
                .stream()
                .map(LearningDirection::getId)
                .toList();
        if (directionIds.isEmpty()) {
            return List.of();
        }
        return planRepository.findByDirectionIdInOrderByCreatedAtDesc(directionIds)
                .stream()
                .map(LearningPlan::getId)
                .toList();
    }

    private AppUser currentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("RESOURCE_NOT_FOUND", "User not found"));
    }

    private CheckinStatsResponse stats(Long userId, List<Long> planIds, AppUser user, LocalDate date) {
        OffsetDateTime start = date.atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        OffsetDateTime end = date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        int qaCount = toInt(qaRecordRepository.countByUserIdAndCreatedAtBetween(userId, start, end));
        int answered = toInt(answerRecordRepository.countByUserIdAndAnsweredAtBetween(userId, start, end));
        int correct = toInt(answerRecordRepository.countByUserIdAndCorrectTrueAndAnsweredAtBetween(userId, start, end));
        int completedTasks = toInt(taskRecordRepository.countByUserIdAndCompletedTrueAndCompletedAtBetween(userId, start, end));
        int uploadedDocuments = 0;
        int generatedDocuments = 0;
        if (!planIds.isEmpty()) {
            uploadedDocuments = toInt(documentRepository.countByPlanIdInAndUploadedAtBetween(planIds, start, end));
            generatedDocuments = toInt(documentRepository.countByPlanIdInAndNameStartingWithAndUploadedAtBetween(
                    planIds, GENERATED_DOCUMENT_PREFIX, start, end
            ));
        }
        int onlineMinutes = onlineMinutes(user, date);
        int activeScore = qaCount * 2 + answered * 2 + correct + completedTasks * 3 + uploadedDocuments * 4 + generatedDocuments * 4 + Math.min(onlineMinutes / 10, 12);
        return new CheckinStatsResponse(qaCount, answered, correct, completedTasks, uploadedDocuments, generatedDocuments, onlineMinutes, activeScore);
    }

    private int onlineMinutes(AppUser user, LocalDate date) {
        if (!date.equals(LocalDate.now(BUSINESS_ZONE)) || user.getLastLoginAt() == null) {
            return 0;
        }
        OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);
        OffsetDateTime startOfToday = date.atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        OffsetDateTime sessionStart = user.getLastLoginAt().isAfter(startOfToday) ? user.getLastLoginAt() : startOfToday;
        long minutes = Duration.between(sessionStart, now).toMinutes();
        return minutes <= 0 ? 0 : toInt(minutes);
    }

    private int currentStreak(Set<LocalDate> checkedDates) {
        int streak = 0;
        LocalDate cursor = LocalDate.now(BUSINESS_ZONE);
        while (checkedDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private int longestStreak(Set<LocalDate> checkedDates) {
        if (checkedDates.isEmpty()) {
            return 0;
        }
        int best = 0;
        int current = 0;
        LocalDate previous = null;
        for (LocalDate date : checkedDates.stream().sorted().toList()) {
            current = previous != null && date.equals(previous.plusDays(1)) ? current + 1 : 1;
            best = Math.max(best, current);
            previous = date;
        }
        return best;
    }

    private int toInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private String clean(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }
}
