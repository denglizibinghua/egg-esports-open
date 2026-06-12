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
          <el-button type="text" class="nav-btn" :class="{ 'nav-btn--active': $route.path === '/leaderboard' }" @click="$router.push('/leaderboard')">排行榜</el-button>
          <el-button v-if="isAdmin" type="text" class="nav-btn admin-btn" :class="{ 'nav-btn--active': $route.path.startsWith('/admin') }" @click="$router.push('/admin')">管理</el-button>
          <span class="nav-divider"></span>
          <template v-if="isLoggedIn">
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
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { getToken, getUser, removeToken } from '@/utils/auth'

export default {
  name: 'App',
  computed: {
    isLoggedIn() {
      return !!getToken()
    },
    user() {
      return getUser() || {}
    },
    userNickname() {
      return this.user.nickname || this.user.username || '用户'
    },
    isAdmin() {
      return this.user && this.user.role === 1
    }
  },
  methods: {
    handleUserCommand(command) {
      if (command === 'profile') {
        this.$router.push('/user')
      } else if (command === 'logout') {
        removeToken()
        this.$router.push('/login')
      }
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
</style>