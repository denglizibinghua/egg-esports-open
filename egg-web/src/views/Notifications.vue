<template>
  <div class="notifications-page">
    <header class="page-header">
      <div class="header-main">
        <span class="page-eyebrow">// INBOX · 消息中心</span>
        <h1 class="page-title">通知</h1>
        <p class="page-sub">比赛开始与结算提醒 · 共 {{ total }} 条</p>
      </div>
      <div class="header-actions">
        <el-button v-if="unreadCount > 0" link class="mark-all-btn" @click="markAllRead">
          全部标为已读 ({{ unreadCount }})
        </el-button>
      </div>
    </header>

    <el-tabs v-model="activeTab" class="inbox-tabs" @tab-click="handleTabClick">
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
      <div class="empty-mark">//</div>
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
          <el-icon><component :is="getIcon(item.type)" /></el-icon>
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
            <button
              v-if="item.isRead === 0"
              type="button"
              class="mark-one-btn"
              @click.stop="markOneRead(item)"
            >标为已读</button>
          </div>
        </div>
        <span class="notification-card-arrow" aria-hidden="true">›</span>
      </div>
    </div>

    <el-pagination
      v-if="total > pageSize"
      class="pagination"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      v-model:current-page="currentPage"
      @current-change="fetchNotifications"
    />
  </div>
</template>

<script>
import request from '@/utils/request'
// 分页数据提取工具（兼容 records/list/content），统一提取自 @/utils/pagination
import { extractList } from '@/utils/pagination'
import { getToken } from '@/utils/auth'
import { Bell, CircleCheck, ChatDotSquare, MagicStick, Message, InfoFilled } from '@element-plus/icons-vue'

export default {
  name: 'NotificationList',
  components: { Bell, CircleCheck, ChatDotSquare, MagicStick, Message, InfoFilled },
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
  beforeUnmount() {
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
        // 兼容 records/list/content 多种后端返回格式
        this.notifications = extractList(data)
        if (Array.isArray(data)) {
          this.total = data.length
        } else if (data.records) {
          this.total = data.total || data.records.length
        } else {
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
      } else if (item.type === 5) {
        // 公告通知：跳转到主页
        this.$router.push('/')
      }
    },
    markOneRead(item) {
      if (item.isRead !== 0) return
      request.put('/notification/read/' + item.id).then(function () {
        item.isRead = 1
        this.unreadCount = Math.max(0, this.unreadCount - 1)
      }.bind(this)).catch(function () {})
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
        1: Bell,
        2: CircleCheck,
        3: ChatDotSquare,
        4: MagicStick,
        5: Message
      }
      return icons[type] || InfoFilled
    },
    getTypeLabel(type) {
      var labels = {
        1: '系统通知',
        2: '结算通知',
        3: '评论回复',
        4: '@提及',
        5: '公告通知'
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
.notifications-page {
  max-width: 900px;
  margin: 0 auto;
}

/* ---------- Header ---------- */
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 16px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--border-subtle);
  position: relative;
}

.page-header::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 44px;
  height: 2px;
  background: var(--brand);
}

.header-main {
  min-width: 0;
}

.page-eyebrow {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--brand);
  margin-bottom: 8px;
}

.page-title {
  font-family: var(--font-body);
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.15;
  color: var(--text-primary);
}

.page-sub {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
  letter-spacing: 0.03em;
  font-variant-numeric: tabular-nums;
}

.header-actions {
  flex-shrink: 0;
  padding-bottom: 2px;
}

.mark-all-btn {
  color: var(--text-secondary) !important;
  font-size: 12px !important;
  font-weight: 600;
  font-family: var(--font-mono);
  letter-spacing: 0.03em;
}

.mark-all-btn:hover {
  color: var(--brand) !important;
}

/* ---------- Tabs ---------- */
.inbox-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.inbox-tabs :deep(.el-tabs__item) {
  font-family: var(--font-mono);
  font-size: 12px !important;
  letter-spacing: 0.1em;
  font-weight: 700 !important;
  height: 42px !important;
}

/* ---------- List ---------- */
.notification-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notification-card {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 18px 15px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-left: 2px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition:
    transform var(--duration-normal) var(--ease-out),
    border-color var(--duration-normal) var(--ease-out),
    background var(--duration-fast) ease,
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
  border-color: var(--border-default);
  border-left-color: var(--brand-line);
  box-shadow: var(--shadow-md);
}

