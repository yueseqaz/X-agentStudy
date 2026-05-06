<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Aim, Check, Clock, RefreshRight } from '@element-plus/icons-vue'
import { http, type ApiResponse } from '../api/http'

interface PlanSummary {
  id: number
  directionId: number
  directionName: string
  title: string
  status: string
  createdAt: string
}

interface DashboardSummary {
  planCount: number
  documentCount: number
  questionCount: number
  pendingReviewCount: number
}

interface LearningCoach {
  planId: number | null
  planTitle: string | null
  directionName: string | null
  headline: string
  reason: string
  estimatedMinutes: number
  primaryActionLabel: string
  primaryActionPath: string
  nextSteps: string[]
  signals: string[]
}

interface CheckinStats {
  qaCount: number
  answeredQuestionCount: number
  correctAnswerCount: number
  completedTaskCount: number
  uploadedDocumentCount: number
  generatedDocumentCount: number
  onlineMinutes: number
  activeScore: number
}

interface CheckinDay {
  date: string
  checkedIn: boolean
  summary: string
  mood: string
  studyMinutes: number
  canCheckIn: boolean
  stats: CheckinStats
  updatedAt: string | null
}

interface DailyStudyActionItem {
  title: string
  detail: string
  path: string
  primary: boolean
}

interface DailyStudyWorkspace {
  date: string
  coach: LearningCoach
  checkin: CheckinDay
  actionItems: DailyStudyActionItem[]
  totalEstimatedMinutes: number
  completionText: string
}

interface Billing {
  planCode: string
  planName: string
  monthlyPriceCents: number
  monthlyAgentQuota: number
  usedAgentCalls: number
  remainingAgentCalls: number
  storageQuotaMb: number
  usedStorageMb: number
  planQuota: number
  usedPlanCount: number
  periodStart: string
  periodEnd: string | null
}

const router = useRouter()
const loading = ref(false)
const plans = ref<PlanSummary[]>([])
const billing = ref<Billing | null>(null)
const coach = ref<LearningCoach | null>(null)
const workspace = ref<DailyStudyWorkspace | null>(null)
const coachLoading = ref(false)
const checkinSaving = ref(false)
const summary = ref<DashboardSummary>({
  planCount: 0,
  documentCount: 0,
  questionCount: 0,
  pendingReviewCount: 0,
})

const latestPlans = computed(() => plans.value.slice(0, 3))
const agentPercent = computed(() => {
  if (!billing.value || billing.value.monthlyAgentQuota <= 0) {
    return 0
  }
  return Math.min(100, Math.round((billing.value.usedAgentCalls / billing.value.monthlyAgentQuota) * 100))
})

async function fetchPlans() {
  loading.value = true
  try {
    const [plansResponse, summaryResponse, billingResponse] = await Promise.all([
      http.get<ApiResponse<PlanSummary[]>>('/plans'),
      http.get<ApiResponse<DashboardSummary>>('/dashboard/summary'),
      http.get<ApiResponse<Billing>>('/billing/current'),
    ])
    plans.value = plansResponse.data.data
    summary.value = summaryResponse.data.data
    billing.value = billingResponse.data.data
    await refreshWorkspace()
  } finally {
    loading.value = false
  }
}

async function refreshCoach() {
  await refreshWorkspace()
}

async function refreshWorkspace() {
  coachLoading.value = true
  try {
    const response = await http.get<ApiResponse<DailyStudyWorkspace>>('/dashboard/workspace/today')
    workspace.value = response.data.data
    coach.value = response.data.data.coach
  } catch {
    workspace.value = null
    coach.value = {
      planId: null,
      planTitle: null,
      directionName: null,
      headline: '先定一个清晰学习方向',
      reason: '陪跑建议暂时不可用，先从创建学习方向开始也能继续使用。',
      estimatedMinutes: 12,
      primaryActionLabel: '创建学习方向',
      primaryActionPath: '/directions',
      nextSteps: ['创建一个具体学习方向', '完成画像问答', '生成第一份学习计划'],
      signals: ['暂无可用建议'],
    }
  } finally {
    coachLoading.value = false
  }
}

async function completeTodayCheckin() {
  if (!workspace.value || workspace.value.checkin.checkedIn) {
    return
  }
  checkinSaving.value = true
  try {
    const summary = coach.value
      ? `${coach.value.headline}：${coach.value.nextSteps.join('；')}`
      : '完成今日学习。'
    await http.post<ApiResponse<CheckinDay>>(`/checkins/${workspace.value.date}`, {
      summary,
      mood: '稳步推进',
      studyMinutes: workspace.value.totalEstimatedMinutes,
    })
    ElMessage.success('今日打卡已完成')
    await refreshWorkspace()
  } finally {
    checkinSaving.value = false
  }
}

onMounted(fetchPlans)

const steps = [
  { title: '画像', desc: '目标、基础、时间和风险识别' },
  { title: '规划', desc: '阶段路径、周任务和每日任务' },
  { title: '知识库', desc: '资料解析、摘要、切片和检索' },
  { title: '答疑', desc: '基于计划资料的个性化讲解' },
  { title: '测验复习', desc: '题库、错题、薄弱点和报告' },
]
</script>

