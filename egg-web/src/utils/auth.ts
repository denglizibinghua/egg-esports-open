import type { UserInfo } from '@/types'

const TOKEN_KEY = 'egg_token'
const USER_KEY = 'egg_user'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export function getUser(): UserInfo | null {
  const userStr = localStorage.getItem(USER_KEY)
  if (!userStr) return null
  try {
    return JSON.parse(userStr) as UserInfo
  } catch (e) {
    return null
  }
}

export function setUser(user: UserInfo): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function removeUser(): void {
  localStorage.removeItem(USER_KEY)
}

export function isLoggedIn(): boolean {
  return !!getToken()
}

export function isAdmin(): boolean {
  const user = getUser()
  return !!user && user.role === 1
}