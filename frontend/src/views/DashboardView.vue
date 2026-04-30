<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
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
  } finally {
    loading.value = false
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
            <h2>今日学习控制台</h2>
            <p>围绕完整 SaaS 版本组织：计划、资料、问答、测验、复习、报告和订阅额度。</p>
          </div>
          <el-tag type="success">已连接真实计划</el-tag>
        </div>

        <el-skeleton v-if="loading" :rows="5" animated />
        <el-empty v-else-if="latestPlans.length === 0" description="还没有学习计划，先创建方向并生成计划" />
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
