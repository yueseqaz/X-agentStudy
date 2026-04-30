<script setup lang="ts">
import { Calendar, Check, RefreshLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { http, type ApiResponse } from '../api/http'

interface CheckinStats {
  qaCount: number
  answeredQuestionCount: number
  correctAnswerCount: number
  completedTaskCount: number
  uploadedDocumentCount: number
  generatedDocumentCount: number
  onlineMinutes: number
  activeScore: number
}

interface CheckinDay {
  date: string
  checkedIn: boolean
  summary: string
  mood: string
  studyMinutes: number
  canCheckIn: boolean
  stats: CheckinStats
  updatedAt: string | null
}

interface CheckinMonth {
  month: string
  today: string
  checkedDays: number
  currentStreak: number
  longestStreak: number
  days: CheckinDay[]
}

const moods = [
  { value: '专注', label: '专注', note: '状态稳定，学习推进顺畅' },
  { value: '高效', label: '高效', note: '产出明显，完成度较高' },
  { value: '稳定', label: '稳定', note: '按计划完成了基本学习' },
  { value: '轻松', label: '轻松', note: '难度合适，负担较小' },
  { value: '吃力', label: '吃力', note: '理解成本偏高，需要复习' },
  { value: '卡住', label: '卡住', note: '遇到阻塞，需要问答或重学' },
  { value: '中断', label: '中断', note: '今天学习被打断' },
]
const loading = ref(false)
const saving = ref(false)
const month = ref(toMonth(new Date()))
const selectedDate = ref(toDate(new Date()))
const monthData = ref<CheckinMonth | null>(null)
const selectedDay = ref<CheckinDay | null>(null)
const form = ref({
  summary: '',
  mood: '专注',
})

const weekDays = ['一', '二', '三', '四', '五', '六', '日']

const calendarCells = computed(() => {
  const days = monthData.value?.days ?? []
  if (!days.length) {
    return []
  }
  const first = new Date(`${days[0].date}T00:00:00`)
  const offset = (first.getDay() + 6) % 7
  return [...Array.from({ length: offset }, () => null), ...days]
})

const dayStats = computed(() => selectedDay.value?.stats ?? emptyStats())
const answeredLabel = computed(() => {
  const stats = dayStats.value
  if (!stats.answeredQuestionCount) {
    return '暂无作答'
  }
  return `${stats.correctAnswerCount}/${stats.answeredQuestionCount} 正确`
})
const isTodaySelected = computed(() => selectedDate.value === monthData.value?.today)
const selectedMood = computed(() => moods.find((mood) => mood.value === form.value.mood) ?? moods[0])
const studyMinutesLabel = computed(() => {
  const minutes = selectedDay.value?.studyMinutes ?? 0
  if (minutes < 60) {
    return `${minutes} 分钟`
  }
  return `${Math.floor(minutes / 60)} 小时 ${minutes % 60} 分钟`
})

onMounted(loadMonth)

async function loadMonth() {
  loading.value = true
  try {
    const response = await http.get<ApiResponse<CheckinMonth>>('/checkins/month', {
      params: { month: month.value },
    })
    monthData.value = response.data.data
    const fallbackDate = response.data.data.days.some((day) => day.date === selectedDate.value)
      ? selectedDate.value
      : response.data.data.today
    await selectDate(fallbackDate)
  } finally {
    loading.value = false
  }
}

async function selectDate(date: string) {
  selectedDate.value = date
  const response = await http.get<ApiResponse<CheckinDay>>(`/checkins/${date}`)
  selectedDay.value = response.data.data
  form.value = {
    summary: selectedDay.value.summary || '',
    mood: selectedDay.value.mood || '专注',
  }
}

async function saveCheckin() {
  if (!isTodaySelected.value) {
    ElMessage.warning('只能为今天打卡，历史日期和未来日期仅支持查看')
    return
  }
  saving.value = true
  try {
    const response = await http.post<ApiResponse<CheckinDay>>(`/checkins/${selectedDate.value}`, {
      summary: form.value.summary,
      mood: form.value.mood,
    })
    selectedDay.value = response.data.data
    ElMessage.success('今日打卡已保存')
    await loadMonth()
  } finally {
    saving.value = false
  }
}

function changeMonth(delta: number) {
  const [year, currentMonth] = month.value.split('-').map(Number)
  const next = new Date(year, currentMonth - 1 + delta, 1)
  month.value = toMonth(next)
  selectedDate.value = toDate(next)
  loadMonth()
}

function toMonth(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

function toDate(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function emptyStats(): CheckinStats {
  return {
    qaCount: 0,
    answeredQuestionCount: 0,
    correctAnswerCount: 0,
    completedTaskCount: 0,
    uploadedDocumentCount: 0,
    generatedDocumentCount: 0,
    onlineMinutes: 0,
    activeScore: 0,
  }
}
</script>

<template>
  <section class="checkin-page" v-loading="loading">
    <div class="checkin-hero">
      <div>
        <span class="eyebrow">Daily learning</span>
        <h2>每日学习打卡</h2>
        <p>全局记录每天的学习行为，打卡只面向当天，历史日期用于回看。</p>
      </div>
      <div class="checkin-streaks">
        <div>
          <span>本月打卡</span>
          <strong>{{ monthData?.checkedDays ?? 0 }}</strong>
        </div>
        <div>
          <span>连续打卡</span>
          <strong>{{ monthData?.currentStreak ?? 0 }}</strong>
        </div>
        <div>
          <span>最长连续</span>
          <strong>{{ monthData?.longestStreak ?? 0 }}</strong>
        </div>
      </div>
    </div>

    <div class="checkin-layout">
      <section class="calendar-board">
        <div class="calendar-toolbar">
          <el-button :icon="RefreshLeft" @click="changeMonth(-1)">上月</el-button>
          <div>
            <h3>{{ month }}</h3>
            <span>数字为当天活跃分，绿色代表已打卡。</span>
          </div>
          <el-button @click="changeMonth(1)">下月</el-button>
        </div>
        <div class="weekday-grid">
          <span v-for="day in weekDays" :key="day">{{ day }}</span>
        </div>
        <div class="calendar-grid">
          <button
            v-for="(day, index) in calendarCells"
            :key="day?.date ?? `empty-${index}`"
            class="calendar-day"
            :class="{
              checked: day?.checkedIn,
              today: day?.date === monthData?.today,
              selected: day?.date === selectedDate,
              future: day && day.date > (monthData?.today ?? ''),
              active: !!day?.stats.activeScore,
            }"
            :disabled="!day"
            @click="day && selectDate(day.date)"
          >
            <template v-if="day">
              <div class="calendar-day-top">
                <span>{{ Number(day.date.slice(-2)) }}</span>
                <i v-if="day.checkedIn"></i>
              </div>
              <strong>{{ day.stats.activeScore }}</strong>
              <small v-if="day.checkedIn">{{ day.mood || '已打卡' }}</small>
              <small v-else-if="day.date === monthData?.today">今日</small>
              <small v-else-if="day.stats.activeScore">有学习</small>
              <small v-else-if="day.date > (monthData?.today ?? '')">未开始</small>
            </template>
          </button>
        </div>
      </section>

      <aside class="checkin-detail">
        <div class="detail-title">
          <div>
            <span class="eyebrow">{{ selectedDate }}</span>
            <h3>当天记录</h3>
          </div>
          <el-tag :type="selectedDay?.checkedIn ? 'success' : 'info'">
            {{ selectedDay?.checkedIn ? '已打卡' : isTodaySelected ? '今日可打卡' : '仅可查看' }}
          </el-tag>
        </div>

        <div class="daily-stats">
          <div>
            <span>活跃分</span>
            <strong>{{ dayStats.activeScore }}</strong>
          </div>
          <div>
            <span>登录学习时长</span>
            <strong>{{ studyMinutesLabel }}</strong>
          </div>
          <div>
            <span>问答</span>
            <strong>{{ dayStats.qaCount }}</strong>
          </div>
          <div>
            <span>作答</span>
            <strong>{{ answeredLabel }}</strong>
          </div>
          <div>
            <span>资料</span>
            <strong>{{ dayStats.uploadedDocumentCount + dayStats.generatedDocumentCount }}</strong>
          </div>
        </div>

        <div class="checkin-note" :class="{ locked: !isTodaySelected }">
          <strong>{{ isTodaySelected ? '今天可以打卡' : '非当天不可打卡' }}</strong>
          <span>
            {{ isTodaySelected ? '学习时长由当前登录时长自动统计，不需要手动填写。' : '历史和未来日期只用于查看记录，不能补签或提前打卡。' }}
          </span>
        </div>

        <el-form label-position="top" class="checkin-form">
          <el-form-item label="学习状态">
            <div class="mood-grid">
              <button
                v-for="mood in moods"
                :key="mood.value"
                type="button"
                :class="{ active: form.mood === mood.value }"
                :disabled="!isTodaySelected"
                @click="form.mood = mood.value"
              >
                <strong>{{ mood.label }}</strong>
                <span>{{ mood.note }}</span>
              </button>
            </div>
          </el-form-item>
          <p class="mood-current">当前选择：{{ selectedMood.label }} · {{ selectedMood.note }}</p>
          <el-form-item label="每日总结">
            <el-input
              v-model="form.summary"
              type="textarea"
              :rows="6"
              maxlength="1000"
              show-word-limit
              :disabled="!isTodaySelected"
              placeholder="今天学了什么，卡在哪里，明天继续什么。"
            />
          </el-form-item>
          <el-button type="primary" :icon="Check" :loading="saving" :disabled="!isTodaySelected" @click="saveCheckin">
            {{ selectedDay?.checkedIn ? '更新今日打卡' : '完成今日打卡' }}
          </el-button>
        </el-form>

        <div class="checkin-auto-log">
          <h4><el-icon><Calendar /></el-icon> 系统自动汇总</h4>
          <p>完成任务 {{ dayStats.completedTaskCount }} 个，生成学习文档 {{ dayStats.generatedDocumentCount }} 篇，上传资料 {{ dayStats.uploadedDocumentCount }} 份。</p>
          <p>这些数据来自全平台学习行为，不绑定单个计划。</p>
        </div>
      </aside>
    </div>
  </section>
</template>
