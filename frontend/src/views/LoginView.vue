<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http, type ApiResponse } from '../api/http'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const resetVisible = ref(false)
const resetLoading = ref(false)
const resetToken = ref('')

const form = reactive({
  nickname: '',
  account: 'demo@xagentstudy.local',
  password: 'demo123456',
})

const resetForm = reactive({
  account: 'demo@xagentstudy.local',
  token: '',
  newPassword: '',
})

interface TokenResponse {
  message: string
  token: string
}

async function submit() {
  loading.value = true
  try {
    if (mode.value === 'login') {
      await auth.login({ account: form.account, password: form.password })
      ElMessage.success('登录成功')
    } else {
      await auth.register({
        nickname: form.nickname || form.account,
        account: form.account,
        password: form.password,
      })
      ElMessage.success('注册成功')
    }
    router.push((route.query.redirect as string) || '/')
  } catch (error: any) {
    const code = error.response?.data?.code
    const message = error.response?.data?.message || '登录失败'
    if (code === 'ACCOUNT_DISABLED') {
      ElMessage.error(message)
    } else {
      ElMessage.error(message)
    }
  } finally {
    loading.value = false
  }
}

async function requestResetToken() {
  if (!resetForm.account.trim()) {
    ElMessage.warning('请先填写账号')
    return
  }
  resetLoading.value = true
  try {
    const response = await http.post<ApiResponse<TokenResponse>>('/auth/password-reset/request', {
      account: resetForm.account,
    })
    resetToken.value = response.data.data.token
    resetForm.token = response.data.data.token
    ElMessage.success('密码重置 token 已生成')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '生成重置 token 失败')
  } finally {
    resetLoading.value = false
  }
}

async function confirmResetPassword() {
  if (!resetForm.token || !resetForm.newPassword) {
    ElMessage.warning('请填写 token 和新密码')
    return
  }
  resetLoading.value = true
  try {
    await http.post('/auth/password-reset/confirm', {
      token: resetForm.token,
      newPassword: resetForm.newPassword,
    })
    form.account = resetForm.account
    form.password = resetForm.newPassword
    resetVisible.value = false
    ElMessage.success('密码已重置，请使用新密码登录')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '重置密码失败')
  } finally {
    resetLoading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-panel">
      <div>
        <span class="eyebrow">X-AgentStudy</span>
        <h1>登录学习工作台</h1>
        <p>登录后学习方向、知识库、题库和复习记录都会按用户隔离。</p>
      </div>

      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item v-if="mode === 'register'" label="昵称">
          <el-input v-model="form.nickname" placeholder="你的昵称" />
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="form.account" placeholder="邮箱或账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%">
          {{ mode === 'login' ? '登录' : '注册并进入' }}
        </el-button>
      </el-form>

      <el-button link type="primary" @click="mode = mode === 'login' ? 'register' : 'login'">
        {{ mode === 'login' ? '没有账号，创建一个' : '已有账号，返回登录' }}
      </el-button>
      <el-button v-if="mode === 'login'" link @click="resetVisible = true; resetForm.account = form.account">
        忘记密码？
      </el-button>
    </section>

    <el-dialog v-model="resetVisible" title="忘记密码" width="420px">
      <el-form label-position="top">
        <el-form-item label="账号">
          <el-input v-model="resetForm.account" placeholder="邮箱或账号" />
        </el-form-item>
        <el-button :loading="resetLoading" style="width: 100%; margin-bottom: 14px" @click="requestResetToken">
          生成重置 token
        </el-button>
        <el-form-item label="重置 token">
          <el-input v-model="resetForm.token" placeholder="本地开发环境会自动填入" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-alert
          v-if="resetToken"
          type="info"
          :closable="false"
          show-icon
          title="本地开发模式已生成 token，生产环境应改为邮件发送。"
        />
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetLoading" @click="confirmResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </main>
</template>
