<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Edit, Plus, Refresh, UploadFilled } from '@element-plus/icons-vue'
import { http, type ApiResponse } from '../api/http'
import type {
  CandidateResource,
  CollaboratorWorkspace,
  IngestionTask,
  ResourceSource,
} from '../types/resourceCollaboration'

const loading = ref(false)
const savingSource = ref(false)
const savingTask = ref(false)
const savingCandidate = ref(false)
const submittingCandidateId = ref<number | null>(null)
const workspace = ref<CollaboratorWorkspace>({
  sources: [],
  tasks: [],
  candidates: [],
  publishedResources: [],
})
const activeTab = ref('sources')
const editingCandidateId = ref<number | null>(null)

const sourceForm = ref({
  name: '',
  sourceType: 'DOCUMENT',
  baseUrl: '',
  sourceCategory: '',
})

const taskForm = ref({
  sourceId: undefined as number | undefined,
  targetUrl: '',
  title: '',
  summary: '',
  tags: '',
  authorName: '',
  coverImageUrl: '',
  durationSeconds: undefined as number | undefined,
  rawContent: '',
})

const candidateForm = ref({
  title: '',
  summary: '',
  tags: '',
  authorName: '',
  coverImageUrl: '',
  durationSeconds: undefined as number | undefined,
  rawContent: '',
})

const sourceOptions = computed(() => workspace.value.sources.map((source) => ({
  label: `${source.name} · ${source.baseUrl}`,
  value: source.id,
})))

const summaryCards = computed(() => [
  { label: '我的来源', value: workspace.value.sources.length, hint: '名下可跟踪来源' },
  { label: '采集任务', value: workspace.value.tasks.length, hint: '只统计当前账号任务' },
  { label: '候选资源', value: workspace.value.candidates.length, hint: '待整理与待审核资源' },
  { label: '正式入库', value: workspace.value.publishedResources.length, hint: '已通过管理员审核' },
])

onMounted(fetchWorkspace)

async function fetchWorkspace() {
  loading.value = true
  try {
    const response = await http.get<ApiResponse<CollaboratorWorkspace>>('/resource-collaboration/workspace')
    workspace.value = response.data.data
  } catch {
    workspace.value = {
      sources: [],
      tasks: [],
      candidates: [],
      publishedResources: [],
    }
  } finally {
    loading.value = false
  }
}

async function createSource() {
  if (!sourceForm.value.name.trim() || !sourceForm.value.baseUrl.trim()) {
    ElMessage.warning('请填写来源名称和基础链接')
    return
  }
  savingSource.value = true
  try {
    await http.post<ApiResponse<ResourceSource>>('/resource-collaboration/sources', {
      name: sourceForm.value.name.trim(),
      sourceType: sourceForm.value.sourceType,
      baseUrl: sourceForm.value.baseUrl.trim(),
      sourceCategory: sourceForm.value.sourceCategory.trim() || null,
    })
    ElMessage.success('来源已创建')
    sourceForm.value = {
      name: '',
      sourceType: 'DOCUMENT',
      baseUrl: '',
      sourceCategory: '',
    }
    await fetchWorkspace()
    activeTab.value = 'sources'
  } finally {
    savingSource.value = false
  }
}

async function createTask() {
  if (!taskForm.value.sourceId || !taskForm.value.targetUrl.trim() || !taskForm.value.title.trim()) {
    ElMessage.warning('请先选择来源，并填写采集链接和标题')
    return
  }
  savingTask.value = true
  try {
    await http.post<ApiResponse<IngestionTask>>('/resource-collaboration/tasks', {
      sourceId: taskForm.value.sourceId,
      targetUrl: taskForm.value.targetUrl.trim(),
      title: taskForm.value.title.trim(),
      summary: taskForm.value.summary.trim() || null,
      tags: taskForm.value.tags.trim() || null,
      authorName: taskForm.value.authorName.trim() || null,
      coverImageUrl: taskForm.value.coverImageUrl.trim() || null,
      durationSeconds: taskForm.value.durationSeconds || null,
      rawContent: taskForm.value.rawContent.trim() || null,
    })
    ElMessage.success('采集任务已创建，候选资源也已同步生成')
    taskForm.value = {
      sourceId: undefined,
      targetUrl: '',
      title: '',
      summary: '',
      tags: '',
      authorName: '',
      coverImageUrl: '',
      durationSeconds: undefined,
      rawContent: '',
    }
    await fetchWorkspace()
    activeTab.value = 'candidates'
  } finally {
    savingTask.value = false
  }
}

