<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowRight,
  CircleCheck,
  Collection,
  DataAnalysis,
  Document,
  Files,
  QuestionFilled,
  Refresh,
  Tickets,
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
}

interface ReviewSummary {
  pendingCount: number
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

const route = useRoute()
const router = useRouter()
const planId = computed(() => Number(route.params.planId))
const loading = ref(false)
const plan = ref<Plan | null>(null)
const tasks = ref<PlanTask[]>([])
const documents = ref<KnowledgeDocument[]>([])
const report = ref<LearningReport | null>(null)
const reviewSummary = ref<ReviewSummary | null>(null)

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
const activeStageIndex = computed(() => {
  const firstOpenTask = tasks.value.find((task) => !task.completed)
  if (firstOpenTask) {
    return firstOpenTask.stageIndex
  }
  return Math.max(0, Math.min(stages.value.length - 1, plan.value?.currentStageIndex ?? 0))
})
const activeStage = computed(() => stages.value[activeStageIndex.value] || stages.value[0] || null)
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
    const [planResponse, tasksResponse, documentsResponse, reportResponse, reviewResponse] = await Promise.all([
      http.get<ApiResponse<Plan>>(`/plans/${planId.value}`),
      http.get<ApiResponse<PlanTask[]>>(`/plans/${planId.value}/tasks`),
      http.get<ApiResponse<KnowledgeDocument[]>>(`/plans/${planId.value}/documents`),
      http.get<ApiResponse<LearningReport>>(`/plans/${planId.value}/report`),
      http.get<ApiResponse<ReviewSummary>>(`/plans/${planId.value}/reviews/today`),
    ])
    plan.value = planResponse.data.data
    tasks.value = tasksResponse.data.data
    documents.value = documentsResponse.data.data
    report.value = reportResponse.data.data
    reviewSummary.value = reviewResponse.data.data
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
  .workflow-detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
