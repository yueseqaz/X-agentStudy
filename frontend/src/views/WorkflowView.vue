<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowRight,
  Aim,
  CircleCheck,
  Collection,
  Connection,
  DataAnalysis,
  Document,
  DocumentAdd,
  Files,
  QuestionFilled,
  Refresh,
  Tickets,
  TrendCharts,
  VideoCamera,
  View,
} from '@element-plus/icons-vue'
import { http, type ApiResponse } from '../api/http'

interface PlanStage {
  name: string
  focus: string
  duration: string
  tasks?: string[]
  chapterIndex?: number
  outcome?: string
  units?: PlanUnit[]
}

interface PlanUnit {
  unitIndex: number
  name: string
  goal: string
  knowledgePoints: KnowledgePoint[]
}

interface KnowledgePoint {
  id: string
  title: string
  level: string
  outcome: string
  estimatedMinutes: number
}

interface Plan {
  id: number
  directionId: number
  directionName: string | null
  directionCategory: string | null
  profileId: number | null
  title: string
  status: string
  goal: string
  stages: string
  currentStageIndex: number
  createdAt: string
}

interface PlanTask {
  id: number
  stageIndex: number
  taskIndex: number
  taskText: string
  completed: boolean
  completedAt: string | null
}

interface KnowledgeDocument {
  id: number
  planId: number
  name: string
  type: string
  parseStatus: string
  summary: string
  learningStatus: string
  uploadedAt: string
}

interface LearningReport {
  questionCount: number
  answerAttemptCount: number
  answeredQuestionCount: number
  correctQuestionCount: number
  wrongQuestionCount: number
  accuracyRate: number
  taskCompletionRate: number
  masteryScore: number
  weakPoints: string[]
}

interface ReviewSummary {
  pendingCount: number
}

interface Profile {
  id: number
  goal: string
  currentLevel: string
  timeBudget: string
  preference: string
  risks: string
  strategy: string
}

interface LearningResource {
  id: number
  title: string
  resourceType: string
  sourceUrl?: string | null
}

interface WorkflowNode {
  id: string
  title: string
  eyebrow: string
  body: string
  status: 'done' | 'active' | 'idle'
  actionLabel: string
  path: string
  icon: typeof Collection
}

interface AgentCard {
  id: string
  name: string
  role: string
  state: 'done' | 'active' | 'idle'
  score: number
  metric: string
  evidence: string[]
  path: string
  actionLabel: string
  icon: typeof Collection
}

interface DynamicProfileDimension {
  name: string
  value: string
  confidence: number
  status: string
  evidence: string[]
}

interface DynamicProfileSignal {
  source: string
  reason: string
  impactLevel: string
}

interface DynamicProfileRecommendation {
  target: string
  reason: string
  evidence: string
}

interface DynamicProfile {
  planId: number
  completenessScore: number
  summary: string
  dimensions: DynamicProfileDimension[]
  updateSignals: DynamicProfileSignal[]
  recommendationReasons: DynamicProfileRecommendation[]
}

interface ResourceQualityItem {
  name: string
  score: number
  status: string
  reason: string
  evidence: string[]
}

interface ResourceQuality {
  planId: number
  overallScore: number
  label: string
  items: ResourceQualityItem[]
  suggestions: string[]
}

const route = useRoute()
const router = useRouter()
const planId = computed(() => Number(route.params.planId))
const loading = ref(false)
const plan = ref<Plan | null>(null)
const tasks = ref<PlanTask[]>([])
const documents = ref<KnowledgeDocument[]>([])
const report = ref<LearningReport | null>(null)
const reviewSummary = ref<ReviewSummary | null>(null)
const profile = ref<Profile | null>(null)
const recommendedResources = ref<LearningResource[]>([])
const dynamicProfile = ref<DynamicProfile | null>(null)
const resourceQuality = ref<ResourceQuality | null>(null)

const stages = computed<PlanStage[]>(() => {
  if (!plan.value?.stages) {
    return []
  }
  try {
    return JSON.parse(plan.value.stages) as PlanStage[]
  } catch {
    return []
  }
})

