<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Delete, Refresh, SwitchButton, UploadFilled, View, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type UploadRequestOptions } from 'element-plus'
import { http, type ApiResponse } from '../api/http'
import FirstRunGuide, { type GuideStep } from '../components/FirstRunGuide.vue'
import ResourcePreviewPanel, { type LearningResource } from '../components/ResourcePreviewPanel.vue'

interface Summary {
  users: number
  directions: number
  plans: number
  documents: number
  questions: number
  tasks: number
  runningTasks: number
  failedTasks: number
  subscriptions: number
  disabledUsers: number
  lockedUsers: number
  quotaUsed: number
  quotaTotal: number
}

interface AdminUser {
  id: number
  nickname: string
  account: string
  role: string
  avatarUrl: string | null
  disabled: boolean
  disabledUntil: string | null
  emailVerified: boolean
  failedLoginCount: number
  lockedUntil: string | null
  lastLoginAt: string | null
  createdAt: string
}

interface AdminTask {
  id: number
  planId: number | null
  taskType: string
  status: string
  inputPayload: string | null
  outputPayload: string | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
}

interface AdminQuota {
  userId: number
  nickname: string
  account: string
  planCode: string
  monthlyAgentQuota: number
  usedAgentCalls: number
  remainingAgentCalls: number
  storageQuotaMb: number
  usedStorageMb: number
  planQuota: number
  usedPlanCount: number
  walletBalanceCents: number
  usageRate: number
}

interface AuditLog {
  id: number
  actorUserId: number | null
  targetType: string
  targetId: number | null
  action: string
  detail: string | null
  createdAt: string
}

interface ModelConfig {
  id: number
  provider: string
  modelName: string
  baseUrl: string | null
  apiKeyMask: string | null
  enabled: boolean
  updatedAt: string
}

interface CourseCandidate {
  id: number
  title: string
  description: string | null
  courseUrl: string
  sourceName: string
  coverUrl: string | null
  subjectName: string
  subjectScope: string
  tags: string
  tagList: string[]
  difficulty: string | null
  status: string
  publishedResourceId: number | null
  createdAt: string
}

const loading = ref(false)
const summary = ref<Summary | null>(null)
const users = ref<AdminUser[]>([])
const recentTasks = ref<AdminTask[]>([])
const failedTasks = ref<AdminTask[]>([])
const quotas = ref<AdminQuota[]>([])
const auditLogs = ref<AuditLog[]>([])
const modelConfigs = ref<ModelConfig[]>([])
const resources = ref<LearningResource[]>([])
const courseCandidates = ref<CourseCandidate[]>([])
const activeAdminTab = ref('overview')
const activeTask = ref<AdminTask | null>(null)
const activeResource = ref<LearningResource | null>(null)
const taskDrawerVisible = ref(false)
const resourcePreviewVisible = ref(false)
const banUntilByUser = ref<Record<number, string>>({})
const rechargeYuanByUser = ref<Record<number, number>>({})
const savingModelConfig = ref(false)
const uploadingResource = ref(false)
const crawlingCourses = ref(false)
const editingModelConfig = ref<ModelConfig | null>(null)
const modelDialogVisible = ref(false)
const resourceForm = ref({
  title: '',
  description: '',
  subjectName: '',
  subjectScope: '',
  tags: '',
})
const courseCrawlerForm = ref({
  query: '',
  url: '',
  subjectName: '',
  subjectScope: '',
  tags: '',
  limit: 8,
})
const adminTabGuide = computed(() => {
  const map: Record<string, [string, [string, string][]]> = {
    overview: ['管理概览引导', [
      ['看平台状态', '概览展示用户、学习资产、题库、任务和额度使用。'],
      ['发现异常', '任务失败、用户锁定和额度异常都可以从这里快速发现。'],
    ]],
    model: ['模型网关引导', [
      ['统一维护模型', '这里配置平台使用的模型供应商、模型名称、Base URL 和 API Key。'],
      ['保存前会验证', '新增或修改配置时会先请求模型，验证通过后才保存。'],
    ]],
    resources: ['学习资源上传引导', [
      ['资源只在这里上传', '平台资源库由管理员维护，普通用户只能查看和预览。'],
      ['标签影响推荐', '学科名匹配方向名称，学科范畴匹配技能分类，标签用于补充命中。'],
      ['支持多种资源', '视频、音频、图片、PDF、Office 文档、Markdown 和 TXT 都可以上传。'],
      ['课程采集先审核', '当前只采集哔哩哔哩公开视频搜索结果，确认后才发布到资源库。'],
    ]],
    tasks: ['任务管理引导', [
      ['查看执行状态', '这里展示文档解析和 Agent 长任务的运行结果。'],
      ['处理失败任务', '失败的文档解析任务可以查看原因并重试。'],
    ]],
    quotas: ['额度管理引导', [
      ['查看用户额度', '这里按用户展示 Agent 调用额度、学习计划数量和钱包余额。'],
      ['人工处理额度', '管理员可以重置本期额度使用量或给用户充值。'],
    ]],
    users: ['用户安全引导', [
      ['查看账号状态', '这里展示邮箱验证、封禁、锁定和最近登录情况。'],
      ['处理风险账号', '可以临时或永久禁用用户，也可以恢复启用。'],
    ]],
    audit: ['审计日志引导', [
      ['追踪后台操作', '用户封禁、额度重置、任务重试等关键操作都会记录。'],
      ['定位责任和时间', '日志包含操作者、对象、动作、详情和时间。'],
    ]],
  }
  const matched = map[activeAdminTab.value] || map.overview
  return {
    key: `admin-tab-${activeAdminTab.value}`,
    title: matched[0],
    steps: matched[1].map(([title, body]) => ({ title, body })) as GuideStep[],
  }
})
const modelConfigForm = ref({
  provider: 'DeepSeek',
  modelName: 'deepseek-chat',
  baseUrl: 'https://api.deepseek.com',
  apiKey: '',
})

