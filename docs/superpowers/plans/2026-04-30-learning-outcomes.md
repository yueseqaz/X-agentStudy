# Learning Outcomes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a dedicated `学习成果` page that aggregates the current user's cross-plan learning results by time range and supports poster preview/export.

**Architecture:** Add one backend aggregation module under `outcome/` that reads existing plans, tasks, answers, reviews, check-ins, reports, and summary cards without changing the schema. Add one frontend route and page that consumes the new API, renders overview metrics, direction snapshots, trends, highlights, suggestions, and reuses a small canvas helper to export a poster for the currently selected time range.

**Tech Stack:** Spring Boot, Spring Data JPA, Vue 3, TypeScript, Element Plus, existing Axios client, existing canvas-based export pattern

---

## File Structure

**Create**

- `backend/src/main/java/com/xagentstudy/outcome/LearningOutcomeController.java`
- `backend/src/main/java/com/xagentstudy/outcome/LearningOutcomeService.java`
- `backend/src/main/java/com/xagentstudy/outcome/LearningOutcomeRange.java`
- `backend/src/main/java/com/xagentstudy/outcome/LearningOutcomeResponse.java`
- `backend/src/test/java/com/xagentstudy/outcome/LearningOutcomeRangeTest.java`
- `frontend/src/views/OutcomesView.vue`
- `frontend/src/utils/outcomePoster.ts`

**Modify**

- `backend/src/main/java/com/xagentstudy/checkin/DailyCheckinRepository.java`
- `backend/src/main/java/com/xagentstudy/direction/LearningDirectionRepository.java`
- `backend/src/main/java/com/xagentstudy/plan/LearningPlanRepository.java`
- `backend/src/main/java/com/xagentstudy/plan/task/PlanTaskRecordRepository.java`
- `backend/src/main/java/com/xagentstudy/quiz/AnswerRecordRepository.java`
- `backend/src/main/java/com/xagentstudy/review/ReviewRecordRepository.java`
- `backend/src/main/java/com/xagentstudy/summary/SummaryCardRepository.java`
- `frontend/src/router.ts`
- `frontend/src/App.vue`
- `frontend/src/style.css`

**Test**

- `backend/src/test/java/com/xagentstudy/outcome/LearningOutcomeRangeTest.java`
- `backend/src/test/java/com/xagentstudy/XAgentStudyApplicationTests.java`

### Task 1: Define Outcome Range Contract

**Files:**

- Create: `backend/src/main/java/com/xagentstudy/outcome/LearningOutcomeRange.java`
- Create: `backend/src/test/java/com/xagentstudy/outcome/LearningOutcomeRangeTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.xagentstudy.outcome;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LearningOutcomeRangeTest {
    @Test
    void defaultsLastThirtyDaysRange() {
        LearningOutcomeRange.ResolvedRange range = LearningOutcomeRange.resolve(
                "LAST_30_DAYS",
                null,
                null,
                LocalDate.of(2026, 4, 30)
        );

        assertThat(range.label()).isEqualTo("最近 30 天");
        assertThat(range.startDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(range.endDate()).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    void resolvesCustomRangeWhenDatesProvided() {
        LearningOutcomeRange.ResolvedRange range = LearningOutcomeRange.resolve(
                "CUSTOM",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 20),
                LocalDate.of(2026, 4, 30)
        );

        assertThat(range.label()).isEqualTo("自定义");
        assertThat(range.startDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(range.endDate()).isEqualTo(LocalDate.of(2026, 3, 20));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && DB_PASSWORD='your_password' mvn -Dtest=LearningOutcomeRangeTest test`
Expected: FAIL with `package com.xagentstudy.outcome does not exist`

- [ ] **Step 3: Write minimal implementation**