const completedTaskCount = computed(() => tasks.value.filter((task) => task.completed).length)
const taskProgress = computed(() =>
  tasks.value.length === 0 ? 0 : Math.round((completedTaskCount.value * 100) / tasks.value.length),
)
const knowledgePointCount = computed(() =>
  stages.value.reduce((total, stage) =>
    total + (stage.units || []).reduce((unitTotal, unit) => unitTotal + (unit.knowledgePoints?.length || 0), 0),
  0),
)
const parsedDocumentCount = computed(() =>
  documents.value.filter((document) => document.parseStatus === 'PARSED').length,
)
const completedDocumentCount = computed(() =>
  documents.value.filter((document) => ['MASTERED', 'COMPLETED'].includes(document.learningStatus || '')).length,
)
const externalCourseCount = computed(() =>
  recommendedResources.value.filter((resource) => resource.resourceType === 'EXTERNAL_COURSE').length,
)
const activeStageIndex = computed(() => {
  const firstOpenTask = tasks.value.find((task) => !task.completed)
  if (firstOpenTask) {
    return firstOpenTask.stageIndex
  }
  return Math.max(0, Math.min(stages.value.length - 1, plan.value?.currentStageIndex ?? 0))
})
const activeStage = computed(() => stages.value[activeStageIndex.value] || stages.value[0] || null)
const qualityScore = computed(() => {
  const documentScore = knowledgePointCount.value === 0
    ? (documents.value.length > 0 ? 70 : 0)
    : Math.min(100, Math.round((parsedDocumentCount.value * 100) / knowledgePointCount.value))
  const quizScore = report.value?.questionCount
    ? Math.min(100, Math.round(((report.value.answeredQuestionCount || 0) * 100) / report.value.questionCount))
    : 0
  const resourceScore = recommendedResources.value.length > 0 ? 100 : 0
  const mastery = report.value?.masteryScore ?? 0
  return Math.round((documentScore + quizScore + resourceScore + mastery) / 4)
})
const cockpitReadiness = computed(() => {
  if (agentCards.value.length === 0) {
    return 0
  }
  return Math.round(agentCards.value.reduce((total, agent) => total + agent.score, 0) / agentCards.value.length)
})
const runningAgentCount = computed(() => agentCards.value.filter((agent) => agent.state === 'active').length)
const doneAgentCount = computed(() => agentCards.value.filter((agent) => agent.state === 'done').length)
const coreProfileDimensions = computed(() => dynamicProfile.value?.dimensions.slice(0, 8) || [])
const cockpitSummary = computed(() => {
  if (!profile.value) {
    return '先补齐学习画像，后续资源生成和路径推荐才有个性化依据。'
  }
  if (documents.value.length === 0) {
    return '画像和路径已就绪，下一步应生成或上传知识资料。'
  }
  if ((report.value?.questionCount ?? 0) === 0) {
    return '知识资料已就绪，适合让题库 Agent 生成测验，建立评估闭环。'
  }
  if ((reviewSummary.value?.pendingCount ?? 0) > 0) {
    return '已有待复习项，教练 Agent 建议先处理复习，再推进新内容。'
  }
  return '核心链路已打通，可以继续生成视频、实操案例和更高难度测验。'
})
const agentCards = computed<AgentCard[]>(() => {
  const profileScore = profile.value ? 100 : (plan.value?.profileId ? 70 : 0)
  const pathScore = stages.value.length > 0 ? 100 : 0
  const documentScore = knowledgePointCount.value === 0
    ? (documents.value.length > 0 ? 70 : 0)
    : Math.min(100, Math.round((parsedDocumentCount.value * 100) / knowledgePointCount.value))
  const quizScore = report.value?.questionCount
    ? Math.max(50, Math.round(((report.value.answeredQuestionCount || 0) * 100) / report.value.questionCount))
    : 0
  const videoScore = parsedDocumentCount.value > 0 ? 82 : (knowledgePointCount.value > 0 ? 46 : 0)
  const resourceScore = recommendedResources.value.length > 0 ? 100 : (plan.value?.directionName ? 42 : 0)
  const assessmentScore = report.value ? Math.max(report.value.masteryScore, report.value.questionCount > 0 ? 45 : 0) : 0
  const coachScore = plan.value ? Math.max(55, Math.round((taskProgress.value + (report.value?.masteryScore ?? 0)) / 2)) : 0
  const cardState = (score: number): AgentCard['state'] => score >= 75 ? 'done' : (score > 0 ? 'active' : 'idle')
  const agents: AgentCard[] = [
    {
      id: 'profile-agent',
      name: '画像 Agent',
      role: '抽取目标、基础、偏好和风险，作为个性化入口。',
      state: cardState(profileScore),
      score: profileScore,
      metric: profile.value ? profile.value.currentLevel : '等待画像',
      evidence: [
        profile.value ? `目标：${profile.value.goal}` : '尚未读取到画像',
        profile.value ? `偏好：${profile.value.preference}` : '创建方向后进入画像问答',
        profile.value ? `节奏：${profile.value.timeBudget}` : '至少需要目标、基础、时间三个维度',
      ],
      path: plan.value ? `/directions/${plan.value.directionId}/profile` : '/directions',
      actionLabel: profile.value ? '查看画像' : '补齐画像',
      icon: Aim,
    },
    {
      id: 'path-agent',
      name: '路径 Agent',
      role: '把学习目标拆成阶段、单元和知识点顺序。',
      state: cardState(pathScore),
      score: pathScore,
      metric: `${stages.value.length} 个阶段`,
      evidence: [
        activeStage.value ? `当前阶段：${activeStage.value.name}` : '暂无阶段',
        `${knowledgePointCount.value} 个知识点可承接文档、题库和视频`,
        `任务进度 ${completedTaskCount.value}/${tasks.value.length}`,
      ],
      path: `/plans/${planId.value}`,
      actionLabel: '查看路径',
      icon: Connection,
    },
    {
      id: 'document-agent',
      name: '文档 Agent',
      role: '生成或解析讲解文档，沉淀为知识库。',
      state: cardState(documentScore),
      score: documentScore,
      metric: `${parsedDocumentCount.value}/${documents.value.length} 已解析`,
      evidence: [
        `${documents.value.length} 份资料进入知识库`,
        `${completedDocumentCount.value} 份资料已完成学习`,
        documents.value[0]?.name ? `最近资料：${documents.value[0].name}` : '建议先生成一个知识点讲义',
      ],
      path: `/plans/${planId.value}?tab=knowledge`,
      actionLabel: '打开知识库',
      icon: DocumentAdd,
    },
    {
      id: 'quiz-agent',
      name: '题库 Agent',
      role: '生成不同题型，并把错题送回薄弱点中心。',
      state: cardState(quizScore),
      score: quizScore,
      metric: `${report.value?.questionCount ?? 0} 道题`,
      evidence: [
        `已作答 ${report.value?.answeredQuestionCount ?? 0} 道`,
        `正确率 ${report.value?.accuracyRate ?? 0}%`,
        `错题 ${report.value?.wrongQuestionCount ?? 0} 道`,
      ],
      path: `/plans/${planId.value}?tab=quiz`,
      actionLabel: '进入题库',
      icon: Tickets,
    },
    {
      id: 'video-agent',
      name: '视频 Agent',
      role: '根据知识文档提炼 Remotion 教学视频脚本。',
      state: cardState(videoScore),
      score: videoScore,
      metric: parsedDocumentCount.value > 0 ? '可生成视频' : '等待资料',
      evidence: [
        parsedDocumentCount.value > 0 ? '可从已解析文档提炼分镜' : '需要先有文档内容',
        '支持代码驱动动画和视频预览',
        activeStage.value ? `适配阶段：${activeStage.value.name}` : '暂无阶段上下文',
      ],
      path: `/plans/${planId.value}?tab=knowledge`,
      actionLabel: '生成视频',
      icon: VideoCamera,
    },
    {
      id: 'resource-agent',
      name: '资源 Agent',
      role: '按计划主题推荐课程资源，补充外部学习材料。',
      state: cardState(resourceScore),
      score: resourceScore,
      metric: `${recommendedResources.value.length} 个资源`,
      evidence: [
        `${externalCourseCount.value} 个 Bilibili 课程资源`,
        recommendedResources.value[0]?.title ? `推荐：${recommendedResources.value[0].title}` : '暂无匹配资源',
        plan.value?.directionName ? `匹配主题：${plan.value.directionName}` : '等待计划主题',
      ],
      path: '/resources',
      actionLabel: '查看资源',
      icon: Files,
    },
    {
      id: 'assessment-agent',
      name: '评估 Agent',
      role: '综合任务、题目、复习和薄弱点评估掌握程度。',
      state: cardState(assessmentScore),
      score: assessmentScore,
      metric: `掌握度 ${report.value?.masteryScore ?? 0}`,
      evidence: [
        `任务完成 ${taskProgress.value}%`,
        `待复习 ${reviewSummary.value?.pendingCount ?? 0} 项`,
        `资源质量闭环 ${qualityScore.value}%`,
      ],
      path: `/plans/${planId.value}?tab=report`,
      actionLabel: '查看评估',
      icon: TrendCharts,
    },
  ]
  const activeCount = agents.filter((agent) => agent.state === 'active').length
  const doneCount = agents.filter((agent) => agent.state === 'done').length
  agents.push({
    id: 'coach-agent',
    name: '教练 Agent',
    role: '把各 Agent 的结果汇总成下一步行动。',
    state: cardState(coachScore),
    score: coachScore,
    metric: activeCount > 0 ? `${activeCount} 个待推进` : '节奏稳定',
    evidence: [
      cockpitSummary.value,
      activeStage.value ? `当前聚焦：${activeStage.value.name}` : '等待计划生成',
      `协作完成度 ${doneCount}/${agents.length}`,
    ],
    path: `/plans/${planId.value}`,
    actionLabel: '继续学习',
    icon: View,
  })
  return agents
})
const workflowNodes = computed<WorkflowNode[]>(() => [
  {
    id: 'profile',
    title: '学习目标',
    eyebrow: plan.value?.directionName || '当前方向',
    body: plan.value?.goal || '确认目标、基础和时间安排。',
    status: plan.value ? 'done' : 'idle',
    actionLabel: '查看计划',
    path: `/plans/${planId.value}`,
    icon: Collection,
  },
  {
    id: 'structure',
    title: '阶段路径',
    eyebrow: `${stages.value.length} 个阶段`,
    body: activeStage.value ? `当前建议：${activeStage.value.name}` : '生成章节、单元和知识点。',
    status: stages.value.length > 0 ? 'active' : 'idle',
    actionLabel: '继续学习',
    path: `/plans/${planId.value}`,
    icon: DataAnalysis,
  },
  {
    id: 'knowledge',
    title: '知识库',
    eyebrow: `${documents.value.length} 份资料`,
    body: parsedDocumentCount.value > 0
      ? `${parsedDocumentCount.value} 份资料已解析，${completedDocumentCount.value} 份已完成。`
      : '上传资料或生成知识点文档。',
    status: parsedDocumentCount.value > 0 ? 'done' : 'active',
    actionLabel: '打开知识库',
    path: `/plans/${planId.value}/knowledge`,
    icon: Files,
  },
  {
    id: 'qa',
    title: 'AI 问答',
    eyebrow: '围绕当前计划',
    body: '带着资料和知识点上下文提问，补齐理解缺口。',
    status: documents.value.length > 0 ? 'active' : 'idle',
    actionLabel: '去提问',
    path: `/plans/${planId.value}?tab=qa`,
    icon: QuestionFilled,
  },
  {
    id: 'quiz',
    title: '测验题库',
    eyebrow: `${report.value?.questionCount ?? 0} 道题`,
    body: report.value?.answeredQuestionCount
      ? `已作答 ${report.value.answeredQuestionCount} 道，正确率 ${report.value.accuracyRate}%。`
      : '生成题目后开始测验。',
    status: (report.value?.answeredQuestionCount ?? 0) > 0 ? 'done' : 'active',
    actionLabel: '去做题',
    path: `/plans/${planId.value}?tab=quiz`,
    icon: Tickets,
  },
  {
    id: 'review',
    title: '复习闭环',
    eyebrow: `${reviewSummary.value?.pendingCount ?? 0} 个待复习`,
    body: (reviewSummary.value?.pendingCount ?? 0) > 0
      ? '优先处理待复习内容。'
      : '暂无待复习项，保持当前节奏。',
    status: (reviewSummary.value?.pendingCount ?? 0) === 0 ? 'done' : 'active',
    actionLabel: '去复习',
    path: `/plans/${planId.value}?tab=review`,
    icon: Refresh,
  },
  {
    id: 'report',
    title: '学习报告',
    eyebrow: `掌握度 ${report.value?.masteryScore ?? 0}`,
    body: `任务完成 ${taskProgress.value}%，错题 ${report.value?.wrongQuestionCount ?? 0} 道。`,
    status: (report.value?.masteryScore ?? 0) >= 80 ? 'done' : 'active',
    actionLabel: '看报告',
    path: `/plans/${planId.value}?tab=report`,
    icon: Document,
  },
])

