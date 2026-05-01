<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen, Promotion } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { http, type ApiResponse } from '../api/http'
import { useAuthStore } from '../stores/auth'
import type { CollaboratorApplication } from '../types/resourceCollaboration'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const submitting = ref(false)
const application = ref<CollaboratorApplication | null>(null)
const form = ref({
  reason: '',
  expertise: '',
})

const statusMeta = computed(() => {
  if (!application.value) {
    return {
      label: '未提交',
      type: 'info' as const,
      description: '提交后可在这里查看审核状态与审核说明。',
    }
  }
  if (application.value.status === 'APPROVED') {
    return {
      label: '已通过',
      type: 'success' as const,
      description: '已具备资源协作者权限，可直接进入“我的资源管理”。',
    }
  }
  if (application.value.status === 'REJECTED') {
    return {
      label: '已拒绝',
      type: 'danger' as const,
      description: '可根据审核说明补充理由后再次提交。',
    }
  }
  return {
    label: '待审核',
    type: 'warning' as const,
    description: '申请已提交，等待管理员处理。',
  }
})

const canSubmit = computed(() => {
  if (auth.canManageResources) {
    return false
  }
  if (!application.value) {
    return true
  }
  return application.value.status === 'REJECTED'
})

onMounted(fetchApplication)

async function fetchApplication() {
  loading.value = true
  try {
    const response = await http.get<ApiResponse<CollaboratorApplication | null>>('/resource-collaboration/application')
    application.value = response.data.data
    if (application.value) {
      form.value.reason = application.value.reason
      form.value.expertise = application.value.expertise || ''
    }
  } catch {
    application.value = null
  } finally {
    loading.value = false
  }
}

async function submitApplication() {
  if (!form.value.reason.trim() || !form.value.expertise.trim()) {
    ElMessage.warning('请完整填写申请理由和擅长方向')
    return
  }
  submitting.value = true
  try {
    const response = await http.post<ApiResponse<CollaboratorApplication>>('/resource-collaboration/application', {
      reason: form.value.reason.trim(),
      expertise: form.value.expertise.trim(),
    })
    application.value = response.data.data
    ElMessage.success('申请已提交')
    await auth.fetchMe()
  } finally {
    submitting.value = false
  }
}

function formatDate(value: string | null) {
  return value ? new Date(value).toLocaleString() : '-'
}
</script>

<template>
  <section class="resource-application-page">
    <div class="surface panel-pad collaboration-hero">
      <div>
        <span>资源协作</span>
        <h2>申请成为资源协作者</h2>
        <p>提交理由、擅长方向与资源来源说明，管理员审核通过后即可进入独立资源管理页面。</p>
      </div>
      <div class="resource-entry-actions">
        <el-tag :type="statusMeta.type">{{ statusMeta.label }}</el-tag>
        <el-button v-if="auth.canManageResources" :icon="Promotion" type="primary" @click="router.push('/resource-management')">
          进入我的资源管理
        </el-button>
      </div>
    </div>

    <el-skeleton v-if="loading" :rows="8" animated />
    <template v-else>
      <div class="collaboration-layout">
        <section class="surface panel-pad">
          <div class="section-head">
            <div>
              <h2>申请信息</h2>
              <p>优先说明你熟悉的方向、常用资料来源，以及能长期维护的资源范围。</p>
            </div>
            <el-tag v-if="canSubmit" type="info">可提交</el-tag>
          </div>

          <el-form label-position="top" class="resource-form-grid">
            <el-form-item label="申请理由">
              <el-input
                v-model="form.reason"
                type="textarea"
                :rows="5"
                :disabled="!canSubmit"
                placeholder="例如：长期整理 Vue / React 官方文档与课程资源，能持续补充前端学习资料。"
              />
            </el-form-item>
            <el-form-item label="擅长方向 / 来源说明">
              <el-input
                v-model="form.expertise"
                type="textarea"
                :rows="4"
                :disabled="!canSubmit"
                placeholder="例如：Vue、React、TypeScript；来源包括官方文档、GitHub Docs、公开课程页。"
              />
            </el-form-item>
          </el-form>

          <div class="item-actions">
            <el-button :icon="EditPen" type="primary" :loading="submitting" :disabled="!canSubmit" @click="submitApplication">
              {{ application?.status === 'REJECTED' ? '重新提交申请' : '提交申请' }}
            </el-button>
            <el-button @click="router.push('/resources')">返回资源库</el-button>
          </div>
        </section>

        <aside class="side-stack">
          <section class="surface panel-pad">
            <div class="section-head">
              <div>
                <h2>审核状态</h2>
                <p>{{ statusMeta.description }}</p>
              </div>
              <el-tag :type="statusMeta.type">{{ statusMeta.label }}</el-tag>
            </div>

            <div class="security-timeline">
              <div>
                <span>提交时间</span>
                <strong>{{ formatDate(application?.createdAt || null) }}</strong>
              </div>
              <div>
                <span>更新时间</span>
                <strong>{{ formatDate(application?.updatedAt || null) }}</strong>
              </div>
              <div>
                <span>审核时间</span>
                <strong>{{ formatDate(application?.reviewedAt || null) }}</strong>
              </div>
            </div>

            <el-alert
              :title="application?.reviewNote || '暂无审核说明'"
              :type="application?.status === 'REJECTED' ? 'warning' : 'info'"
              :closable="false"
              show-icon
            />
          </section>
        </aside>
      </div>
    </template>
  </section>
</template>