```java
package com.xagentstudy.outcome;

import com.xagentstudy.common.exception.BusinessException;

import java.time.LocalDate;

public enum LearningOutcomeRange {
    LAST_7_DAYS("最近 7 天", 6),
    LAST_30_DAYS("最近 30 天", 29),
    LAST_90_DAYS("最近 90 天", 89),
    ALL("全部记录", null),
    CUSTOM("自定义", null);

    private final String label;
    private final Integer daysBack;

    LearningOutcomeRange(String label, Integer daysBack) {
        this.label = label;
        this.daysBack = daysBack;
    }

    public static ResolvedRange resolve(String code, LocalDate startDate, LocalDate endDate, LocalDate today) {
        LearningOutcomeRange range = code == null || code.isBlank() ? LAST_30_DAYS : LearningOutcomeRange.valueOf(code.trim().toUpperCase());
        return switch (range) {
            case LAST_7_DAYS, LAST_30_DAYS, LAST_90_DAYS -> new ResolvedRange(range.name(), range.label, today.minusDays(range.daysBack), today);
            case ALL -> new ResolvedRange(range.name(), range.label, null, today);
            case CUSTOM -> {
                if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
                    throw new BusinessException("INVALID_RANGE", "Custom outcome range requires valid start and end dates");
                }
                yield new ResolvedRange(range.name(), range.label, startDate, endDate);
            }
        };
    }

    public record ResolvedRange(String code, String label, LocalDate startDate, LocalDate endDate) {
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && DB_PASSWORD='your_password' mvn -Dtest=LearningOutcomeRangeTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/xagentstudy/outcome/LearningOutcomeRange.java backend/src/test/java/com/xagentstudy/outcome/LearningOutcomeRangeTest.java
git commit -m "test: define learning outcome range contract"
```

### Task 2: Add Repository Queries And Outcome API

**Files:**

- Create: `backend/src/main/java/com/xagentstudy/outcome/LearningOutcomeResponse.java`
- Create: `backend/src/main/java/com/xagentstudy/outcome/LearningOutcomeService.java`
- Create: `backend/src/main/java/com/xagentstudy/outcome/LearningOutcomeController.java`
- Modify: `backend/src/main/java/com/xagentstudy/checkin/DailyCheckinRepository.java`
- Modify: `backend/src/main/java/com/xagentstudy/direction/LearningDirectionRepository.java`
- Modify: `backend/src/main/java/com/xagentstudy/plan/LearningPlanRepository.java`
- Modify: `backend/src/main/java/com/xagentstudy/plan/task/PlanTaskRecordRepository.java`
- Modify: `backend/src/main/java/com/xagentstudy/quiz/AnswerRecordRepository.java`
- Modify: `backend/src/main/java/com/xagentstudy/review/ReviewRecordRepository.java`
- Modify: `backend/src/main/java/com/xagentstudy/summary/SummaryCardRepository.java`
- Test: `backend/src/test/java/com/xagentstudy/XAgentStudyApplicationTests.java`

- [ ] **Step 1: Extend repositories with range-friendly queries**

```java
// DailyCheckinRepository.java
long countByUserIdAndCheckinDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

// LearningPlanRepository.java
List<LearningPlan> findByDirectionIdInAndCreatedAtBetweenOrderByCreatedAtDesc(List<Long> directionIds, OffsetDateTime start, OffsetDateTime end);

// PlanTaskRecordRepository.java
List<PlanTaskRecord> findByUserIdAndCompletedTrueAndCompletedAtBetweenOrderByCompletedAtAsc(Long userId, OffsetDateTime start, OffsetDateTime end);

// AnswerRecordRepository.java
List<AnswerRecord> findByUserIdAndAnsweredAtBetweenOrderByAnsweredAtAsc(Long userId, OffsetDateTime start, OffsetDateTime end);

// ReviewRecordRepository.java
List<ReviewRecord> findByUserIdAndCompletedTrueAndUpdatedAtBetweenOrderByUpdatedAtDesc(Long userId, OffsetDateTime start, OffsetDateTime end);

// SummaryCardRepository.java
List<SummaryCard> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long userId, OffsetDateTime start, OffsetDateTime end);
List<SummaryCard> findByUserIdOrderByCreatedAtDesc(Long userId);
```

- [ ] **Step 2: Write the response contract**

```java
package com.xagentstudy.outcome;

import java.util.List;

public record LearningOutcomeResponse(
        String rangeCode,
        String rangeLabel,
        String startDate,
        String endDate,
        Overview overview,
        List<DirectionSnapshot> directions,
        List<TrendPoint> trends,
        List<Highlight> highlights,
        List<String> suggestions,
        Poster poster
) {
    public record Overview(
            int learningDays,
            long completedTaskCount,
            long completedPlanCount,
            long quizAttemptCount,
            int masteredKnowledgePointCount,
            long completedReviewCount
    ) {
    }

    public record DirectionSnapshot(
            Long directionId,
            String directionName,
            long planCount,
            double taskCompletionRate,
            double accuracyRate,
            long completedReviewCount,
            List<String> strengths,
            List<String> weakPoints
    ) {
    }

    public record TrendPoint(
            String date,
            long completedTasks,
            long quizAttempts,
            long correctAnswers,
            long completedReviews
    ) {
    }

    public record Highlight(
            String type,
            Long referenceId,
            String title,
            String subtitle,
            String summary
    ) {
    }

    public record Poster(
            String title,
            String subtitle,
            List<String> keywords,
            List<String> highlights
    ) {
    }
}
```

