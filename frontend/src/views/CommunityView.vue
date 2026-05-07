<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ArrowLeft, ChatDotRound, Connection, EditPen, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { http, type ApiResponse } from '../api/http'

interface CommunityAnswer {
  id: number
  questionId: number
  userId: number | null
  authorName: string
  source: 'USER' | 'AI'
  content: string
  createdAt: string
}

interface CommunityQuestion {
  id: number
  userId: number
  authorName: string
  title: string
  content: string
  tags: string
  answerCount: number
  answers: CommunityAnswer[]
  createdAt: string
  updatedAt: string
}

const loading = ref(false)
const detailLoading = ref(false)
const submittingQuestion = ref(false)
const submittingAnswer = ref(false)
const invitingAi = ref(false)
const questions = ref<CommunityQuestion[]>([])
const selectedQuestion = ref<CommunityQuestion | null>(null)

const questionForm = reactive({
  title: '',
  content: '',
  tags: '',
})

const answerForm = reactive({
  content: '',
})

const tagList = computed(() => splitTags(selectedQuestion.value?.tags || ''))
const totalAnswers = computed(() => questions.value.reduce((sum, question) => sum + question.answerCount, 0))

onMounted(fetchQuestions)

async function fetchQuestions() {
  loading.value = true
  try {
    const response = await http.get<ApiResponse<CommunityQuestion[]>>('/community/questions')
    questions.value = response.data.data
    if (selectedQuestion.value) {
      const stillExists = questions.value.some((question) => question.id === selectedQuestion.value?.id)
      if (stillExists) {
        await openQuestion(selectedQuestion.value.id)
      } else {
        selectedQuestion.value = null
      }
    }
  } finally {
    loading.value = false
  }
}

async function openQuestion(questionId: number) {
  detailLoading.value = true
  try {
    const response = await http.get<ApiResponse<CommunityQuestion>>(`/community/questions/${questionId}`)
    selectedQuestion.value = response.data.data
  } finally {
    detailLoading.value = false
  }
}

async function createQuestion() {
  if (!questionForm.title.trim() || !questionForm.content.trim()) {
    ElMessage.warning('标题和问题内容都需要填写')
    return
  }
  submittingQuestion.value = true
  try {
    const response = await http.post<ApiResponse<CommunityQuestion>>('/community/questions', {
      title: questionForm.title.trim(),
      content: questionForm.content.trim(),
      tags: questionForm.tags.trim(),
    })
    questionForm.title = ''
    questionForm.content = ''
    questionForm.tags = ''
    ElMessage.success('问题已发布')
    await fetchQuestions()
    await openQuestion(response.data.data.id)
  } finally {
    submittingQuestion.value = false
  }
}

async function submitAnswer() {
  if (!selectedQuestion.value || !answerForm.content.trim()) {
    ElMessage.warning('请先写下回答内容')
    return
  }
  submittingAnswer.value = true
  try {
    await http.post<ApiResponse<CommunityAnswer>>(`/community/questions/${selectedQuestion.value.id}/answers`, {
      content: answerForm.content.trim(),
    })
    answerForm.content = ''
    await refreshCurrentQuestion()
    ElMessage.success('回答已发布')
  } finally {
    submittingAnswer.value = false
  }
}

async function inviteAi() {
  if (!selectedQuestion.value) {
    return
  }
  invitingAi.value = true
  try {
    await http.post<ApiResponse<CommunityAnswer>>(`/community/questions/${selectedQuestion.value.id}/ai-answer`, {})
    await refreshCurrentQuestion()
    ElMessage.success('AI 回答已生成')
  } finally {
    invitingAi.value = false
  }
}

async function refreshCurrentQuestion() {
  if (selectedQuestion.value) {
    await openQuestion(selectedQuestion.value.id)
    await fetchQuestions()
  }
}

function backToQuestions() {
  selectedQuestion.value = null
  answerForm.content = ''
}

function splitTags(tags: string) {
  return tags.split(',').map((tag) => tag.trim()).filter(Boolean)
}

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}
</script>

