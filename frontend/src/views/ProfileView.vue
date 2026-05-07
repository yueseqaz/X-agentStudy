<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http, type ApiResponse } from '../api/http'

interface Profile {
  id: number
  directionId: number
  goal: string
  currentLevel: string
  timeBudget: string
  preference: string
  risks: string
  strategy: string
  rawConversation: string
}

interface Plan {
  id: number
  title?: string
}

const route = useRoute()
const router = useRouter()
const directionId = computed(() => Number(route.params.directionId))
const initialLoading = ref(false)
const loading = ref(false)
const planLoading = ref(false)
const profile = ref<Profile | null>(null)
const latestPlan = ref<Plan | null>(null)

const questions = [
  '你想学习这项技能主要是为了什么？',
  '你之前是否接触过，当前基础如何？',
  '你每周大概能投入多少学习时间？',
  '你理解新知识时更偏好哪种方式？',
  '你现在最大的学习困难是什么？',
  '你希望系统优先生成哪类学习资源？',
]

const form = reactive({
  answers: questions.map((question) => ({ question, answer: '' })),
})

function resetState() {
  profile.value = null
  latestPlan.value = null
  form.answers.forEach((item) => {
    item.answer = ''
  })
}

function isNotFound(error: unknown) {
  const response = (error as { response?: { status?: number; data?: { code?: string } } }).response
  return response?.status === 404 || response?.data?.code === 'RESOURCE_NOT_FOUND'
}

function isTimeout(error: unknown) {
  return (error as { code?: string }).code === 'ECONNABORTED'
}

function restoreAnswers(rawConversation: string) {
  if (!rawConversation) {
    return
  }
  try {
    const lines = (JSON.parse(rawConversation) as string[]).join('\n').split('\n')
    form.answers.forEach((item) => {
      const matched = lines.find((line) => line.startsWith(`${item.question}：`))
      if (matched) {
        item.answer = matched.slice(item.question.length + 1)
      }
    })
  } catch {
    // Older records can still be shown through the profile result card.
  }
}

async function fetchExistingState() {
  if (!directionId.value) {
    return
  }
  initialLoading.value = true
  resetState()
  try {
    try {
      const profileResponse = await http.get<ApiResponse<Profile>>(`/directions/${directionId.value}/profile/latest`)
      profile.value = profileResponse.data.data
      restoreAnswers(profile.value.rawConversation)
    } catch (error) {
      if (!isNotFound(error)) {
        throw error
      }
    }

    try {
      const planResponse = await http.get<ApiResponse<Plan>>(`/directions/${directionId.value}/plans/latest`)
      latestPlan.value = planResponse.data.data
      router.replace(`/plans/${latestPlan.value.id}`)
    } catch (error) {
      if (!isNotFound(error)) {
        throw error
      }
    }
  } finally {
    initialLoading.value = false
  }
}

async function generateProfile() {
  const answered = form.answers.filter((item) => item.answer.trim())
  if (answered.length < 3) {
    ElMessage.warning('至少回答 3 个问题，画像会更准确')
    return
  }
  loading.value = true
  try {
    const response = await http.post<ApiResponse<Profile>>(
      `/directions/${directionId.value}/profile/generate`,
      { answers: answered },
    )
    profile.value = response.data.data
    latestPlan.value = null
    ElMessage.success('学习画像已生成')
  } finally {
    loading.value = false
  }
}

async function generatePlan() {
  planLoading.value = true
  try {
    const response = await http.post<ApiResponse<Plan>>(
      `/directions/${directionId.value}/plans/generate`,
      {},
      { timeout: 90000 },
    )
    latestPlan.value = response.data.data
    ElMessage.success('学习计划已生成')
    router.replace(`/plans/${response.data.data.id}`)
  } catch (error) {
    if (!isTimeout(error)) {
      throw error
    }
    ElMessage.warning('计划生成耗时较长，正在回查生成结果')
    const latest = await waitForLatestPlan()
    if (latest) {
      latestPlan.value = latest
      ElMessage.success('学习计划已生成')
      router.replace(`/plans/${latest.id}`)
    } else {
      ElMessage.error('计划仍在生成中，请稍后从学习方向进入')
    }
  } finally {
    planLoading.value = false
  }
}