<template>
  <div>
    <div class="metric-grid">
      <div class="metric">
        <span>学习计划</span>
        <strong>{{ summary.planCount }}</strong>
        <small>来自当前用户计划列表</small>
      </div>
      <div class="metric">
        <span>知识库资料</span>
        <strong>{{ summary.documentCount }}</strong>
        <small>所有计划下的资料</small>
      </div>
      <div class="metric">
        <span>题库题目</span>
        <strong>{{ summary.questionCount }}</strong>
        <small>测验 Agent 已生成</small>
      </div>
      <div class="metric">
        <span>待复习</span>
        <strong>{{ summary.pendingReviewCount }}</strong>
        <small>未完成复习项</small>
      </div>
    </div>

    <div class="page-grid">
      <section class="surface panel-pad">
        <div class="section-head">
          <div>
            <h2>每日学习工作台</h2>
            <p>把陪跑建议、今日行动和打卡状态放在同一个入口。</p>
          </div>
          <el-button :icon="RefreshRight" :loading="coachLoading" @click="refreshCoach">刷新建议</el-button>
        </div>

        <el-skeleton v-if="loading" :rows="5" animated />
        <div v-else-if="coach" class="coach-card">
          <div class="today-workbench">
            <div class="coach-main">
              <div>
                <span class="coach-kicker">{{ coach.directionName || '今日起步' }}</span>
                <h3>{{ coach.headline }}</h3>
                <p>{{ coach.reason }}</p>
              </div>
              <div class="coach-time">
                <el-icon><Clock /></el-icon>
                <strong>{{ workspace?.totalEstimatedMinutes || coach.estimatedMinutes }}</strong>
                <span>分钟</span>
              </div>
            </div>

            <div class="today-checkin-card" :class="{ done: workspace?.checkin.checkedIn }">
              <span>{{ workspace?.completionText || '今日还未打卡' }}</span>
              <strong>{{ workspace?.checkin.studyMinutes || 0 }} 分钟</strong>
              <small>活跃分 {{ workspace?.checkin.stats.activeScore || 0 }}</small>
              <el-button
                :icon="Check"
                type="primary"
                :loading="checkinSaving"
                :disabled="workspace?.checkin.checkedIn"
                @click="completeTodayCheckin"
              >
                {{ workspace?.checkin.checkedIn ? '已完成打卡' : '完成今日打卡' }}
              </el-button>
            </div>
          </div>
          <div class="coach-signals">
            <el-tag v-for="signal in coach.signals" :key="signal">{{ signal }}</el-tag>
          </div>
          <div class="today-action-list">
            <button
              v-for="(item, index) in workspace?.actionItems || []"
              :key="item.title"
              type="button"
              :class="{ primary: item.primary }"
              @click="router.push(item.path)"
            >
              <span>{{ index + 1 }}</span>
              <strong>{{ item.title }}</strong>
              <small>{{ item.detail }}</small>
            </button>
          </div>
          <div class="item-actions">
            <el-button :icon="Aim" type="primary" @click="router.push(coach.primaryActionPath)">
              {{ coach.primaryActionLabel }}
            </el-button>
          </div>
        </div>
      </section>

      <section class="surface panel-pad">
        <div class="section-head">
          <div>
            <h2>最近学习计划</h2>
            <p>保留常用入口，方便直接进入计划、知识库或工作流。</p>
          </div>
          <el-tag type="success">已连接真实计划</el-tag>
        </div>

        <el-empty v-if="!loading && latestPlans.length === 0" description="还没有学习计划，先创建方向并生成计划" />
        <div v-else class="direction-list">
          <div v-for="plan in latestPlans" :key="plan.id" class="direction-item">
            <div>
              <h3>{{ plan.title }}</h3>
              <p>{{ plan.directionName }} · 当前状态：{{ plan.status }}。建议继续补充资料、提问并生成测验。</p>
              <div class="tag-row">
                <el-tag>规划 Agent</el-tag>
                <el-tag type="success">Plan #{{ plan.id }}</el-tag>
                <el-tag type="warning">可继续学习</el-tag>
              </div>
            </div>
            <div class="item-actions">
              <el-button @click="router.push(`/plans/${plan.id}/workflow`)">工作流</el-button>
              <el-button @click="router.push(`/plans/${plan.id}/knowledge`)">知识库</el-button>
              <el-button type="primary" @click="router.push(`/plans/${plan.id}`)">继续学习</el-button>
            </div>
          </div>
        </div>
      </section>

      <aside class="side-stack">
        <section class="surface panel-pad">
          <div class="section-head">
            <div>
              <h2>Agent 协作链路</h2>
              <p>每个 Agent 的输出会沉淀到计划上下文。</p>
            </div>
          </div>
          <div class="roadmap">
            <div class="roadmap-step" v-for="(step, index) in steps" :key="step.title">
              <span class="step-index">{{ index + 1 }}</span>
              <div>
                <strong>{{ step.title }}</strong>
                <p>{{ step.desc }}</p>
              </div>
            </div>
          </div>
        </section>

        <section class="surface panel-pad">
          <div class="section-head">
            <div>
              <h2>额度状态</h2>
              <p>与个人中心一致，来自当前账号订阅额度。</p>
            </div>
            <el-tag v-if="billing" type="success">{{ billing.planName }}</el-tag>
          </div>
          <template v-if="billing">
            <el-progress :percentage="agentPercent" :stroke-width="10" />
            <p style="margin-top: 12px; color: #736d62; font-size: 13px">
              本月 Agent 调用 {{ billing.usedAgentCalls }} / {{ billing.monthlyAgentQuota }} 次
            </p>
            <p style="margin-top: 6px; color: #736d62; font-size: 13px">剩余额度 {{ billing.remainingAgentCalls }} 次</p>
          </template>
          <el-skeleton v-else :rows="2" animated />
        </section>
      </aside>
    </div>
  </div>
</template>
