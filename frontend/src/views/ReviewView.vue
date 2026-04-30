<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { CircleCheck, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { http, type ApiResponse } from '../api/http'
import PlanSelector from '../components/PlanSelector.vue'
import { formatDate } from '../utils/format'

interface ReviewRecord {
  id: number
  planId: number
  knowledgePoint: string
  reason: string
  priorityLevel: number
  completed: boolean
  masteryScore: number
  intervalDays: number
  dueAt: string
  recommendedAt: string
  completedAt: string | null
}

interface ReviewSummary {
  planId: number
  totalCount: number
  pendingCount: number
  completedCount: number
  records: ReviewRecord[]
}

const props = defineProps<{
  embeddedPlanId?: number | null
}>()

const selectedPlanId = ref<number | null>(null)
const planId = computed(() => props.embeddedPlanId ?? selectedPlanId.value)
const loading = ref(false)
const summary = ref<ReviewSummary | null>(null)

const progress = computed(() => {
  if (!summary.value || summary.value.totalCount === 0) {
    return 0
  }
  return Math.round((summary.value.completedCount * 100) / summary.value.totalCount)
})

async function fetchReviews() {
  if (!planId.value) {
    return
  }
  loading.value = true
  try {
    const response = await http.get<ApiResponse<ReviewSummary>>(`/plans/${planId.value}/reviews/today`)
    summary.value = response.data.data
  } finally {
    loading.value = false
  }
}

async function completeReview(record: ReviewRecord) {
  if (!planId.value) {
    return
  }
  const response = await http.post<ApiResponse<ReviewRecord>>(
    `/plans/${planId.value}/reviews/${record.id}/complete`,
    { quality: 4 },
  )
  if (summary.value) {
    summary.value.records = summary.value.records.map((item) =>
      item.id === record.id ? response.data.data : item,
    )
    summary.value.completedCount = summary.value.records.filter((item) => item.completed).length
    summary.value.pendingCount = summary.value.records.length - summary.value.completedCount
  }
  ElMessage.success('已标记完成')
}

function handlePlanChange() {
  summary.value = null
  fetchReviews()
}

watch(() => props.embeddedPlanId, () => handlePlanChange())

onMounted(fetchReviews)
</script>

<template>
  <div class="page-grid">
    <section class="surface panel-pad review-main">
      <div class="section-head">
        <div>
          <h2>今日复习</h2>
          <p>根据错题聚合出的薄弱点生成复习清单，完成后会保留进度。</p>
        </div>
        <div class="topbar-actions">
          <PlanSelector v-if="!embeddedPlanId" v-model="selectedPlanId" @change="handlePlanChange" />
          <el-tag v-else type="info">Plan #{{ embeddedPlanId }}</el-tag>
          <el-button :icon="Refresh" :loading="loading" @click="fetchReviews">同步</el-button>
        </div>
      </div>

      <el-empty v-if="!planId" description="请先创建并生成一个学习计划" />
      <el-skeleton v-else-if="loading && !summary" :rows="8" animated />
      <el-empty v-else-if="!summary || summary.totalCount === 0" description="暂无复习项，先完成测验并产生错题" />
      <template v-else>
        <div class="review-progress">
          <div>
            <span>复习完成度</span>
            <strong>{{ progress }}%</strong>
          </div>
          <el-progress :percentage="progress" :stroke-width="10" />
        </div>

        <div class="review-card-list">
          <article
            v-for="record in summary.records"
            :key="record.id"
            class="review-card"
            :class="{ completed: record.completed }"
          >
            <div class="review-card-head">
              <div>
                <span>Priority {{ record.priorityLevel }} · 掌握度 {{ record.masteryScore }}</span>
                <h3>{{ record.knowledgePoint }}</h3>
              </div>
              <el-tag :type="record.completed ? 'success' : 'warning'">
                {{ record.completed ? '已完成' : '待复习' }}
              </el-tag>
            </div>
            <p>{{ record.reason }}</p>
            <div class="tag-row">
              <el-tag>间隔 {{ record.intervalDays }} 天</el-tag>
              <el-tag type="info">下次 {{ formatDate(record.dueAt) }}</el-tag>
            </div>
            <div class="quiz-actions">
              <el-button
                :icon="CircleCheck"
                type="primary"
                :disabled="record.completed"
                @click="completeReview(record)"
              >
                标记完成
              </el-button>
            </div>
          </article>
        </div>
      </template>
    </section>

    <aside class="side-stack">
      <section class="surface panel-pad">
        <div class="section-head">
          <div>
            <h2>复习概览</h2>
            <p>按当前计划隔离统计。</p>
          </div>
        </div>
        <div class="metric-grid quiz-stats">
          <div class="metric">
            <span>待复习</span>
            <strong>{{ summary?.pendingCount ?? 0 }}</strong>
          </div>
          <div class="metric">
            <span>已完成</span>
            <strong>{{ summary?.completedCount ?? 0 }}</strong>
          </div>
        </div>
      </section>

      <section class="surface panel-pad">
        <div class="section-head">
          <div>
            <h2>下一步</h2>
            <p>完成复习后回到测验页重做错题。</p>
          </div>
        </div>
        <button class="suggestion" @click="$router.push(embeddedPlanId ? `/plans/${embeddedPlanId}?tab=quiz` : '/directions')">去测验题库重做</button>
        <button class="suggestion" @click="$router.push(embeddedPlanId ? `/plans/${embeddedPlanId}?tab=report` : '/directions')">查看学习报告</button>
      </section>
    </aside>
  </div>
</template>