async function waitForLatestPlan() {
  for (let index = 0; index < 8; index++) {
    await new Promise((resolve) => window.setTimeout(resolve, 1500))
    try {
      const response = await http.get<ApiResponse<Plan>>(`/directions/${directionId.value}/plans/latest`)
      return response.data.data
    } catch (error) {
      if (!isNotFound(error)) {
        throw error
      }
    }
  }
  return null
}

const riskItems = computed(() => {
  if (!profile.value?.risks) {
    return []
  }
  try {
    const parsed = JSON.parse(profile.value.risks)
    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string' && item.trim().length > 0) : []
  } catch {
    return [profile.value.risks]
  }
})

const profileDimensions = computed(() => {
  if (!profile.value) {
    return []
  }
  return [
    { label: '学习目标', value: profile.value.goal },
    { label: '知识基础', value: profile.value.currentLevel },
    { label: '学习节奏', value: profile.value.timeBudget },
    { label: '认知风格', value: profile.value.preference },
    { label: '易错点', value: riskItems.value.slice(0, 2).join('；') || '待通过测验发现' },
    { label: '资源偏好', value: profile.value.preference },
  ]
})

watch(directionId, fetchExistingState, { immediate: true })
</script>

<template>
  <div class="page-grid">
    <section class="surface panel-pad">
      <div class="section-head">
        <div>
          <h2>画像 Agent 对话</h2>
          <p>{{ profile ? '已读取最近一次画像，可直接继续；修改回答后也能重新生成。' : '用少量问题建立学习目标、基础、时间、偏好和风险画像。' }}</p>
        </div>
        <el-tag :type="profile ? 'success' : undefined">{{ profile ? '画像已生成' : `Direction #${directionId}` }}</el-tag>
      </div>

      <el-skeleton v-if="initialLoading" :rows="8" animated />
      <div v-else class="profile-chat">
        <div v-for="(item, index) in form.answers" :key="item.question" class="qa-block">
          <div class="agent-bubble">
            <strong>画像 Agent</strong>
            <span>{{ index + 1 }}. {{ item.question }}</span>
          </div>
          <el-input
            v-model="item.answer"
            type="textarea"
            :rows="3"
            placeholder="用自己的话回答即可"
          />
        </div>
        <el-button type="primary" :loading="loading" @click="generateProfile">
          {{ profile ? '重新生成学习画像' : '生成学习画像' }}
        </el-button>
      </div>
    </section>

    <aside class="surface panel-pad">
      <div class="section-head">
        <div>
          <h2>画像结果</h2>
          <p>确认画像后，规划 Agent 会生成阶段学习计划。</p>
        </div>
        <el-tag v-if="profile" type="success">6 维画像</el-tag>
      </div>

      <el-skeleton v-if="initialLoading" :rows="6" animated />
      <el-empty v-else-if="!profile" description="回答问题后生成画像" />
      <div v-else class="profile-result">
        <div class="profile-dimensions">
          <article v-for="dimension in profileDimensions" :key="dimension.label">
            <span>{{ dimension.label }}</span>
            <strong>{{ dimension.value }}</strong>
          </article>
        </div>
        <div>
          <span>学习目标</span>
          <strong>{{ profile.goal }}</strong>
        </div>
        <div>
          <span>当前水平</span>
          <strong>{{ profile.currentLevel }}</strong>
        </div>
        <div>
          <span>时间投入</span>
          <strong>{{ profile.timeBudget }}</strong>
        </div>
        <div>
          <span>学习偏好</span>
          <strong>{{ profile.preference }}</strong>
        </div>
        <div>
          <span>风险与薄弱点</span>
          <p>{{ riskItems.join('；') || '暂无明显风险' }}</p>
        </div>
        <div>
          <span>推荐策略</span>
          <p>{{ profile.strategy }}</p>
        </div>
        <div class="profile-actions">
          <el-button
            v-if="latestPlan"
            type="primary"
            style="width: 100%"
            @click="router.push(`/plans/${latestPlan.id}`)"
          >
            进入已有学习计划
          </el-button>
          <el-button type="primary" plain :loading="planLoading" style="width: 100%" @click="generatePlan">
            {{ latestPlan ? '基于当前画像重新生成计划' : '确认画像并生成计划' }}
          </el-button>
        </div>
      </div>
    </aside>
  </div>
</template>
