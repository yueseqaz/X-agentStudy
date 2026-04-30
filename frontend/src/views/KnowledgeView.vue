<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, type UploadRequestOptions } from 'element-plus'
import { Delete, DocumentChecked, Download, Picture, UploadFilled } from '@element-plus/icons-vue'
import { http, type ApiResponse } from '../api/http'
import PlanSelector from '../components/PlanSelector.vue'
import { formatDateTime } from '../utils/format'
import {
  buildSummaryCardFilename,
  downloadSummaryCardImage,
  exportSummaryCardPng,
  parseSummaryCardContent,
  type SummaryCardContent,
} from '../utils/summaryCard'

interface KnowledgeDocument {
  id: number
  planId: number
  name: string
  type: string
  parseStatus: string
  summary: string
  sectionsJson: string | null
  keyPointsJson: string | null
  knowledgeTreeJson: string | null
  contentLength: number | null
  parseError: string | null
  learningStatus: string
  uploadedAt: string
}

interface SectionItem {
  order: number
  title: string
  source: string
}

interface AgentTask {
  id: number
  taskType: string
  status: string
  outputPayload: string | null
  errorMessage: string | null
}

interface DocumentUploadResult {
  document: KnowledgeDocument
  task: AgentTask
}

interface KnowledgeChunk {
  id: number
  documentId: number
  chunkIndex: number
  content: string
  sourceLocation: string
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

const props = defineProps<{
  embeddedPlanId?: number | null
}>()
const emit = defineEmits<{
  summaryCardGenerated: []
}>()

const route = useRoute()
const routePlanId = computed(() => {
  if (props.embeddedPlanId) {
    return props.embeddedPlanId
  }
  const value = Number(route.params.planId)
  return Number.isFinite(value) && value > 0 ? value : null
})
const selectedPlanId = ref<number | null>(routePlanId.value)
const planId = computed(() => routePlanId.value ?? selectedPlanId.value)
const loading = ref(false)
const uploading = ref(false)
const documents = ref<KnowledgeDocument[]>([])
const chunks = ref<KnowledgeChunk[]>([])
const latestTask = ref<AgentTask | null>(null)
const pollingTimer = ref<number | null>(null)
const detailVisible = ref(false)
const activeDocument = ref<KnowledgeDocument | null>(null)
const summaryGeneratingDocumentId = ref<number | null>(null)
const summaryExportRef = ref<HTMLElement | null>(null)
const summaryExportContent = ref<SummaryCardContent | null>(null)
const summaryCards = ref<SummaryCard[]>([])
const activeSummaryCard = ref<SummaryCard | null>(null)
const summaryPreviewVisible = ref(false)

async function fetchDocuments() {
  if (!planId.value) {
    return
  }
  loading.value = true
  try {
    const [documentsResponse, chunksResponse] = await Promise.all([
      http.get<ApiResponse<KnowledgeDocument[]>>(`/plans/${planId.value}/documents`),
      http.get<ApiResponse<KnowledgeChunk[]>>(`/plans/${planId.value}/chunks`),
    ])
    documents.value = documentsResponse.data.data
    chunks.value = chunksResponse.data.data
    await fetchSummaryCards()
  } finally {
    loading.value = false
  }
}

async function fetchSummaryCards() {
  if (!planId.value) {
    summaryCards.value = []
    return
  }
  const response = await http.get<ApiResponse<SummaryCard[]>>(`/plans/${planId.value}/summary-cards`)
  summaryCards.value = response.data.data
}

async function uploadFile(options: UploadRequestOptions) {
  if (!planId.value) {
    ElMessage.warning('请先选择学习计划')
    return
  }
  uploading.value = true
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const response = await http.post<ApiResponse<DocumentUploadResult>>(
      `/plans/${planId.value}/documents`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    )
    documents.value.unshift(response.data.data.document)
    latestTask.value = response.data.data.task
    ElMessage.success(`资料已上传，解析任务 #${response.data.data.task.id} 已创建`)
    pollTask(response.data.data.task.id)
    await fetchDocuments()
    options.onSuccess(response.data)
  } catch (error) {
    ElMessage.error('资料上传失败')
    throw error
  } finally {
    uploading.value = false
  }
}

async function pollTask(taskId: number) {
  if (pollingTimer.value) {
    window.clearTimeout(pollingTimer.value)
    pollingTimer.value = null
  }
  const response = await http.get<ApiResponse<AgentTask>>(`/tasks/${taskId}`)
  latestTask.value = response.data.data
  if (['SUCCESS', 'FAILED'].includes(response.data.data.status)) {
    await fetchDocuments()
    if (response.data.data.status === 'SUCCESS') {
      ElMessage.success(`解析任务 #${taskId} 已完成`)
    } else {
      ElMessage.error(response.data.data.errorMessage || '解析任务失败')
    }
    return
  }
  pollingTimer.value = window.setTimeout(() => pollTask(taskId), 1200)
}

async function deleteDocument(document: KnowledgeDocument) {
  if (!planId.value) {
    return
  }
  await http.delete(`/plans/${planId.value}/documents/${document.id}`)
  documents.value = documents.value.filter((item) => item.id !== document.id)
  ElMessage.success('资料已删除')
}

