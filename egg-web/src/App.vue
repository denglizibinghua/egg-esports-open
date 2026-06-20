<template>
  <div id="app">
    <div class="grain-overlay" aria-hidden="true"></div>
    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <router-link to="/" class="logo">EGG ESPORTS</router-link>
        </div>
        <div class="header-right">
          <el-button type="text" class="nav-btn" :class="{ 'nav-btn--active': $route.path === '/' }" @click="$router.push('/')">主页</el-button>
          <el-button type="text" class="nav-btn" :class="{ 'nav-btn--active': $route.path === '/matches' }" @click="$router.push('/matches')">赛程</el-button>
          <el-button type="text" class="nav-btn" :class="{ 'nav-btn--active': $route.path === '/leaderboard' }" @click="$router.push('/leaderboard')">排行榜</el-button>
          <el-button v-if="isAdmin" type="text" class="nav-btn admin-btn" :class="{ 'nav-btn--active': $route.path.startsWith('/admin') }" @click="$router.push('/admin')">管理</el-button>
          <span class="nav-divider"></span>
          <template v-if="isLoggedIn">
            <!-- Notification Bell -->
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="nav-badge">
              <el-popover
                placement="bottom"
                width="360"
                trigger="click"
                v-model="notificationPopoverVisible"
              >
                <div class="notification-preview">
                  <div class="notification-preview-header">
                    <span class="notification-preview-title">通知</span>
                    <el-button type="text" size="small" @click="markAllRead">全部已读</el-button>
                  </div>
                  <div v-if="recentNotifications.length === 0" class="notification-empty">
                    暂无通知
                  </div>
                  <div v-else>
                    <div
                      v-for="item in recentNotifications"
                      :key="item.id"
                      class="notification-item"
                      :class="{ 'notification-item--unread': item.isRead === 0 }"
                      @click="handleNotificationClick(item)"
                    >
                      <div class="notification-item-icon">
                        <i :class="getNotificationIcon(item.type)"></i>
                      </div>
                      <div class="notification-item-content">
                        <div class="notification-item-title">{{ item.title }}</div>
                        <div class="notification-item-desc">{{ item.content }}</div>
                        <div class="notification-item-time">{{ formatNotificationTime(item.createTime) }}</div>
                      </div>
                    </div>
                  </div>
                  <div class="notification-preview-footer">
                    <router-link to="/notifications" @click.native="notificationPopoverVisible = false">
                      查看全部通知
                    </router-link>
                  </div>
                </div>
                <el-button slot="reference" class="nav-btn nav-bell-btn" :class="{ 'has-notification': unreadCount > 0 }">
                  <i class="el-icon-bell"></i>
                </el-button>
              </el-popover>
            </el-badge>
            <el-dropdown trigger="hover" @command="handleUserCommand">
              <span class="user-dropdown-trigger">
                {{ userNickname }}
                <i class="el-icon-arrow-down el-icon--right"></i>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="profile" icon="el-icon-user">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" icon="el-icon-switch-button" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="text" class="nav-btn" @click="$router.push('/login')">登录</el-button>
          </template>
        </div>
        <!-- Mobile Hamburger Toggle -->
        <button class="mobile-toggle" @click="toggleMobileMenu" aria-label="菜单">
          <span class="mobile-toggle-bar"></span>
          <span class="mobile-toggle-bar"></span>
          <span class="mobile-toggle-bar"></span>
        </button>
      </el-header>
      <!-- Mobile Drawer -->
      <transition name="drawer-fade">
        <div v-if="mobileMenuOpen" class="drawer-overlay" @click.self="mobileMenuOpen = false">
          <transition name="drawer-slide">
            <div v-if="mobileMenuOpen" class="drawer-panel">
              <div class="drawer-header">
                <span class="drawer-title">菜单</span>
                <button class="drawer-close" @click="mobileMenuOpen = false" aria-label="关闭">
                  <span></span><span></span>
                </button>
              </div>
              <div class="drawer-body">
                <div v-if="isLoggedIn" class="drawer-user">
                  <span class="drawer-user-name">{{ userNickname }}</span>
                </div>
                <div class="drawer-nav">
                  <button class="nav-btn drawer-nav-item" :class="{ 'nav-btn--active': $route.path === '/' }" @click="$router.push('/'); mobileMenuOpen = false">主页</button>
                  <button class="nav-btn drawer-nav-item" :class="{ 'nav-btn--active': $route.path === '/matches' }" @click="$router.push('/matches'); mobileMenuOpen = false">赛程</button>
                  <button class="nav-btn drawer-nav-item" :class="{ 'nav-btn--active': $route.path === '/leaderboard' }" @click="$router.push('/leaderboard'); mobileMenuOpen = false">排行榜</button>
                  <button v-if="isAdmin" class="nav-btn drawer-nav-item admin-btn" :class="{ 'nav-btn--active': $route.path.startsWith('/admin') }" @click="$router.push('/admin'); mobileMenuOpen = false">管理</button>
                </div>
                <div class="drawer-divider"></div>
                <template v-if="isLoggedIn">
                  <button class="nav-btn drawer-nav-item" @click="$router.push('/notifications'); mobileMenuOpen = false">
                    <i class="el-icon-bell drawer-nav-icon"></i>通知
                    <span v-if="unreadCount > 0" class="drawer-badge">{{ unreadCount }}</span>
                  </button>
                  <div class="drawer-divider"></div>
                  <button class="nav-btn drawer-nav-item drawer-logout" @click="handleUserCommand('logout'); mobileMenuOpen = false">退出登录</button>
                </template>
                <template v-else>
                  <button class="nav-btn drawer-nav-item" @click="$router.push('/login'); mobileMenuOpen = false">登录</button>
                </template>
              </div>
            </div>
          </transition>
        </div>
      </transition>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { getToken, getUser, removeToken } from '@/utils/auth'