async function fetchWorkflow() {
  loading.value = true
  try {
    const [planResponse, tasksResponse, documentsResponse, reportResponse, reviewResponse, resourcesResponse, dynamicProfileResponse, resourceQualityResponse] = await Promise.all([
      http.get<ApiResponse<Plan>>(`/plans/${planId.value}`),
      http.get<ApiResponse<PlanTask[]>>(`/plans/${planId.value}/tasks`),
      http.get<ApiResponse<KnowledgeDocument[]>>(`/plans/${planId.value}/documents`),
      http.get<ApiResponse<LearningReport>>(`/plans/${planId.value}/report`),
      http.get<ApiResponse<ReviewSummary>>(`/plans/${planId.value}/reviews/today`),
      http.get<ApiResponse<LearningResource[]>>(`/plans/${planId.value}/resources/recommended`),
      http.get<ApiResponse<DynamicProfile>>(`/plans/${planId.value}/dynamic-profile`),
      http.get<ApiResponse<ResourceQuality>>(`/plans/${planId.value}/resource-quality`),
    ])
    plan.value = planResponse.data.data
    tasks.value = tasksResponse.data.data
    documents.value = documentsResponse.data.data
    report.value = reportResponse.data.data
    reviewSummary.value = reviewResponse.data.data
    recommendedResources.value = resourcesResponse.data.data
    dynamicProfile.value = dynamicProfileResponse.data.data
    resourceQuality.value = resourceQualityResponse.data.data
    if (plan.value?.directionId) {
      try {
        const profileResponse = await http.get<ApiResponse<Profile>>(`/directions/${plan.value.directionId}/profile/latest`)
        profile.value = profileResponse.data.data
      } catch {
        profile.value = null
      }
    }
  } finally {
    loading.value = false
  }
}