async function generateSummaryCard(document: KnowledgeDocument) {
  if (!planId.value) {
    return
  }
  summaryGeneratingDocumentId.value = document.id
  try {
    const response = await http.post<ApiResponse<SummaryCard>>(
      `/plans/${planId.value}/summary-cards/documents/${document.id}/generate`,
      {},
      { timeout: 90000 },
    )
    ElMessage.info('正在渲染并保存总结图')
    summaryExportContent.value = parseSummaryCardContent(response.data.data.contentJson)
    await nextTick()
    if (!summaryExportRef.value) {
      throw new Error('Summary card template is not ready')
    }
    const imageData = await exportSummaryCardPng(summaryExportRef.value, summaryExportContent.value)
    const saved = await http.put<ApiResponse<SummaryCard>>(
      `/plans/${planId.value}/summary-cards/${response.data.data.id}/image`,
      { imageData },
    )
    summaryExportContent.value = null
    summaryCards.value.unshift(saved.data.data)
    activeSummaryCard.value = saved.data.data
    summaryPreviewVisible.value = true
    emit('summaryCardGenerated')
    ElMessage.success('总结图已生成')
  } catch (error) {
    summaryExportContent.value = null
    ElMessage.error('总结图生成失败，请稍后重试')
    throw error
  } finally {
    summaryGeneratingDocumentId.value = null
  }
}