<template>
  <div class="community-page">
    <section v-if="!selectedQuestion" class="community-home">
      <div class="section-head">
        <div>
          <h2>问答社区</h2>
          <p>先浏览所有问题，点进问题后再在回答区互动，也可以邀请 AI 助教补充。</p>
        </div>
        <el-button :icon="RefreshRight" :loading="loading" @click="fetchQuestions">刷新</el-button>
      </div>

      <div class="community-home-grid">
        <aside class="community-compose-card surface panel-pad">
          <div>
            <h3>发布新问题</h3>
            <p>描述清楚问题背景，其他用户和 AI 才能给出更准确的回答。</p>
          </div>
          <el-form class="community-compose" label-position="top">
            <el-form-item label="问题标题">
              <el-input v-model="questionForm.title" maxlength="180" show-word-limit placeholder="例如：Redis AOF 为什么会重写？" />
            </el-form-item>
            <el-form-item label="问题描述">
              <el-input
                v-model="questionForm.content"
                type="textarea"
                :rows="5"
                maxlength="4000"
                show-word-limit
                placeholder="写清楚你卡在哪里、已经尝试过什么。"
              />
            </el-form-item>
            <el-form-item label="标签">
              <el-input v-model="questionForm.tags" placeholder="用英文逗号分隔，例如 Redis,持久化" />
            </el-form-item>
            <el-button type="primary" :icon="EditPen" :loading="submittingQuestion" @click="createQuestion">发布问题</el-button>
          </el-form>
        </aside>

        <div class="community-overview">
          <div class="community-stats">
            <div>
              <span>问题</span>
              <strong>{{ questions.length }}</strong>
            </div>
            <div>
              <span>回答</span>
              <strong>{{ totalAnswers }}</strong>
            </div>
            <div>
              <span>AI 入口</span>
              <strong>@ai</strong>
            </div>
          </div>

          <el-skeleton v-if="loading" :rows="8" animated />
          <el-empty v-else-if="questions.length === 0" description="还没有社区问题" />
          <div v-else class="community-card-grid">
            <button
              v-for="question in questions"
              :key="question.id"
              class="community-question-card"
              type="button"
              @click="openQuestion(question.id)"
            >
              <div class="community-question-topline">
                <span>{{ question.authorName }}</span>
                <span>{{ formatDate(question.updatedAt) }}</span>
              </div>
              <div>
                <h3>{{ question.title }}</h3>
                <p>{{ question.content }}</p>
              </div>
              <div class="community-tags compact">
                <el-tag v-for="tag in splitTags(question.tags)" :key="tag" size="small">{{ tag }}</el-tag>
              </div>
              <div class="community-card-meta">
                <span>{{ question.answerCount }} 个回答</span>
                <strong>进入讨论</strong>
              </div>
            </button>
          </div>
        </div>
      </div>
    </section>

    <section v-else class="community-detail surface panel-pad">
      <el-skeleton v-if="detailLoading" :rows="10" animated />
      <template v-else>
        <el-button class="community-back" :icon="ArrowLeft" text @click="backToQuestions">返回问题列表</el-button>
        <div class="community-detail-head">
          <div>
            <h2>{{ selectedQuestion.title }}</h2>
            <p>{{ selectedQuestion.content }}</p>
          </div>
          <el-button type="primary" plain :icon="Connection" :loading="invitingAi" @click="inviteAi">邀请 AI 回答</el-button>
        </div>

        <div class="community-tags">
          <el-tag v-for="tag in tagList" :key="tag">{{ tag }}</el-tag>
        </div>
        <p class="community-time">{{ selectedQuestion.authorName }} · {{ formatDate(selectedQuestion.createdAt) }}</p>

        <div class="community-answer-editor">
          <el-input
            v-model="answerForm.content"
            type="textarea"
            :rows="4"
            maxlength="4000"
            show-word-limit
            placeholder="写下你的回答；输入 @ai 也可以让 AI 助教直接回答。"
          />
          <el-button type="primary" :icon="ChatDotRound" :loading="submittingAnswer" @click="submitAnswer">发布回答</el-button>
        </div>

        <el-divider />

        <el-empty v-if="selectedQuestion.answers.length === 0" description="还没有回答" />
        <div v-else class="community-answers">
          <article v-for="answer in selectedQuestion.answers" :key="answer.id" class="community-answer" :class="{ ai: answer.source === 'AI' }">
            <div class="community-answer-head">
              <strong>{{ answer.authorName }}</strong>
              <el-tag v-if="answer.source === 'AI'" type="success">AI</el-tag>
              <span>{{ formatDate(answer.createdAt) }}</span>
            </div>
            <p>{{ answer.content }}</p>
          </article>
        </div>
      </template>
    </section>
  </div>
</template>