function openNode(node: WorkflowNode) {
  router.push(node.path)
}

function stageTaskCount(stageIndex: number) {
  return tasks.value.filter((task) => task.stageIndex === stageIndex).length
}

function completedStageTaskCount(stageIndex: number) {
  return tasks.value.filter((task) => task.stageIndex === stageIndex && task.completed).length
}

onMounted(fetchWorkflow)
</script>

<template>
  <section class="workflow-page">
    <el-skeleton v-if="loading" :rows="10" animated />
    <template v-else-if="plan">
      <div class="workflow-hero">
        <div>
          <span>{{ plan.directionName || `方向 #${plan.directionId}` }}</span>
          <h2>{{ plan.title }}</h2>
          <p>{{ plan.goal }}</p>
        </div>
        <div class="workflow-score">
          <small>整体进度</small>
          <strong>{{ taskProgress }}%</strong>
          <el-progress :percentage="taskProgress" :stroke-width="10" />
        </div>
      </div>

      <div class="workflow-metrics">
        <div>
          <span>阶段</span>
          <strong>{{ stages.length }}</strong>
        </div>
        <div>
          <span>知识点</span>
          <strong>{{ knowledgePointCount }}</strong>
        </div>
        <div>
          <span>资料</span>
          <strong>{{ documents.length }}</strong>
        </div>
        <div>
          <span>题目</span>
          <strong>{{ report?.questionCount ?? 0 }}</strong>
        </div>
      </div>

      <section class="agent-cockpit">
        <div class="agent-cockpit-head">
          <div>
            <span>Multi-Agent Cockpit</span>
            <h2>多 Agent 协作驾驶舱</h2>
            <p>{{ cockpitSummary }}</p>
          </div>
          <div class="agent-readiness">
            <small>协作完成度</small>
            <strong>{{ cockpitReadiness }}%</strong>
            <div>
              <b>{{ doneAgentCount }} 个已产出</b>
              <b>{{ runningAgentCount }} 个待推进</b>
            </div>
          </div>
        </div>

        <div class="agent-flow">
          <button
            v-for="agent in agentCards"
            :key="agent.id"
            class="agent-card"
            :class="agent.state"
            type="button"
            @click="router.push(agent.path)"
          >
            <span class="agent-card-top">
              <span class="agent-icon"><el-icon><component :is="agent.icon" /></el-icon></span>
              <span class="agent-state">{{ agent.state === 'done' ? '已产出' : agent.state === 'active' ? '待推进' : '待启动' }}</span>
            </span>
            <strong>{{ agent.name }}</strong>
            <small>{{ agent.role }}</small>
            <span class="agent-meter">
              <i :style="{ width: `${agent.score}%` }" />
            </span>
            <em>{{ agent.metric }}</em>
            <span class="agent-evidence">
              <b v-for="item in agent.evidence" :key="`${agent.id}-${item}`">{{ item }}</b>
            </span>
            <span class="agent-action">
              {{ agent.actionLabel }}
              <el-icon><ArrowRight /></el-icon>
            </span>
          </button>
        </div>
      </section>

      <section v-if="dynamicProfile" class="dynamic-profile-center">
        <div class="dynamic-profile-head">
          <div>
            <span>Adaptive Learner Model</span>
            <h2>动态学习画像中心</h2>
            <p>{{ dynamicProfile.summary }}</p>
          </div>
          <div class="profile-completeness">
            <small>画像完整度</small>
            <strong>{{ dynamicProfile.completenessScore }}%</strong>
            <el-progress :percentage="dynamicProfile.completenessScore" :stroke-width="8" />
          </div>
        </div>

        <div class="profile-dimension-grid">
          <article v-for="dimension in coreProfileDimensions" :key="dimension.name" class="profile-dimension-card">
            <div>
              <span>{{ dimension.name }}</span>
              <el-tag size="small" :type="dimension.status === '稳定' ? 'success' : dimension.status === '观察中' ? 'warning' : 'info'">
                {{ dimension.status }}
              </el-tag>
            </div>
            <strong>{{ dimension.value }}</strong>
            <span class="profile-confidence">
              <i :style="{ width: `${dimension.confidence}%` }" />
            </span>
            <small>{{ dimension.confidence }}% 可信度</small>
            <p v-for="item in dimension.evidence" :key="`${dimension.name}-${item}`">{{ item }}</p>
          </article>
        </div>

        <div class="profile-insight-grid">
          <section>
            <div class="section-head compact">
              <div>
                <h2>画像更新记录</h2>
                <p>根据答题、资料和任务行为自动更新。</p>
              </div>
            </div>
            <div class="profile-signal-list">
              <article v-for="signal in dynamicProfile.updateSignals" :key="`${signal.source}-${signal.reason}`">
                <span>{{ signal.source }}</span>
                <strong>{{ signal.reason }}</strong>
                <em>{{ signal.impactLevel }}影响</em>
              </article>
            </div>
          </section>

          <section>
            <div class="section-head compact">
              <div>
                <h2>个性化依据</h2>
                <p>说明为什么推荐这些资料、题目和学习步骤。</p>
              </div>
            </div>
            <div class="profile-reason-list">
              <article v-for="reason in dynamicProfile.recommendationReasons" :key="`${reason.target}-${reason.reason}`">
                <span>{{ reason.target }}</span>
                <strong>{{ reason.reason }}</strong>
                <p>{{ reason.evidence }}</p>
              </article>
            </div>
          </section>
        </div>
      </section>

      <section v-if="resourceQuality" class="resource-quality-center">
        <div class="resource-quality-head">
          <div>
            <span>Quality Agent</span>
            <h2>资源质量评估 Agent</h2>
            <p>检查当前计划的资源是否覆盖知识点、是否具备多模态形态、是否形成测验闭环。</p>
          </div>
          <div class="resource-quality-score">
            <small>{{ resourceQuality.label }}</small>
            <strong>{{ resourceQuality.overallScore }}%</strong>
          </div>
        </div>
        <div class="resource-quality-grid">
          <article v-for="item in resourceQuality.items" :key="item.name" class="resource-quality-card">
            <div>
              <span>{{ item.name }}</span>
              <el-tag size="small" :type="item.status === '优秀' ? 'success' : item.status === '可用' ? 'warning' : 'info'">
                {{ item.status }}
              </el-tag>
            </div>
            <strong>{{ item.score }}%</strong>
            <i><b :style="{ width: `${item.score}%` }" /></i>
            <p>{{ item.reason }}</p>
            <small v-for="evidence in item.evidence" :key="`${item.name}-${evidence}`">{{ evidence }}</small>
          </article>
        </div>
        <div class="resource-quality-suggestions">
          <strong>补强建议</strong>
          <span v-for="suggestion in resourceQuality.suggestions" :key="suggestion">{{ suggestion }}</span>
        </div>
      </section>

      <div class="workflow-board">
        <button
          v-for="(node, index) in workflowNodes"
          :key="node.id"
          class="workflow-node"
          :class="node.status"
          type="button"
          @click="openNode(node)"
        >
          <span class="node-index">{{ index + 1 }}</span>
          <span class="node-icon"><el-icon><component :is="node.icon" /></el-icon></span>
          <span class="node-copy">
            <small>{{ node.eyebrow }}</small>
            <strong>{{ node.title }}</strong>
            <em>{{ node.body }}</em>
          </span>
          <span class="node-action">
            {{ node.actionLabel }}
            <el-icon><ArrowRight /></el-icon>
          </span>
        </button>
      </div>

      <div class="workflow-detail-grid">
        <section class="surface panel-pad">
          <div class="section-head">
            <div>
              <h2>阶段进度</h2>
              <p>按当前计划的阶段和任务计算。</p>
            </div>
            <el-tag>{{ completedTaskCount }} / {{ tasks.length }} 任务</el-tag>
          </div>
          <div class="workflow-stage-list">
            <article
              v-for="(stage, index) in stages"
              :key="stage.name"
              class="workflow-stage"
              :class="{ current: index === activeStageIndex }"
            >
              <div>
                <span>Stage {{ stage.chapterIndex || index + 1 }}</span>
                <h3>{{ stage.name }}</h3>
                <p>{{ stage.outcome || stage.focus }}</p>
              </div>
              <el-tag :type="completedStageTaskCount(index) === stageTaskCount(index) && stageTaskCount(index) > 0 ? 'success' : 'info'">
                {{ completedStageTaskCount(index) }} / {{ stageTaskCount(index) }}
              </el-tag>
            </article>
          </div>
        </section>

        <section class="surface panel-pad">
          <div class="section-head">
            <div>
              <h2>下一步</h2>
              <p>根据当前资料、任务和测验状态给出建议。</p>
            </div>
          </div>
          <div class="workflow-next">
            <el-icon><CircleCheck /></el-icon>
            <strong>{{ activeStage?.name || '继续完善计划' }}</strong>
            <p v-if="documents.length === 0">先补充知识库资料，后续问答和测验会更准确。</p>
            <p v-else-if="(report?.questionCount ?? 0) === 0">资料已就绪，适合生成一组测验题。</p>
            <p v-else-if="(reviewSummary?.pendingCount ?? 0) > 0">已有待复习内容，先完成复习再推进新任务。</p>
            <p v-else>继续完成当前阶段任务，并用报告检查薄弱点。</p>
            <el-button type="primary" @click="router.push(`/plans/${plan.id}`)">回到学习计划</el-button>
          </div>
        </section>
      </div>
    </template>
    <el-empty v-else description="没有找到学习计划" />
  </section>
