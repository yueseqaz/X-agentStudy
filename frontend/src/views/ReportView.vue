<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Refresh, Tickets } from '@element-plus/icons-vue'
import { http, type ApiResponse } from '../api/http'
import PlanSelector from '../components/PlanSelector.vue'
import { formatDateTime } from '../utils/format'

interface WrongQuestion {
  questionId: number
  stem: string
  userAnswer: string
  standardAnswer: string
  explanation: string
  knowledgePoints: string[]
  sourceScope: string
  sourceLabel: string
  answeredAt: string
}

interface WeakPointSource {
  knowledgePoint: string
  sourceScope: string
  sourceLabel: string
  wrongCount: number
}

interface LearningReport {
  planId: number
  questionCount: number
  answerAttemptCount: number
  answeredQuestionCount: number
  correctQuestionCount: number
  wrongQuestionCount: number
  accuracyRate: number
  taskCompletionRate: number
  masteryScore: number
  weakPoints: string[]
  weaknessReasons: string[]
  weakPointSources: WeakPointSource[]
  wrongQuestions: WrongQuestion[]
  reviewSuggestions: string[]
  nextActions: string[]
  trend: { date: string; attempts: number; correct: number }[]
}

const props = defineProps<{
  embeddedPlanId?: number | null
}>()

const selectedPlanId = ref<number | null>(null)
const planId = computed(() => props.embeddedPlanId ?? selectedPlanId.value)
const loading = ref(false)
const report = ref<LearningReport | null>(null)

const completionRate = computed(() => {
  if (!report.value || report.value.questionCount === 0) {
    return 0
  }
  return Math.round((report.value.answeredQuestionCount * 1000) / report.value.questionCount) / 10
})

const maxTrendAttempts = computed(() => {
  return Math.max(1, ...(report.value?.trend.map((item) => item.attempts) || [1]))
})

const maxWeakCount = computed(() => {
  return Math.max(1, ...(report.value?.weakPointSources.map((item) => item.wrongCount) || [1]))
})

async function fetchReport() {
  if (!planId.value) {
    return
  }
  loading.value = true
  try {
    const response = await http.get<ApiResponse<LearningReport>>(`/plans/${planId.value}/report`)
    report.value = response.data.data
  } finally {
    loading.value = false
  }
}

function parseAnswer(value: string): string {
  try {
    const parsed = JSON.parse(value) as string[]
    return parsed.join('、')
  } catch {
    return value
  }
}

function handlePlanChange() {
  report.value = null
  fetchReport()
}

watch(() => props.embeddedPlanId, () => handlePlanChange())

onMounted(fetchReport)
</script>