import request from '@/utils/request'
import { connect as wsConnect, disconnect as wsDisconnect } from '@/utils/websocket'

export default {
  name: 'App',
  data() {
    return {
      unreadCount: 0,
      notificationPopoverVisible: false,
      recentNotifications: [],
      notificationPollTimer: null,
      currentUser: null,
      mobileMenuOpen: false
    }
  },
  computed: {
    isLoggedIn() {
      return !!getToken()
    },
    user() {
      return this.currentUser || {}
    },
    userNickname() {
      return this.user.nickname || this.user.username || '用户'
    },
    isAdmin() {
      return this.user && this.user.role === 1
    }
  },
  created() {
    this.refreshUser()
    if (getToken()) {
      this.fetchUnreadCount()
      this.fetchRecentNotifications()
      this.notificationPollTimer = setInterval(() => {
        this.fetchUnreadCount()
      }, 30000)
      // 建立 WebSocket 连接（评论实时推送）
      wsConnect(getToken()).catch(() => {})
    }
  },
  watch: {
    $route() {
      // 路由变化时刷新用户信息（处理登录/切换账号后 localStorage 更新）
      this.refreshUser()
    },
    notificationPopoverVisible(val) {
      // 打开通知弹窗时刷新列表
      if (val && getToken()) {
        this.fetchRecentNotifications()
      }
    }
  },
  beforeDestroy() {
    if (this.notificationPollTimer) {
      clearInterval(this.notificationPollTimer)
      this.notificationPollTimer = null
    }
    wsDisconnect()
  },
  methods: {
    refreshUser() {
      this.currentUser = getUser()
    },
    toggleMobileMenu() {
      this.mobileMenuOpen = !this.mobileMenuOpen
    },
    handleUserCommand(command) {
      if (command === 'profile') {
        this.$router.push('/user')
      } else if (command === 'logout') {
        removeToken()
        this.currentUser = null
        this.$router.push('/login')
      }
    },
    fetchUnreadCount() {
      request.get('/notification/unread-count').then(function (res) {
        var newCount = res.data != null ? res.data : 0
        // 未读数增加时刷新通知列表
        if (newCount > this.unreadCount) {
          this.fetchRecentNotifications()
        }
        this.unreadCount = newCount
      }.bind(this)).catch(function () {})
    },
    fetchRecentNotifications() {
      request.get('/notification/list', { params: { page: 1, size: 5 } }).then(function (res) {
        var data = res.data || res
        this.recentNotifications = data.records || data.list || []
      }.bind(this)).catch(function () {})
    },
    markAllRead() {
      request.put('/notification/read-all').then(function () {
        this.unreadCount = 0
        this.recentNotifications.forEach(function (n) { n.isRead = 1 })
      }.bind(this)).catch(function () {})
    },
    handleNotificationClick(item) {
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
      this.notificationPopoverVisible = false
    },
    getNotificationIcon(type) {
      var icons = {
        1: 'el-icon-bell',
        2: 'el-icon-s-check',
        3: 'el-icon-chat-dot-square',
        4: 'el-icon-attract'
      }
      return icons[type] || 'el-icon-info'
    },
    formatNotificationTime(time) {
      if (!time) return ''
      var d = new Date(time)
      var now = new Date()
      var diff = now - d
      if (diff < 60000) return '刚刚'
      if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
      if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
      return d.toLocaleDateString('zh-CN')
    }
  }
}
</script>