</template>

<style scoped>
.workflow-page {
  display: grid;
  gap: 18px;
}

.workflow-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 0.32fr);
  gap: 18px;
  align-items: stretch;
  padding: 22px;
  border: 1px solid #ded8cc;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(255, 253, 248, 0.96), rgba(242, 247, 243, 0.92)),
    linear-gradient(90deg, rgba(31, 107, 84, 0.14) 1px, transparent 1px);
  background-size: auto, 34px 34px;
  box-shadow: 0 18px 50px rgba(54, 48, 39, 0.08);
}

.workflow-hero span,
.workflow-score small,
.workflow-metrics span,
.workflow-stage span {
  color: #8a5b13;
  font-size: 12px;
  font-weight: 900;
}

.workflow-hero h2 {
  margin: 8px 0;
  color: #202124;
  font-size: 28px;
  line-height: 1.2;
}

.workflow-hero p,
.workflow-stage p,
.workflow-next p {
  color: #5e584e;
  line-height: 1.65;
}

.workflow-score {
  display: grid;
  gap: 10px;
  align-content: center;
  padding: 16px;
  border: 1px solid rgba(31, 107, 84, 0.28);
  border-radius: 8px;
  background: #fffdf8;
}

.workflow-score strong {
  color: #1f6b54;
  font-size: 38px;
  line-height: 1;
}