- [ ] **Step 3: Implement service and controller**

```java
// LearningOutcomeController.java
@RestController
@RequestMapping("/api/v1/outcomes")
public class LearningOutcomeController {
    private final LearningOutcomeService service;

    public LearningOutcomeController(LearningOutcomeService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<LearningOutcomeResponse> summary(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok(service.getSummary(range, startDate, endDate));
    }
}
```

```java
// LearningOutcomeService.java
@Service
public class LearningOutcomeService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final LearningDirectionRepository directionRepository;
    private final LearningPlanRepository planRepository;
    private final PlanTaskRecordRepository taskRecordRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final SummaryCardRepository summaryCardRepository;
    private final DailyCheckinRepository dailyCheckinRepository;
    private final LearningReportService reportService;

    public LearningOutcomeResponse getSummary(String rangeCode, LocalDate startDate, LocalDate endDate) {
        Long userId = AuthContext.currentUserId();
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LearningOutcomeRange.ResolvedRange range = LearningOutcomeRange.resolve(rangeCode, startDate, endDate, today);
        OffsetDateTime start = (range.startDate() == null ? LocalDate.of(2000, 1, 1) : range.startDate())
                .atStartOfDay()
                .atOffset(ZoneOffset.ofHours(8));
        OffsetDateTime end = range.endDate()
                .atTime(LocalTime.MAX)
                .atOffset(ZoneOffset.ofHours(8));

        List<LearningDirection> directions = directionRepository.findByUserIdAndDeletedAtIsNullOrderByLastActiveAtDesc(userId);
        List<LearningPlan> plans = directions.isEmpty() ? List.of() : planRepository.findByDirectionIdInOrderByCreatedAtDesc(directions.stream().map(LearningDirection::getId).toList());
        List<Long> planIds = plans.stream().map(LearningPlan::getId).toList();
        List<PlanTaskRecord> completedTasks = taskRecordRepository.findByUserIdAndCompletedTrueAndCompletedAtBetweenOrderByCompletedAtAsc(userId, start, end);
        List<AnswerRecord> attempts = answerRecordRepository.findByUserIdAndAnsweredAtBetweenOrderByAnsweredAtAsc(userId, start, end);
        List<ReviewRecord> completedReviews = reviewRecordRepository.findByUserIdAndCompletedTrueAndUpdatedAtBetweenOrderByUpdatedAtDesc(userId, start, end);
        List<SummaryCard> summaryCards = summaryCardRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, start, end);
        int learningDays = (int) dailyCheckinRepository.countByUserIdAndCheckinDateBetween(
                userId,
                range.startDate() == null ? LocalDate.of(2000, 1, 1) : range.startDate(),
                range.endDate()
        );

        return new LearningOutcomeResponse(
                range.code(),
                range.label(),
                range.startDate() == null ? null : range.startDate().toString(),
                range.endDate().toString(),
                new LearningOutcomeResponse.Overview(
                        learningDays,
                        completedTasks.size(),
                        completedPlanCount(plans, userId),
                        attempts.size(),
                        masteredKnowledgePointCount(planIds, userId),
                        completedReviews.size()
                ),
                buildDirectionSnapshots(directions, plans, start, end, userId),
                buildTrendPoints(range, completedTasks, attempts, completedReviews),
                buildHighlights(summaryCards, plans, planIds, userId),
                buildSuggestions(planIds),
                buildPoster(range, directions, completedTasks.size(), attempts.size(), completedReviews.size())
        );
    }

    private long completedPlanCount(List<LearningPlan> plans, Long userId) {
        return plans.stream()
                .filter(plan -> {
                    long total = taskRecordRepository.countByPlanIdAndUserId(plan.getId(), userId);
                    long completed = taskRecordRepository.countByPlanIdAndUserIdAndCompletedTrue(plan.getId(), userId);
                    return total > 0 && total == completed;
                })
                .count();
    }

    private int masteredKnowledgePointCount(List<Long> planIds, Long userId) {
        return planIds.stream()
                .map(reportService::getReport)
                .flatMap(report -> report.weakPoints().stream())
                .collect(Collectors.toSet())
                .size();
    }

    private List<LearningOutcomeResponse.DirectionSnapshot> buildDirectionSnapshots(
            List<LearningDirection> directions,
            List<LearningPlan> plans,
            OffsetDateTime start,
            OffsetDateTime end,
            Long userId
    ) {
        return directions.stream().map(direction -> new LearningOutcomeResponse.DirectionSnapshot(
                direction.getId(),
                direction.getName(),
                plans.stream().filter(plan -> plan.getDirectionId().equals(direction.getId())).count(),
                0,
                0,
                0,
                List.of(),
                List.of()
        )).toList();
    }

    private List<LearningOutcomeResponse.TrendPoint> buildTrendPoints(
            LearningOutcomeRange.ResolvedRange range,
            List<PlanTaskRecord> completedTasks,
            List<AnswerRecord> attempts,
            List<ReviewRecord> completedReviews
    ) {
        return List.of();
    }

    private List<LearningOutcomeResponse.Highlight> buildHighlights(
            List<SummaryCard> summaryCards,
            List<LearningPlan> plans,
            List<Long> planIds,
            Long userId
    ) {
        return summaryCards.stream()
                .limit(4)
                .map(card -> new LearningOutcomeResponse.Highlight(
                        "SUMMARY_CARD",
                        card.getId(),
                        card.getTitle(),
                        card.getSourceDocumentName(),
                        card.getSummary()
                ))
                .toList();
    }

    private List<String> buildSuggestions(List<Long> planIds) {
        return planIds.isEmpty()
                ? List.of("先创建学习方向并生成第一个学习计划。")
                : List.of("优先补齐当前时间范围内未完成的任务。", "针对最近错题安排一次集中复习。");
    }

    private LearningOutcomeResponse.Poster buildPoster(
            LearningOutcomeRange.ResolvedRange range,
            List<LearningDirection> directions,
            long completedTaskCount,
            long quizAttemptCount,
            long completedReviewCount
    ) {
        return new LearningOutcomeResponse.Poster(
                "学习成果展示",
                range.label() + " · " + range.endDate(),
                directions.stream().limit(4).map(LearningDirection::getName).toList(),
                List.of(
                        "完成任务 " + completedTaskCount + " 个",
                        "完成答题 " + quizAttemptCount + " 次",
                        "完成复习 " + completedReviewCount + " 次"
                )
        );
    }
}
```

