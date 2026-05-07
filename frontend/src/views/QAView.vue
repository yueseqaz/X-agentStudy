<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ChatLineRound, Connection } from '@element-plus/icons-vue'
import { http, type ApiResponse } from '../api/http'
import MarkdownContent from '../components/MarkdownContent.vue'
import PlanSelector from '../components/PlanSelector.vue'
import { formatDateTime } from '../utils/format'
import { playTypewriter } from '../utils/typewriter'

interface QARecord {
  id: number
  planId: number
  question: string
  answer: string
  citations: string
  relatedPoints: string
  createdAt: string
}

interface Citation {
  documentId: number
  documentName: string
  chunkId: number
  sourceLocation: string
  excerpt: string
}

const props = defineProps<{
  embeddedPlanId?: number | null
}>()

const selectedPlanId = ref<number | null>(null)
const planId = computed(() => props.embeddedPlanId ?? selectedPlanId.value)
const question = ref('')
const loading = ref(false)
const history = ref<QARecord[]>([])
const displayAnswers = ref<Record<number, string>>({})
const activeTypewriters = new Map<number, () => void>()

const latest = computed(() => history.value[0])

async function fetchHistory() {
  if (!planId.value) {
    return
  }
  const response = await http.get<ApiResponse<QARecord[]>>(`/plans/${planId.value}/qa`)
  history.value = response.data.data
  displayAnswers.value = Object.fromEntries(history.value.map((record) => [record.id, record.answer]))
}

async function ask() {
  if (!planId.value || !question.value.trim()) {
    return
  }
  loading.value = true
  try {
    const response = await http.post<ApiResponse<QARecord>>(`/plans/${planId.value}/qa`, {
      question: question.value,
    })
    history.value.unshift(response.data.data)
    animateAnswer(response.data.data)
    question.value = ''
  } finally {
    loading.value = false
  }
}

function parseCitations(value: string): Citation[] {
  try {
    return JSON.parse(value) as Citation[]
  } catch {
    return []
  }
}

function parseList(value: string): string[] {
  try {
    return JSON.parse(value) as string[]
  } catch {
    return []
  }
}

function handlePlanChange() {
  stopTypewriters()
  history.value = []
  displayAnswers.value = {}
  fetchHistory()
}

watch(() => props.embeddedPlanId, () => handlePlanChange())

onMounted(fetchHistory)
onBeforeUnmount(stopTypewriters)

function animateAnswer(record: QARecord) {
  activeTypewriters.get(record.id)?.()
  const stop = playTypewriter(record.answer, (value) => {
    displayAnswers.value = { ...displayAnswers.value, [record.id]: value }
  })
  activeTypewriters.set(record.id, stop)
}

function answerText(record: QARecord) {
  return displayAnswers.value[record.id] ?? record.answer
}

function stopTypewriters() {
  activeTypewriters.forEach((stop) => stop())
  activeTypewriters.clear()
}
</script>

<template>
  <div class="page-grid">
    <section class="surface panel-pad qa-main">
      <div class="section-head">
        <div>
          <h2>计划内 AI 问答</h2>
          <p>无上传资料时也会结合当前计划；上传资料后自动升级为知识库增强回答。</p>
        </div>
        <PlanSelector v-if="!embeddedPlanId" v-model="selectedPlanId" @change="handlePlanChange" />
        <el-tag v-else type="info">Plan #{{ embeddedPlanId }}</el-tag>
      </div>

      <el-empty v-if="!planId" description="请先创建并生成一个学习计划" />
      <div v-else class="ask-box">
        <el-input
          v-model="question"
          type="textarea"
          :rows="5"
          placeholder="例如：这个知识点在当前计划里应该怎么理解？"
          @keydown.meta.enter="ask"
          @keydown.ctrl.enter="ask"
        />
        <div class="ask-actions">
          <el-tag type="info">Ctrl/⌘ + Enter 发送</el-tag>
          <el-button :icon="ChatLineRound" type="primary" :loading="loading" @click="ask">提问</el-button>
        </div>
      </div>

      <el-empty v-if="planId && history.length === 0" description="还没有问答记录，可以直接提问，也可以上传资料增强回答" />
      <div v-else class="qa-list">
        <article v-for="record in history" :key="record.id" class="qa-card">
          <div class="question-line">
            <span>Q</span>
            <strong>{{ record.question }}</strong>
          </div>
          <small>{{ formatDateTime(record.createdAt) }}</small>
          <MarkdownContent class="answer-text" :content="answerText(record)" />
          <div class="tag-row" v-if="parseList(record.relatedPoints).length">
            <el-tag v-for="point in parseList(record.relatedPoints)" :key="point">{{ point }}</el-tag>
          </div>
        </article>
      </div>
    </section>

    <aside class="side-stack">
      <section class="surface panel-pad">
        <div class="section-head">
          <div>
            <h2>引用来源</h2>
            <p>当前回答命中的知识切片。</p>
          </div>
          <el-icon><Connection /></el-icon>
        </div>
        <el-empty v-if="!latest || parseCitations(latest.citations).length === 0" description="暂无引用" />
        <div v-else class="citation-list">
          <div v-for="citation in parseCitations(latest.citations)" :key="citation.chunkId" class="citation-item">
            <strong>{{ citation.documentName || `Document #${citation.documentId}` }}</strong>
            <span>Chunk #{{ citation.chunkId }} / {{ citation.sourceLocation }}</span>
            <p>{{ citation.excerpt }}</p>
          </div>
        </div>
      </section>

      <section class="surface panel-pad">
        <div class="section-head">
          <div>
            <h2>推荐追问</h2>
            <p>冷启动问题也会结合当前学习计划。</p>
          </div>
        </div>
        <div class="roadmap">
          <button class="suggestion" @click="question = '能举一个具体例子吗？'">能举一个具体例子吗？</button>
          <button class="suggestion" @click="question = '帮我生成一道练习题'">帮我生成一道练习题</button>
          <button class="suggestion" @click="question = '讲得更简单一点'">讲得更简单一点</button>
        </div>
      </section>
    </aside>
  </div>
</template>