<style>
:root {
  --bg-page: #0a0a0f;
  --bg-surface-1: #12121a;
  --bg-surface-2: #1a1a26;
  --bg-surface-3: #22222f;
  --border-subtle: rgba(255,255,255,0.06);
  --border-default: rgba(255,255,255,0.10);
  --border-strong: rgba(255,255,255,0.18);
  --text-primary: #f4f4f7;
  --text-secondary: #a8a8b8;
  --text-tertiary: #6b6b7c;
  --accent-primary: #00e5a0;
  --accent-secondary: #5b8cff;
  --accent-warning: #ffb547;
  --accent-danger: #ff4d6d;
  --team-a: #ff4d6d;
  --team-b: #5b8cff;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 16px;
  --radius-xl: 20px;
  --radius-full: 9999px;
  --bp-sm: 480px;
  --bp-md: 768px;
  --bp-lg: 1200px;
  --shadow-sm: 0 1px 2px rgba(0,0,0,0.40);
  --shadow-md: 0 4px 12px rgba(0,0,0,0.45), 0 0 0 1px rgba(255,255,255,0.04);
  --shadow-lg: 0 8px 24px rgba(0,0,0,0.50), 0 0 0 1px rgba(255,255,255,0.05);
  --font-display: 'Bebas Neue', sans-serif;
  --font-body: 'Inter', system-ui, sans-serif;
  --font-mono: 'JetBrains Mono', monospace;
  --ease-out: cubic-bezier(0.16, 1, 0.3, 1);
  --duration-fast: 150ms;
  --duration-normal: 250ms;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  background: var(--bg-page);
  color: var(--text-primary);
  font-family: var(--font-body);
  font-size: 14px;
  line-height: 1.5;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  background:
    radial-gradient(ellipse 60% 50% at 20% 0%, rgba(91,140,255,0.08) 0%, transparent 60%),
    radial-gradient(ellipse 50% 40% at 80% 10%, rgba(0,229,160,0.06) 0%, transparent 55%),
    var(--bg-page);
  min-height: 100vh;
}

.grain-overlay {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 9999;
  opacity: 0.035;
  mix-blend-mode: overlay;
  background-image: url("data:image/svg+xml;utf8,<svg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/><feColorMatrix type='saturate' values='0'/></filter><rect width='100%' height='100%' filter='url(%23n)'/></svg>");
}

/* Header */
.app-header {
  background: rgba(18,18,26,0.85);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px !important;
}