.workflow-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.workflow-metrics > div {
  display: grid;
  gap: 8px;
  min-height: 100px;
  padding: 16px;
  border: 1px solid #ded8cc;
  border-radius: 8px;
  background: #fffdf8;
}

.workflow-metrics strong {
  color: #202124;
  font-size: 30px;
}

.agent-cockpit {
  position: relative;
  display: grid;
  gap: 18px;
  padding: 20px;
  overflow: hidden;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--surface-solid) 94%, transparent), color-mix(in srgb, var(--surface-muted) 88%, transparent)),
    repeating-linear-gradient(90deg, transparent 0 38px, color-mix(in srgb, var(--border) 32%, transparent) 38px 39px);
  box-shadow: 0 24px 60px var(--shadow);
}

.agent-cockpit::before {
  content: '';
  position: absolute;
  inset: 20px auto auto 20px;
  width: 62px;
  height: 2px;
  background: var(--accent);
  box-shadow:
    86px 0 0 color-mix(in srgb, var(--accent) 42%, transparent),
    172px 0 0 color-mix(in srgb, var(--accent) 18%, transparent);
}

.agent-cockpit-head {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(210px, 0.24fr);
  gap: 18px;
  align-items: end;
  padding-top: 14px;
}

.agent-cockpit-head span {
  color: var(--muted);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.agent-cockpit-head h2 {
  margin: 8px 0;
  color: var(--text);
  font-size: 28px;
  line-height: 1.12;
}

.agent-cockpit-head p {
  max-width: 760px;
  margin: 0;
  color: var(--text-soft);
  line-height: 1.7;
}

.agent-readiness {
  display: grid;
  gap: 9px;
  justify-items: end;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--surface-raised) 88%, transparent);
}

