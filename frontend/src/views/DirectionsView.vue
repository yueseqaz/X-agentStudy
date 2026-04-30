<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { http, type ApiResponse } from '../api/http'
import { useDirectionStore } from '../stores/directions'

const store = useDirectionStore()
const router = useRouter()
const enteringId = ref<number | null>(null)
const deletingId = ref<number | null>(null)

interface Plan {
  id: number
}

const form = reactive({
  name: '',
  category: '',
  description: '',
})

async function submit() {
  if (!form.name.trim()) {
    return
  }
  const direction = await store.createDirection({ ...form })
  form.name = ''
  form.category = ''
  form.description = ''
  router.push(`/directions/${direction.id}/profile`)
}

function isNotFound(error: unknown) {
  const response = (error as { response?: { status?: number; data?: { code?: string } } }).response
  return response?.status === 404 || response?.data?.code === 'RESOURCE_NOT_FOUND'
}

async function enterDirection(directionId: number) {
  enteringId.value = directionId
  try {
    try {
      const response = await http.get<ApiResponse<Plan>>(`/directions/${directionId}/plans/latest`)
      router.push(`/plans/${response.data.data.id}`)
      return
    } catch (error) {
      if (!isNotFound(error)) {
        throw error
      }
    }
    router.push(`/directions/${directionId}/profile`)
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '进入学习方向失败')
  } finally {
    enteringId.value = null
  }
}

async function deleteDirection(direction: { id: number; name: string }) {
  await ElMessageBox.confirm(
    `删除后会同时清理「${direction.name}」下的画像、学习计划、知识库、问答、题库、作答记录和复习数据，且无法恢复。确定删除吗？`,
    '删除学习方向',
    {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger',
    },
  )
  deletingId.value = direction.id
  try {
    await store.deleteDirection(direction.id)
  } finally {
    deletingId.value = null
  }
}

onMounted(() => {
  store.fetchDirections()
})
</script>

<template>
  <div class="page-grid">
    <section class="surface panel-pad">
      <div class="section-head">
        <div>
          <h2>学习方向</h2>
          <p>方向是画像、计划、知识库、问答和题库的入口。</p>
        </div>
        <el-tag>计划级隔离</el-tag>
      </div>

      <el-skeleton v-if="store.loading" :rows="4" animated />
      <el-empty v-else-if="store.directions.length === 0" description="还没有学习方向" />
      <div v-else class="direction-list">
        <div v-for="direction in store.directions" :key="direction.id" class="direction-item">
          <div>
            <h3>{{ direction.name }}</h3>
            <p>{{ direction.description || '暂未补充说明' }}</p>
            <div class="tag-row">
              <el-tag>{{ direction.category || '未分类' }}</el-tag>
              <el-tag type="success">学习入口</el-tag>
            </div>
          </div>
          <div class="item-actions">
            <el-button
              :icon="Delete"
              type="danger"
              plain
              :loading="deletingId === direction.id"
              @click="deleteDirection(direction)"
            >
              删除
            </el-button>
            <el-button type="primary" :loading="enteringId === direction.id" @click="enterDirection(direction.id)">
              进入
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <aside class="surface panel-pad">
      <div class="section-head">
        <div>
          <h2>创建方向</h2>
          <p>方向名称会匹配资源学科名，技能分类会匹配资源学科范畴。</p>
        </div>
      </div>

      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="方向名称">
          <el-input v-model="form.name" placeholder="例如：Java 后端、Vue 前端、线性代数" />
        </el-form-item>
        <el-form-item label="技能分类">
          <el-input v-model="form.category" placeholder="例如：后端开发、前端开发、数学基础" />
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            placeholder="目标周期、当前基础、希望达成的结果"
          />
        </el-form-item>
        <el-button type="primary" native-type="submit" style="width: 100%">创建并进入画像</el-button>
      </el-form>
    </aside>
  </div>
</template>