async function saveCandidate() {
  if (!editingCandidateId.value || !candidateForm.value.title.trim()) {
    ElMessage.warning('请先从候选资源列表选择一条记录再整理')
    return
  }
  savingCandidate.value = true
  try {
    await http.patch<ApiResponse<CandidateResource>>(`/resource-collaboration/candidates/${editingCandidateId.value}`, {
      title: candidateForm.value.title.trim(),
      summary: candidateForm.value.summary.trim() || null,
      tags: candidateForm.value.tags.trim() || null,
      authorName: candidateForm.value.authorName.trim() || null,
      coverImageUrl: candidateForm.value.coverImageUrl.trim() || null,
      durationSeconds: candidateForm.value.durationSeconds || null,
      rawContent: candidateForm.value.rawContent.trim() || null,
    })
    ElMessage.success('候选资源已更新')
    resetCandidateForm()
    await fetchWorkspace()
    activeTab.value = 'candidates'
  } finally {
    savingCandidate.value = false
  }
}

function startEditCandidate(candidate: CandidateResource) {
  editingCandidateId.value = candidate.id
  candidateForm.value = {
    title: candidate.title,
    summary: candidate.summary || '',
    tags: candidate.tags || '',
    authorName: candidate.authorName || '',
    coverImageUrl: candidate.coverImageUrl || '',
    durationSeconds: candidate.durationSeconds || undefined,
    rawContent: candidate.rawContent || '',
  }
}

function resetCandidateForm() {
  editingCandidateId.value = null
  candidateForm.value = {
    title: '',
    summary: '',
    tags: '',
    authorName: '',
    coverImageUrl: '',
    durationSeconds: undefined,
    rawContent: '',
  }
}

async function submitCandidate(candidate: CandidateResource) {
  submittingCandidateId.value = candidate.id
  try {
    await http.post<ApiResponse<CandidateResource>>(`/resource-collaboration/candidates/${candidate.id}/submit`)
    ElMessage.success('候选资源已提交审核')
    await fetchWorkspace()
  } finally {
    submittingCandidateId.value = null
  }
}

function formatDate(value: string | null) {
  return value ? new Date(value).toLocaleString() : '-'
}

function taskTagType(status: string) {
  if (status === 'FAILED' || status === 'REJECTED') return 'danger'
  if (status === 'RUNNING' || status === 'PENDING') return 'warning'
  if (status === 'SUCCESS' || status === 'APPROVED' || status === 'COMPLETED') return 'success'
  return 'info'
}

function sourceTagType(source: ResourceSource) {
  return source.enabled ? 'success' : 'info'
}
</script>