.agent-readiness small {
  color: var(--muted);
  font-weight: 900;
}

.agent-readiness strong {
  color: var(--text);
  font-size: 42px;
  line-height: 1;
}

.agent-readiness div {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.agent-readiness b {
  padding: 5px 8px;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--text-soft);
  font-size: 12px;
}

.agent-flow {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 12px;
}

.agent-card {
  position: relative;
  display: grid;
  gap: 10px;
  min-height: 270px;
  padding: 15px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--surface-solid) 92%, transparent);
  color: var(--text);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease,
    background 0.18s ease;
}

.agent-card::after {
  content: '';
  position: absolute;
  inset: auto 14px 14px auto;
  width: 28px;
  height: 28px;
  border-right: 1px solid color-mix(in srgb, var(--accent) 42%, transparent);
  border-bottom: 1px solid color-mix(in srgb, var(--accent) 42%, transparent);
  opacity: 0.6;
}

.agent-card:hover {
  border-color: var(--border-strong);
  box-shadow: 0 18px 42px var(--shadow);
  transform: translateY(-3px);
}

.agent-card.done {
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--success) 10%, transparent), transparent 46%),
    var(--surface-solid);
}

.agent-card.active {
  border-color: color-mix(in srgb, var(--warning) 58%, var(--border));
}

.agent-card.idle {
  opacity: 0.72;
}

.agent-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.agent-icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--accent);
  color: var(--inverse);
  font-size: 18px;
}

.agent-state {
  padding: 5px 8px;
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--text-soft);
  font-size: 12px;
  font-weight: 900;
}

.agent-card strong {
  color: var(--text);
  font-size: 18px;
  line-height: 1.3;
}

.agent-card small {
  color: var(--text-soft);
  line-height: 1.55;
}

.agent-meter {
  display: block;
  height: 7px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface-muted);
}

.agent-meter i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--accent);
  transition: width 0.28s ease;
}

.agent-card em {
  color: var(--text);
  font-style: normal;
  font-weight: 900;
}

.agent-evidence {
  display: grid;
  gap: 7px;
}

.agent-evidence b {
  position: relative;
  padding-left: 14px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.45;
}

.agent-evidence b::before {
  content: '';
  position: absolute;
  top: 0.6em;
  left: 0;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--accent);
}

.agent-action {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  align-self: end;
  color: var(--text);
  font-size: 13px;
  font-weight: 900;
}

.dynamic-profile-center {
  display: grid;
  gap: 18px;
  padding: 20px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--surface-raised) 96%, transparent), color-mix(in srgb, var(--surface-solid) 92%, transparent)),
    repeating-linear-gradient(0deg, transparent 0 31px, color-mix(in srgb, var(--border) 26%, transparent) 31px 32px);
  box-shadow: 0 20px 48px var(--shadow);
}

.dynamic-profile-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 0.26fr);
  gap: 18px;
  align-items: end;
}

.dynamic-profile-head span {
  color: var(--muted);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.dynamic-profile-head h2 {
  margin: 8px 0;
  color: var(--text);
  font-size: 26px;
}

.dynamic-profile-head p {
  margin: 0;
  color: var(--text-soft);
  line-height: 1.7;
}

.profile-completeness {
  display: grid;
  gap: 9px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface-solid);
}

.profile-completeness small,
.profile-dimension-card small {
  color: var(--muted);
  font-weight: 900;
}

.profile-completeness strong {
  color: var(--text);
  font-size: 38px;
  line-height: 1;
}

.profile-dimension-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.profile-dimension-card {
  display: grid;
  gap: 9px;
  min-height: 210px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--surface-solid) 92%, transparent);
}

.profile-dimension-card > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.profile-dimension-card span {
  color: var(--muted);
  font-size: 12px;
  font-weight: 900;
}

.profile-dimension-card strong {
  color: var(--text);
  font-size: 16px;
  line-height: 1.45;
}

.profile-confidence {
  display: block;
  height: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--surface-muted);
}

.profile-confidence i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--accent);
}

.profile-dimension-card p {
  margin: 0;
  color: var(--text-soft);
  font-size: 12px;
  line-height: 1.5;
}

.profile-insight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.profile-signal-list,
.profile-reason-list {
  display: grid;
  gap: 10px;
}

.profile-signal-list article,
.profile-reason-list article {
  display: grid;
  gap: 6px;
  padding: 13px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface-solid);
}