async function fetchAdmin() {
  loading.value = true
  try {
    const [
      summaryResponse,
      usersResponse,
      recentTasksResponse,
      failedTasksResponse,
      quotasResponse,
      auditLogsResponse,
      modelConfigsResponse,
      resourcesResponse,
      courseCandidatesResponse,
    ] = await Promise.all([
      http.get<ApiResponse<Summary>>('/admin/summary'),
      http.get<ApiResponse<AdminUser[]>>('/admin/users'),
      http.get<ApiResponse<AdminTask[]>>('/admin/tasks/recent'),
      http.get<ApiResponse<AdminTask[]>>('/admin/tasks/failed'),
      http.get<ApiResponse<AdminQuota[]>>('/admin/quotas'),
      http.get<ApiResponse<AuditLog[]>>('/admin/audit-logs'),
      http.get<ApiResponse<ModelConfig[]>>('/model-configs'),
      http.get<ApiResponse<LearningResource[]>>('/resources'),
      http.get<ApiResponse<CourseCandidate[]>>('/admin/course-crawler/candidates'),
    ])
    summary.value = summaryResponse.data.data
    users.value = usersResponse.data.data
    recentTasks.value = recentTasksResponse.data.data
    failedTasks.value = failedTasksResponse.data.data
    quotas.value = quotasResponse.data.data
    auditLogs.value = auditLogsResponse.data.data
    modelConfigs.value = modelConfigsResponse.data.data
    resources.value = resourcesResponse.data.data
    courseCandidates.value = courseCandidatesResponse.data.data
  } finally {
    loading.value = false
  }
}

function formatDate(value: string | null) {
  return value ? new Date(value).toLocaleString() : '-'
}

function taskTagType(status: string) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING') return 'warning'
  return 'info'
}

async function openTask(task: AdminTask) {
  const response = await http.get<ApiResponse<AdminTask>>(`/admin/tasks/${task.id}`)
  activeTask.value = response.data.data
  taskDrawerVisible.value = true
}

async function retryTask(task: AdminTask) {
  await http.post<ApiResponse<AdminTask>>(`/admin/tasks/${task.id}/retry`)
  ElMessage.success(`任务 #${task.id} 已重新提交`)
  await fetchAdmin()
}

