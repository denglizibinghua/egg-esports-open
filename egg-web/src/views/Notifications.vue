<template>
  <div class="notifications-page">
    <div class="page-header">
      <h1 class="page-title">通知</h1>
      <el-button v-if="unreadCount > 0" type="text" class="mark-all-btn" @click="markAllRead">
        全部标为已读 ({{ unreadCount }})
      </el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="全部" name="all"></el-tab-pane>
      <el-tab-pane label="未读" name="unread"></el-tab-pane>
    </el-tabs>

    <!-- Loading skeleton -->
    <div v-if="loading" class="skeleton-list">
      <div v-for="i in 5" :key="'skel-' + i" class="skeleton-card">
        <div class="skeleton-card-icon">
          <div class="skeleton-line skeleton-line--icon"></div>
        </div>
        <div class="skeleton-card-body">
          <div class="skeleton-line skeleton-line--title"></div>
          <div class="skeleton-line skeleton-line--desc"></div>
          <div class="skeleton-line skeleton-line--time"></div>
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-else-if="notifications.length === 0" class="empty-state">
      <svg class="empty-svg" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M40 12 C28 12, 20 22, 20 34 L20 46 L16 52 L64 52 L60 46 L60 34 C60 22, 52 12, 40 12 Z" stroke="#5b8cff" stroke-width="2" fill="rgba(91,140,255,0.06)"/>
        <path d="M34 52 C34 56, 36 60, 40 60 C44 60, 46 56, 46 52" stroke="#5b8cff" stroke-width="1.5" fill="none"/>
        <circle cx="40" cy="10" r="3" fill="#5b8cff" opacity="0.4"/>
        <line x1="40" y1="7" x2="40" y2="4" stroke="#5b8cff" stroke-width="1" stroke-linecap="round" opacity="0.3"/>
        <line x1="36" y1="8" x2="34" y2="6" stroke="#5b8cff" stroke-width="1" stroke-linecap="round" opacity="0.2"/>
        <line x1="44" y1="8" x2="46" y2="6" stroke="#5b8cff" stroke-width="1" stroke-linecap="round" opacity="0.2"/>
        <path d="M28 36 Q34 32, 40 36 Q46 40, 52 36" stroke="#6b6b7c" stroke-width="1" fill="none" opacity="0.2"/>
        <path d="M28 42 Q34 38, 40 42 Q46 46, 52 42" stroke="#6b6b7c" stroke-width="1" fill="none" opacity="0.15"/>
      </svg>
      <h3 class="empty-title">暂无通知</h3>
      <p class="empty-desc">比赛结算或开始前会收到通知</p>
    </div>

    <!-- Notification list -->
    <div v-else class="notification-list">
      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-card"
        :class="{ 'notification-card--unread': item.isRead === 0 }"
        @click="handleClick(item)"
      >
        <div class="notification-card-icon">
          <i :class="getIcon(item.type)"></i>
        </div>
        <div class="notification-card-body">
          <div class="notification-card-header">
            <span class="notification-card-title">{{ item.title }}</span>
            <span v-if="item.isRead === 0" class="unread-dot"></span>
          </div>
          <div class="notification-card-content">{{ item.content }}</div>
          <div class="notification-card-footer">
            <span class="notification-card-type">{{ getTypeLabel(item.type) }}</span>
            <span class="notification-card-time">{{ formatTime(item.createTime) }}</span>
          </div>
        </div>
      </div>
    </div>

    <el-pagination
      v-if="total > pageSize"
      class="pagination"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page.sync="currentPage"
      @current-change="fetchNotifications"
    />
  </div>
</template>

<script>
import request from '@/utils/request'
import { getToken } from '@/utils/auth'

export default {
  name: 'NotificationList',
  data() {
    return {
      loading: true,
      notifications: [],
      total: 0,
      currentPage: 1,
      pageSize: 20,
      activeTab: 'all',
      pollTimer: null,
      unreadCount: 0
    }
  },
  created() {
    if (!getToken()) {
      this.$router.push('/login')
      return
    }
    this.fetchNotifications()
    this.fetchUnreadCount()
    this.startPolling()
  },
  beforeDestroy() {
    this.stopPolling()
  },
  methods: {
    fetchNotifications() {
      this.loading = true
      var params = {
        page: this.currentPage,
        size: this.pageSize
      }
      if (this.activeTab === 'unread') {
        params.unreadOnly = true
      }
      request.get('/notification/list', { params: params }).then(function (res) {
        var data = res.data || res
        if (Array.isArray(data)) {
          this.notifications = data
          this.total = data.length
        } else if (data.records) {
          this.notifications = data.records
          this.total = data.total || data.records.length
        } else {
          this.notifications = data.list || data.content || []
          this.total = data.total || data.totalElements || 0
        }
      }.bind(this)).catch(function () {}).finally(function () {
        this.loading = false
      }.bind(this))
    },
    fetchUnreadCount() {
      request.get('/notification/unread-count').then(function (res) {
        var data = res.data || res
        this.unreadCount = data || 0
      }.bind(this)).catch(function () {})
    },
    handleClick(item) {
      if (item.isRead === 0) {
        request.put('/notification/read/' + item.id).then(function () {
          item.isRead = 1
          this.unreadCount = Math.max(0, this.unreadCount - 1)
        }.bind(this)).catch(function () {})
      }
      if (item.type === 1) {
        // 比赛即将开始
        if (item.relatedId) {
          this.$router.push('/match/' + item.relatedId)
        } else {
          this.$router.push('/matches')
        }
      } else if (item.type === 2) {
        // 押注已结算
        this.$router.push('/matches')
      } else if (item.type === 3 || item.type === 4) {
        // 评论被回复 / 被@提及：跳转到对应赛事评论区
        if (item.relatedId) {
          this.$router.push('/match/' + item.relatedId)
        } else {
          this.$router.push('/matches')
        }
      }
    },
    markAllRead() {
      request.put('/notification/read-all').then(function () {
        this.unreadCount = 0
        this.notifications.forEach(function (n) { n.isRead = 1 })
        this.$message.success('已全部标为已读')
      }.bind(this)).catch(function () {})
    },
    handleTabClick() {
      this.currentPage = 1
      this.fetchNotifications()
    },
    startPolling() {
      var self = this
      this.pollTimer = setInterval(function () {
        if (!document.hidden) {
          self.fetchUnreadCount()
        }
      }, 30000)
    },
    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },
    getIcon(type) {
      var icons = {
        1: 'el-icon-bell',
        2: 'el-icon-s-check',
        3: 'el-icon-chat-dot-square',
        4: 'el-icon-attract'
      }
      return icons[type] || 'el-icon-info'
    },
    getTypeLabel(type) {
      var labels = {
        1: '系统通知',
        2: '结算通知',
        3: '评论回复',
        4: '@提及'
      }
      return labels[type] || '通知'
    },
    formatTime(time) {
      if (!time) return ''
      var d = new Date(time)
      var now = new Date()
      var diff = now - d
      if (diff < 60000) return '刚刚'
      if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
      if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
      var month = (d.getMonth() + 1).toString().padStart(2, '0')
      var day = d.getDate().toString().padStart(2, '0')
      var hour = d.getHours().toString().padStart(2, '0')
      var min = d.getMinutes().toString().padStart(2, '0')
      return month + '-' + day + ' ' + hour + ':' + min
    }
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 32px;
  letter-spacing: 1px;
}