.notification-card--unread {
  border-left-color: var(--brand);
  background: var(--bg-surface-1);
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--brand) 6%, transparent) 0%, transparent 44%),
    var(--bg-surface-1);
}

.notification-card--unread:hover {
  border-color: color-mix(in srgb, var(--brand) 28%, var(--border-subtle));
  border-left-color: var(--brand);
}

.notification-card-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 1px;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  color: var(--text-tertiary);
  font-size: 17px;
  transition: color var(--duration-fast) ease, border-color var(--duration-fast) ease;
}

.notification-card--unread .notification-card-icon {
  color: var(--brand);
  border-color: var(--brand-line);
  background: var(--brand-soft);
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
  min-width: 0;
}

.notification-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.45;
  overflow-wrap: anywhere;
  min-width: 0;
}

.notification-card--unread .notification-card-title {
  font-weight: 700;
}

.unread-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--brand);
  box-shadow: 0 0 8px var(--brand);
  flex-shrink: 0;
}

.notification-card-content {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow-wrap: anywhere;
}

.notification-card-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.notification-card-type {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-tertiary);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  padding: 1px 7px;
  white-space: nowrap;
}

.notification-card-time {
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.03em;
  color: var(--text-faint);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.mark-one-btn {
  margin-left: auto;
  padding: 1px 0;
  background: transparent;
  border: 0;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.06em;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: color var(--duration-fast) ease;
}

.mark-one-btn:hover {
  color: var(--brand);
}

.notification-card-arrow {
  flex-shrink: 0;
  align-self: center;
  font-family: var(--font-mono);
  font-size: 18px;
  line-height: 1;
  color: var(--text-faint);
  transition: color var(--duration-fast) ease, transform var(--duration-fast) ease;
}

.notification-card:hover .notification-card-arrow {
  color: var(--brand);
  transform: translateX(2px);
}

/* ---------- Pagination ---------- */
.pagination {
  margin-top: 24px;
}

/* ---------- Empty state ---------- */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 56px 24px;
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-lg);
  background: var(--bg-surface-1);
}

.empty-mark {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
  font-family: var(--font-mono);
  font-size: 20px;
  font-weight: 700;
  color: var(--brand);
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-md);
}

.empty-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.empty-desc {
  font-size: 13px;
  color: var(--text-tertiary);
  max-width: 300px;
  line-height: 1.7;
}

/* ---------- Skeleton ---------- */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
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
  border-radius: var(--radius-md);
  padding: 16px 18px;
  min-height: 96px;
  pointer-events: none;
}

.skeleton-card-icon {
  flex-shrink: 0;
}

.skeleton-card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 9px;
  min-width: 0;
}

.skeleton-card .skeleton-line {
  background: linear-gradient(
    90deg,
    var(--bg-surface-2) 0%,
    var(--bg-surface-3) 40%,
    var(--bg-surface-2) 80%
  );
  background-size: 200% 100%;
  animation: shimmer 1.6s ease-in-out infinite;
  border-radius: var(--radius-sm);
}

.skeleton-line--icon { width: 40px; height: 40px; border-radius: var(--radius-sm); }
.skeleton-line--title { width: 42%; min-width: 120px; height: 14px; }
.skeleton-line--desc { width: 100%; height: 12px; }
.skeleton-line--time { width: 96px; height: 10px; margin-top: 2px; }

/* ---------- Responsive ---------- */
@media (max-width: 767px) {
  .page-title {
    font-size: 26px;
  }

  .notification-card {
    padding: 14px 14px 13px;
    gap: 11px;
  }

  .notification-card-icon {
    width: 36px;
    height: 36px;
    font-size: 16px;
  }

  .notification-card-arrow {
    display: none;
  }

  .skeleton-card {
    padding: 14px;
    gap: 11px;
  }
}

@media (max-width: 480px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .header-actions {
    padding-bottom: 0;
  }

  .page-title {
    font-size: 24px;
  }

  .notification-card {
    padding: 12px;
    gap: 10px;
  }

  .notification-card-content {
    -webkit-line-clamp: 3;
  }

  .notification-card-footer {
    gap: 8px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .notification-card {
    animation: none;
  }
}
</style>