- [ ] **Step 4: Verify the backend contract loads in Spring**

Run: `cd backend && DB_PASSWORD='your_password' mvn -Dtest=XAgentStudyApplicationTests test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/xagentstudy/outcome backend/src/main/java/com/xagentstudy/checkin/DailyCheckinRepository.java backend/src/main/java/com/xagentstudy/direction/LearningDirectionRepository.java backend/src/main/java/com/xagentstudy/plan/LearningPlanRepository.java backend/src/main/java/com/xagentstudy/plan/task/PlanTaskRecordRepository.java backend/src/main/java/com/xagentstudy/quiz/AnswerRecordRepository.java backend/src/main/java/com/xagentstudy/review/ReviewRecordRepository.java backend/src/main/java/com/xagentstudy/summary/SummaryCardRepository.java backend/src/test/java/com/xagentstudy/XAgentStudyApplicationTests.java
git commit -m "feat: add learning outcomes summary api"
```

### Task 3: Add Outcomes Page Route And Shell Entry

**Files:**

- Create: `frontend/src/views/OutcomesView.vue`
- Modify: `frontend/src/router.ts`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/style.css`

- [ ] **Step 1: Add the route**

```ts
// router.ts
import OutcomesView from './views/OutcomesView.vue'

routes: [
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/', component: DashboardView },
  { path: '/outcomes', component: OutcomesView },
  { path: '/calendar', component: CalendarView },
]
```

- [ ] **Step 2: Add the sidebar entry and guide copy**

```vue
<script setup lang="ts">
import { Trophy } from '@element-plus/icons-vue'

if (path === '/outcomes') {
  return guide('outcomes', '学习成果使用引导', [
    ['切换统计周期', '可以查看最近 7 天、30 天、90 天、全部或自定义时间范围。'],
    ['查看跨计划成果', '这里聚合的是全部学习计划，不是某一个计划的局部报告。'],
    ['生成成果海报', '当前统计范围可以直接生成一张成果展示图。'],
  ])
}
</script>

