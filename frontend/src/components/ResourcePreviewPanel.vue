<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { http, type ApiResponse } from '../api/http'

export interface LearningResource {
  id: number
  title: string
  description: string | null
  resourceType: string
  originalFilename: string
  contentType: string | null
  subjectName: string
  subjectScope: string
  tags: string
  tagList: string[]
  fileSize: number
  createdAt: string
}

interface PreviewResponse {
  resource: LearningResource
  previewType: string
  content: string
}

const visible = defineModel<boolean>('visible', { default: false })
const props = defineProps<{
  resource: LearningResource | null
}>()

const loading = ref(false)
const preview = ref<PreviewResponse | null>(null)
const objectUrl = ref('')

const canUseObjectUrl = computed(() =>
  ['VIDEO', 'AUDIO', 'IMAGE'].includes(props.resource?.resourceType || '') || preview.value?.previewType === 'PDF',
)

watch(
  () => [visible.value, props.resource?.id],
  () => {
    if (visible.value && props.resource) {
      loadPreview(props.resource)
    } else {
      clearObjectUrl()
      preview.value = null
    }
  },
  { immediate: true },
)

onBeforeUnmount(clearObjectUrl)

async function loadPreview(resource: LearningResource) {
  loading.value = true
  clearObjectUrl()
  try {
    if (resource.resourceType === 'DOCUMENT') {
      const response = await http.get<ApiResponse<PreviewResponse>>(`/resources/${resource.id}/preview`)
      preview.value = response.data.data
    } else {
      preview.value = { resource, previewType: resource.resourceType, content: '' }
    }
    if (canUseObjectUrl.value) {
      const fileResponse = await http.get(`/resources/${resource.id}/file`, { responseType: 'blob' })
      objectUrl.value = URL.createObjectURL(fileResponse.data)
    }
  } catch {
    ElMessage.error('资源预览失败')
  } finally {
    loading.value = false
  }
}

function clearObjectUrl() {
  if (objectUrl.value) {
    URL.revokeObjectURL(objectUrl.value)
    objectUrl.value = ''
  }
}

function formatFileSize(size: number) {
  if (!size) {
    return '0 KB'
  }
  if (size < 1024 * 1024) {
    return `${Math.max(1, Math.round(size / 1024))} KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
</script>

<template>
  <el-dialog v-model="visible" :title="resource?.title || '资源预览'" width="860px" class="resource-preview-dialog">
    <el-skeleton v-if="loading" :rows="8" animated />
    <div v-else-if="resource" class="resource-preview">
      <div class="resource-preview-meta">
        <el-tag>{{ resource.resourceType }}</el-tag>
        <el-tag type="success">{{ resource.subjectName }}</el-tag>
        <el-tag type="info">{{ resource.subjectScope }}</el-tag>
        <el-tag type="warning">{{ formatFileSize(resource.fileSize) }}</el-tag>
      </div>

      <video v-if="resource.resourceType === 'VIDEO' && objectUrl" class="resource-media" controls :src="objectUrl" />
      <audio v-else-if="resource.resourceType === 'AUDIO' && objectUrl" class="resource-audio" controls :src="objectUrl" />
      <img v-else-if="resource.resourceType === 'IMAGE' && objectUrl" class="resource-image-preview" :src="objectUrl" :alt="resource.title" />
      <iframe v-else-if="preview?.previewType === 'PDF' && objectUrl" class="resource-document-frame" :src="objectUrl" />
      <pre v-else-if="preview?.previewType === 'TEXT'" class="resource-text-preview">{{ preview.content }}</pre>
      <el-empty v-else description="当前资源暂不支持在线预览" />
    </div>
  </el-dialog>
</template>