.profile-signal-list span,
.profile-reason-list span {
  color: var(--muted);
  font-size: 12px;
  font-weight: 900;
}

.profile-signal-list strong,
.profile-reason-list strong {
  color: var(--text);
  font-size: 15px;
  line-height: 1.45;
}

.profile-signal-list em,
.profile-reason-list p {
  margin: 0;
  color: var(--text-soft);
  font-size: 13px;
  font-style: normal;
  line-height: 1.55;
}

.resource-quality-center {
  display: grid;
  gap: 16px;
  padding: 20px;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--surface-solid) 94%, transparent), color-mix(in srgb, var(--surface-muted) 86%, transparent)),
    repeating-linear-gradient(90deg, transparent 0 42px, color-mix(in srgb, var(--border) 28%, transparent) 42px 43px);
  box-shadow: 0 20px 48px var(--shadow);
}

.resource-quality-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(180px, 0.22fr);
  gap: 18px;
  align-items: end;
}

.resource-quality-head span,
.resource-quality-score small {
  color: var(--muted);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.resource-quality-head h2 {
  margin: 8px 0;
  color: var(--text);
  font-size: 26px;
}

.resource-quality-head p {
  margin: 0;
  color: var(--text-soft);
  line-height: 1.7;
}

.resource-quality-score {
  display: grid;
  gap: 8px;
  justify-items: end;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface-solid);
}

.resource-quality-score strong {
  color: var(--text);
  font-size: 40px;
  line-height: 1;
}

.resource-quality-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.resource-quality-card {
  display: grid;
  gap: 9px;
  min-height: 210px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface-solid);
}

.resource-quality-card > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.resource-quality-card span,
.resource-quality-card small {
  color: var(--muted);
  font-size: 12px;
  font-weight: 900;
}

.resource-quality-card strong {
  color: var(--text);
  font-size: 30px;
  line-height: 1;
}

.resource-quality-card i {
  display: block;
  height: 7px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--surface-muted);
}

.resource-quality-card b {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--accent);
}

.resource-quality-card p {
  margin: 0;
  color: var(--text-soft);
  font-size: 13px;
  line-height: 1.55;
}

.resource-quality-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.resource-quality-suggestions strong,
.resource-quality-suggestions span {
  padding: 7px 10px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface-solid);
  color: var(--text-soft);
  font-size: 12px;
  font-weight: 900;
}

.resource-quality-suggestions strong {
  border-color: var(--border-strong);
  color: var(--text);
}

.workflow-board {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 14px;
}

.workflow-node {
  position: relative;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  min-height: 180px;
  padding: 16px;
  border: 1px solid #d8d0c1;
  border-radius: 8px;
  background: #fffdf8;
  color: #202124;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    transform 0.16s ease,
    box-shadow 0.16s ease;
}

.workflow-node:hover {
  border-color: #1f6b54;
  box-shadow: 0 16px 32px rgba(31, 107, 84, 0.12);
  transform: translateY(-2px);
}

.workflow-node.done {
  background: #f3f8f5;
}

.workflow-node.active {
  border-color: #1f6b54;
}

.node-index {
  position: absolute;
  top: 12px;
  right: 14px;
  color: #c4b9a8;
  font-size: 28px;
  font-weight: 900;
}

.node-icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 8px;
  background: #202124;
  color: #fffdf8;
  font-size: 19px;
}

.node-copy {
  display: grid;
  gap: 7px;
  padding-right: 18px;
}

.node-copy small {
  color: #8a5b13;
  font-size: 12px;
  font-weight: 900;
}

.node-copy strong {
  color: #202124;
  font-size: 18px;
}

.node-copy em {
  color: #5e584e;
  font-size: 13px;
  font-style: normal;
  line-height: 1.55;
}

.node-action {
  grid-column: 1 / -1;
  display: inline-flex;
  gap: 6px;
  align-items: center;
  align-self: end;
  color: #1f6b54;
  font-size: 13px;
  font-weight: 900;
}

.workflow-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(300px, 0.85fr);
  gap: 18px;
  align-items: start;
}

.workflow-stage-list {
  display: grid;
  gap: 10px;
}

.workflow-stage {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
  padding: 14px;
  border: 1px solid #ded8cc;
  border-radius: 8px;
  background: #fffdf8;
}

.workflow-stage.current {
  border-color: #1f6b54;
  background: #f3f8f5;
}

.workflow-stage h3 {
  margin: 5px 0;
  color: #202124;
  font-size: 16px;
}

.workflow-next {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid #d9e2dd;
  border-radius: 8px;
  background: #f7faf8;
}

.workflow-next .el-icon {
  color: #1f6b54;
  font-size: 26px;
}

.workflow-next strong {
  color: #202124;
  font-size: 18px;
}

@media (max-width: 980px) {
  .workflow-hero,
  .workflow-metrics,
  .agent-cockpit-head,
  .dynamic-profile-head,
  .profile-insight-grid,
  .resource-quality-head,
  .resource-quality-grid,
  .workflow-detail-grid {
    grid-template-columns: 1fr;
  }

  .agent-readiness {
    justify-items: start;
  }

  .agent-readiness div {
    justify-content: flex-start;
  }
}
</style>