function documentSummaryCards(documentId: number) {
  return summaryCards.value.filter((card) => card.documentId === documentId)
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

async function openDocumentDetail(document: KnowledgeDocument) {
  if (!planId.value) {
    return
  }
  const response = await http.get<ApiResponse<KnowledgeDocument>>(`/plans/${planId.value}/documents/${document.id}`)
  activeDocument.value = response.data.data
  detailVisible.value = true
}

function parseSections(value?: string | null): SectionItem[] {
  try {
    return JSON.parse(value || '[]') as SectionItem[]
  } catch {
    return []
  }
}

function parseList(value?: string | null): string[] {
  try {
    return JSON.parse(value || '[]') as string[]
  } catch {
    return []
  }
}

function handlePlanChange() {
  documents.value = []
  chunks.value = []
  latestTask.value = null
  fetchDocuments()
}

function learningStatusLabel(status: string) {
  return {
    NOT_STARTED: '未开始',
    LEARNING: '学习中',
    TESTED: '已测验',
    NEEDS_REVIEW: '待复习',
    MASTERED: '已掌握',
    COMPLETED: '已完成',
  }[status] || status
}

function learningStatusType(status: string) {
  return {
    NOT_STARTED: 'info',
    LEARNING: 'warning',
    TESTED: 'primary',
    NEEDS_REVIEW: 'danger',
    MASTERED: 'success',
    COMPLETED: 'success',
  }[status] || 'info'
}

watch(() => props.embeddedPlanId, () => handlePlanChange())

onMounted(() => {
  if (routePlanId.value) {
    fetchDocuments()
  }
})

onBeforeUnmount(() => {
  if (pollingTimer.value) {
    window.clearTimeout(pollingTimer.value)
  }
})
</script>

<template>
  <div class="page-grid">
    <section class="surface panel-pad">
      <div class="section-head">
        <div>
          <h2>计划级知识库</h2>
          <p>所有资料只归属当前计划，后续问答、测验和复习默认使用这里的资料。</p>
        </div>
        <el-tag v-if="routePlanId">Plan #{{ routePlanId }}</el-tag>
        <PlanSelector v-else v-model="selectedPlanId" @change="handlePlanChange" />
      </div>

      <el-empty v-if="!planId" description="请先创建并生成一个学习计划" />
      <el-upload
        v-else
        drag
        :show-file-list="false"
        :http-request="uploadFile"
        :disabled="uploading"
        accept=".pdf,.ppt,.pptx,.md,.markdown,.txt,.docx"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽资料到这里，或点击上传</div>
        <template #tip>
          <div class="el-upload__tip">支持 PDF、PPT、Markdown、TXT、DOCX，当前单文件上限 20MB。</div>
        </template>
      </el-upload>

      <el-alert
        v-if="latestTask"
        style="margin-top: 14px"
        :type="latestTask.status === 'FAILED' ? 'error' : latestTask.status === 'SUCCESS' ? 'success' : 'info'"
        :closable="false"
        show-icon
        :title="`最近解析任务 #${latestTask.id}：${latestTask.taskType} / ${latestTask.status}`"
        :description="latestTask.errorMessage || undefined"
      />
    </section>

    <section class="surface panel-pad">
      <div class="section-head">
        <div>
          <h2>资料列表</h2>
          <p>当前版本先完成上传、状态和摘要占位，下一步接异步解析与切片。</p>
        </div>
        <el-button @click="fetchDocuments">刷新</el-button>
      </div>

      <el-empty v-if="!planId" description="请先选择学习计划" />
      <el-skeleton v-else-if="loading" :rows="5" animated />
      <el-empty v-else-if="documents.length === 0" description="当前计划还没有资料" />
      <div v-else class="document-list">
        <div v-for="document in documents" :key="document.id" class="document-item">
          <div>
            <div class="document-title">
              <h3>{{ document.name }}</h3>
              <el-tag
                :type="
                  document.parseStatus === 'PARSE_SUCCESS'
                    ? 'success'
                    : document.parseStatus === 'PARSE_FAILED'
                      ? 'danger'
                      : 'warning'
                "
              >
                {{ document.parseStatus }}
              </el-tag>
              <el-tag :type="learningStatusType(document.learningStatus)">
                {{ learningStatusLabel(document.learningStatus) }}
              </el-tag>
            </div>
            <p>{{ document.summary }}</p>
            <div class="tag-row">
              <el-tag>{{ document.type }}</el-tag>
              <el-tag v-if="document.contentLength" type="success">{{ document.contentLength }} 字符</el-tag>
              <el-tag type="info">{{ formatDateTime(document.uploadedAt) }}</el-tag>
            </div>
          </div>
          <div class="document-actions">
            <el-button :icon="DocumentChecked" @click="openDocumentDetail(document)">详情</el-button>
            <el-button
              :icon="Picture"
              :loading="summaryGeneratingDocumentId === document.id"
              @click="generateSummaryCard(document)"
            >
              生成总结图
            </el-button>
            <el-button :icon="Delete" circle @click="deleteDocument(document)" />
          </div>
        </div>
      </div>
    </section>

    <section class="surface panel-pad chunk-panel">
      <div class="section-head">
        <div>
          <h2>知识切片</h2>
          <p>TXT/Markdown 会立即解析为切片，供下一步问答检索使用。</p>
        </div>
        <el-tag>{{ chunks.length }} chunks</el-tag>
      </div>

      <el-empty v-if="!planId" description="请先选择学习计划" />
      <el-empty v-else-if="chunks.length === 0" description="还没有可检索切片" />
      <div v-else class="chunk-list">
        <div v-for="chunk in chunks" :key="chunk.id" class="chunk-item">
          <div class="chunk-meta">
            <span>Document #{{ chunk.documentId }}</span>
            <span>{{ chunk.sourceLocation }}</span>
          </div>
          <p>{{ chunk.content }}</p>
        </div>
      </div>
    </section>

    <el-drawer v-model="detailVisible" size="520px" title="文档详情" destroy-on-close>
      <template v-if="activeDocument">
        <div class="doc-detail">
          <div class="doc-detail-head">
            <h2>{{ activeDocument.name }}</h2>
            <el-tag>{{ activeDocument.parseStatus }}</el-tag>
          </div>
          <p>{{ activeDocument.summary }}</p>

          <section>
            <h3>重点知识点</h3>
            <div class="tag-row" v-if="parseList(activeDocument.keyPointsJson).length">
              <el-tag v-for="point in parseList(activeDocument.keyPointsJson)" :key="point" type="success">
                {{ point }}
              </el-tag>
            </div>
            <el-empty v-else description="暂无知识点" />
          </section>

          <section>
            <h3>章节结构</h3>
            <div v-if="parseSections(activeDocument.sectionsJson).length" class="section-list">
              <div v-for="section in parseSections(activeDocument.sectionsJson)" :key="section.order">
                <span>{{ section.order }}</span>
                <div>
                  <strong>{{ section.title }}</strong>
                  <small>{{ section.source }}</small>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无章节结构" />
          </section>

          <section>
            <h3>知识点树</h3>
            <pre>{{ activeDocument.knowledgeTreeJson }}</pre>
          </section>

          <section>
            <h3>总结图</h3>
            <el-button
              :icon="Picture"
              type="primary"
              :loading="summaryGeneratingDocumentId === activeDocument.id"
              @click="generateSummaryCard(activeDocument)"
            >
              生成总结图
            </el-button>
            <div v-if="documentSummaryCards(activeDocument.id).length" class="doc-summary-strip">
              <button
                v-for="card in documentSummaryCards(activeDocument.id)"
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
        </div>
      </template>
    </el-drawer>

    <aside v-if="summaryPreviewVisible" class="summary-preview-panel">
      <div class="learning-side-title">
        <div>
          <span class="eyebrow">Summary Card</span>
          <h2>总结图预览</h2>
        </div>
        <el-button circle @click="summaryPreviewVisible = false">×</el-button>
      </div>
      <div v-if="activeSummaryCard" class="summary-preview-dialog">
        <img v-if="activeSummaryCard.imageData" :src="activeSummaryCard.imageData" :alt="activeSummaryCard.title" />
        <div v-else class="summary-preview-empty">
          <el-icon><Picture /></el-icon>
          <span>图片尚未保存，请重新生成。</span>
        </div>
        <h3>{{ activeSummaryCard.title }}</h3>
        <p>{{ activeSummaryCard.summary }}</p>
        <div class="summary-card-actions">
          <el-button :icon="Download" type="primary" @click="downloadSummaryCard(activeSummaryCard)">下载图片</el-button>
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
  </div>
</template>