.header-left .logo {
  font-family: var(--font-display);
  font-size: 26px;
  color: var(--accent-primary);
  text-decoration: none;
  letter-spacing: 3px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-btn {
  color: var(--text-secondary) !important;
  font-size: 14px;
  font-weight: 500;
  transition: color var(--duration-fast) ease;
}

.nav-btn:hover {
  color: var(--accent-primary) !important;
}

.nav-btn--active {
  color: var(--accent-primary) !important;
  background: rgba(0, 229, 160, 0.08);
  border-radius: var(--radius-md);
}

.admin-btn {
  color: var(--accent-primary) !important;
  font-weight: 600;
}

.nav-divider {
  width: 1px;
  height: 20px;
  background: var(--border-default);
  margin: 0 8px;
}

.user-dropdown-trigger {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: var(--radius-md);
  transition: background var(--duration-fast) ease;
}

.user-dropdown-trigger:hover {
  background: var(--bg-surface-2);
  color: var(--accent-primary);
}

/* Dropdown menu dark theme */
.el-dropdown-menu {
  background: var(--bg-surface-2) !important;
  border: 1px solid var(--border-default) !important;
  border-radius: var(--radius-md) !important;
  box-shadow: var(--shadow-lg) !important;
  padding: 4px 0 !important;
}

.el-dropdown-menu__item {
  color: var(--text-secondary) !important;
  font-size: 14px;
  padding: 8px 16px;
  transition: all var(--duration-fast) ease;
}

.el-dropdown-menu__item:hover {
  background: var(--bg-surface-3) !important;
  color: var(--text-primary) !important;
}

.el-dropdown-menu__item--divided {
  border-top-color: var(--border-subtle) !important;
}

.app-main {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

a {
  text-decoration: none;
}

/* Element UI: Card */
.el-card {
  background: var(--bg-surface-1) !important;
  border: 1px solid var(--border-subtle) !important;
  border-radius: var(--radius-lg) !important;
  box-shadow: var(--shadow-md) !important;
  color: var(--text-primary) !important;
}

.el-card__header {
  border-bottom-color: var(--border-subtle) !important;
}

/* Element UI: Table */
.el-table {
  background: transparent !important;
  color: var(--text-primary) !important;
}

.el-table::before,
.el-table::after {
  display: none !important;
}

.el-table th.el-table__cell {
  background: var(--bg-surface-2) !important;
  color: var(--text-tertiary) !important;
  font-size: 11px !important;
  font-weight: 600 !important;
  text-transform: uppercase !important;
  letter-spacing: 0.08em !important;
  border-bottom-color: var(--border-default) !important;
}

.el-table td.el-table__cell {
  background: transparent !important;
  border-bottom-color: var(--border-subtle) !important;
  font-size: 14px !important;
}

.el-table tr {
  background: transparent !important;
}

.el-table--enable-row-hover .el-table__body tr:hover > td {
  background: var(--bg-surface-2) !important;
}

/* Element UI: Tabs */
.el-tabs__item {
  color: var(--text-secondary) !important;
  font-weight: 500;
  transition: color var(--duration-fast) ease;
}

.el-tabs__item.is-active {
  color: var(--accent-primary) !important;
}

.el-tabs__active-bar {
  background-color: var(--accent-primary) !important;
}

/* Element UI: Input */
.el-input__inner {
  background: var(--bg-surface-1) !important;
  border-color: var(--border-default) !important;
  color: var(--text-primary) !important;
  border-radius: var(--radius-md) !important;
  font-family: var(--font-body) !important;
  transition: border-color var(--duration-fast) ease, box-shadow var(--duration-fast) ease;
}

.el-input__inner:hover {
  border-color: var(--border-strong) !important;
}

.el-input__inner:focus {
  border-color: var(--accent-primary) !important;
  box-shadow: 0 0 0 3px rgba(0,229,160,0.15) !important;
}

.el-input__inner::placeholder {
  color: var(--text-tertiary) !important;
}

/* Element UI: Dialog */
.el-dialog {
  background: var(--bg-surface-2) !important;
  border: 1px solid var(--border-default) !important;
  border-radius: var(--radius-xl) !important;
  box-shadow: 0 16px 48px rgba(0,0,0,0.60) !important;
}

.el-dialog__header {
  padding: 20px 24px !important;
  border-bottom: 1px solid var(--border-subtle);
}

.el-dialog__title {
  color: var(--text-primary) !important;
  font-weight: 600 !important;
}

.el-dialog__body {
  padding: 24px !important;
  color: var(--text-secondary) !important;
}

.el-overlay {
  background: rgba(5,5,8,0.75) !important;
  backdrop-filter: blur(4px);
}

/* Element UI: Select Dropdown */
.el-select-dropdown {
  background: var(--bg-surface-2) !important;
  border-color: var(--border-default) !important;
}

.el-select-dropdown__item {
  color: var(--text-secondary) !important;
}

.el-select-dropdown__item.hover,
.el-select-dropdown__item:hover {
  background: var(--bg-surface-3) !important;
}

/* Element UI: Form */
.el-form-item__label {
  color: var(--text-secondary) !important;
}

/* Element UI: Message Box */
.el-message-box {
  background: var(--bg-surface-2) !important;
  border-color: var(--border-default) !important;
}

/* Element UI: Date Picker */
.el-date-picker {
  background: var(--bg-surface-2) !important;
}

/* Element UI: Pagination */
.el-pagination {
  text-align: center;
  margin-top: 20px;
}

/* Badge System */
.badge-live {
  color: var(--accent-primary);
  background: rgba(0,229,160,0.10);
  border: 1px solid rgba(0,229,160,0.25);
  border-radius: var(--radius-sm);
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.badge-live::before {
  content: '';
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent-primary);
  box-shadow: 0 0 8px var(--accent-primary);
  margin-right: 6px;
  animation: live-pulse 2s ease-in-out infinite;
}

@keyframes live-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.badge-upcoming {
  color: var(--accent-secondary);
  background: rgba(91,140,255,0.10);
  border: 1px solid rgba(91,140,255,0.25);
  border-radius: var(--radius-sm);
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.badge-finished {
  color: var(--text-tertiary);
  background: rgba(255,255,255,0.04);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.badge-won {
  color: var(--accent-primary);
  background: rgba(0,229,160,0.08);
  border: 1px solid rgba(0,229,160,0.20);
  border-radius: var(--radius-sm);
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
}

.badge-lost {
  color: var(--accent-danger);
  background: rgba(255,77,109,0.08);
  border: 1px solid rgba(255,77,109,0.20);
  border-radius: var(--radius-sm);
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
}

/* Status Badge System */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  border-radius: var(--radius-full);
  font-family: var(--font-body);
}

.status-badge--live {
  color: var(--accent-primary);
  background: rgba(0, 229, 160, 0.10);
  border: 1px solid rgba(0, 229, 160, 0.25);
}

.status-badge--upcoming {
  color: var(--accent-secondary);
  background: rgba(91, 140, 255, 0.10);
  border: 1px solid rgba(91, 140, 255, 0.25);
}

.status-badge--finished {
  color: var(--text-tertiary);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-default);
}

.status-badge--won {
  color: var(--accent-primary);
  background: rgba(0, 229, 160, 0.10);
  border: 1px solid rgba(0, 229, 160, 0.25);
}

.status-badge--lost {
  color: var(--accent-danger);
  background: rgba(255, 77, 109, 0.10);
  border: 1px solid rgba(255, 77, 109, 0.25);
}

.status-badge--pending {
  color: var(--text-tertiary);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-default);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}

.status-dot--pulse {
  animation: status-pulse 2s ease-in-out infinite;
  box-shadow: 0 0 6px currentColor;
}

@keyframes status-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* Tournament Chip */
.tournament-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 500;
  color: var(--text-tertiary);
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  letter-spacing: 0.03em;
}

