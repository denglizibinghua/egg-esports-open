<template>
  <div id="app">
    <div class="grain-overlay" aria-hidden="true"></div>
    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <router-link to="/" class="logo">EGG ESPORTS</router-link>
        </div>
        <div class="header-right">
          <!-- 桌面端：横向主导航 -->
          <div class="desktop-nav">
            <el-button link class="nav-btn" :class="{ 'nav-btn--active': $route.path === '/' }" @click="$router.push('/')">主页</el-button>
            <el-button link class="nav-btn" :class="{ 'nav-btn--active': $route.path === '/matches' }" @click="$router.push('/matches')">赛程</el-button>
            <el-button link class="nav-btn" :class="{ 'nav-btn--active': $route.path === '/leaderboard' }" @click="$router.push('/leaderboard')">排行</el-button>
            <!-- 银行按钮（仅登录后显示，带债务小红点） -->
            <el-badge v-if="isLoggedIn" :value="loanDebt > 0 ? loanDebt : 0" :hidden="loanDebt <= 0" class="nav-badge">
              <el-button link class="nav-btn" :class="{ 'nav-btn--active': $route.path === '/bank' }" @click="$router.push('/bank')">银行</el-button>
            </el-badge>
            <!-- 关于按钮（公开） -->
            <el-button link class="nav-btn" :class="{ 'nav-btn--active': $route.path === '/about' }" @click="$router.push('/about')">关于</el-button>
            <el-button v-if="isAdmin" link class="nav-btn admin-btn" :class="{ 'nav-btn--active': $route.path.startsWith('/admin') }" @click="$router.push('/admin')">管理</el-button>
          </div>
          <!-- 移动端：汉堡菜单（下拉包含主导航） -->
          <el-dropdown class="mobile-nav" trigger="click" @command="handleNavCommand">
            <el-button circle class="mobile-nav-trigger">
              <el-icon><Menu /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/">主页</el-dropdown-item>
                <el-dropdown-item command="/matches">赛程</el-dropdown-item>
                <el-dropdown-item command="/leaderboard">排行</el-dropdown-item>
                <el-dropdown-item v-if="isLoggedIn" command="/bank">银行<span v-if="loanDebt > 0" class="mobile-nav-debt">●</span></el-dropdown-item>
                <el-dropdown-item command="/about">关于</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="/admin" divided>管理</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <span class="nav-divider"></span>
          <template v-if="isLoggedIn">
            <!-- Notification Bell -->
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="nav-badge">
              <el-popover
                placement="bottom"
                width="360"
                trigger="click"
                v-model:visible="notificationPopoverVisible"
              >
                <div class="notification-preview">
                  <div class="notification-preview-header">
                    <span class="notification-preview-title">通知</span>
                    <el-button link size="small" @click="markAllRead">全部已读</el-button>
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
                        <!-- Element Plus：图标改为组件方式 -->
                        <el-icon><component :is="getNotificationIcon(item.type)" /></el-icon>
                      </div>
                      <div class="notification-item-content">
                        <div class="notification-item-title">{{ item.title }}</div>
                        <div class="notification-item-desc">{{ item.content }}</div>
                        <div class="notification-item-time">{{ formatNotificationTime(item.createTime) }}</div>
                      </div>
                    </div>
                  </div>
                  <div class="notification-preview-footer">
                    <router-link to="/notifications" @click="notificationPopoverVisible = false">
                      查看全部通知
                    </router-link>
                  </div>
                </div>
                <!-- Element Plus：slot="reference" → #reference -->
                <template #reference>
                  <el-button class="nav-btn nav-bell-btn" :class="{ 'has-notification': unreadCount > 0 }">
                    <el-icon><Bell /></el-icon>
                  </el-button>
                </template>
              </el-popover>
            </el-badge>
            <el-dropdown trigger="hover" @command="handleUserCommand">
              <span class="user-dropdown-trigger">
                {{ userNickname }}
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </span>
              <!-- Element Plus：slot="dropdown" → #dropdown；icon 属性改为组件 -->
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile" :icon="User">个人中心</el-dropdown-item>
                  <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button link class="nav-btn btn-hover-lift" @click="$router.push('/login')">登录</el-button>
          </template>
        </div>
      </el-header>
      <el-main class="app-main">
        <transition name="page" mode="out-in">
          <router-view />
        </transition>
      </el-main>
      <el-footer class="app-footer">
        <div class="footer-links">
          <router-link to="/privacy" class="footer-link">隐私政策</router-link>
          <span class="footer-divider">|</span>
          <router-link to="/terms" class="footer-link">服务条款</router-link>
        </div>
        <div class="footer-icp">
          <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">京ICP备2026XXXXXX号</a>
        </div>
      </el-footer>
    </el-container>
    <!-- 公告弹窗（自驱组件，无需 props/events） -->
    <AnnouncementPopup />
  </div>