async function resetQuota(row: AdminQuota) {
  await ElMessageBox.confirm(`确认重置 ${row.nickname}（${row.account}，#${row.userId}）的本期额度使用量？`, '重置额度', {
    confirmButtonText: '重置',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await http.post<ApiResponse<AdminQuota>>(`/admin/quotas/${row.userId}/reset`)
  ElMessage.success('额度已重置')
  await fetchAdmin()
}

async function rechargeWallet(row: AdminQuota) {
  const yuan = rechargeYuanByUser.value[row.userId]
  if (!yuan || yuan <= 0) {
    ElMessage.warning('请输入充值金额')
    return
  }
  await http.post<ApiResponse<AdminQuota>>(`/admin/quotas/${row.userId}/wallet/recharge`, {
    amountCents: Math.round(yuan * 100),
    remark: 'admin recharge',
  })
  ElMessage.success(`已给 ${row.nickname}（${row.account}）充值 ¥${yuan}`)
  rechargeYuanByUser.value[row.userId] = 0
  await fetchAdmin()
}

async function setUserDisabled(row: AdminUser, disabled: boolean) {
  await http.patch<ApiResponse<AdminUser>>(`/admin/users/${row.id}/disabled`, {
    disabled,
    disabledUntil: disabled ? banUntilByUser.value[row.id] || null : null,
  })
  ElMessage.success(disabled ? '用户已禁用' : '用户已启用')
  await fetchAdmin()
}

async function saveModelConfig() {
  if (!editingModelConfig.value && !modelConfigForm.value.apiKey.trim()) {
    ElMessage.warning('请输入 API Key')
    return
  }
  savingModelConfig.value = true
  try {
    if (editingModelConfig.value) {
      await http.put<ApiResponse<ModelConfig>>(`/model-configs/${editingModelConfig.value.id}`, { ...modelConfigForm.value })
      ElMessage.success('模型连通性验证通过，配置已更新')
    } else {
      await http.post<ApiResponse<ModelConfig>>('/model-configs', { ...modelConfigForm.value })
      ElMessage.success('模型连通性验证通过，配置已保存')
    }
    closeModelDialog()
    await fetchAdmin()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '模型配置保存失败')
  } finally {
    savingModelConfig.value = false
  }
}

function openCreateModelConfig() {
  editingModelConfig.value = null
  modelConfigForm.value = {
    provider: 'DeepSeek',
    modelName: 'deepseek-chat',
    baseUrl: 'https://api.deepseek.com',
    apiKey: '',
  }
  modelDialogVisible.value = true
}

function openEditModelConfig(row: ModelConfig) {
  editingModelConfig.value = row
  modelConfigForm.value = {
    provider: row.provider,
    modelName: row.modelName,
    baseUrl: row.baseUrl || '',
    apiKey: '',
  }
  modelDialogVisible.value = true
}

function closeModelDialog() {
  modelDialogVisible.value = false
  editingModelConfig.value = null
  modelConfigForm.value.apiKey = ''
}

async function deleteModelConfig(row: ModelConfig) {
  await ElMessageBox.confirm(`确认删除模型配置 ${row.provider} / ${row.modelName}？删除后平台会回退到运行环境默认配置。`, '删除模型配置', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
  try {
    await http.delete<ApiResponse<void>>(`/model-configs/${row.id}`)
    ElMessage.success('模型配置已删除')
    await fetchAdmin()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '模型配置删除失败')
  }
}

async function uploadResource(options: UploadRequestOptions) {
  if (!resourceForm.value.subjectName.trim() || !resourceForm.value.subjectScope.trim() || !resourceForm.value.tags.trim()) {
    ElMessage.warning('请先填写学科名、学科范畴和标签')
    options.onError(new Error('missing tags') as any)
    return
  }
  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('title', resourceForm.value.title)
  formData.append('description', resourceForm.value.description)
  formData.append('subjectName', resourceForm.value.subjectName)
  formData.append('subjectScope', resourceForm.value.subjectScope)
  formData.append('tags', resourceForm.value.tags)
  uploadingResource.value = true
  try {
    const response = await http.post<ApiResponse<LearningResource>>('/admin/resources', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000,
    })
    resources.value.unshift(response.data.data)
    resourceForm.value = {
      title: '',
      description: '',
      subjectName: '',
      subjectScope: '',
      tags: '',
    }
    ElMessage.success('学习资源已上传')
    options.onSuccess(response.data)
  } catch (error: any) {
    options.onError(error)
  } finally {
    uploadingResource.value = false
  }
}

