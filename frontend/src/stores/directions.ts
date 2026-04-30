import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { http, type ApiResponse } from '../api/http'

export interface Direction {
  id: number
  name: string
  category: string
  description: string
  lastActiveAt: string
}

export const useDirectionStore = defineStore('directions', {
  state: () => ({
    directions: [] as Direction[],
    loading: false,
  }),
  actions: {
    async fetchDirections() {
      this.loading = true
      try {
        const response = await http.get<ApiResponse<Direction[]>>('/directions')
        this.directions = response.data.data
      } finally {
        this.loading = false
      }
    },
    async createDirection(payload: { name: string; category: string; description: string }) {
      const response = await http.post<ApiResponse<Direction>>('/directions', payload)
      this.directions.unshift(response.data.data)
      ElMessage.success('学习方向已创建')
      return response.data.data
    },
    async deleteDirection(id: number) {
      await http.delete<ApiResponse<null>>(`/directions/${id}`)
      this.directions = this.directions.filter((direction) => direction.id !== id)
      ElMessage.success('学习方向已删除')
    },
  },
})
