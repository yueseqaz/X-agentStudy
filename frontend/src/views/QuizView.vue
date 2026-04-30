<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { CircleCheck, RefreshLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { http, type ApiResponse } from '../api/http'
import PlanSelector from '../components/PlanSelector.vue'
import { formatDateTime } from '../utils/format'

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

const props = defineProps<{
  embeddedPlanId?: number | null
}>()

const selectedPlanId = ref<number | null>(null)
const planId = computed(() => props.embeddedPlanId ?? selectedPlanId.value)
const loading = ref(false)
const submitting = ref(false)
const wrongOnly = ref(false)
const questions = ref<Question[]>([])
const unansweredQuestionIds = ref<number[]>([])
const answers = reactive<Record<number, string>>({})
const results = reactive<Record<number, AnswerResult>>({})

const stats = computed(() => {
  const answered = Object.keys(results).length
  const correct = Object.values(results).filter((item) => item.correct).length
  return { answered, correct }
})

async function fetchQuestions() {
  if (!planId.value) {
    return
  }
  loading.value = true
  try {
    const path = wrongOnly.value ? `/plans/${planId.value}/questions/wrong` : `/plans/${planId.value}/questions`
    const response = await http.get<ApiResponse<Question[]>>(path)
    questions.value = response.data.data
  } finally {
    loading.value = false
  }
}

async function submitAllAnswers() {
  if (!planId.value || questions.value.length === 0) {
    return
  }
  const unanswered = questions.value.filter((question) => {
    const answer = answers[question.id]
    return !answer || !String(answer).trim()
  })
  if (unanswered.length > 0) {
    unansweredQuestionIds.value = unanswered.map((question) => question.id)
    ElMessage.warning(`还有 ${unanswered.length} 道题未作答，请补齐后再提交`)
    setTimeout(() => {
      document.querySelector('.quiz-card.unanswered')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    })
    return
  }
  unansweredQuestionIds.value = []
  submitting.value = true
  try {
    const responses = await Promise.all(
      questions.value.map((question) =>
        http.post<ApiResponse<AnswerResult>>(
          `/plans/${planId.value}/questions/${question.id}/answers`,
          { answer: answers[question.id], redoOfQuestionId: wrongOnly.value ? question.id : null },
          { timeout: 90000 },
        ),
      ),
    )
    responses.forEach((response) => {
      results[response.data.data.questionId] = response.data.data
    })
    const correct = responses.filter((response) => response.data.data.correct).length
    ElMessage.success(`已提交 ${responses.length} 道题，正确 ${correct} 道`)
    if (wrongOnly.value) {
      questions.value = questions.value.filter((question) => !results[question.id]?.correct)
    }
  } catch {
    ElMessage.error('提交失败，已保留当前作答，请稍后重试')
  } finally {
    submitting.value = false
  }
}

function isUnanswered(question: Question) {
  return unansweredQuestionIds.value.includes(question.id)
}

function clearUnansweredMark(question: Question) {
  if (!isUnanswered(question)) {
    return
  }
  const answer = answers[question.id]
  if (!answer || !String(answer).trim()) {
    return
  }
  unansweredQuestionIds.value = unansweredQuestionIds.value.filter((id) => id !== question.id)
}

function parseOptions(value: string): OptionItem[] {
  try {
    return JSON.parse(value) as OptionItem[]
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

function handlePlanChange() {
  questions.value = []
  unansweredQuestionIds.value = []
  Object.keys(answers).forEach((key) => delete answers[Number(key)])
  Object.keys(results).forEach((key) => delete results[Number(key)])
  fetchQuestions()
}

watch(() => props.embeddedPlanId, () => handlePlanChange())

onMounted(fetchQuestions)
</script>

<template>
  <div class="page-grid">
    <section class="surface panel-pad quiz-main">
      <div class="section-head">
        <div>
          <h2>题库与错题</h2>
          <p>这里专注展示计划题库、错题和重做记录。新题请进入计划页的知识点学习文档中生成。</p>
        </div>
        <div class="topbar-actions">
          <PlanSelector v-if="!embeddedPlanId" v-model="selectedPlanId" @change="handlePlanChange" />
          <el-tag v-else type="info">Plan #{{ embeddedPlanId }}</el-tag>
          <el-switch
            v-model="wrongOnly"
            active-text="只看错题"
            inactive-text="全部题库"
            @change="fetchQuestions"
          />
          <el-button v-if="!embeddedPlanId" type="primary" @click="$router.push(planId ? `/plans/${planId}` : '/directions')">
            去知识点生成题目
          </el-button>
        </div>
      </div>

      <el-empty v-if="!planId" description="请先创建并生成一个学习计划" />
      <el-skeleton v-else-if="loading" :rows="8" animated />
      <el-empty v-else-if="questions.length === 0" description="还没有题目，请进入计划页知识点学习文档生成测验" />
      <div v-else class="quiz-list">
        <article
          v-for="question in questions"
          :key="question.id"
          class="quiz-card"
          :class="{ unanswered: isUnanswered(question) }"
        >
          <div class="quiz-head">
            <div>
              <h3>{{ question.stem }}</h3>
              <div class="tag-row">
                <el-tag>{{ question.type }}</el-tag>
                <el-tag type="warning">{{ question.difficulty }}</el-tag>
                <el-tag type="info">{{ question.sourceLabel || question.sourceScope }}</el-tag>
                <el-tag type="info">{{ formatDateTime(question.createdAt) }}</el-tag>
              </div>
            </div>
            <el-tag v-if="results[question.id]" :type="results[question.id].correct ? 'success' : 'danger'">
              {{ results[question.id].correct ? '正确' : '错误' }}
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
            v-model="answers[question.id]"
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
            v-model="answers[question.id]"
            type="textarea"
            :rows="question.type === 'CODE' ? 8 : 4"
            placeholder="写下你的答案，主观类题目会由 AI 批改并给出反馈"
            @input="clearUnansweredMark(question)"
          />

          <div v-if="wrongOnly" class="quiz-actions">
            <el-button :icon="RefreshLeft" @click="answers[question.id] = ''">重做错题</el-button>
          </div>

          <el-alert
            v-if="results[question.id]"
            :title="feedbackSummary(results[question.id], question)"
            type="info"
            :closable="false"
            show-icon
          />
        </article>
        <div class="quiz-submit-bar">
          <div>
            <strong>整组提交</strong>
            <span>当前列表 {{ questions.length }} 道题会统一批改，结果写回本计划题库、复习和报告。</span>
          </div>
          <el-button type="primary" :icon="CircleCheck" :loading="submitting" @click="submitAllAnswers">
            提交全部答案
          </el-button>
        </div>
      </div>
    </section>

    <aside class="side-stack">
      <section class="surface panel-pad">
        <div class="section-head">
          <div>
            <h2>作答概览</h2>
            <p>当前页面的即时作答结果。</p>
          </div>
        </div>
        <div class="metric-grid quiz-stats">
          <div class="metric">
            <span>已作答</span>
            <strong>{{ stats.answered }}</strong>
          </div>
          <div class="metric">
            <span>正确</span>
            <strong>{{ stats.correct }}</strong>
          </div>
        </div>
      </section>

      <section class="surface panel-pad">
        <div class="section-head">
          <div>
            <h2>知识点</h2>
            <p>题目覆盖的知识标签。</p>
          </div>
        </div>
        <div class="tag-row" v-if="questions[0]">
          <el-tag v-for="point in parseList(questions[0].knowledgePoints)" :key="point">{{ point }}</el-tag>
        </div>
        <el-empty v-else description="暂无知识点" />
      </section>
    </aside>
  </div>
</template>
