import { defineStore } from 'pinia'
import { http, type ApiResponse } from '../api/http'

export interface UserSession {
  id: number
  nickname: string
  account: string
  role: string
  resourceManager: boolean
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
const USER_KEY = 'x-agent-study-user'

function persistUserSession(user: UserSession | null) {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
    return
  }
  localStorage.removeItem(USER_KEY)
}

export function writeStoredUserSession(user: UserSession | null) {
  persistUserSession(user)
}

export function readStoredUserSession(): UserSession | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    const parsed = JSON.parse(raw) as Partial<UserSession>
    if (
      typeof parsed.id !== 'number'
      || typeof parsed.nickname !== 'string'
      || typeof parsed.account !== 'string'
      || typeof parsed.role !== 'string'
    ) {
      return null
    }
    return {
      id: parsed.id,
      nickname: parsed.nickname,
      account: parsed.account,
      role: parsed.role,
      resourceManager: Boolean(parsed.resourceManager),
      avatarUrl: parsed.avatarUrl || null,
      emailVerified: Boolean(parsed.emailVerified),
      disabled: Boolean(parsed.disabled),
      disabledUntil: parsed.disabledUntil || null,
    }
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readStoredUserSession(),
    ready: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.role === 'ADMIN',
    isResourceManager: (state) => Boolean(state.user?.resourceManager),
    canManageResources(): boolean {
      return this.isAdmin || this.isResourceManager
    },
  },
  actions: {
    setSession(payload: AuthPayload) {
      this.token = payload.token
      this.user = payload.user
      this.ready = true
      localStorage.setItem(TOKEN_KEY, payload.token)
      localStorage.setItem(REFRESH_TOKEN_KEY, payload.refreshToken)
      persistUserSession(payload.user)
    },
    clearSession() {
      this.token = ''
      this.user = null
      this.ready = true
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(REFRESH_TOKEN_KEY)
      persistUserSession(null)
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
        persistUserSession(this.user)
      } catch {
        this.clearSession()
      } finally {
        this.ready = true
      }
    },
  },
})
