<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Search, View } from '@element-plus/icons-vue'
import { http, type ApiResponse } from '../api/http'
import ResourcePreviewPanel, { type LearningResource } from '../components/ResourcePreviewPanel.vue'

const props = defineProps<{
  embeddedPlanId?: number
}>()

const loading = ref(false)
const resources = ref<LearningResource[]>([])
const keyword = ref('')
const typeFilter = ref('')
const subjectFilter = ref('')
const scopeFilter = ref('')
const tagFilter = ref('')
const activeResource = ref<LearningResource | null>(null)
const previewVisible = ref(false)

const filteredResources = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  return resources.value.filter((resource) =>
    (!typeFilter.value || resource.resourceType === typeFilter.value)
    && (!subjectFilter.value || resource.subjectName === subjectFilter.value)
    && (!scopeFilter.value || resource.subjectScope === scopeFilter.value)
    && (!tagFilter.value || resource.tagList.includes(tagFilter.value))
    && (!value || [
      resource.title,
      resource.description || '',
      resource.subjectName,
      resource.subjectScope,
      resource.tags,
      resource.originalFilename,
    ].some((item) => item.toLowerCase().includes(value))),
  )
})
const subjectOptions = computed(() => unique(resources.value.map((resource) => resource.subjectName)))
const scopeOptions = computed(() => unique(resources.value.map((resource) => resource.subjectScope)))
const tagOptions = computed(() => unique(resources.value.flatMap((resource) => resource.tagList)))

watch(
  () => props.embeddedPlanId,
  () => fetchResources(),
)

onMounted(fetchResources)

async function fetchResources() {
  loading.value = true
  try {
    const endpoint = props.embeddedPlanId
      ? `/plans/${props.embeddedPlanId}/resources/recommended`
      : '/resources'
    const response = await http.get<ApiResponse<LearningResource[]>>(endpoint)
    resources.value = response.data.data
  } finally {
    loading.value = false
  }
}

function openPreview(resource: LearningResource) {
  activeResource.value = resource
  previewVisible.value = true
}

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}

function resetFilters() {
  keyword.value = ''
  typeFilter.value = ''
  subjectFilter.value = ''
  scopeFilter.value = ''
  tagFilter.value = ''
}

function unique(values: string[]) {
  return Array.from(new Set(values.map((value) => value.trim()).filter(Boolean))).sort()
}
</script>

<template>
  <section class="resource-library" :class="{ embedded: embeddedPlanId }">
    <div class="section-head">
      <div>
        <h2>{{ embeddedPlanId ? '推荐学习资源' : '学习资源库' }}</h2>
        <p>{{ embeddedPlanId ? '按学习方向名、技能分类与资源的学科名、学科范畴、标签匹配。' : '平台统一沉淀的视频、音频、图片和文档资源。' }}</p>
      </div>
      <el-tag>{{ resources.length }} 个资源</el-tag>
    </div>

    <div class="resource-match-guide">
      <span>匹配关系</span>
      <strong>方向名称 ↔ 学科名 / 标签</strong>
      <strong>技能分类 ↔ 学科范畴 / 标签</strong>
      <p>资源上传时字段越接近学习方向，推荐结果越准确。例如方向名称「Vue 前端」，资源学科名填「Vue」，学科范畴填「前端开发」。</p>
    </div>

    <div class="resource-toolbar">
      <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索标题、学科、范畴或标签" />
      <el-button @click="resetFilters">清空</el-button>
      <el-button @click="fetchResources">刷新</el-button>
    </div>

    <div class="resource-filter-panel">
      <div>
        <span>类型</span>
        <button :class="{ active: !typeFilter }" type="button" @click="typeFilter = ''">全部</button>
        <button :class="{ active: typeFilter === 'VIDEO' }" type="button" @click="typeFilter = 'VIDEO'">视频</button>
        <button :class="{ active: typeFilter === 'AUDIO' }" type="button" @click="typeFilter = 'AUDIO'">音频</button>
        <button :class="{ active: typeFilter === 'IMAGE' }" type="button" @click="typeFilter = 'IMAGE'">图片</button>
        <button :class="{ active: typeFilter === 'DOCUMENT' }" type="button" @click="typeFilter = 'DOCUMENT'">文档</button>
      </div>
      <div>
        <span>学科名</span>
        <button :class="{ active: !subjectFilter }" type="button" @click="subjectFilter = ''">全部</button>
        <button
          v-for="subject in subjectOptions"
          :key="subject"
          :class="{ active: subjectFilter === subject }"
          type="button"
          @click="subjectFilter = subject"
        >
          {{ subject }}
        </button>
      </div>
      <div>
        <span>学科范畴</span>
        <button :class="{ active: !scopeFilter }" type="button" @click="scopeFilter = ''">全部</button>
        <button
          v-for="scope in scopeOptions"
          :key="scope"
          :class="{ active: scopeFilter === scope }"
          type="button"
          @click="scopeFilter = scope"
        >
          {{ scope }}
        </button>
      </div>
      <div>
        <span>标签</span>
        <button :class="{ active: !tagFilter }" type="button" @click="tagFilter = ''">全部</button>
        <button
          v-for="tag in tagOptions"
          :key="tag"
          :class="{ active: tagFilter === tag }"
          type="button"
          @click="tagFilter = tag"
        >
          {{ tag }}
        </button>
      </div>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <el-empty v-else-if="resources.length === 0" :description="embeddedPlanId ? '暂无匹配资源' : '暂无学习资源'" />
    <el-empty v-else-if="filteredResources.length === 0" description="没有匹配的资源" />
    <div v-else class="resource-card-grid">
      <article v-for="resource in filteredResources" :key="resource.id" class="resource-card">
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
        <el-button :icon="View" type="primary" @click="openPreview(resource)">预览</el-button>
      </article>
    </div>

    <ResourcePreviewPanel v-model:visible="previewVisible" :resource="activeResource" />
  </section>
</template>