</template>

<script>
// Element Plus 图标按需引入（用于 :icon 属性绑定与动态 component）
import { markRaw } from 'vue'
import { Bell, ArrowDown, User, SwitchButton, CircleCheck, ChatDotSquare, MagicStick, InfoFilled, Menu } from '@element-plus/icons-vue'
import { getToken, getUser, removeToken } from '@/utils/auth'
import request from '@/utils/request'
import { connect as wsConnect, disconnect as wsDisconnect } from '@/utils/websocket'
import AnnouncementPopup from '@/components/AnnouncementPopup.vue'
import { extractList } from '@/utils/pagination'

export default {
  name: 'App',
  components: {
    AnnouncementPopup
  },
  data() {
    return {
      unreadCount: 0,
      notificationPopoverVisible: false,
      recentNotifications: [],
      notificationPollTimer: null,
      currentUser: null,
      loanDebt: 0,
      appToken: getToken(),
      // 暴露给模板的图标组件引用（用于 :icon 属性，用 markRaw 避免响应化触发 Vue 警告）
      Bell: markRaw(Bell),
      ArrowDown: markRaw(ArrowDown),
      User: markRaw(User),
      SwitchButton: markRaw(SwitchButton),
      Menu: markRaw(Menu)
    }
  },
  computed: {
    isLoggedIn() {
      return !!this.appToken
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
      this.fetchLoanDebt()
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
  // Vue 3：beforeDestroy → beforeUnmount
  beforeUnmount() {
    if (this.notificationPollTimer) {
      clearInterval(this.notificationPollTimer)
      this.notificationPollTimer = null
    }
    wsDisconnect()
  },
  methods: {
    refreshUser() {
      this.currentUser = getUser()
      this.appToken = getToken()
    },
    handleUserCommand(command) {
      if (command === 'profile') {
        this.$router.push('/user')
      } else if (command === 'logout') {
        removeToken()
        this.currentUser = null
        this.appToken = null
        this.$router.push('/login')
      }
    },
    // 移动端汉堡菜单跳转
    handleNavCommand(command) {
      if (command) this.$router.push(command)
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
        // 兼容 records/list/content 多种后端返回格式
        this.recentNotifications = extractList(data)
      }.bind(this)).catch(function () {})
    },
    fetchLoanDebt() {
      request.get('/loan/status').then(function (res) {
        var data = res.data || res
        this.loanDebt = Math.abs(data.loanAmount || 0)
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
    // Element Plus：返回图标组件引用而非类名字符串
    getNotificationIcon(type) {
      var icons = {
        1: Bell,
        2: CircleCheck,
        3: ChatDotSquare,
        4: MagicStick
      }
      return icons[type] || InfoFilled
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
/* 全局样式已外移到 @/styles/global.scss（:root 变量、Element Plus 主题覆盖、全局工具类、响应式适配） */
@import '@/styles/global.scss';
</style>
