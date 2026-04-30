import { defineStore } from 'pinia'
import { http, type ApiResponse } from '../api/http'

export interface UserSession {
  id: number
  nickname: string
  account: string
  role: string
  avatarUrl: string | null
  emailVerified: boolean
  disabled: boolean
  disabledUntil: string | null
}

interface AuthPayload {
  token: string
  refreshToken: string
  user: UserSession
}

const TOKEN_KEY = 'x-agent-study-token'
const REFRESH_TOKEN_KEY = 'x-agent-study-refresh-token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null as UserSession | null,
    ready: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.role === 'ADMIN',
  },
  actions: {
    setSession(payload: AuthPayload) {
      this.token = payload.token
      this.user = payload.user
      localStorage.setItem(TOKEN_KEY, payload.token)
      localStorage.setItem(REFRESH_TOKEN_KEY, payload.refreshToken)
    },
    clearSession() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(REFRESH_TOKEN_KEY)
    },
    async login(payload: { account: string; password: string }) {
      const response = await http.post<ApiResponse<AuthPayload>>('/auth/login', payload)
      this.setSession(response.data.data)
    },
    async register(payload: { nickname: string; account: string; password: string }) {
      const response = await http.post<ApiResponse<AuthPayload>>('/auth/register', payload)
      this.setSession(response.data.data)
    },
    async fetchMe() {
      if (!this.token) {
        this.ready = true
        return
      }
      try {
        const response = await http.get<ApiResponse<UserSession>>('/auth/me')
        this.user = response.data.data
      } catch {
        this.clearSession()
      } finally {
        this.ready = true
      }
    },
  },
})
