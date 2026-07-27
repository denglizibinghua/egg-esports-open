import { defineStore } from 'pinia'
import request from '@/utils/request'
import type { NotificationInfo, ApiResponse } from '@/types'

interface NotificationState {
  unreadCount: number
  notifications: NotificationInfo[]
  loading: boolean
}

let pollTimer: ReturnType<typeof setInterval> | null = null

export const useNotificationStore = defineStore('notification', {
  state: (): NotificationState => ({
    unreadCount: 0,
    notifications: [],
    loading: false
  }),
  actions: {
    async fetchUnreadCount(): Promise<void> {
      try {
        const res = await request.get<unknown, ApiResponse<number>>('/notification/unread/count')
        this.unreadCount = res.data
      } catch (e) {
        // 静默失败：未读数轮询不应打断用户
      }
    },
    async fetchList(): Promise<void> {
      this.loading = true
      try {
        const res = await request.get<unknown, ApiResponse<NotificationInfo[]>>('/notification/list')
        this.notifications = res.data
      } finally {
        this.loading = false
      }
    },
    async markRead(id: number): Promise<void> {
      await request.post<unknown, ApiResponse<unknown>>(`/notification/read/${id}`)
      const target = this.notifications.find(n => n.id === id)
      if (target && !target.isRead) {
        target.isRead = true
        this.unreadCount = Math.max(0, this.unreadCount - 1)
      }
    },
    async markAllRead(): Promise<void> {
      await request.post<unknown, ApiResponse<unknown>>('/notification/read/all')
      this.notifications.forEach(n => { n.isRead = true })
      this.unreadCount = 0
    },
    /** 登录后启动 30s 轮询未读数 */
    startPolling(): void {
      this.stopPolling()
      this.fetchUnreadCount()
      pollTimer = setInterval(() => this.fetchUnreadCount(), 30000)
    },
    /** 登出或离开页面时停止轮询 */
    stopPolling(): void {
      if (pollTimer !== null) {
        clearInterval(pollTimer)
        pollTimer = null
      }
    }
  }
})