<template>
  <section class="resource-management-page">
    <div class="surface panel-pad collaboration-hero">
      <div>
        <span>资源协作工作台</span>
        <h2>我的资源管理</h2>
        <p>管理自己的来源、采集任务与候选资源，整理完成后再提交管理员审核。</p>
      </div>
      <div class="resource-entry-actions">
        <el-tag type="warning">仅本人资源</el-tag>
        <el-button :icon="Refresh" :loading="loading" @click="fetchWorkspace">刷新</el-button>
      </div>
    </div>

    <el-skeleton v-if="loading" :rows="10" animated />
    <template v-else>
      <div class="admin-metrics collaborator-metrics">
        <div v-for="card in summaryCards" :key="card.label" class="metric-card">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.hint }}</small>
        </div>
      </div>

      <div class="collaboration-layout">
        <section class="surface panel-pad">
          <div class="section-head">
            <div>
              <h2>新增来源</h2>
              <p>先登记你负责维护的来源站点或频道，后续采集任务和候选资源都归到自己的来源名下。</p>
            </div>
            <el-tag type="success">来源登记</el-tag>
          </div>
          <el-form label-position="top" class="resource-form-grid">
            <div class="resource-two-column">
              <el-form-item label="来源名称">
                <el-input v-model="sourceForm.name" placeholder="例如：Spring 官方文档 / Bilibili 某课程合集" />
              </el-form-item>
              <el-form-item label="来源类型">
                <el-select v-model="sourceForm.sourceType">
                  <el-option label="文档" value="DOCUMENT" />
                  <el-option label="视频" value="VIDEO" />
                </el-select>
              </el-form-item>
            </div>
            <el-form-item label="基础链接">
              <el-input v-model="sourceForm.baseUrl" placeholder="https://example.com 或 https://www.bilibili.com/video/..." />
            </el-form-item>
            <el-form-item label="来源分类">
              <el-input v-model="sourceForm.sourceCategory" placeholder="例如：官方文档、课程页、Bilibili" />
            </el-form-item>
          </el-form>
          <div class="item-actions">
            <el-button :icon="Plus" type="primary" :loading="savingSource" @click="createSource">新增来源</el-button>
          </div>
        </section>

        <section class="surface panel-pad">
          <div class="section-head">
            <div>
              <h2>新建采集任务</h2>
              <p>从自己的来源里选择一条，录入目标链接后会自动生成一条候选资源，后面再继续整理。</p>
            </div>
            <el-tag type="info">任务创建</el-tag>
          </div>
          <el-form label-position="top" class="resource-form-grid">
            <div class="resource-two-column">
              <el-form-item label="关联来源">
                <el-select v-model="taskForm.sourceId" placeholder="请选择">
                  <el-option v-for="source in sourceOptions" :key="source.value" :label="source.label" :value="source.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="作者 / UP 主">
                <el-input v-model="taskForm.authorName" placeholder="选填" />
              </el-form-item>
            </div>
            <el-form-item label="待采集链接">
              <el-input v-model="taskForm.targetUrl" placeholder="https://example.com/docs/getting-started" />
            </el-form-item>
            <el-form-item label="任务标题">
              <el-input v-model="taskForm.title" placeholder="例如：Vue 官方入门文档采集" />
            </el-form-item>
            <el-form-item label="摘要">
              <el-input v-model="taskForm.summary" type="textarea" :rows="3" placeholder="资源内容、适合阶段、覆盖范围" />
            </el-form-item>
            <div class="resource-two-column">
              <el-form-item label="标签">
                <el-input v-model="taskForm.tags" placeholder="多个标签用逗号分隔" />
              </el-form-item>
              <el-form-item label="时长（秒）">
                <el-input-number v-model="taskForm.durationSeconds" :min="0" :step="60" controls-position="right" />
              </el-form-item>
            </div>
            <el-form-item label="封面链接">
              <el-input v-model="taskForm.coverImageUrl" placeholder="选填" />
            </el-form-item>
            <el-form-item label="正文或补充内容">
              <el-input v-model="taskForm.rawContent" type="textarea" :rows="4" placeholder="文档类可以补正文，视频类会自动按索引信息处理。" />
            </el-form-item>
          </el-form>
          <div class="item-actions">
            <el-button :icon="Plus" type="primary" :loading="savingTask" @click="createTask">创建采集任务</el-button>
          </div>
        </section>

        <section class="surface panel-pad">
          <div class="section-head">
            <div>
              <h2>{{ editingCandidateId ? '整理候选资源' : '候选资源整理' }}</h2>
              <p>采集任务创建后会自动生成候选资源，在这里补充摘要、标签和正文，再提交审核。</p>
            </div>
            <el-tag :type="editingCandidateId ? 'warning' : 'info'">{{ editingCandidateId ? '编辑中' : '请先选择候选资源' }}</el-tag>
          </div>
          <el-form label-position="top" class="resource-form-grid">
            <el-form-item label="标题">
              <el-input v-model="candidateForm.title" placeholder="候选资源标题" />
            </el-form-item>
            <el-form-item label="摘要">
              <el-input v-model="candidateForm.summary" type="textarea" :rows="3" placeholder="资源内容、适合阶段、覆盖范围" />
            </el-form-item>
            <div class="resource-two-column">
              <el-form-item label="标签">
                <el-input v-model="candidateForm.tags" placeholder="多个标签用逗号分隔" />
              </el-form-item>
              <el-form-item label="作者 / UP 主">
                <el-input v-model="candidateForm.authorName" placeholder="选填" />
              </el-form-item>
            </div>
            <div class="resource-two-column">
              <el-form-item label="封面链接">
                <el-input v-model="candidateForm.coverImageUrl" placeholder="选填" />
              </el-form-item>
              <el-form-item label="时长（秒）">
                <el-input-number v-model="candidateForm.durationSeconds" :min="0" :step="60" controls-position="right" />
              </el-form-item>
            </div>
            <el-form-item label="正文或补充内容">
              <el-input v-model="candidateForm.rawContent" type="textarea" :rows="5" placeholder="文档类可以补正文，视频类会保持索引信息。" />
            </el-form-item>
          </el-form>
          <div class="item-actions">
            <el-button :icon="UploadFilled" type="primary" :loading="savingCandidate" @click="saveCandidate">保存修改</el-button>
            <el-button v-if="editingCandidateId" @click="resetCandidateForm">取消编辑</el-button>
          </div>
        </section>
      </div>

      <section class="surface panel-pad collaborator-workspace">
        <div class="section-head">
          <div>
            <h2>我的协作数据</h2>
            <p>来源、任务和候选资源都只展示当前账号名下数据。</p>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="admin-tabs collaborator-tabs">
          <el-tab-pane label="我的来源" name="sources">
            <el-empty v-if="workspace.sources.length === 0" description="暂无来源" />
            <div v-else class="resource-source-grid">
              <article v-for="source in workspace.sources" :key="source.id" class="resource-source-card">
                <div class="resource-card-head">
                  <el-tag :type="sourceTagType(source)">{{ source.enabled ? 'ENABLED' : 'DISABLED' }}</el-tag>
                  <span>{{ formatDate(source.updatedAt) }}</span>
                </div>
                <h3>{{ source.name }}</h3>
                <p>{{ source.baseUrl }} · {{ source.sourceType }} · {{ source.sourceCategory || '未分类' }}</p>
                <div class="tag-row">
                  <el-tag>归属用户 #{{ source.ownerUserId }}</el-tag>
                  <el-tag type="info">{{ formatDate(source.createdAt) }}</el-tag>
                </div>
              </article>
            </div>
          </el-tab-pane>

          <el-tab-pane label="我的采集任务" name="tasks">
            <el-table :data="workspace.tasks" class="data-table">
              <el-table-column prop="titleHint" label="任务" min-width="220" />
              <el-table-column prop="sourceType" label="来源类型" min-width="120" />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="taskTagType(row.status)">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="targetUrl" label="链接" min-width="240" show-overflow-tooltip />
              <el-table-column label="更新时间" width="190">
                <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="我的候选资源" name="candidates">
            <el-table :data="workspace.candidates" class="data-table">
              <el-table-column prop="title" label="标题" min-width="220" />
              <el-table-column prop="sourceType" label="来源类型" width="100" />
              <el-table-column label="采集模式" min-width="180">
                <template #default="{ row }">{{ row.contentCaptureMode }} / 资源 #{{ row.publishedResourceId || '-' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="taskTagType(row.reviewStatus)">{{ row.reviewStatus }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="审核说明" min-width="200" show-overflow-tooltip>
                <template #default="{ row }">{{ row.reviewNote || '-' }}</template>
              </el-table-column>
              <el-table-column label="更新时间" width="190">
                <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="220" fixed="right">
                <template #default="{ row }">
                  <div class="admin-inline-actions">
                    <el-button :icon="Edit" size="small" @click="startEditCandidate(row)">编辑</el-button>
                    <el-button
                      :icon="Check"
                      size="small"
                      type="primary"
                      :loading="submittingCandidateId === row.id"
                      :disabled="row.reviewStatus === 'PENDING' || row.reviewStatus === 'APPROVED'"
                      @click="submitCandidate(row)"
                    >
                      提交审核
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="正式入库" name="published">
            <el-table :data="workspace.publishedResources" class="data-table">
              <el-table-column prop="title" label="标题" min-width="220" />
              <el-table-column prop="resourceType" label="类型" width="100" />
              <el-table-column label="方向" min-width="180">
                <template #default="{ row }">{{ row.subjectName }} / {{ row.subjectScope }}</template>
              </el-table-column>
              <el-table-column prop="tags" label="标签" min-width="180" show-overflow-tooltip />
              <el-table-column label="入库时间" width="190">
                <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </section>
    </template>
  </section>
</template>