async function crawlCourses() {
  if (!courseCrawlerForm.value.query.trim() && !courseCrawlerForm.value.url.trim()) {
    ElMessage.warning('请输入关键词或课程目录 URL')
    return
  }
  crawlingCourses.value = true
  try {
    const response = await http.post<ApiResponse<CourseCandidate[]>>('/admin/course-crawler/crawl', {
      ...courseCrawlerForm.value,
    }, { timeout: 60000 })
    courseCandidates.value = [...response.data.data, ...courseCandidates.value]
    ElMessage.success(`已发现 ${response.data.data.length} 个候选课程`)
  } finally {
    crawlingCourses.value = false
  }
}

async function publishCourseCandidate(candidate: CourseCandidate) {
  const response = await http.post<ApiResponse<LearningResource>>(`/admin/course-crawler/candidates/${candidate.id}/publish`)
  resources.value.unshift(response.data.data)
  courseCandidates.value = courseCandidates.value.map((item) =>
    item.id === candidate.id
      ? { ...item, status: 'PUBLISHED', publishedResourceId: response.data.data.id }
      : item,
  )
  ElMessage.success('课程已发布到资源库')
}

async function rejectCourseCandidate(candidate: CourseCandidate) {
  await http.post<ApiResponse<CourseCandidate>>(`/admin/course-crawler/candidates/${candidate.id}/reject`)
  courseCandidates.value = courseCandidates.value.map((item) =>
    item.id === candidate.id ? { ...item, status: 'REJECTED' } : item,
  )
  ElMessage.success('候选课程已忽略')
}

function previewResource(resource: LearningResource) {
  activeResource.value = resource
  resourcePreviewVisible.value = true
}