/* Accessibility */
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}

/* Notification Bell */
.nav-badge {
  margin-right: 4px;
}

.nav-badge .el-badge__content {
  background-color: var(--accent-danger) !important;
}

.nav-bell-btn {
  background: transparent !important;
  border: 1px solid var(--bg-surface-3) !important;
  color: var(--text-secondary) !important;
  padding: 8px 12px !important;
  border-radius: var(--radius-md) !important;
  cursor: pointer;
  font-size: 16px !important;
  transition: all 0.2s var(--ease-out);
  line-height: 1 !important;
  height: auto !important;
}

.nav-bell-btn:hover {
  background: var(--bg-surface-2) !important;
  color: var(--text-primary) !important;
}

.nav-bell-btn.has-notification {
  color: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
}

.notification-preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--bg-surface-3);
  margin-bottom: 8px;
}

.notification-preview-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.notification-preview-footer {
  text-align: center;
  padding-top: 8px;
  border-top: 1px solid var(--bg-surface-3);
  margin-top: 8px;
}

.notification-preview-footer a {
  color: var(--accent-secondary);
  font-size: 13px;
  text-decoration: none;
}

.notification-preview-footer a:hover {
  color: var(--accent-primary);
}

.notification-empty {
  text-align: center;
  padding: 20px;
  color: var(--text-tertiary);
  font-size: 13px;
}

.notification-item {
  display: flex;
  padding: 10px 8px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.15s;
}

.notification-item--unread {
  background: rgba(0, 229, 160, 0.05);
}

