import axios from 'axios'
import { ElMessage } from 'element-plus'
import { router } from '../router'

export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('x-agent-study-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('x-agent-study-token')
      if (router.currentRoute.value.path !== '/login') {
        router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      }
    } else if (error.response?.status === 403) {
      ElMessage.error(error.response?.data?.message || '当前账号没有权限访问该功能')
    } else if (
      ['QUOTA_EXCEEDED', 'PLAN_QUOTA_EXCEEDED', 'STORAGE_QUOTA_EXCEEDED', 'PLAN_DOWNGRADE_FORBIDDEN', 'PLAN_ALREADY_ACTIVE']
        .includes(error.response?.data?.code)
    ) {
      ElMessage.error(error.response?.data?.message || '当前套餐额度不足，请升级后继续')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else if (error.response?.data?.code === 'RESOURCE_NOT_FOUND') {
      return Promise.reject(error)
    } else if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else if (error.response?.status >= 500) {
      ElMessage.error('服务端处理失败，请稍后重试')
    } else if (!error.response) {
      ElMessage.error('无法连接后端服务，请确认服务已启动')
    }
    return Promise.reject(error)
  },
)

export interface ApiResponse<T> {
  code: string
  message: string
  data: T
}