async function deleteResource(resource: LearningResource) {
  await ElMessageBox.confirm(`确认删除资源「${resource.title}」？`, '删除学习资源', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await http.delete<ApiResponse<void>>(`/admin/resources/${resource.id}`)
  resources.value = resources.value.filter((item) => item.id !== resource.id)
  ElMessage.success('学习资源已删除')
}

function prettyPayload(value: string | null) {
  if (!value) {
    return '-'
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function formatPrice(cents: number) {
  return `¥${Math.round((cents || 0) / 100)}`
}

onMounted(fetchAdmin)
</script>

<template>
  <section class="admin-console">
    <el-skeleton v-if="loading" :rows="8" animated />
    <template v-else>
      <div class="surface panel-pad admin-hero">
        <div>
          <h2>管理后台</h2>
          <p>用户安全、Agent 任务、额度水位和学习资产的运营控制台。</p>
        </div>
        <div class="topbar-actions">
          <el-tag type="warning">ADMIN</el-tag>
          <el-button :icon="Refresh" :loading="loading" @click="fetchAdmin">刷新</el-button>
        </div>
      </div>

      <el-tabs v-model="activeAdminTab" class="admin-tabs">
        <el-tab-pane label="概览" name="overview">
          <div v-if="summary" class="admin-metrics">
            <div class="metric-card"><span>用户</span><strong>{{ summary.users }}</strong><small>禁用 {{ summary.disabledUsers }} / 锁定 {{ summary.lockedUsers }}</small></div>
            <div class="metric-card"><span>学习资产</span><strong>{{ summary.plans }}</strong><small>{{ summary.directions }} 方向 / {{ summary.documents }} 文档</small></div>
            <div class="metric-card"><span>题库</span><strong>{{ summary.questions }}</strong><small>计划内沉淀题目</small></div>
            <div class="metric-card"><span>Agent 任务</span><strong>{{ summary.tasks }}</strong><small>运行 {{ summary.runningTasks }} / 失败 {{ summary.failedTasks }}</small></div>
            <div class="metric-card"><span>额度使用</span><strong>{{ summary.quotaUsed }}</strong><small>总额度 {{ summary.quotaTotal || 0 }}</small></div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="模型网关" name="model">
      <section class="surface panel-pad admin-model-gateway">
        <div class="section-head">
          <div>
            <h2>平台模型网关</h2>
            <p>管理员统一维护模型供应商和 Key。保存前会真实请求一次模型，收到回复后才写入配置。</p>
          </div>
          <div class="admin-inline-actions">
            <el-tag type="success">Admin only</el-tag>
            <el-button type="primary" @click="openCreateModelConfig">新增配置</el-button>
          </div>
        </div>

        <div class="admin-model-layout">
          <el-table :data="modelConfigs" class="data-table">
            <el-table-column prop="provider" label="供应商" width="130" />
            <el-table-column prop="modelName" label="模型" min-width="160" />
            <el-table-column prop="baseUrl" label="Base URL" min-width="220" />
            <el-table-column prop="apiKeyMask" label="Key" width="140" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" width="190">
              <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openEditModelConfig(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteModelConfig(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>
        </el-tab-pane>

        <el-tab-pane label="学习资源" name="resources">
          <section class="surface panel-pad admin-resource-panel">
            <div class="section-head">
              <div>
                <h2>学习资源库</h2>
                <p>上传视频、音频、图片和文档。学科名用于匹配方向名称，学科范畴用于匹配技能分类，标签用于补充命中。</p>
              </div>
              <el-tag type="success">{{ resources.length }} 个资源</el-tag>
            </div>

            <div class="admin-resource-layout">
              <div class="admin-resource-form">
                <div class="resource-match-guide">
                  <span>课程采集</span>
                  <strong>哔哩哔哩公开视频</strong>
                  <strong>先候选后发布</strong>
                  <p>输入关键词，系统会从哔哩哔哩公开视频搜索结果中采集课程标题、UP 主、封面、播放信息和链接。</p>
                </div>
                <el-form label-position="top">
                  <el-form-item label="关键词">
                    <el-input v-model="courseCrawlerForm.query" placeholder="例如：Redis 入门、Vue 组件设计" />
                  </el-form-item>
                  <el-form-item label="哔哩哔哩搜索 URL">
                    <el-input v-model="courseCrawlerForm.url" placeholder="可选，例如 search.bilibili.com 的搜索结果页" />
                  </el-form-item>
                  <el-form-item label="学科名">
                    <el-input v-model="courseCrawlerForm.subjectName" placeholder="为空则自动推断" />
                  </el-form-item>
                  <el-form-item label="学科范畴">
                    <el-input v-model="courseCrawlerForm.subjectScope" placeholder="例如：后端开发、前端开发" />
                  </el-form-item>
                  <el-form-item label="补充标签">
                    <el-input v-model="courseCrawlerForm.tags" placeholder="多个标签用逗号分隔" />
                  </el-form-item>
                  <el-button :icon="Refresh" type="primary" :loading="crawlingCourses" @click="crawlCourses">
                    采集公开课程
                  </el-button>
                </el-form>

                <div class="resource-match-guide">
                  <span>填写规则</span>
                  <strong>方向名称 ↔ 学科名 / 标签</strong>
                  <strong>技能分类 ↔ 学科范畴 / 标签</strong>
                  <p>例如学习方向是「Vue 前端」，资源学科名建议填「Vue」，学科范畴填「前端开发」，标签填「Vue, 组件, Composition API」。</p>
                </div>
                <el-form label-position="top">
                  <el-form-item label="标题">
                    <el-input v-model="resourceForm.title" placeholder="为空则使用文件名" />
                  </el-form-item>
                  <el-form-item label="简介">
                    <el-input v-model="resourceForm.description" type="textarea" :rows="3" placeholder="资源内容说明" />
                  </el-form-item>
                  <el-form-item label="学科名">
                    <el-input v-model="resourceForm.subjectName" placeholder="对应方向名称，例如：Vue、Java、线性代数" />
                  </el-form-item>
                  <el-form-item label="学科范畴">
                    <el-input v-model="resourceForm.subjectScope" placeholder="对应技能分类，例如：前端开发、后端开发、数学基础" />
                  </el-form-item>
                  <el-form-item label="标签">
                    <el-input v-model="resourceForm.tags" placeholder="补充可匹配关键词，多个标签用逗号分隔" />
                  </el-form-item>
                </el-form>
                <el-upload
                  drag
                  :show-file-list="false"
                  :http-request="uploadResource"
                  :disabled="uploadingResource"
                  accept=".pdf,.docx,.ppt,.pptx,.md,.txt,.mp4,.webm,.mov,.mp3,.wav,.m4a,.aac,.ogg,.png,.jpg,.jpeg,.gif,.webp"
                >
                  <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                  <div class="el-upload__text">拖拽资源到这里，或点击上传</div>
                  <template #tip>
                    <div class="el-upload__tip">支持视频、音频、图片、PDF、Office 文档、Markdown 和 TXT。</div>
                  </template>
                </el-upload>
              </div>

              <div class="admin-resource-list">
                <section class="course-candidate-list">
                  <div class="section-head">
                    <div>
                      <h2>候选课程</h2>
                      <p>采集结果需要确认后才会进入正式资源库。</p>
                    </div>
                    <el-tag>{{ courseCandidates.length }} 个候选</el-tag>
                  </div>
                  <el-empty v-if="courseCandidates.length === 0" description="暂无候选课程" />
                  <article v-for="candidate in courseCandidates" v-else :key="candidate.id" class="admin-resource-item">
                    <img
                      v-if="candidate.coverUrl"
                      class="course-candidate-cover"
                      :src="candidate.coverUrl"
                      :alt="candidate.title"
                    />
                    <div>
                      <div class="resource-card-head">
                        <el-tag :type="candidate.status === 'PUBLISHED' ? 'success' : candidate.status === 'REJECTED' ? 'info' : 'warning'">
                          {{ candidate.status }}
                        </el-tag>
                        <span>{{ candidate.sourceName }}</span>
                      </div>
                      <h3>{{ candidate.title }}</h3>
                      <p>{{ candidate.description || candidate.courseUrl }}</p>
                      <div class="tag-row">
                        <el-tag type="success">{{ candidate.subjectName }}</el-tag>
                        <el-tag type="info">{{ candidate.subjectScope }}</el-tag>
                        <el-tag v-for="tag in candidate.tagList" :key="tag">{{ tag }}</el-tag>
                      </div>
                    </div>
                    <div class="admin-inline-actions">
                      <el-button tag="a" :href="candidate.courseUrl" target="_blank">打开</el-button>
                      <el-button
                        type="primary"
                        :disabled="candidate.status === 'PUBLISHED'"
                        @click="publishCourseCandidate(candidate)"
                      >
                        发布
                      </el-button>
                      <el-button
                        :disabled="candidate.status === 'REJECTED'"
                        @click="rejectCourseCandidate(candidate)"
                      >
                        忽略
                      </el-button>
                    </div>
                  </article>
                </section>

                <el-empty v-if="resources.length === 0" description="暂无学习资源" />
                <article v-for="resource in resources" v-else :key="resource.id" class="admin-resource-item">
                  <div>
                    <div class="resource-card-head">
                      <el-tag>{{ resource.resourceType }}</el-tag>
                      <span>{{ formatDate(resource.createdAt) }}</span>
                    </div>
                    <h3>{{ resource.title }}</h3>
                    <p>{{ resource.description || resource.originalFilename }}</p>
                    <div class="tag-row">
                      <el-tag type="success">{{ resource.subjectName }}</el-tag>
                      <el-tag type="info">{{ resource.subjectScope }}</el-tag>
                      <el-tag v-for="tag in resource.tagList" :key="tag">{{ tag }}</el-tag>
                    </div>
                  </div>
                  <div class="admin-inline-actions">
                    <el-button :icon="View" @click="previewResource(resource)">预览</el-button>
                    <el-button :icon="Delete" type="danger" plain @click="deleteResource(resource)">删除</el-button>
                  </div>
                </article>
              </div>
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane label="任务" name="tasks">
      <div class="admin-grid">
        <section class="surface panel-pad">
          <div class="section-head">
            <div>
              <h2>最近任务</h2>
              <p>文档解析、Agent 长任务和失败任务状态。</p>
            </div>
          </div>
          <el-table :data="recentTasks" class="data-table">
            <el-table-column prop="id" label="ID" width="72" />
            <el-table-column prop="taskType" label="任务" min-width="140" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="taskTagType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="计划" width="90">
              <template #default="{ row }">#{{ row.planId || '-' }}</template>
            </el-table-column>
            <el-table-column label="更新时间" width="190">
              <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button :icon="View" size="small" @click="openTask(row)">详情</el-button>
                <el-button
                  v-if="row.status === 'FAILED' && row.taskType === 'DOCUMENT_PARSE'"
                  size="small"
                  type="warning"
                  @click="retryTask(row)"
                >
                  重试
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="surface panel-pad">
          <div class="section-head">
            <div>
              <h2>异常任务</h2>
              <p>优先处理解析失败和 Agent 执行失败。</p>
            </div>
            <el-icon><WarningFilled /></el-icon>
          </div>
          <el-empty v-if="failedTasks.length === 0" description="暂无失败任务" />
          <div v-else class="admin-alert-list">
            <div v-for="task in failedTasks" :key="task.id">
              <strong>#{{ task.id }} · {{ task.taskType }}</strong>
              <span>{{ formatDate(task.updatedAt) }}</span>
              <p>{{ task.errorMessage || '无错误信息' }}</p>
              <div class="admin-inline-actions">
                <el-button :icon="View" size="small" @click="openTask(task)">详情</el-button>
                <el-button
                  v-if="task.taskType === 'DOCUMENT_PARSE'"
                  size="small"
                  type="warning"
                  @click="retryTask(task)"
                >
                  重试
                </el-button>
              </div>
            </div>
          </div>
        </section>
      </div>
        </el-tab-pane>

        <el-tab-pane label="额度" name="quotas">
        <section class="surface panel-pad">
          <div class="section-head">
            <div>
              <h2>额度水位</h2>
              <p>按用户查看 Agent 调用额度使用情况。</p>
            </div>
          </div>
          <el-table :data="quotas" class="data-table">
            <el-table-column label="用户" min-width="210">
              <template #default="{ row }">
                <div class="admin-user-cell">
                  <strong>{{ row.nickname }}</strong>
                  <span>{{ row.account }}</span>
                  <small>#{{ row.userId }}</small>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="planCode" label="套餐" width="100" />
            <el-table-column label="Agent 额度" min-width="220">
              <template #default="{ row }">
                <div class="quota-cell">
                  <span>{{ row.usedAgentCalls }} / {{ row.monthlyAgentQuota }}</span>
                  <el-progress :percentage="row.usageRate" :stroke-width="8" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="学习计划" width="130">
              <template #default="{ row }">{{ row.usedPlanCount }} / {{ row.planQuota }} 个</template>
            </el-table-column>
            <el-table-column label="钱包" width="110">
              <template #default="{ row }">{{ formatPrice(row.walletBalanceCents) }}</template>
            </el-table-column>
            <el-table-column label="充值" width="210">
              <template #default="{ row }">
                <div class="admin-inline-actions">
                  <el-input-number
                    v-model="rechargeYuanByUser[row.userId]"
                    :min="0"
                    :step="10"
                    size="small"
                    controls-position="right"
                    style="width: 108px"
                  />
                  <el-button size="small" type="primary" @click="rechargeWallet(row)">充值</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110">
              <template #default="{ row }">
                <el-button size="small" @click="resetQuota(row)">重置</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
        </el-tab-pane>

        <el-tab-pane label="用户安全" name="users">
        <section class="surface panel-pad">
          <div class="section-head">
            <div>
              <h2>用户安全状态</h2>
              <p>邮箱验证、禁用、锁定和最近登录。</p>
            </div>
          </div>
          <el-table :data="users" class="data-table">
            <el-table-column prop="id" label="ID" width="66" />
            <el-table-column prop="account" label="账号" min-width="190" />
            <el-table-column label="状态" width="190">
              <template #default="{ row }">
                <div class="tag-row">
                  <el-tag :type="row.disabled ? 'danger' : 'success'">{{ row.disabled ? '禁用' : '启用' }}</el-tag>
                  <el-tag :type="row.emailVerified ? 'success' : 'warning'">{{ row.emailVerified ? '已验证' : '未验证' }}</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="失败/锁定" width="150">
              <template #default="{ row }">{{ row.failedLoginCount || 0 }} 次 / {{ row.lockedUntil ? '锁定' : '正常' }}</template>
            </el-table-column>
            <el-table-column label="封禁截止" width="190">
              <template #default="{ row }">
                <span v-if="row.disabled">{{ row.disabledUntil ? formatDate(row.disabledUntil) : '永久' }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="最近登录" width="190">
              <template #default="{ row }">{{ formatDate(row.lastLoginAt) }}</template>
            </el-table-column>
            <el-table-column label="封禁时间" width="210">
              <template #default="{ row }">
                <el-date-picker
                  v-model="banUntilByUser[row.id]"
                  type="datetime"
                  value-format="YYYY-MM-DDTHH:mm:ssZ"
                  placeholder="为空则永久"
                  style="width: 180px"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  :icon="SwitchButton"
                  size="small"
                  :type="row.disabled ? 'success' : 'danger'"
                  @click="setUserDisabled(row, !row.disabled)"
                >
                  {{ row.disabled ? '启用' : '禁用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
        </el-tab-pane>

        <el-tab-pane label="审计日志" name="audit">
      <section class="surface panel-pad">
        <div class="section-head">
          <div>
            <h2>审计日志</h2>
            <p>记录后台关键操作：用户封禁、额度重置、任务重试。</p>
          </div>
        </div>
        <el-table :data="auditLogs" class="data-table">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="action" label="动作" width="150" />
          <el-table-column label="操作者" width="100">
            <template #default="{ row }">#{{ row.actorUserId || '-' }}</template>
          </el-table-column>
          <el-table-column label="对象" width="160">
            <template #default="{ row }">{{ row.targetType }} #{{ row.targetId || '-' }}</template>
          </el-table-column>
          <el-table-column prop="detail" label="详情" min-width="240" />
          <el-table-column label="时间" width="190">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </section>
        </el-tab-pane>
      </el-tabs>

      <el-drawer v-model="taskDrawerVisible" size="560px" title="任务详情">
        <div v-if="activeTask" class="admin-task-detail">
          <div class="task-detail-head">
            <div>
              <span>Task #{{ activeTask.id }}</span>
              <h3>{{ activeTask.taskType }}</h3>
            </div>
            <el-tag :type="taskTagType(activeTask.status)">{{ activeTask.status }}</el-tag>
          </div>
          <div class="security-timeline">
            <div>
              <span>计划</span>
              <strong>#{{ activeTask.planId || '-' }}</strong>
            </div>
            <div>
              <span>创建时间</span>
              <strong>{{ formatDate(activeTask.createdAt) }}</strong>
            </div>
            <div>
              <span>更新时间</span>
              <strong>{{ formatDate(activeTask.updatedAt) }}</strong>
            </div>
          </div>
          <section>
            <h3>输入</h3>
            <pre>{{ prettyPayload(activeTask.inputPayload) }}</pre>
          </section>
          <section>
            <h3>输出</h3>
            <pre>{{ prettyPayload(activeTask.outputPayload) }}</pre>
          </section>
          <section v-if="activeTask.errorMessage">
            <h3>错误</h3>
            <pre>{{ activeTask.errorMessage }}</pre>
          </section>
        </div>
      </el-drawer>

      <el-dialog
        v-model="modelDialogVisible"
        :title="editingModelConfig ? '编辑模型配置' : '新增模型配置'"
        width="520px"
        @closed="closeModelDialog"
      >
        <el-form class="admin-model-form" label-position="top" @submit.prevent="saveModelConfig">
          <el-form-item label="供应商">
            <el-input v-model="modelConfigForm.provider" />
          </el-form-item>
          <el-form-item label="模型名称">
            <el-input v-model="modelConfigForm.modelName" />
          </el-form-item>
          <el-form-item label="Base URL">
            <el-input v-model="modelConfigForm.baseUrl" />
          </el-form-item>
          <el-form-item :label="editingModelConfig ? 'API Key（留空则沿用原 Key）' : 'API Key'">
            <el-input v-model="modelConfigForm.apiKey" type="password" show-password />
          </el-form-item>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="点击保存后会先请求一次模型，验证成功才会写入配置。"
          />
        </el-form>
        <template #footer>
          <el-button @click="modelDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingModelConfig" @click="saveModelConfig">验证并保存</el-button>
        </template>
      </el-dialog>

      <ResourcePreviewPanel v-model:visible="resourcePreviewVisible" :resource="activeResource" />
      <FirstRunGuide
        :guide-key="adminTabGuide.key"
        :title="adminTabGuide.title"
        :steps="adminTabGuide.steps"
      />
    </template>
  </section>
</template>
