import { defineStore } from 'pinia'
import request from '@/utils/request'
import { getToken, setToken, removeToken, getUser, setUser, removeUser } from '@/utils/auth'
import type { UserInfo, ApiResponse } from '@/types'

interface LoginResponse {
  token: string
  user: UserInfo
}

interface AuthState {
  user: UserInfo | null
  token: string | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    token: null
  }),
  getters: {
    isLoggedIn: (state): boolean => !!state.token,
    isAdmin: (state): boolean => !!state.user && state.user.role === 1,
    currentUser: (state): UserInfo | null => state.user
  },
  actions: {
    async login(username: string, password: string): Promise<void> {
      const res = await request.post<unknown, ApiResponse<LoginResponse>>('/user/login', {
        username,
        password
      })
      const { token, user } = res.data
      this.token = token
      this.user = user
      setToken(token)
      setUser(user)
    },
    logout(): void {
      this.token = null
      this.user = null
      removeToken()
      removeUser()
    },
    restoreFromStorage(): void {
      this.token = getToken()
      this.user = getUser()
    }
  }
})