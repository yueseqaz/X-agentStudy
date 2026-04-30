<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http, type ApiResponse } from '../api/http'

interface ModelConfig {
  id: number
  provider: string
  modelName: string
  baseUrl: string | null
  apiKeyMask: string | null
  enabled: boolean
  updatedAt: string
}

const loading = ref(false)
const saving = ref(false)
const configs = ref<ModelConfig[]>([])
const form = reactive({
  provider: 'DeepSeek',
  modelName: 'deepseek-chat',
  baseUrl: 'https://api.deepseek.com',
  apiKey: '',
})

async function fetchConfigs() {
  loading.value = true
  try {
    const response = await http.get<ApiResponse<ModelConfig[]>>('/model-configs')
    configs.value = response.data.data
  } finally {
    loading.value = false
  }
}

async function saveConfig() {
  saving.value = true
  try {
    await http.post<ApiResponse<ModelConfig>>('/model-configs', { ...form })
    form.apiKey = ''
    ElMessage.success('模型配置已保存')
    fetchConfigs()
  } finally {
    saving.value = false
  }
}

onMounted(fetchConfigs)
</script>

<template>
  <div class="page-grid">
    <section class="surface panel-pad">
      <div class="section-head">
        <div>
          <h2>模型配置</h2>
          <p>仅管理员可维护平台模型网关。普通用户通过订阅额度使用 Agent，不支持自带 Key 绕过平台计费。</p>
        </div>
        <el-tag>LLM Gateway</el-tag>
      </div>

      <el-table v-loading="loading" :data="configs" class="data-table">
        <el-table-column prop="provider" label="供应商" width="130" />
        <el-table-column prop="modelName" label="模型" />
        <el-table-column prop="baseUrl" label="Base URL" />
        <el-table-column prop="apiKeyMask" label="Key" width="140" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <aside class="surface panel-pad">
      <div class="section-head">
        <div>
          <h2>新增配置</h2>
          <p>配置保存后作为平台级模型调用来源，密钥只在后端加密保存，前端不展示明文。</p>
        </div>
      </div>

      <el-form label-position="top" @submit.prevent="saveConfig">
        <el-form-item label="供应商">
          <el-input v-model="form.provider" />
        </el-form-item>
        <el-form-item label="模型名称">
          <el-input v-model="form.modelName" />
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input v-model="form.baseUrl" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="saving" style="width: 100%">保存配置</el-button>
      </el-form>
    </aside>
  </div>
</template>