<template>
  <div class="page-grid">
    <section class="surface panel-pad report-main">
      <div class="section-head">
        <div>
          <h2>学习报告</h2>
          <p>基于计划内题库和最近作答结果，生成掌握度、错题和复习优先级。</p>
        </div>
        <div class="topbar-actions">
          <PlanSelector v-if="!embeddedPlanId" v-model="selectedPlanId" @change="handlePlanChange" />
          <el-tag v-else type="info">Plan #{{ embeddedPlanId }}</el-tag>
          <el-button :icon="Refresh" :loading="loading" @click="fetchReport">刷新</el-button>
        </div>
      </div>

      <el-empty v-if="!planId" description="请先创建并生成一个学习计划" />
      <el-skeleton v-else-if="loading && !report" :rows="10" animated />
      <el-empty v-else-if="!report || report.questionCount === 0" description="暂无报告数据，先生成并完成一组测验" />
      <template v-else>
        <div class="metric-grid report-metrics">
          <div class="metric">
            <span>题库总数</span>
            <strong>{{ report.questionCount }}</strong>
            <small>累计作答 {{ report.answerAttemptCount }} 次</small>
          </div>
          <div class="metric">
            <span>完成率</span>
            <strong>{{ completionRate }}%</strong>
            <small>{{ report.answeredQuestionCount }} / {{ report.questionCount }} 已作答</small>
          </div>
          <div class="metric">
            <span>正确率</span>
            <strong>{{ report.accuracyRate }}%</strong>
            <small>{{ report.correctQuestionCount }} 题最新作答正确</small>
          </div>
          <div class="metric danger-metric">
            <span>待复习错题</span>
            <strong>{{ report.wrongQuestionCount }}</strong>
            <small>按最近一次作答统计</small>
          </div>
          <div class="metric">
            <span>任务完成</span>
            <strong>{{ report.taskCompletionRate }}%</strong>
            <small>计划任务真实勾选记录</small>
          </div>
          <div class="metric">
            <span>掌握度</span>
            <strong>{{ report.masteryScore }}</strong>
            <small>任务、正确率、覆盖率综合</small>
          </div>
        </div>

        <div class="report-band">
          <div>
            <span>当前复习策略</span>
            <strong>{{ report.reviewSuggestions[0] }}</strong>
          </div>
          <el-button :icon="Tickets" type="primary" @click="$router.push(embeddedPlanId ? `/plans/${embeddedPlanId}?tab=quiz` : '/directions')">去重做题目</el-button>
        </div>

        <div class="report-charts">
          <section>
            <h3>正确率趋势</h3>
            <div v-if="report.trend.length" class="trend-bars">
              <div v-for="point in report.trend" :key="point.date" class="trend-row">
                <span>{{ point.date.slice(5) }}</span>
                <div>
                  <i :style="{ width: `${(point.correct / maxTrendAttempts) * 100}%` }" />
                  <em :style="{ width: `${(point.attempts / maxTrendAttempts) * 100}%` }" />
                </div>
                <strong>{{ point.correct }}/{{ point.attempts }}</strong>
              </div>
            </div>
            <el-empty v-else description="暂无趋势" />
          </section>

          <section>
            <h3>薄弱点来源分布</h3>
            <div v-if="report.weakPointSources.length" class="weak-bars">
              <div v-for="item in report.weakPointSources" :key="`${item.knowledgePoint}-${item.sourceScope}`">
                <div>
                  <strong>{{ item.knowledgePoint }}</strong>
                  <span>{{ item.sourceLabel }}</span>
                </div>
                <i :style="{ width: `${(item.wrongCount / maxWeakCount) * 100}%` }" />
                <b>{{ item.wrongCount }}</b>
              </div>
            </div>
            <el-empty v-else description="暂无来源分布" />
          </section>
        </div>

        <div class="wrong-list">
          <article v-for="question in report.wrongQuestions" :key="question.questionId" class="wrong-card">
            <div class="quiz-head">
              <div>
                <h3>{{ question.stem }}</h3>
                <div class="tag-row">
                  <el-tag v-for="point in question.knowledgePoints" :key="point" type="warning">
                    {{ point }}
                  </el-tag>
                </div>
              </div>
              <el-tag type="danger">错题</el-tag>
            </div>
            <el-tag class="source-chip" type="info">{{ question.sourceLabel }}</el-tag>
            <el-tag class="source-chip" type="info">{{ formatDateTime(question.answeredAt) }}</el-tag>
            <div class="answer-compare">
              <div>
                <span>你的答案</span>
                <strong>{{ parseAnswer(question.userAnswer) }}</strong>
              </div>
              <div>
                <span>标准答案</span>
                <strong>{{ parseAnswer(question.standardAnswer) }}</strong>
              </div>
            </div>
            <p>{{ question.explanation }}</p>
          </article>
        </div>
      </template>
    </section>

    <aside class="side-stack">
      <section class="surface panel-pad">
        <div class="section-head">
          <div>
            <h2>薄弱点</h2>
            <p>从错题知识标签中自动聚合。</p>
          </div>
        </div>
        <div v-if="report?.weakPoints.length" class="tag-row">
          <el-tag v-for="point in report.weakPoints" :key="point" type="danger">{{ point }}</el-tag>
        </div>
        <el-empty v-else description="暂无薄弱点" />
      </section>

      <section class="surface panel-pad">
        <div class="section-head">
          <div>
            <h2>薄弱点归因</h2>
            <p>结合错题知识标签说明原因。</p>
          </div>
        </div>
        <div v-if="report?.weaknessReasons.length" class="review-list">
          <div v-for="(item, index) in report.weaknessReasons" :key="item" class="review-item">
            <span>{{ index + 1 }}</span>
            <p>{{ item }}</p>
          </div>
        </div>
        <el-empty v-else description="暂无归因" />
      </section>

      <section class="surface panel-pad">
        <div class="section-head">
          <div>
            <h2>下一步行动</h2>
            <p>下一步动作保持短而具体。</p>
          </div>
        </div>
        <div v-if="report?.nextActions.length" class="review-list">
          <div v-for="(item, index) in report.nextActions" :key="item" class="review-item">
            <span>{{ index + 1 }}</span>
            <p>{{ item }}</p>
          </div>
        </div>
        <el-empty v-else description="暂无行动建议" />
      </section>
    </aside>
  </div>
</template>