.mark-all-btn {
  color: var(--accent-secondary) !important;
  font-size: 13px !important;
  font-weight: 500;
}

.mark-all-btn:hover {
  color: var(--accent-primary) !important;
}

/* Notification List */
.notification-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notification-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  cursor: pointer;
  transition: transform var(--duration-normal) var(--ease-out),
              border-color var(--duration-normal) var(--ease-out),
              box-shadow var(--duration-normal) var(--ease-out);
  animation: card-enter 0.4s var(--ease-out) both;
}

.notification-card:nth-child(1) { animation-delay: 0ms; }
.notification-card:nth-child(2) { animation-delay: 30ms; }
.notification-card:nth-child(3) { animation-delay: 60ms; }
.notification-card:nth-child(4) { animation-delay: 90ms; }
.notification-card:nth-child(5) { animation-delay: 120ms; }
.notification-card:nth-child(6) { animation-delay: 150ms; }
.notification-card:nth-child(7) { animation-delay: 180ms; }
.notification-card:nth-child(8) { animation-delay: 210ms; }
.notification-card:nth-child(9) { animation-delay: 240ms; }
.notification-card:nth-child(10) { animation-delay: 270ms; }

@keyframes card-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.notification-card:hover {
  transform: translateY(-2px);
  border-color: var(--border-strong);
  box-shadow: var(--shadow-lg);
}

.notification-card--unread {
  border-left: 2px solid var(--accent-primary);
  background: linear-gradient(90deg, rgba(0, 229, 160, 0.03) 0%, var(--bg-surface-1) 30%);
}

.notification-card-icon {
  font-size: 20px;
  color: var(--accent-secondary);
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.notification-card-body {
  flex: 1;
  min-width: 0;
}

.notification-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.notification-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.unread-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--accent-primary);
  box-shadow: 0 0 6px var(--accent-primary);
  flex-shrink: 0;
  animation: dot-pulse 2s ease-in-out infinite;
}

@keyframes dot-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.notification-card-content {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notification-card-footer {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notification-card-type {
  font-size: 11px;
  font-weight: 500;
  color: var(--text-tertiary);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  padding: 1px 8px;
  letter-spacing: 0.03em;
}

.notification-card-time {
  font-size: 12px;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

/* Pagination */
.pagination {
  margin-top: 24px;
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  text-align: center;
  max-width: 400px;
  margin: 0 auto;
}

.empty-svg {
  width: 72px;
  height: 72px;
  margin-bottom: 20px;
  opacity: 0.7;
  animation: empty-float 3s ease-in-out infinite;
}

@keyframes empty-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-tertiary);
  max-width: 300px;
}

/* Skeleton Loading */
@keyframes shimmer {
  0% { background-position: -200% 0; transform: scaleY(1); }
  50% { transform: scaleY(1.012); }
  100% { background-position: 200% 0; transform: scaleY(1); }
}
@keyframes skeleton-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  animation: skeleton-fade-in 0.3s ease both;
}

.skeleton-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  pointer-events: none;
}

.skeleton-card-icon {
  flex-shrink: 0;
}

.skeleton-card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-card .skeleton-line {
  background: linear-gradient(90deg, var(--bg-surface-2) 0%, var(--bg-surface-3) 35%, rgba(0,229,160,0.03) 50%, var(--bg-surface-3) 65%, var(--bg-surface-2) 100%);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
  border-radius: var(--radius-sm);
}

.skeleton-line--icon { width: 28px; height: 28px; border-radius: var(--radius-md); }
.skeleton-line--title { width: 180px; height: 16px; }
.skeleton-line--desc { width: 100%; height: 14px; }
.skeleton-line--time { width: 100px; height: 12px; }

@media (max-width: 767px) {
  .page-title {
    font-size: 24px;
  }
  .notification-card {
    padding: 12px 14px;
    gap: 10px;
  }
  .page-header {
    gap: 8px;
  }
}

@media (max-width: 479px) {
  .page-title {
    font-size: 20px;
  }
  .notification-card {
    padding: 10px 12px;
    gap: 8px;
  }
}
</style>
