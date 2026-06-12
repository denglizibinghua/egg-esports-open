<template>
  <div id="app">
    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <router-link to="/" class="logo">EGG ESPORTS</router-link>
        </div>
        <div class="header-right">
          <template v-if="isLoggedIn">
            <el-button type="text" class="nav-btn" @click="$router.push('/user')">{{ userNickname }}</el-button>
            <el-button type="text" class="nav-btn" @click="handleLogout">退出</el-button>
          </template>
          <template v-else>
            <el-button type="text" class="nav-btn" @click="$router.push('/login')">登录</el-button>
          </template>
          <el-button type="text" class="nav-btn" @click="$router.push('/')">主页</el-button>
          <el-button type="text" class="nav-btn" @click="$router.push('/leaderboard')">排行榜</el-button>
          <el-button v-if="isAdmin" type="text" class="nav-btn admin-btn" @click="$router.push('/admin')">管理</el-button>
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
    handleLogout() {
      removeToken()
      this.$router.push('/login')
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  background-color: #1a1a2e;
  color: #e0e0e0;
  font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;
}

#app {
  min-height: 100vh;
}

.app-header {
  background-color: #16213e;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 2px solid #FF4655;
  height: 56px !important;
}

.header-left .logo {
  font-size: 22px;
  font-weight: 800;
  color: #FF4655;
  text-decoration: none;
  letter-spacing: 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-btn {
  color: #e0e0e0 !important;
  font-size: 14px;
}

.nav-btn:hover {
  color: #FF4655 !important;
}

.admin-btn {
  color: #FF4655 !important;
  font-weight: 600;
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

/* Element UI dark overrides */
.el-card {
  background-color: #16213e !important;
  border-color: #2a2a4a !important;
  color: #e0e0e0 !important;
}

.el-card__header {
  border-bottom-color: #2a2a4a !important;
}

.el-tabs__item {
  color: #a0a0b0 !important;
}

.el-tabs__item.is-active {
  color: #FF4655 !important;
}

.el-tabs__active-bar {
  background-color: #FF4655 !important;
}

.el-table {
  background-color: #16213e !important;
  color: #e0e0e0 !important;
}

.el-table th.el-table__cell {
  background-color: #1a1a2e !important;
  color: #e0e0e0 !important;
}

.el-table tr {
  background-color: #16213e !important;
}

.el-table--enable-row-hover .el-table__body tr:hover > td {
  background-color: #1e2a4a !important;
}

.el-table td, .el-table th {
  border-bottom-color: #2a2a4a !important;
}

.el-pagination {
  text-align: center;
  margin-top: 20px;
}

.el-input__inner {
  background-color: #1a1a2e !important;
  border-color: #2a2a4a !important;
  color: #e0e0e0 !important;
}

.el-form-item__label {
  color: #a0a0b0 !important;
}

.el-dialog {
  background-color: #16213e !important;
}

.el-dialog__title {
  color: #e0e0e0 !important;
}

.el-dialog__body {
  color: #e0e0e0 !important;
}

.el-select-dropdown {
  background-color: #16213e !important;
}

.el-select-dropdown__item {
  color: #e0e0e0 !important;
}

.el-select-dropdown__item.hover,
.el-select-dropdown__item:hover {
  background-color: #1e2a4a !important;
}

.el-message-box {
  background-color: #16213e !important;
  border-color: #2a2a4a !important;
}

.el-date-picker {
  background-color: #16213e !important;
}
</style>