.notification-item:hover {
  background: var(--bg-surface-2);
}

.notification-item-icon {
  margin-right: 10px;
  font-size: 18px;
  color: var(--accent-secondary);
  width: 24px;
  text-align: center;
  flex-shrink: 0;
}

.notification-item-content {
  flex: 1;
  min-width: 0;
}

.notification-item-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.notification-item-desc {
  font-size: 12px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-item-time {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
}

/* Popover dark theme */
.el-popover {
  background: var(--bg-surface-2) !important;
  border: 1px solid var(--border-default) !important;
  border-radius: var(--radius-md) !important;
  box-shadow: var(--shadow-lg) !important;
  color: var(--text-primary) !important;
}

.el-popover .popper__arrow::after,
.el-popover .popper__arrow {
  border-bottom-color: var(--bg-surface-2) !important;
}

/* ── Mobile Hamburger & Drawer ── */
.mobile-toggle {
  display: none;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 5px;
  width: 40px;
  height: 40px;
  background: transparent;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  cursor: pointer;
  padding: 8px;
  transition: background var(--duration-fast) ease;
}

.mobile-toggle:hover {
  background: var(--bg-surface-2);
}

.mobile-toggle-bar {
  display: block;
  width: 18px;
  height: 2px;
  background: var(--text-primary);
  border-radius: 1px;
  transition: transform var(--duration-normal) var(--ease-out);
}

/* Drawer Overlay */
.drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.5);
}

/* Drawer Panel */
.drawer-panel {
  position: fixed;
  top: 0;
  right: 0;
  width: 280px;
  height: 100vh;
  background: var(--bg-surface-2);
  border-left: 1px solid var(--border-subtle);
  box-shadow: -8px 0 24px rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-subtle);
}

.drawer-title {
  font-family: var(--font-display);
  font-size: 20px;
  color: var(--text-primary);
  letter-spacing: 2px;
}

.drawer-close {
  position: relative;
  width: 32px;
  height: 32px;
  background: transparent;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background var(--duration-fast) ease;
}

.drawer-close:hover {
  background: var(--bg-surface-3);
}

.drawer-close span {
  position: absolute;
  width: 14px;
  height: 2px;
  background: var(--text-secondary);
  border-radius: 1px;
}

.drawer-close span:first-child {
  transform: rotate(45deg);
}

.drawer-close span:last-child {
  transform: rotate(-45deg);
}

.drawer-body {
  flex: 1;
  padding: 12px 12px 24px;
  display: flex;
  flex-direction: column;
}

.drawer-user {
  padding: 12px 12px 16px;
}

.drawer-user-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--accent-primary);
}

.drawer-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.drawer-nav-item {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 12px !important;
  font-size: 15px !important;
  border-radius: var(--radius-md) !important;
  text-align: left;
  background: transparent !important;
  border: none !important;
  cursor: pointer;
  transition: background var(--duration-fast) ease, color var(--duration-fast) ease;
}

.drawer-nav-item:hover {
  background: var(--bg-surface-3) !important;
}

.drawer-nav-icon {
  margin-right: 8px;
  font-size: 16px;
}

.drawer-badge {
  margin-left: auto;
  background: var(--accent-danger);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  min-width: 20px;
  height: 20px;
  line-height: 20px;
  text-align: center;
  border-radius: var(--radius-full);
  padding: 0 6px;
}

.drawer-divider {
  height: 1px;
  background: var(--border-subtle);
  margin: 8px 12px;
}

.drawer-logout {
  color: var(--accent-danger) !important;
}

.drawer-logout:hover {
  background: rgba(255, 77, 109, 0.08) !important;
}

/* Drawer transitions */
.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity var(--duration-normal) var(--ease-out);
}

.drawer-fade-enter,
.drawer-fade-leave-to {
  opacity: 0;
}

.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: transform var(--duration-normal) var(--ease-out);
}

.drawer-slide-enter,
.drawer-slide-leave-to {
  transform: translateX(100%);
}

/* ── Mobile Responsive ── */
@media (max-width: 767px) {
  .header-right {
    display: none !important;
  }

  .mobile-toggle {
    display: flex;
  }

  .app-header {
    padding: 0 16px;
  }

  .app-main {
    padding: 16px;
  }
}
</style>