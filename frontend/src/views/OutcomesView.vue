<script setup lang="ts">
import { Download, Refresh, Trophy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { http, type ApiResponse } from '../api/http'
import { buildOutcomePosterFilename, downloadOutcomePoster, exportOutcomePosterPng, type OutcomePosterData } from '../utils/outcomePoster'

interface OutcomeOverview {
  learningDays: number
  completedTaskCount: number
  completedPlanCount: number
  quizAttemptCount: number
  masteredKnowledgePointCount: number
  completedReviewCount: number
}

interface OutcomeDirection {
  directionId: number
  directionName: string
  planCount: number
  taskCompletionRate: number
  accuracyRate: number
  completedReviewCount: number
  strengths: string[]
  weakPoints: string[]
}

interface OutcomeTrend {
  date: string
  completedTasks: number
  quizAttempts: number
  correctAnswers: number
  completedReviews: number
}

interface OutcomeHighlight {
  type: string
  referenceId: number | null
  title: string
  subtitle: string
  summary: string
}

interface OutcomePoster {
  title: string
  subtitle: string
  keywords: string[]
  highlights: string[]
}

interface OutcomeResponse {
  rangeCode: string
  rangeLabel: string
  startDate: string | null
  endDate: string
  overview: OutcomeOverview
  directions: OutcomeDirection[]
  trends: OutcomeTrend[]
  highlights: OutcomeHighlight[]
  suggestions: string[]
  poster: OutcomePoster
}

const loading = ref(false)
const exporting = ref(false)
const posterExpanded = ref(false)
const range = ref('LAST_30_DAYS')
const customDates = ref<[string, string] | null>(null)
const outcomes = ref<OutcomeResponse | null>(null)
const posterSectionRef = ref<HTMLElement | null>(null)

const rangeOptions = [
  { label: '7 天', value: 'LAST_7_DAYS' },
  { label: '30 天', value: 'LAST_30_DAYS' },
  { label: '90 天', value: 'LAST_90_DAYS' },
  { label: '全部', value: 'ALL' },
  { label: '自定义', value: 'CUSTOM' },
]

const overviewCards = computed(() => {
  const overview = outcomes.value?.overview
  if (!overview) {
    return []
  }
  return [
    { label: '学习天数', value: overview.learningDays, note: '按真实学习记录统计' },
    { label: '完成任务', value: overview.completedTaskCount, note: '跨全部学习计划汇总' },
    { label: '完成计划', value: overview.completedPlanCount, note: '统计周期内完全收尾的计划' },
    { label: '测验次数', value: overview.quizAttemptCount, note: '来自已提交作答记录' },
    { label: '掌握知识点', value: overview.masteredKnowledgePointCount, note: '基于现有掌握情况汇总' },
    { label: '完成复习', value: overview.completedReviewCount, note: '按复习完成记录统计' },
  ]
})

const maxTrendValue = computed(() => {
  return Math.max(
    1,
    ...(outcomes.value?.trends.map((item) => Math.max(item.completedTasks, item.quizAttempts, item.completedReviews)) || [1]),
  )
})

const posterData = computed<OutcomePosterData>(() => {
  const poster = outcomes.value?.poster
  return {
    title: poster?.title || '学习成果展示',
    subtitle: poster?.subtitle || activePeriodLabel.value,
    metrics: overviewCards.value.slice(0, 6).map((item) => ({ label: item.label, value: String(item.value) })),
    keywords: poster?.keywords?.length ? poster.keywords : (outcomes.value?.directions.map((item) => item.directionName).slice(0, 4) || ['学习成长']),
    highlights: poster?.highlights?.length
      ? poster.highlights
      : (outcomes.value?.suggestions.slice(0, 3) || ['持续完成学习任务', '保持测验与复习节奏']),
  }
})

const activePeriodLabel = computed(() => {
  if (!outcomes.value) {
    return '最近 30 天'
  }
  if (outcomes.value.rangeCode === 'ALL') {
    return outcomes.value.rangeLabel
  }
  const start = outcomes.value.startDate ? `${outcomes.value.startDate} - ` : ''
  return `${outcomes.value.rangeLabel} · ${start}${outcomes.value.endDate}`
})

async function fetchOutcomes() {
  loading.value = true
  try {
    const params = range.value === 'CUSTOM' && customDates.value
      ? { range: range.value, startDate: customDates.value[0], endDate: customDates.value[1] }
      : { range: range.value }
    const response = await http.get<ApiResponse<OutcomeResponse>>('/outcomes', { params })
    outcomes.value = response.data.data
  } finally {
    loading.value = false
  }
}

async function openPoster() {
  if (!outcomes.value) {
    return
  }
  posterExpanded.value = true
  await nextTick()
  posterSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function savePoster() {
  exporting.value = true
  try {
    await new Promise((resolve) => window.setTimeout(resolve, 16))
    const image = exportOutcomePosterPng(posterData.value)
    downloadOutcomePoster(image, buildOutcomePosterFilename(posterData.value.title))
    ElMessage.success('成果海报已保存')
  } finally {
    exporting.value = false
  }
}

function trendWidth(value: number) {
  return `${Math.max(8, (value / maxTrendValue.value) * 100)}%`
}

watch(range, (value) => {
  if (value !== 'CUSTOM') {
    customDates.value = null
    fetchOutcomes()
  } else if (customDates.value) {
    fetchOutcomes()
  }
})

watch(customDates, (value) => {
  if (range.value === 'CUSTOM' && value?.[0] && value?.[1]) {
    fetchOutcomes()
  }
})

onMounted(fetchOutcomes)
</script>

<template>
  <section class="outcomes-page" v-loading="loading">
    <div class="outcomes-hero surface panel-pad">
      <div>
        <span class="eyebrow">Learning outcomes</span>
        <h2>学习成果</h2>
        <p>把分散在计划、题库、复习、打卡和总结卡里的成果，整理成一份能看懂也能展示的总档案。</p>
      </div>
      <div class="outcomes-toolbar">
        <el-radio-group v-model="range" size="large">
          <el-radio-button v-for="item in rangeOptions" :key="item.value" :label="item.value">{{ item.label }}</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-if="range === 'CUSTOM'"
          v-model="customDates"
          type="daterange"
          unlink-panels
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
        <el-button :icon="Refresh" @click="fetchOutcomes">刷新</el-button>
        <el-button type="primary" :icon="Download" :disabled="!outcomes" :loading="exporting" @click="savePoster">生成成果海报</el-button>
      </div>
    </div>

    <div v-if="outcomes" class="outcomes-period">
      <el-tag type="success">{{ activePeriodLabel }}</el-tag>
    </div>

    <el-empty
      v-if="outcomes && outcomes.overview.learningDays === 0"
      class="surface panel-pad"
      description="当前时间范围内还没有学习记录，先创建方向或完成一次学习。"
    />

    <template v-else-if="outcomes">
      <div class="metric-grid outcomes-metrics">
        <div v-for="card in overviewCards" :key="card.label" class="metric">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.note }}</small>
        </div>
      </div>

      <div class="outcomes-layout">
        <section class="surface panel-pad">
          <div class="section-head">
            <div>
              <h2>能力地图</h2>
              <p>按学习方向查看掌握稳定度、答题表现和当前薄弱点。</p>
            </div>
            <el-tag type="warning">{{ outcomes.directions.length }} 个方向</el-tag>
          </div>

          <div v-if="outcomes.directions.length" class="outcome-direction-list">
            <article v-for="item in outcomes.directions" :key="item.directionId" class="outcome-direction-card">
              <div class="outcome-direction-head">
                <div>
                  <h3>{{ item.directionName }}</h3>
                  <p>{{ item.planCount }} 个计划 · {{ item.completedReviewCount }} 次已完成复习</p>
                </div>
                <el-tag type="info">正确率 {{ item.accuracyRate }}%</el-tag>
              </div>
              <div class="outcome-rate-grid">
                <div>
                  <span>任务完成</span>
                  <strong>{{ item.taskCompletionRate }}%</strong>
                </div>
                <div>
                  <span>测验正确率</span>
                  <strong>{{ item.accuracyRate }}%</strong>
                </div>
              </div>
              <div class="tag-row">
                <el-tag v-for="point in item.strengths" :key="`${item.directionId}-${point}`" type="success">{{ point }}</el-tag>
                <el-tag v-for="point in item.weakPoints" :key="`${item.directionId}-${point}`" type="danger">{{ point }}</el-tag>
              </div>
            </article>
          </div>
          <el-empty v-else description="暂无方向能力数据" />
        </section>

        <aside class="side-stack">
          <section class="surface panel-pad">
            <div class="section-head">
              <div>
                <h2>下一步建议</h2>
                <p>只保留需要立刻行动的内容。</p>
              </div>
            </div>
            <div v-if="outcomes.suggestions.length" class="review-list">
              <div v-for="(item, index) in outcomes.suggestions" :key="item" class="review-item">
                <span>{{ index + 1 }}</span>
                <p>{{ item }}</p>
              </div>
            </div>
            <el-empty v-else description="暂无建议" />
          </section>

          <section class="surface panel-pad">
            <div class="section-head">
              <div>
                <h2>代表成果</h2>
                <p>优先展示最能说明学习过程的真实产出。</p>
              </div>
            </div>
            <div v-if="outcomes.highlights.length" class="outcome-highlight-list">
              <article v-for="item in outcomes.highlights" :key="`${item.type}-${item.referenceId}`" class="outcome-highlight-card">
                <div class="outcome-highlight-head">
                  <el-icon><Trophy /></el-icon>
                  <div>
                    <strong>{{ item.title }}</strong>
                    <span>{{ item.subtitle }}</span>
                  </div>
                </div>
                <p>{{ item.summary }}</p>
              </article>
            </div>
            <el-empty v-else description="暂无代表成果" />
          </section>
        </aside>
      </div>

      <div class="outcomes-layout">
        <section class="surface panel-pad">
          <div class="section-head">
            <div>
              <h2>成长趋势</h2>
              <p>在一个周期里，任务推进、答题和复习是否保持了节奏。</p>
            </div>
          </div>
          <div v-if="outcomes.trends.length" class="outcome-trend-list">
            <div v-for="point in outcomes.trends" :key="point.date" class="outcome-trend-row">
              <span>{{ point.date.slice(5) }}</span>
              <div class="outcome-trend-bars">
                <i class="tasks" :style="{ width: trendWidth(point.completedTasks) }" />
                <i class="quizzes" :style="{ width: trendWidth(point.quizAttempts) }" />
                <i class="reviews" :style="{ width: trendWidth(point.completedReviews) }" />
              </div>
              <strong>{{ point.completedTasks }}/{{ point.quizAttempts }}/{{ point.completedReviews }}</strong>
            </div>
          </div>
          <el-empty v-else description="暂无趋势数据" />
        </section>

        <section class="surface panel-pad">
          <div class="section-head">
            <div>
              <h2>对外展示内容</h2>
              <p>当前统计周期会直接进入成果海报。</p>
            </div>
            <el-button type="primary" text :icon="Download" :disabled="!outcomes" @click="openPoster">预览海报</el-button>
          </div>
          <div ref="posterSectionRef" class="outcome-poster-preview" :class="{ expanded: posterExpanded }">
            <span class="eyebrow">{{ activePeriodLabel }}</span>
            <h3>{{ posterData.title }}</h3>
            <p>{{ posterData.subtitle }}</p>
            <div class="tag-row">
              <el-tag v-for="keyword in posterData.keywords" :key="keyword" type="success">{{ keyword }}</el-tag>
            </div>
            <div class="review-list">
              <div v-for="(item, index) in posterData.highlights" :key="item" class="review-item">
                <span>{{ index + 1 }}</span>
                <p>{{ item }}</p>
              </div>
            </div>
            <div v-if="posterExpanded" class="outcome-poster-actions">
              <el-button @click="posterExpanded = false">收起预览</el-button>
              <el-button type="primary" :icon="Download" :loading="exporting" @click="savePoster">保存海报</el-button>
            </div>
          </div>
        </section>
      </div>
    </template>
  </section>
</template>
