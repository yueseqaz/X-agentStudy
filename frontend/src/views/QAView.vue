<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ChatLineRound, Connection } from '@element-plus/icons-vue'
import { http, type ApiResponse } from '../api/http'
import PlanSelector from '../components/PlanSelector.vue'
import { formatDateTime } from '../utils/format'

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

const latest = computed(() => history.value[0])

async function fetchHistory() {
  if (!planId.value) {
    return
  }
  const response = await http.get<ApiResponse<QARecord[]>>(`/plans/${planId.value}/qa`)
  history.value = response.data.data
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
  history.value = []
  fetchHistory()
}

watch(() => props.embeddedPlanId, () => handlePlanChange())

onMounted(fetchHistory)
</script>

<template>
  <div class="page-grid">
    <section class="surface panel-pad qa-main">
      <div class="section-head">
        <div>
          <h2>计划内 AI 问答</h2>
          <p>无资料时使用通用讲解；上传资料后自动升级为知识库增强回答。</p>
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
          placeholder="例如：Controller、Service、Repository 应该怎么分工？"
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
          <p class="answer-text">{{ record.answer }}</p>
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
            <p>可先冷启动提问，资料上传后再追问细节。</p>
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
