<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Close, Connection, Delete, DocumentAdd, Download, EditPen, Files, Picture, Refresh, View } from '@element-plus/icons-vue'
import { http, type ApiResponse } from '../api/http'
import KnowledgeView from './KnowledgeView.vue'
import QAView from './QAView.vue'
import QuizView from './QuizView.vue'
import ReviewView from './ReviewView.vue'
import ReportView from './ReportView.vue'
import ResourceLibraryView from './ResourceLibraryView.vue'
import FirstRunGuide, { type GuideStep } from '../components/FirstRunGuide.vue'
import { formatDateTime } from '../utils/format'
import {
  buildSummaryCardFilename,
  downloadSummaryCardImage,
  exportSummaryCardPng,
  parseSummaryCardContent,
  type SummaryCardContent,
} from '../utils/summaryCard'

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

interface PlanAdjustment {
  taskCompletionRate: number
  accuracyRate: number
  risks: string[]
  adjustments: string[]
  nextActions: string[]
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

interface GeneratedDocument {
  document: KnowledgeDocument
  markdown: string
}

interface OptionItem {
  key: string
  text: string
}

interface Question {
  id: number
  planId: number
  sourceScope: string
  quizBatchId: string | null
  type: string
  difficulty: string
  stem: string
  options: string
  standardAnswer: string
  explanation: string
  knowledgePoints: string
  sourceLabel: string
  createdAt: string
}

interface AnswerResult {
  id: number
  questionId: number
  userAnswer: string
  correct: boolean
  score: number | null
  feedback: string | null
  answeredAt: string
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

interface SummaryCard {
  id: number
  planId: number
  documentId: number | null
  title: string
  summary: string
  sourceDocumentName: string
  imageData: string | null
  contentJson: string
  templateType: string
  createdAt: string
  updatedAt: string
}

const route = useRoute()
const router = useRouter()
const planId = computed(() => Number(route.params.planId))
const loading = ref(false)
const activeTab = ref(String(route.query.tab || 'details'))
const plan = ref<Plan | null>(null)
const tasks = ref<PlanTask[]>([])
const adjustment = ref<PlanAdjustment | null>(null)
const documents = ref<KnowledgeDocument[]>([])
const report = ref<LearningReport | null>(null)
const reviewSummary = ref<ReviewSummary | null>(null)
const selectedPoint = ref<KnowledgePoint | null>(null)
const selectedChapter = ref<PlanStage | null>(null)
const selectedUnit = ref<PlanUnit | null>(null)
const documentDrawerVisible = ref(false)
const generatingDocument = ref(false)
const loadingActiveDocument = ref(false)
const activeMarkdown = ref('')
const activeGeneratedDocument = ref<KnowledgeDocument | null>(null)
const regeneratingStructure = ref(false)
const documentStyle = ref('PROFESSIONAL')
const generatingPointQuiz = ref(false)
const submittingPointQuiz = ref(false)
const loadingPointQuizHistory = ref(false)
const pointQuestions = ref<Question[]>([])
const pointQuizHistory = ref<Question[]>([])
const pointAnswers = ref<Record<number, string>>({})
const pointResults = ref<Record<number, AnswerResult>>({})
const unansweredQuestionIds = ref<number[]>([])
const quizForm = ref({
  count: 3,
  difficulty: 'MEDIUM',
  questionType: 'SINGLE_CHOICE',
})
const summaryCards = ref<SummaryCard[]>([])
const summarySearch = ref('')
const summaryDocumentFilter = ref<number | null>(null)
const summaryPreviewVisible = ref(false)
const activeSummaryCard = ref<SummaryCard | null>(null)
const summaryExportRef = ref<HTMLElement | null>(null)
const summaryExportContent = ref<SummaryCardContent | null>(null)
const summaryGeneratingDocumentId = ref<number | null>(null)
const summaryRegeneratingCardId = ref<number | null>(null)

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
const planTabGuide = computed(() => {
  const tab = activeTab.value
  const map: Record<string, [string, [string, string][]]> = {
    details: ['计划详情引导', [
      ['查看阶段和任务', '计划详情展示当前学习计划的阶段、任务、知识点和整体进度。'],
      ['使用知识图谱', '点击知识点可以生成在线学习文档，并继续做题和复习。'],
      ['动态调整计划', '系统会根据任务完成和测验表现给出下一步调整建议。'],
    ]],
    knowledge: ['知识库引导', [
      ['上传计划资料', '这里放当前计划专属资料，适合上传课程讲义、笔记和参考文档。'],
      ['查看解析结果', '资料解析后会生成摘要、知识点和切片，后续问答会使用这些内容。'],
      ['生成总结图', '重要文档可以生成总结图，方便复习和保存。'],
    ]],
    resources: ['推荐资源引导', [
      ['查看平台资源', '这里展示管理员上传并与你当前学习方向匹配的资源。'],
      ['理解匹配来源', '方向名称匹配资源学科名，技能分类匹配资源学科范畴，标签补充命中。'],
      ['直接预览学习', '视频、音频、图片和文档都可以在线预览。'],
    ]],
    qa: ['AI 问答引导', [
      ['围绕计划提问', '问题会结合当前计划和知识库资料回答。'],
      ['适合查漏补缺', '不理解的知识点、资料段落和题目解析都可以在这里追问。'],
      ['保留问答记录', '问答内容会沉淀在当前计划下，方便回看。'],
    ]],
    quiz: ['测验题库引导', [
      ['生成练习题', '可以基于当前计划和资料生成题目。'],
      ['提交后看反馈', '答题后会看到正确性、分数和解释。'],
      ['错题进入复习', '答错的内容会帮助形成后续复习清单。'],
    ]],
    summary: ['总结图引导', [
      ['集中查看总结图', '这里展示当前计划下由文档生成的知识总结图。'],
      ['用于复习收藏', '总结图适合快速回顾重点内容。'],
      ['可重新生成', '如果文档内容更新，可以重新生成总结图。'],
    ]],
    review: ['复习清单引导', [
      ['处理待复习项', '这里集中展示当前计划需要复习的内容。'],
      ['完成复习记录', '复习完成后标记状态，系统会更新待复习数量。'],
      ['优先处理薄弱点', '建议先处理错题和低掌握度内容。'],
    ]],
    report: ['学习报告引导', [
      ['查看整体表现', '报告展示题目数量、正确率、掌握度和任务完成情况。'],
      ['定位薄弱点', '通过错题和薄弱点判断下一步学习重点。'],
      ['回到练习改进', '报告中的问题可以回到测验和复习中继续处理。'],
    ]],
  }
  const matched = map[tab] || map.details
  return {
    key: `plan-tab-${tab}`,
    title: matched[0],
    steps: matched[1].map(([title, body]) => ({ title, body })) as GuideStep[],
  }
})

const hasKnowledgeMap = computed(() =>
  stages.value.some((stage) => Array.isArray(stage.units) && stage.units.some((unit) => unit.knowledgePoints?.length)),
)

const renderedMarkdown = computed(() => renderMarkdown(activeMarkdown.value))
const pointAnsweredCount = computed(() => Object.keys(pointResults.value).length)
const pointDraftAnsweredCount = computed(() =>
  pointQuestions.value.filter((question) => {
    const answer = pointAnswers.value[question.id]
    return answer && String(answer).trim()
  }).length,
)
const pointAverageScore = computed(() => {
  const results = Object.values(pointResults.value)
  if (results.length === 0) {
    return 0
  }
  const total = results.reduce((sum, result) => sum + (result.score ?? (result.correct ? 100 : 0)), 0)
  return Math.round(total / results.length)
})
const pointMastered = computed(() =>
  pointQuestions.value.length >= 3
  && pointAnsweredCount.value === pointQuestions.value.length
  && pointAverageScore.value >= 80,
)
const completedTaskCount = computed(() => tasks.value.filter((task) => task.completed).length)
const overallProgress = computed(() =>
  tasks.value.length === 0 ? 0 : Math.round((completedTaskCount.value * 100) / tasks.value.length),
)
const completedDocumentCount = computed(() =>
  documents.value.filter((document) => ['MASTERED', 'COMPLETED'].includes(document.learningStatus || '')).length,
)
const currentMasteryLabel = computed(() => {
  const score = report.value?.masteryScore ?? 0
  if (score >= 85) {
    return '掌握稳定'
  }
  if (score >= 60) {
    return '正在建立'
  }
  return '待加强'
})
const filteredSummaryCards = computed(() => {
  const keyword = summarySearch.value.trim().toLowerCase()
  return summaryCards.value.filter((card) => {
    const matchKeyword = !keyword
      || card.title.toLowerCase().includes(keyword)
      || card.summary.toLowerCase().includes(keyword)
      || (card.sourceDocumentName || '').toLowerCase().includes(keyword)
    const matchDocument = !summaryDocumentFilter.value || card.documentId === summaryDocumentFilter.value
    return matchKeyword && matchDocument
  })
})
const summaryDocumentOptions = computed(() =>
  documents.value.filter((document) => summaryCards.value.some((card) => card.documentId === document.id)),
)

async function fetchPlan() {
  loading.value = true
  try {
    const response = await http.get<ApiResponse<Plan>>(`/plans/${planId.value}`)
    plan.value = response.data.data
    await Promise.all([fetchTasks(), fetchAdjustment(), fetchDocuments(), fetchReport(), fetchReviewSummary(), fetchSummaryCards()])
  } finally {
    loading.value = false
  }
}

async function fetchReport() {
  const response = await http.get<ApiResponse<LearningReport>>(`/plans/${planId.value}/report`)
  report.value = response.data.data
}

async function fetchReviewSummary() {
  const response = await http.get<ApiResponse<ReviewSummary>>(`/plans/${planId.value}/reviews/today`)
  reviewSummary.value = response.data.data
}

async function fetchTasks() {
  const response = await http.get<ApiResponse<PlanTask[]>>(`/plans/${planId.value}/tasks`)
  tasks.value = response.data.data
}

async function fetchAdjustment() {
  const response = await http.get<ApiResponse<PlanAdjustment>>(`/plans/${planId.value}/adjustments`)
  adjustment.value = response.data.data
}

async function fetchDocuments() {
  const response = await http.get<ApiResponse<KnowledgeDocument[]>>(`/plans/${planId.value}/documents`)
  documents.value = response.data.data
}

async function fetchSummaryCards() {
  const response = await http.get<ApiResponse<SummaryCard[]>>(`/plans/${planId.value}/summary-cards`)
  summaryCards.value = response.data.data
}

function taskRecord(stageIndex: number, taskIndex: number) {
  return tasks.value.find((item) => item.stageIndex === stageIndex && item.taskIndex === taskIndex)
}

function pointTaskRecord(point: KnowledgePoint) {
  return tasks.value.find((item) => item.taskText.includes(point.title))
}

async function toggleTask(stageIndex: number, taskIndex: number, completed: boolean) {
  const response = await http.patch<ApiResponse<PlanTask>>(
    `/plans/${planId.value}/tasks/${stageIndex}/${taskIndex}`,
    { completed },
  )
  const updated = response.data.data
  tasks.value = tasks.value.map((item) =>
    item.stageIndex === stageIndex && item.taskIndex === taskIndex ? updated : item,
  )
  fetchAdjustment()
}

async function applyAdjustment() {
  const response = await http.post<ApiResponse<Plan>>(`/plans/${planId.value}/adjustments/apply`, {
    tasks: adjustment.value?.nextActions || [],
  })
  plan.value = response.data.data
  await Promise.all([fetchTasks(), fetchAdjustment()])
}

async function regenerateStructure() {
  if (hasKnowledgeMap.value) {
    await ElMessageBox.confirm(
      '重新生成会覆盖当前章节、单元和知识点结构，已有题库和知识库文档不会删除。确定继续吗？',
      '确认重新生成计划',
      {
        confirmButtonText: '重新生成',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  }
  regeneratingStructure.value = true
  try {
    const response = await http.post<ApiResponse<Plan>>(`/plans/${planId.value}/structure/regenerate`, {}, { timeout: 60000 })
    plan.value = response.data.data
    await fetchTasks()
    ElMessage.success('已生成章节式知识图谱计划')
  } finally {
    regeneratingStructure.value = false
  }
}

async function deletePlan() {
  if (!plan.value) {
    return
  }
  await ElMessageBox.confirm(
    `删除后会同时清理「${plan.value.title}」下的知识库、问答、题库、作答记录、复习清单和任务记录，且无法恢复。确定删除吗？`,
    '删除学习计划',
    {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger',
    },
  )
  await http.delete<ApiResponse<null>>(`/plans/${planId.value}`)
  ElMessage.success('学习计划已删除')
  router.replace('/directions')
}

async function openKnowledgePoint(chapter: PlanStage, unit: PlanUnit, point: KnowledgePoint) {
  documentDrawerVisible.value = false
  await nextTick()
  selectedChapter.value = chapter
  selectedUnit.value = unit
  selectedPoint.value = point
  activeMarkdown.value = ''
  activeGeneratedDocument.value = findGeneratedDocument(point)
  pointQuestions.value = []
  pointQuizHistory.value = []
  pointAnswers.value = {}
  pointResults.value = {}
  unansweredQuestionIds.value = []
  documentDrawerVisible.value = true
  if (activeGeneratedDocument.value) {
    try {
      await loadDocumentContent(activeGeneratedDocument.value.id)
    } catch {
      activeGeneratedDocument.value = null
      activeMarkdown.value = ''
      ElMessage.error('已有学习文档读取失败，请重新生成')
    }
  }
}

async function loadDocumentContent(documentId: number) {
  loadingActiveDocument.value = true
  try {
    const response = await http.get<ApiResponse<{ document: KnowledgeDocument; content: string }>>(
      `/plans/${planId.value}/documents/${documentId}/content`,
    )
    activeGeneratedDocument.value = response.data.data.document
    activeMarkdown.value = response.data.data.content
    await fetchPointQuizHistory()
  } finally {
    loadingActiveDocument.value = false
  }
}

async function generateOnlineDocument() {
  if (!selectedPoint.value || !selectedChapter.value || !selectedUnit.value) {
    return
  }
  generatingDocument.value = true
  try {
    const response = await http.post<ApiResponse<GeneratedDocument>>(
      `/plans/${planId.value}/documents/generate`,
      {
        knowledgePointId: selectedPoint.value.id,
        title: selectedPoint.value.title,
        chapterName: selectedChapter.value.name,
        unitName: selectedUnit.value.name,
        level: selectedPoint.value.level,
        outcome: selectedPoint.value.outcome,
        documentStyle: documentStyle.value,
      },
      { timeout: 90000 },
    )
    activeGeneratedDocument.value = response.data.data.document
    activeMarkdown.value = response.data.data.markdown
    await fetchDocuments()
    await fetchPointQuizHistory()
    ElMessage.success('学习文档已生成，并自动纳入知识库')
  } finally {
    generatingDocument.value = false
  }
}

async function generateSummaryCard(document: KnowledgeDocument) {
  summaryGeneratingDocumentId.value = document.id
  try {
    const response = await http.post<ApiResponse<SummaryCard>>(
      `/plans/${planId.value}/summary-cards/documents/${document.id}/generate`,
      {},
      { timeout: 90000 },
    )
    ElMessage.info('正在渲染并保存总结图')
    const card = await renderAndSaveSummaryCard(response.data.data)
    await fetchSummaryCards()
    activeSummaryCard.value = card
    summaryPreviewVisible.value = true
    ElMessage.success('总结图已生成并归档')
  } finally {
    summaryGeneratingDocumentId.value = null
  }
}

async function regenerateSummaryCard(card: SummaryCard) {
  summaryRegeneratingCardId.value = card.id
  try {
    const response = await http.post<ApiResponse<SummaryCard>>(
      `/plans/${planId.value}/summary-cards/${card.id}/regenerate`,
      {},
      { timeout: 90000 },
    )
    ElMessage.info('正在重新渲染总结图')
    const saved = await renderAndSaveSummaryCard(response.data.data)
    summaryCards.value = summaryCards.value.map((item) => item.id === saved.id ? saved : item)
    activeSummaryCard.value = saved
    summaryPreviewVisible.value = true
    ElMessage.success('总结图已重新生成')
  } finally {
    summaryRegeneratingCardId.value = null
  }
}

async function renderAndSaveSummaryCard(card: SummaryCard) {
  try {
    summaryExportContent.value = parseSummaryCardContent(card.contentJson)
    await nextTick()
    if (!summaryExportRef.value) {
      throw new Error('Summary card template is not ready')
    }
    const imageData = await exportSummaryCardPng(summaryExportRef.value, summaryExportContent.value)
    const response = await http.put<ApiResponse<SummaryCard>>(
      `/plans/${planId.value}/summary-cards/${card.id}/image`,
      { imageData },
    )
    return response.data.data
  } catch (error) {
    ElMessage.error('总结内容已生成，但图片导出失败，请点击重新生成')
    throw error
  } finally {
    summaryExportContent.value = null
  }
}

async function deleteSummaryCard(card: SummaryCard) {
  await ElMessageBox.confirm(`确定删除「${card.title}」吗？`, '删除总结图', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
    confirmButtonClass: 'el-button--danger',
  })
  await http.delete<ApiResponse<null>>(`/plans/${planId.value}/summary-cards/${card.id}`)
  summaryCards.value = summaryCards.value.filter((item) => item.id !== card.id)
  ElMessage.success('总结图已删除')
}

function previewSummaryCard(card: SummaryCard) {
  activeSummaryCard.value = card
  summaryPreviewVisible.value = true
}

function downloadSummaryCard(card: SummaryCard) {
  if (!card.imageData) {
    ElMessage.warning('当前总结图还没有可下载图片，请先重新生成')
    return
  }
  downloadSummaryCardImage(card.imageData, buildSummaryCardFilename(card.title))
  ElMessage.success('总结图已开始下载')
}

function summaryContent(card: SummaryCard | null) {
  return parseSummaryCardContent(card?.contentJson)
}

function documentSummaryCards(documentId: number) {
  return summaryCards.value.filter((card) => card.documentId === documentId)
}

async function fetchPointQuizHistory() {
  if (!activeGeneratedDocument.value) {
    pointQuizHistory.value = []
    return
  }
  loadingPointQuizHistory.value = true
  try {
    const response = await http.get<ApiResponse<Question[]>>(
      `/plans/${planId.value}/documents/${activeGeneratedDocument.value.id}/questions`,
    )
    pointQuizHistory.value = response.data.data
  } finally {
    loadingPointQuizHistory.value = false
  }
}

function loadPointQuizHistory() {
  if (pointQuizHistory.value.length === 0) {
    ElMessage.warning('当前学习文档还没有历史题目')
    return
  }
  const latestBatchId = pointQuizHistory.value.find((question) => question.quizBatchId)?.quizBatchId
  pointQuestions.value = latestBatchId
    ? pointQuizHistory.value.filter((question) => question.quizBatchId === latestBatchId)
    : pointQuizHistory.value
  pointAnswers.value = {}
  pointResults.value = {}
  unansweredQuestionIds.value = []
  ElMessage.success(`已载入最近一批 ${pointQuestions.value.length} 道历史题目`)
}

async function completeSelectedPoint() {
  if (!selectedPoint.value) {
    return
  }
  if (!pointMastered.value) {
    ElMessage.warning('完成学习需要先完成下方测验，并达到平均 80 分以上')
    return
  }
  let record = pointTaskRecord(selectedPoint.value)
  if (!record) {
    await fetchTasks()
    record = pointTaskRecord(selectedPoint.value)
  }
  if (!record) {
    ElMessage.warning('任务记录仍在同步，请稍后再试')
    return
  }
  if (!record.completed) {
    await toggleTask(record.stageIndex, record.taskIndex, true)
  }
  if (selectedChapter.value && chapterCompleted(selectedChapter.value)) {
    ElMessage.success('本章所有知识点已完成，章节已标记完成')
  } else {
    ElMessage.success('该知识点已完成')
  }
}

async function generatePointQuiz() {
  if (!activeGeneratedDocument.value || !selectedPoint.value) {
    ElMessage.warning('请先生成学习文档')
    return
  }
  generatingPointQuiz.value = true
  try {
    const count = Math.max(3, quizForm.value.count || 3)
    const response = await http.post<ApiResponse<Question[]>>(
      `/plans/${planId.value}/quizzes/generate`,
      {
        count,
        difficulty: quizForm.value.difficulty,
        questionType: quizForm.value.questionType,
        documentId: activeGeneratedDocument.value.id,
        knowledgePoint: selectedPoint.value.title,
      },
      { timeout: 70000 },
    )
    pointQuestions.value = response.data.data
    pointAnswers.value = {}
    pointResults.value = {}
    unansweredQuestionIds.value = []
    await fetchPointQuizHistory()
    ElMessage.success(`已根据当前学习文档生成 ${response.data.data.length} 道题，并沉淀到题库`)
  } finally {
    generatingPointQuiz.value = false
  }
}

async function submitPointQuiz() {
  if (pointQuestions.value.length === 0) {
    ElMessage.warning('请先生成或载入题目')
    return
  }
  const unanswered = pointQuestions.value.filter((question) => {
    const answer = pointAnswers.value[question.id]
    return !answer || !String(answer).trim()
  })
  if (unanswered.length > 0) {
    unansweredQuestionIds.value = unanswered.map((question) => question.id)
    ElMessage.warning(`还有 ${unanswered.length} 道题未作答，请返回补齐后再提交`)
    setTimeout(() => {
      document.querySelector('.quiz-card.unanswered')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    })
    return
  }
  unansweredQuestionIds.value = []
  submittingPointQuiz.value = true
  try {
    const responses = await Promise.all(
      pointQuestions.value.map((question) =>
        http.post<ApiResponse<AnswerResult>>(
          `/plans/${planId.value}/questions/${question.id}/answers`,
          { answer: pointAnswers.value[question.id], redoOfQuestionId: null },
          { timeout: 90000 },
        ),
      ),
    )
    pointResults.value = responses.reduce<Record<number, AnswerResult>>((results, response) => {
      results[response.data.data.questionId] = response.data.data
      return results
    }, {})
    const correctCount = Object.values(pointResults.value).filter((result) => result.correct).length
    ElMessage.success(`已提交 ${responses.length} 道题，正确 ${correctCount} 道，平均 ${pointAverageScore.value} 分`)
    if (pointQuestions.value.length >= 3 && pointAverageScore.value >= 80) {
      await completeSelectedPoint()
    }
  } catch {
    ElMessage.error('提交失败，已保留当前作答，请稍后重试')
  } finally {
    submittingPointQuiz.value = false
  }
}

function isUnanswered(question: Question) {
  return unansweredQuestionIds.value.includes(question.id)
}

function clearUnansweredMark(question: Question) {
  if (!isUnanswered(question)) {
    return
  }
  const answer = pointAnswers.value[question.id]
  if (!answer || !String(answer).trim()) {
    return
  }
  unansweredQuestionIds.value = unansweredQuestionIds.value.filter((id) => id !== question.id)
}

function pointQuizDate(question: Question) {
  return formatDateTime(question.createdAt)
}

function findGeneratedDocument(point: KnowledgePoint) {
  return documents.value.find((document) => document.name.includes(point.title)) || null
}

function chapterPoints(chapter: PlanStage) {
  return (chapter.units || []).flatMap((unit) => unit.knowledgePoints || [])
}

function chapterCompleted(chapter: PlanStage) {
  const points = chapterPoints(chapter)
  return points.length > 0 && points.every((point) => pointTaskRecord(point)?.completed)
}

function chapterProgress(chapter: PlanStage) {
  const points = chapterPoints(chapter)
  const completed = points.filter((point) => pointTaskRecord(point)?.completed).length
  return `${completed}/${points.length}`
}

function parseOptions(value: string): OptionItem[] {
  try {
    return JSON.parse(value) as OptionItem[]
  } catch {
    return []
  }
}

function isChoice(question: Question) {
  return question.type === 'SINGLE_CHOICE'
}

function feedbackSummary(result: AnswerResult, question: Question) {
  const value = result.feedback || question.explanation
  try {
    const parsed = JSON.parse(value) as {
      overall?: string
      correctAnswer?: string[]
      suggestions?: string[]
      referenceAnswer?: string
    }
    return [
      parsed.overall,
      parsed.correctAnswer?.length ? `标准答案：${parsed.correctAnswer.join('、')}` : '',
      parsed.referenceAnswer ? `参考思路：${parsed.referenceAnswer}` : '',
      parsed.suggestions?.length ? `建议：${parsed.suggestions.join('；')}` : '',
    ].filter(Boolean).join('\n')
  } catch {
    return value
  }
}

function renderMarkdown(markdown: string) {
  if (!markdown) {
    return ''
  }
  const codeBlocks: string[] = []
  let source = markdown.replace(/\r\n/g, '\n').replace(/```(\w*)\n([\s\S]*?)```/g, (_match, lang, code) => {
    const index = codeBlocks.length
    codeBlocks.push(
      `<pre><code class="language-${escapeHtml(lang || 'text')}">${escapeHtml(code.trim())}</code></pre>`,
    )
    return `@@CODE_BLOCK_${index}@@`
  })

  const lines = source.split('\n')
  const html: string[] = []
  let listOpen = false
  for (const rawLine of lines) {
    const line = rawLine.trim()
    if (!line) {
      if (listOpen) {
        html.push('</ul>')
        listOpen = false
      }
      continue
    }
    if (line.startsWith('@@CODE_BLOCK_')) {
      if (listOpen) {
        html.push('</ul>')
        listOpen = false
      }
      const index = Number(line.match(/@@CODE_BLOCK_(\d+)@@/)?.[1] || 0)
      html.push(codeBlocks[index])
      continue
    }
    const heading = line.match(/^(#{1,6})\s+(.+)$/)
    if (heading) {
      if (listOpen) {
        html.push('</ul>')
        listOpen = false
      }
      const level = Math.min(3, heading[1].length)
      html.push(`<h${level}>${renderInline(heading[2])}</h${level}>`)
      continue
    }
    const bullet = line.match(/^[-*]\s+(.+)$/)
    if (bullet) {
      if (!listOpen) {
        html.push('<ul>')
        listOpen = true
      }
      html.push(`<li>${renderInline(bullet[1])}</li>`)
      continue
    }
    if (listOpen) {
      html.push('</ul>')
      listOpen = false
    }
    html.push(`<p>${renderInline(line)}</p>`)
  }
  if (listOpen) {
    html.push('</ul>')
  }
  return html.join('')
}

function renderInline(value: string) {
  return escapeHtml(value)
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

onMounted(fetchPlan)

watch(
  () => route.query.tab,
  (tab) => {
    if (typeof tab === 'string' && tab) {
      activeTab.value = tab
    }
  },
)
</script>

<template>
  <section class="surface panel-pad">
    <el-skeleton v-if="loading" :rows="8" animated />
    <template v-else-if="plan">
      <el-tabs v-model="activeTab" class="plan-workspace-tabs">
        <el-tab-pane label="计划详情" name="details">
      <div class="section-head">
        <div>
          <h2>{{ plan.title }}</h2>
          <p>{{ plan.directionName || `方向 #${plan.directionId}` }} · {{ plan.directionCategory || '未分类' }} · {{ plan.goal }}</p>
        </div>
        <div class="topbar-actions">
          <el-tag type="success">{{ plan.status }}</el-tag>
          <el-button :icon="Connection" @click="router.push(`/plans/${plan.id}/workflow`)">工作流</el-button>
          <el-button :icon="Files" @click="activeTab = 'knowledge'">知识库</el-button>
          <el-button :icon="Refresh" :loading="regeneratingStructure" type="primary" @click="regenerateStructure">
            {{ hasKnowledgeMap ? '重新生成计划' : '生成章节式计划' }}
          </el-button>
          <el-button :icon="Delete" type="danger" plain @click="deletePlan">删除计划</el-button>
        </div>
      </div>

      <div class="plan-overview-grid">
        <div class="plan-overview-card">
          <span>创建时间</span>
          <strong>{{ formatDateTime(plan.createdAt) }}</strong>
        </div>
        <div class="plan-overview-card">
          <span>总体进度</span>
          <strong>{{ overallProgress }}%</strong>
          <small>{{ completedTaskCount }} / {{ tasks.length }} 任务</small>
        </div>
        <div class="plan-overview-card">
          <span>知识库</span>
          <strong>{{ documents.length }}</strong>
          <small>{{ completedDocumentCount }} 个已掌握/完成</small>
        </div>
        <div class="plan-overview-card">
          <span>待复习</span>
          <strong>{{ reviewSummary?.pendingCount ?? 0 }}</strong>
          <small>按当前计划隔离</small>
        </div>
        <div class="plan-overview-card">
          <span>题库</span>
          <strong>{{ report?.questionCount ?? 0 }}</strong>
          <small>{{ report?.answeredQuestionCount ?? 0 }} 题已作答</small>
        </div>
        <div class="plan-overview-card">
          <span>正确率</span>
          <strong>{{ report?.accuracyRate ?? 0 }}%</strong>
          <small>平均掌握 {{ report?.masteryScore ?? 0 }} · {{ currentMasteryLabel }}</small>
        </div>
      </div>

      <div class="plan-quick-actions">
        <el-button type="primary" @click="activeTab = 'details'">继续学习</el-button>
        <el-button @click="activeTab = 'resources'">推荐资源</el-button>
        <el-button @click="activeTab = 'qa'">去问答</el-button>
        <el-button @click="activeTab = 'quiz'">去做题</el-button>
        <el-button @click="activeTab = 'review'">去复习</el-button>
        <el-button @click="activeTab = 'report'">看报告</el-button>
      </div>

      <div v-if="hasKnowledgeMap" class="knowledge-map">
        <div class="knowledge-map-head">
          <div>
            <h2>章节知识图谱</h2>
            <p>按章节、单元和知识点推进。点击知识点可生成在线学习文档，并自动进入知识库。</p>
          </div>
          <el-tag :icon="Connection">Knowledge Graph</el-tag>
        </div>

        <div class="chapter-map">
          <section v-for="(chapter, chapterIndex) in stages" :key="chapter.name" class="chapter-lane">
            <div class="chapter-meta">
              <span>Chapter {{ chapter.chapterIndex || chapterIndex + 1 }}</span>
              <h3>{{ chapter.name }}</h3>
              <p>{{ chapter.outcome || chapter.focus }}</p>
              <div class="chapter-status">
                <el-tag :type="chapterCompleted(chapter) ? 'success' : 'info'">
                  {{ chapterCompleted(chapter) ? '本章已完成' : `知识点 ${chapterProgress(chapter)}` }}
                </el-tag>
              </div>
            </div>
            <div class="unit-lane">
              <div v-for="unit in chapter.units || []" :key="unit.name" class="unit-node">
                <div class="unit-head">
                  <strong>{{ unit.name }}</strong>
                  <small>{{ unit.goal }}</small>
                </div>
                <div class="point-grid">
                  <button
                    v-for="point in unit.knowledgePoints"
                    :key="point.id"
                    class="point-node"
                    :class="{ completed: pointTaskRecord(point)?.completed }"
                    type="button"
                    @click="openKnowledgePoint(chapter, unit, point)"
                  >
                    <span>{{ point.level }}</span>
                    <strong>{{ point.title }}</strong>
                    <small>{{ point.estimatedMinutes || 45 }} 分钟</small>
                  </button>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>

      <el-alert
        v-else
        style="margin-bottom: 16px"
        type="warning"
        show-icon
        :closable="false"
        title="当前计划仍是旧版阶段结构，建议生成章节式计划后再进入知识点学习。"
      />

      <div class="plan-timeline">
        <div v-for="(stage, index) in stages" :key="stage.name" class="plan-stage">
          <div class="stage-number">{{ index + 1 }}</div>
          <div>
            <div class="stage-head">
              <h3>{{ stage.name }}</h3>
              <el-tag>{{ stage.duration }}</el-tag>
            </div>
            <p>{{ stage.focus }}</p>
            <div class="task-list">
              <label v-for="(task, taskIndex) in stage.tasks || []" :key="task">
                <input
                  type="checkbox"
                  :checked="taskRecord(index, taskIndex)?.completed"
                  @change="toggleTask(index, taskIndex, ($event.target as HTMLInputElement).checked)"
                />
                <span>{{ task }}</span>
              </label>
            </div>
          </div>
        </div>
      </div>

      <div v-if="adjustment" class="adjustment-panel">
        <div class="section-head">
          <div>
            <h2>计划动态调整</h2>
            <p>基于任务完成、测验正确率和薄弱点生成。</p>
          </div>
          <el-tag>{{ adjustment.taskCompletionRate }}% 任务完成</el-tag>
        </div>
        <div class="adjustment-grid">
          <div>
            <span>风险</span>
            <p v-for="item in adjustment.risks" :key="item">{{ item }}</p>
            <p v-if="adjustment.risks.length === 0">暂无明显风险，保持当前节奏。</p>
          </div>
          <div>
            <span>调整建议</span>
            <p v-for="item in adjustment.adjustments" :key="item">{{ item }}</p>
          </div>
          <div>
            <span>下一步</span>
            <p v-for="item in adjustment.nextActions" :key="item">{{ item }}</p>
          </div>
        </div>
        <el-button type="primary" style="margin-top: 14px" @click="applyAdjustment">应用为新阶段</el-button>
      </div>
        </el-tab-pane>
        <el-tab-pane label="知识库" name="knowledge">
          <KnowledgeView :embedded-plan-id="plan.id" @summary-card-generated="fetchSummaryCards" />
        </el-tab-pane>
        <el-tab-pane label="推荐资源" name="resources">
          <ResourceLibraryView :embedded-plan-id="plan.id" />
        </el-tab-pane>
        <el-tab-pane label="AI 问答" name="qa">
          <QAView :embedded-plan-id="plan.id" />
        </el-tab-pane>
        <el-tab-pane label="测验题库" name="quiz">
          <QuizView :embedded-plan-id="plan.id" />
        </el-tab-pane>
        <el-tab-pane label="总结图" name="summary">
          <section class="summary-card-workbench">
            <div class="section-head">
              <div>
                <h2>总结图</h2>
                <p>当前计划下由文档提炼生成的知识总结图，适合收藏、复习和集中查看。</p>
              </div>
              <el-tag>{{ summaryCards.length }} 张</el-tag>
            </div>
            <div class="summary-card-toolbar">
              <el-input v-model="summarySearch" placeholder="搜索标题、摘要或来源文档" clearable />
              <el-select v-model="summaryDocumentFilter" placeholder="按来源文档筛选" clearable>
                <el-option
                  v-for="document in summaryDocumentOptions"
                  :key="document.id"
                  :label="document.name"
                  :value="document.id"
                />
              </el-select>
              <el-button @click="fetchSummaryCards">刷新</el-button>
            </div>
            <el-empty v-if="summaryCards.length === 0" description="还没有总结图，请先在文档详情中生成" />
            <el-empty v-else-if="filteredSummaryCards.length === 0" description="没有匹配的总结图" />
            <div v-else class="summary-card-grid">
              <article v-for="card in filteredSummaryCards" :key="card.id" class="summary-card-item">
                <button class="summary-card-thumb" type="button" @click="previewSummaryCard(card)">
                  <img v-if="card.imageData" :src="card.imageData" :alt="card.title" />
                  <div v-else>
                    <el-icon><Picture /></el-icon>
                    <span>等待生成图片</span>
                  </div>
                </button>
                <div class="summary-card-body">
                  <h3>{{ card.title }}</h3>
                  <p>{{ card.summary }}</p>
                  <div class="tag-row">
                    <el-tag type="info">{{ card.sourceDocumentName || '来源文档' }}</el-tag>
                    <el-tag>{{ formatDateTime(card.createdAt) }}</el-tag>
                  </div>
                  <div class="summary-card-actions">
                    <el-button :icon="View" @click="previewSummaryCard(card)">查看</el-button>
                    <el-button :icon="Download" @click="downloadSummaryCard(card)">下载</el-button>
                    <el-button
                      :icon="Refresh"
                      :loading="summaryRegeneratingCardId === card.id"
                      @click="regenerateSummaryCard(card)"
                    >
                      重新生成
                    </el-button>
                    <el-button :icon="Delete" type="danger" plain @click="deleteSummaryCard(card)">删除</el-button>
                  </div>
                </div>
              </article>
            </div>
          </section>
        </el-tab-pane>
        <el-tab-pane label="复习清单" name="review">
          <ReviewView :embedded-plan-id="plan.id" />
        </el-tab-pane>
        <el-tab-pane label="学习报告" name="report">
          <ReportView :embedded-plan-id="plan.id" />
        </el-tab-pane>
      </el-tabs>
    </template>
    <el-empty v-else description="计划不存在" />

    <aside v-if="documentDrawerVisible" class="learning-side-panel">
      <div class="learning-side-title">
        <div>
          <span class="eyebrow">Knowledge Study</span>
          <h2>知识点学习</h2>
        </div>
        <el-button :icon="Close" circle @click="documentDrawerVisible = false" />
      </div>
      <div v-if="selectedPoint" class="learning-side-body">
        <div class="learning-doc-head">
          <div>
            <span>{{ selectedChapter?.name }} / {{ selectedUnit?.name }}</span>
            <h2>{{ selectedPoint.title }}</h2>
            <p>{{ selectedPoint.outcome }}</p>
          </div>
          <el-tag>{{ selectedPoint.level }}</el-tag>
        </div>

        <div class="learning-doc-actions">
          <el-select v-model="documentStyle" style="width: 132px">
            <el-option label="专业文档" value="PROFESSIONAL" />
            <el-option label="口语讲解" value="CONVERSATIONAL" />
            <el-option label="实战练习" value="PRACTICAL" />
            <el-option label="面试复盘" value="INTERVIEW" />
          </el-select>
          <el-button
            :icon="DocumentAdd"
            type="primary"
            :loading="generatingDocument"
            @click="generateOnlineDocument"
          >
            {{ activeGeneratedDocument ? '重新生成并入库' : '生成学习文档' }}
          </el-button>
          <el-button
            v-if="activeGeneratedDocument"
            :icon="Picture"
            :loading="summaryGeneratingDocumentId === activeGeneratedDocument.id"
            @click="generateSummaryCard(activeGeneratedDocument)"
          >
            生成总结图
          </el-button>
          <el-tag v-if="activeGeneratedDocument" type="success">已纳入知识库 #{{ activeGeneratedDocument.id }}</el-tag>
        </div>

        <el-skeleton v-if="loadingActiveDocument" :rows="8" animated />
        <el-empty v-else-if="!activeMarkdown" description="还没有生成在线学习文档" />
        <template v-else>
          <section v-if="activeGeneratedDocument" class="doc-summary-history">
            <div class="section-head">
              <div>
                <h2>本文档总结图</h2>
                <p>这里显示当前学习文档已生成的总结图，不需要跳转到归档页。</p>
              </div>
              <el-tag>{{ documentSummaryCards(activeGeneratedDocument.id).length }} 张</el-tag>
            </div>
            <el-empty
              v-if="documentSummaryCards(activeGeneratedDocument.id).length === 0"
              description="当前文档还没有总结图"
            />
            <div v-else class="doc-summary-strip">
              <button
                v-for="card in documentSummaryCards(activeGeneratedDocument.id)"
                :key="card.id"
                type="button"
                class="doc-summary-thumb"
                @click="previewSummaryCard(card)"
              >
                <img v-if="card.imageData" :src="card.imageData" :alt="card.title" />
                <span>{{ card.title }}</span>
              </button>
            </div>
          </section>
          <article class="markdown-doc" v-html="renderedMarkdown" />
          <div class="learning-complete-bar">
            <el-button :icon="CircleCheck" type="success" :disabled="!pointMastered" @click="completeSelectedPoint">
              {{ pointMastered ? '完成学习' : '测验掌握后完成' }}
            </el-button>
            <span v-if="selectedChapter">
              本章进度：{{ chapterProgress(selectedChapter) }} · 测验 {{ pointAnsweredCount }}/{{ pointQuestions.length }} · 平均 {{ pointAverageScore }} 分
            </span>
          </div>

          <section class="point-quiz-panel">
            <div class="section-head">
              <div>
                <h2>知识点测验</h2>
                <p>根据当前学习文档生成题目，题目会自动进入题库。</p>
              </div>
            </div>
            <div class="learning-doc-actions">
              <el-select v-model="quizForm.questionType" style="width: 132px">
                <el-option label="选择题" value="SINGLE_CHOICE" />
                <el-option label="填空题" value="FILL_BLANK" />
                <el-option label="主观题" value="SHORT_ANSWER" />
                <el-option label="代码题" value="CODE" />
                <el-option label="案例题" value="CASE_ANALYSIS" />
                <el-option label="面试题" value="INTERVIEW" />
              </el-select>
              <el-select v-model="quizForm.difficulty" style="width: 104px">
                <el-option label="简单" value="EASY" />
                <el-option label="中等" value="MEDIUM" />
                <el-option label="困难" value="HARD" />
              </el-select>
              <el-input-number
                v-model="quizForm.count"
                :min="3"
                :max="20"
                :step="1"
                controls-position="right"
                style="width: 112px"
              />
              <el-button :icon="EditPen" :loading="generatingPointQuiz" type="primary" @click="generatePointQuiz">
                生成 {{ quizForm.count }} 道题
              </el-button>
            </div>
            <div v-if="activeGeneratedDocument" class="point-quiz-history">
              <div>
                <strong>当前文档题目历史</strong>
                <span>{{ pointQuizHistory.length }} 道</span>
              </div>
              <el-button
                :loading="loadingPointQuizHistory"
                :disabled="pointQuizHistory.length === 0"
                @click="loadPointQuizHistory"
              >
                载入最近一批
              </el-button>
            </div>

            <el-empty v-if="pointQuestions.length === 0" description="还没有为该知识点生成题目" />
            <div v-else class="quiz-list compact">
              <article
                v-for="(question, index) in pointQuestions"
                :key="question.id"
                class="quiz-card"
                :class="{ unanswered: isUnanswered(question) }"
              >
                <div class="quiz-head">
                  <div>
                    <h3>{{ index + 1 }}. {{ question.stem }}</h3>
                    <div class="tag-row">
                      <el-tag>{{ question.type }}</el-tag>
                      <el-tag type="warning">{{ question.difficulty }}</el-tag>
                      <el-tag type="info">{{ question.sourceLabel || question.sourceScope }}</el-tag>
                      <el-tag type="info">{{ pointQuizDate(question) }}</el-tag>
                    </div>
                  </div>
                  <el-tag v-if="pointResults[question.id]" :type="pointResults[question.id].correct ? 'success' : 'danger'">
                    {{ pointResults[question.id].correct ? '正确' : '错误' }}
                  </el-tag>
                </div>
                <el-alert
                  v-if="isUnanswered(question)"
                  title="这道题还没有作答"
                  type="warning"
                  :closable="false"
                  show-icon
                />
                <el-radio-group
                  v-if="isChoice(question)"
                  v-model="pointAnswers[question.id]"
                  class="option-list"
                  @change="clearUnansweredMark(question)"
                >
                  <el-radio
                    v-for="option in parseOptions(question.options)"
                    :key="option.key"
                    :value="option.key"
                    border
                  >
                    {{ option.key }}. {{ option.text }}
                  </el-radio>
                </el-radio-group>
                <el-input
                  v-else
                  v-model="pointAnswers[question.id]"
                  type="textarea"
                  :rows="question.type === 'CODE' ? 7 : 4"
                  placeholder="写下你的答案"
                  @input="clearUnansweredMark(question)"
                />
                <el-alert
                  v-if="pointResults[question.id]"
                  :title="feedbackSummary(pointResults[question.id], question)"
                  type="info"
                  :closable="false"
                  show-icon
                />
              </article>
              <div class="quiz-submit-bar">
                <div>
                  <strong>整组提交</strong>
                  <span>已作答 {{ pointDraftAnsweredCount }}/{{ pointQuestions.length }} 道，提交后统一批改并同步掌握度。</span>
                </div>
                <el-button
                  type="primary"
                  :icon="CircleCheck"
                  :loading="submittingPointQuiz"
                  @click="submitPointQuiz"
                >
                  提交全部答案
                </el-button>
              </div>
            </div>
          </section>
        </template>
      </div>
    </aside>

    <aside v-if="summaryPreviewVisible" class="summary-preview-panel">
      <div class="learning-side-title">
        <div>
          <span class="eyebrow">Summary Card</span>
          <h2>总结图预览</h2>
        </div>
        <el-button :icon="Close" circle @click="summaryPreviewVisible = false" />
      </div>
      <div v-if="activeSummaryCard" class="summary-preview-dialog">
        <img v-if="activeSummaryCard.imageData" :src="activeSummaryCard.imageData" :alt="activeSummaryCard.title" />
        <div v-else class="summary-preview-empty">
          <el-icon><Picture /></el-icon>
          <span>图片尚未保存，可点击重新生成。</span>
        </div>
        <h3>{{ activeSummaryCard.title }}</h3>
        <p>{{ activeSummaryCard.summary }}</p>
        <div class="tag-row">
          <el-tag v-for="keyword in summaryContent(activeSummaryCard).keywords" :key="keyword" type="success">
            {{ keyword }}
          </el-tag>
        </div>
        <div class="summary-card-actions">
          <el-button :icon="Download" type="primary" @click="downloadSummaryCard(activeSummaryCard)">下载图片</el-button>
          <el-button
            :icon="Refresh"
            :loading="summaryRegeneratingCardId === activeSummaryCard.id"
            @click="regenerateSummaryCard(activeSummaryCard)"
          >
            重新生成
          </el-button>
        </div>
      </div>
    </aside>

    <div v-if="summaryExportContent" class="summary-export-stage">
      <article ref="summaryExportRef" class="summary-poster">
        <div class="summary-poster-shell">
          <div class="summary-poster-mark">学</div>
          <div>
            <p class="summary-poster-eyebrow">Knowledge Summary</p>
            <h1>{{ summaryExportContent.title }}</h1>
          </div>
          <p class="summary-poster-summary">{{ summaryExportContent.summary }}</p>
          <section class="summary-poster-section">
            <h2>核心要点</h2>
            <div v-for="(item, index) in summaryExportContent.highlights" :key="item" class="summary-poster-highlight">
              <span>{{ index + 1 }}</span>
              <strong>{{ item }}</strong>
            </div>
          </section>
          <section class="summary-poster-section">
            <h2>关键词</h2>
            <div class="summary-poster-tags">
              <span v-for="keyword in summaryExportContent.keywords" :key="keyword">{{ keyword }}</span>
            </div>
          </section>
          <section v-if="summaryExportContent.tips.length" class="summary-poster-section">
            <h2>学习提醒</h2>
            <p v-for="tip in summaryExportContent.tips" :key="tip" class="summary-poster-tip">{{ tip }}</p>
          </section>
          <footer class="summary-poster-source">
            来源文档：{{ summaryExportContent.sourceDocumentName }}<br />
            计划编号：{{ summaryExportContent.planId }}
          </footer>
        </div>
      </article>
    </div>
    <FirstRunGuide
      :guide-key="planTabGuide.key"
      :title="planTabGuide.title"
      :steps="planTabGuide.steps"
    />
  </section>
</template>