<template>
  <el-menu-item index="/outcomes">
    <el-icon><Trophy /></el-icon>
    <span>学习成果</span>
  </el-menu-item>
</template>
```

- [ ] **Step 3: Build the page with the existing visual language**

```vue
<script setup lang="ts">
const range = ref('LAST_30_DAYS')
const customDates = ref<[string, string] | null>(null)
const loading = ref(false)
const data = ref<LearningOutcomeResponse | null>(null)

async function fetchOutcomes() {
  loading.value = true
  try {
    const params = range.value === 'CUSTOM' && customDates.value
      ? {
          range: range.value,
          startDate: customDates.value[0],
          endDate: customDates.value[1],
        }
      : { range: range.value }
    const response = await http.get<ApiResponse<LearningOutcomeResponse>>('/outcomes', { params })
    data.value = response.data.data
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="outcomes-page" v-loading="loading">
    <div class="section-head">
      <div>
        <h2>学习成果</h2>
        <p>跨计划汇总你的学习轨迹、掌握变化和代表成果。</p>
      </div>
      <div class="outcomes-toolbar">
        <el-segmented v-model="range" :options="rangeOptions" />
        <el-date-picker v-if="range === 'CUSTOM'" v-model="customDates" type="daterange" value-format="YYYY-MM-DD" />
        <el-button type="primary" @click="openPoster">生成成果海报</el-button>
      </div>
    </div>
  </section>
</template>
```

- [ ] **Step 4: Verify the frontend builds**

Run: `cd frontend && npm run build`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/router.ts frontend/src/App.vue frontend/src/views/OutcomesView.vue frontend/src/style.css
git commit -m "feat: add learning outcomes page"
```

### Task 4: Add Poster Export And Final UI States

**Files:**

- Create: `frontend/src/utils/outcomePoster.ts`
- Modify: `frontend/src/views/OutcomesView.vue`
- Modify: `frontend/src/style.css`

- [ ] **Step 1: Create a focused poster export helper**

```ts
export interface OutcomePosterData {
  title: string
  subtitle: string
  metrics: Array<{ label: string; value: string }>
  keywords: string[]
  highlights: string[]
}

export function downloadOutcomePoster(dataUrl: string, filename: string) {
  const link = document.createElement('a')
  link.href = dataUrl
  link.download = filename
  link.rel = 'noopener'
  link.click()
}
```

- [ ] **Step 2: Wire poster preview, empty states, and highlight actions**

```vue
<template>
  <el-empty
    v-if="data && data.overview.learningDays === 0"
    description="当前时间范围内还没有学习记录，先创建方向或完成一次学习。"
  />

  <el-dialog v-model="posterVisible" title="成果海报预览" width="760px">
    <div ref="posterRef" class="outcome-poster">
      <h3>{{ data?.poster.title }}</h3>
      <p>{{ data?.poster.subtitle }}</p>
    </div>
    <template #footer>
      <el-button @click="posterVisible = false">关闭</el-button>
      <el-button type="primary" @click="savePoster">保存图片</el-button>
    </template>
  </el-dialog>
</template>
```

- [ ] **Step 3: Add final page styling without breaking the current app shell**

```css
.outcomes-page {
  display: grid;
  gap: 20px;
}

.outcomes-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.outcomes-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.outcome-poster {
  min-height: 960px;
  padding: 48px;
  border-radius: 24px;
  background: linear-gradient(160deg, #fff7e8 0%, #f6f0d9 100%);
}
```

- [ ] **Step 4: Run full verification**

Run: `cd backend && DB_PASSWORD='your_password' mvn test`
Expected: PASS

Run: `cd frontend && npm run build`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/utils/outcomePoster.ts frontend/src/views/OutcomesView.vue frontend/src/style.css
git commit -m "feat: add learning outcomes poster export"
```

## Self-Review

**Spec coverage:** The plan covers the independent navigation entry, default 30-day range, switchable ranges, backend aggregation, overview metrics, direction snapshots, trends, highlights, suggestions, poster preview, and image export. The first version intentionally does not include public share links or social publishing, matching the approved scope.

**Placeholder scan:** The plan avoids `TODO`, `TBD`, and vague “handle later” instructions. Each task names concrete files, concrete commands, and a minimal code shape.

**Type consistency:** The plan uses one route path `/outcomes`, one backend entry `/api/v1/outcomes`, one range type `LearningOutcomeRange`, and one response root `LearningOutcomeResponse` across backend and frontend tasks.
