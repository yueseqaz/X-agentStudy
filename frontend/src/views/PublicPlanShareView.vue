<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http, type ApiResponse } from '../api/http'
import { formatDateTime } from '../utils/format'

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

interface PublicPlan {
  shareCode: string
  title: string
  directionName: string
  directionCategory: string
  goal: string
  stages: string
  createdAt: string
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const applying = ref(false)
const plan = ref<PublicPlan | null>(null)

const stages = computed(() => {
  if (!plan.value?.stages) {
    return []
  }
  try {
    return JSON.parse(plan.value.stages) as PlanStage[]
  } catch {
    return []
  }
})

onMounted(fetchPublicPlan)

async function fetchPublicPlan() {
  loading.value = true
  try {
    const response = await http.get<ApiResponse<PublicPlan>>(`/public/plans/${route.params.shareCode}`)
    plan.value = response.data.data
  } finally {
    loading.value = false
  }
}

function chapterPoints(stage: PlanStage) {
  return (stage.units || []).flatMap((unit) => unit.knowledgePoints || [])
}

function stageUnits(stage: PlanStage) {
  if (stage.units?.length) {
    return stage.units
  }
  return [{
    unitIndex: 1,
    name: '阶段任务',
    goal: stage.focus || stage.outcome || '',
    knowledgePoints: (stage.tasks || []).map((task, index) => ({
      id: `task-${index}`,
      title: task,
      level: 'Task',
      outcome: task,
      estimatedMinutes: 0,
    })),
  }]
}

async function applyPlan() {
  if (!plan.value) {
    return
  }
  applying.value = true
  try {
    const response = await http.post<ApiResponse<{ id: number }>>(`/public/plans/${plan.value.shareCode}/apply`, {})
    ElMessage.success('已应用到你的学习计划')
    router.push(`/plans/${response.data.data.id}`)
  } finally {
    applying.value = false
  }
}

</script>

<template>
  <main class="public-plan-page">
    <el-skeleton v-if="loading" :rows="10" animated />
    <el-empty v-else-if="!plan" description="分享计划不存在或已失效" />
    <template v-else>
      <section class="public-plan-hero">
        <span>{{ plan.directionName }} · {{ plan.directionCategory || '学习计划' }}</span>
        <h1>{{ plan.title }}</h1>
        <p>{{ plan.goal }}</p>
        <div class="public-plan-actions">
          <small>创建于 {{ formatDateTime(plan.createdAt) }}</small>
          <el-button type="primary" :loading="applying" @click="applyPlan">应用这个计划</el-button>
        </div>
      </section>

      <section v-if="stages.length" class="public-plan-map">
        <div class="public-plan-map-head">
          <div>
            <h2>知识图谱</h2>
            <p>公开预览只展示计划结构，不包含个人学习记录。</p>
          </div>
          <el-tag>Read only</el-tag>
        </div>
        <div class="public-knowledge-tree">
          <article v-for="(stage, index) in stages" :key="stage.name" class="public-tree-chapter">
            <div class="public-tree-chapter-head">
              <span>Chapter {{ stage.chapterIndex || index + 1 }}</span>
              <strong>{{ stage.name }}</strong>
              <small>{{ stage.outcome || stage.focus }}</small>
            </div>
            <div class="public-tree-units">
              <section v-for="unit in stageUnits(stage)" :key="unit.name" class="public-tree-unit">
                <h3>{{ unit.name }}</h3>
                <p>{{ unit.goal }}</p>
                <div class="public-tree-points">
                  <span v-for="point in unit.knowledgePoints" :key="point.id">{{ point.title }}</span>
                </div>
              </section>
            </div>
          </article>
        </div>
      </section>

      <section class="public-plan-stages">
        <article v-for="(stage, index) in stages" :key="stage.name" class="public-plan-stage">
          <div class="public-stage-index">{{ index + 1 }}</div>
          <div>
            <div class="public-stage-head">
              <h2>{{ stage.name }}</h2>
              <el-tag>{{ stage.duration }}</el-tag>
            </div>
            <p>{{ stage.outcome || stage.focus }}</p>
            <div v-if="chapterPoints(stage).length" class="public-point-grid">
              <span v-for="point in chapterPoints(stage)" :key="point.id">{{ point.title }}</span>
            </div>
            <ul v-else>
              <li v-for="task in stage.tasks || []" :key="task">{{ task }}</li>
            </ul>
          </div>
        </article>
      </section>
    </template>
  </main>
</template>
