<template>
  <div id="app">
    <div class="app-grid-overlay" aria-hidden="true"></div>
    <div class="grain-overlay" aria-hidden="true"></div>

    <div class="app-shell">
      <el-header class="app-header">
        <div class="header-left">
          <router-link to="/" class="logo" aria-label="Egg Esports 首页">
            <span class="logo-mark" aria-hidden="true">
              <svg viewBox="0 0 24 30" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 1.5C6.4 1.5 1.5 9.2 1.5 17.1c0 6.1 4.7 11.4 10.5 11.4s10.5-5.3 10.5-11.4C22.5 9.2 17.6 1.5 12 1.5Z" stroke="currentColor" stroke-width="1.8"/>
                <path d="M6.2 10.5 17.8 22.4" stroke="currentColor" stroke-width="1.8" stroke-linecap="square"/>
                <path d="M9.2 6.6 13 3.2" stroke="currentColor" stroke-width="1.4" stroke-linecap="square" opacity=".55"/>
              </svg>
            </span>
            <span class="logo-word">EGG<em>ESPORTS</em></span>
            <span class="logo-slash">//</span>
            <span class="logo-sub">PREDICTION ARENA</span>
          </router-link>
        </div>

        <div class="header-right">
          <!-- 桌面端：横向主导航 -->
          <nav class="desktop-nav" aria-label="主导航">
            <template v-for="item in visibleNavItems" :key="item.path">
              <el-button
                link
                class="nav-btn"
                :class="[{ 'nav-btn--active': isActive(item.path) }, { 'admin-btn': item.admin }]"
                @click="go(item.path)"
              >
                {{ item.label }}
                <span v-if="item.path === '/bank' && loanDebt > 0" class="nav-debt-pill" :title="'待还贷款 ' + loanDebt + ' 积分'">{{ loanDebt }}</span>
              </el-button>
            </template>
          </nav>

          <!-- 移动端：抽屉菜单 -->
          <button class="mobile-nav mobile-nav-trigger" type="button" aria-label="打开菜单" @click="mobileNavOpen = true">
            <el-icon><Menu /></el-icon>
          </button>

          <span class="nav-divider"></span>

          <template v-if="isLoggedIn">
            <!-- 通知 -->
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="nav-badge">
              <el-popover
                placement="bottom-end"
                :width="notificationWidth"
                trigger="click"
                v-model:visible="notificationPopoverVisible"
              >
                <div class="notification-preview">
                  <div class="notification-preview-header">
                    <span class="notification-preview-title">Notifications</span>
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
                      查看全部通知 →
                    </router-link>
                  </div>
                </div>
                <template #reference>
                  <button class="nav-btn nav-bell-btn" :class="{ 'has-notification': unreadCount > 0 }" type="button" aria-label="通知">
                    <el-icon><Bell /></el-icon>
                  </button>
                </template>
              </el-popover>
            </el-badge>

            <el-dropdown trigger="hover" @command="handleUserCommand">
              <span class="user-dropdown-trigger">
                <span class="user-chip-avatar">
                  <img v-if="user.avatar" :src="user.avatar" alt="" />
                  <template v-else>{{ nicknameInitial }}</template>
                </span>
                <span class="user-chip-name">{{ userNickname }}</span>
                <el-icon class="user-chip-caret"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile" :icon="User">个人中心</el-dropdown-item>
                  <el-dropdown-item command="bank" :icon="Wallet">积分银行</el-dropdown-item>
                  <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button link class="nav-btn btn-hover-lift" @click="go('/login')">登录</el-button>
          </template>
        </div>
      </el-header>

      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>

      <el-footer class="app-footer">
        <div class="footer-inner">
          <div class="footer-brand-block">
            <router-link to="/" class="logo">
              <span class="logo-mark" aria-hidden="true">
                <svg viewBox="0 0 24 30" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M12 1.5C6.4 1.5 1.5 9.2 1.5 17.1c0 6.1 4.7 11.4 10.5 11.4s10.5-5.3 10.5-11.4C22.5 9.2 17.6 1.5 12 1.5Z" stroke="currentColor" stroke-width="1.8"/>
                  <path d="M6.2 10.5 17.8 22.4" stroke="currentColor" stroke-width="1.8" stroke-linecap="square"/>
                </svg>
              </span>
              <span class="logo-word">EGG<em>ESPORTS</em></span>
            </router-link>
            <p class="footer-tagline">
              非盈利电竞赛事预测社区。<strong>纯彩池制 · 0% 抽水 · 猜中全分</strong>。不设庄家，只比判断力。
            </p>
            <div class="footer-stat-row">
              <div>
                <div class="footer-stat-value">0%</div>
                <div class="footer-stat-label">抽水</div>
              </div>
              <div>
                <div class="footer-stat-value">32</div>
                <div class="footer-stat-label">已交付赛事</div>
              </div>
              <div>
                <div class="footer-stat-value">24/7</div>
                <div class="footer-stat-label">实时赔率</div>
              </div>
            </div>
          </div>

          <div>
            <div class="footer-col-title">Explore</div>
            <nav class="footer-links" aria-label="页脚导航">
              <router-link to="/" class="footer-link">主页</router-link>
              <router-link to="/matches" class="footer-link">赛程</router-link>
              <router-link to="/leaderboard" class="footer-link">排行榜</router-link>
              <router-link to="/showcase" class="footer-link">案例墙</router-link>
            </nav>
          </div>

          <div>
            <div class="footer-col-title">Company</div>
            <nav class="footer-links" aria-label="公司信息">
              <router-link to="/about" class="footer-link">关于我们</router-link>
              <router-link to="/pricing" class="footer-link">定价</router-link>
              <router-link to="/privacy" class="footer-link">隐私政策</router-link>
              <router-link to="/terms" class="footer-link">服务条款</router-link>
            </nav>
          </div>
        </div>
        <div class="footer-icp">
          <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">京ICP备2026XXXXXX号</a>
          <span class="footer-copy">EGG ESPORTS · PARIMUTUEL · NO HOUSE EDGE</span>
        </div>
      </el-footer>
    </div>

    <!-- 移动端抽屉 -->
    <template v-if="mobileNavOpen">
      <div class="mobile-drawer-mask" @click="mobileNavOpen = false"></div>
      <aside class="mobile-drawer" role="dialog" aria-modal="true" aria-label="导航菜单">
        <div class="mobile-drawer-head">
          <span class="mobile-drawer-title">Navigation</span>
          <button class="mobile-drawer-close" type="button" aria-label="关闭菜单" @click="mobileNavOpen = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>
        <nav class="mobile-drawer-list">
          <div
            v-for="item in visibleNavItems"
            :key="'m-' + item.path"
            class="mobile-drawer-item"
            :class="{ 'is-active': isActive(item.path) }"
            @click="handleNavCommand(item.path)"
          >
            <span>{{ item.label }}<span v-if="item.path === '/bank' && loanDebt > 0" class="mobile-nav-debt">●</span></span>
            <span class="idx">{{ item.code }}</span>
          </div>
        </nav>
        <div class="mobile-drawer-sep"></div>
        <div class="mobile-drawer-foot">
          <template v-if="isLoggedIn">
            <div class="mobile-drawer-user">
              <span class="mobile-drawer-avatar">
                <img v-if="user.avatar" :src="user.avatar" alt="" />
                <template v-else>{{ nicknameInitial }}</template>
              </span>
              <div style="min-width:0">
                <div class="mobile-drawer-nick">{{ userNickname }}</div>
                <div class="mobile-drawer-meta">Signed in</div>
              </div>
            </div>
            <el-button class="full-btn" @click="handleNavCommand('/user')">个人中心</el-button>
            <el-button class="full-btn" type="danger" plain @click="handleNavCommand('logout')">退出登录</el-button>
          </template>
          <template v-else>
            <el-button type="primary" class="full-btn" @click="handleNavCommand('/login')">登录 / 注册</el-button>
            <p class="mobile-drawer-meta" style="margin-top:10px">demo_player / demo123</p>
          </template>
        </div>
      </aside>
    </template>

    <AnnouncementPopup />
  </div>
</template>

<script>
// Element Plus 图标按需引入（用于 :icon 属性绑定与动态 component）
import { markRaw } from 'vue'
import { Bell, ArrowDown, User, Wallet, SwitchButton, CircleCheck, ChatDotSquare, MagicStick, InfoFilled, Menu, Close } from '@element-plus/icons-vue'
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
      mobileNavOpen: false,
      windowWidth: typeof window !== 'undefined' ? window.innerWidth : 1440,
      navItems: [
        { path: '/', label: '主页', code: '01' },
        { path: '/matches', label: '赛程', code: '02' },
        { path: '/leaderboard', label: '排行', code: '03' },
        { path: '/bank', label: '银行', code: '04', auth: true },
        { path: '/showcase', label: '案例', code: '05' },
        { path: '/pricing', label: '定价', code: '06' },
        { path: '/about', label: '关于', code: '07' },
        { path: '/admin', label: '管理', code: '08', admin: true }
      ],
      // 暴露给模板的图标组件引用（用于 :icon 属性，用 markRaw 避免响应化触发 Vue 警告）
      Bell: markRaw(Bell),
      ArrowDown: markRaw(ArrowDown),
      User: markRaw(User),
      Wallet: markRaw(Wallet),
      SwitchButton: markRaw(SwitchButton),
      Menu: markRaw(Menu),
      Close: markRaw(Close)
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
    nicknameInitial() {
      var name = this.userNickname
      return name ? name.charAt(0).toUpperCase() : 'E'
    },
    isAdmin() {
      return this.user && this.user.role === 1
    },
    visibleNavItems() {
      var self = this
      return this.navItems.filter(function (item) {
        if (item.auth && !self.isLoggedIn) return false
        if (item.admin && !self.isAdmin) return false
        return true
      })
    },
    notificationWidth() {
      return Math.max(260, Math.min(360, this.windowWidth - 24))
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
  mounted() {
    window.addEventListener('resize', this.onResize)
    window.addEventListener('keydown', this.onKeydown)
  },
  watch: {
    $route() {
      // 路由变化时刷新用户信息（处理登录/切换账号后 localStorage 更新）
      this.refreshUser()
      this.mobileNavOpen = false
    },
    notificationPopoverVisible(val) {
      // 打开通知弹窗时刷新列表
      if (val && getToken()) {
        this.fetchRecentNotifications()
      }
    },
    mobileNavOpen(val) {
      document.body.style.overflow = val ? 'hidden' : ''
    }
  },
  beforeUnmount() {
    if (this.notificationPollTimer) {
      clearInterval(this.notificationPollTimer)
      this.notificationPollTimer = null
    }
    window.removeEventListener('resize', this.onResize)
    window.removeEventListener('keydown', this.onKeydown)
    document.body.style.overflow = ''
    wsDisconnect()
  },
  methods: {
    onResize() {
      this.windowWidth = window.innerWidth
    },
    onKeydown(e) {
      if (e.key === 'Escape' && this.mobileNavOpen) this.mobileNavOpen = false
    },
    isActive(path) {
      if (path === '/') return this.$route.path === '/'
      return this.$route.path === path || this.$route.path.startsWith(path + '/')
    },
    go(path) {
      this.$router.push(path).catch(function () {})
    },
    refreshUser() {
      this.currentUser = getUser()
      this.appToken = getToken()
    },
    handleUserCommand(command) {
      if (command === 'profile') {
        this.$router.push('/user')
      } else if (command === 'bank') {
        this.$router.push('/bank')
      } else if (command === 'logout') {
        this.doLogout()
      }
    },
    doLogout() {
      removeToken()
      this.currentUser = null
      this.appToken = null
      this.mobileNavOpen = false
      this.$router.push('/login')
    },
    // 移动端抽屉跳转
    handleNavCommand(command) {
      this.mobileNavOpen = false
      if (!command) return
      if (command === 'logout') {
        this.doLogout()
        return
      }
      this.$router.push(command).catch(function () {})
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
        if (item.relatedId) {
          this.$router.push('/match/' + item.relatedId)
        } else {
          this.$router.push('/matches')
        }
      } else if (item.type === 2) {
        this.$router.push('/matches')
      } else if (item.type === 3 || item.type === 4) {
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

/* 移动端抽屉内按钮占满宽度 */
.mobile-drawer-foot .full-btn {
  width: 100%;
  margin: 0 0 8px !important;
}
.mobile-drawer-foot .full-btn + .full-btn {
  margin-left: 0 !important;
}

/* 银行待还贷款内联角标（避免 el-badge 覆盖导航文字） */
.nav-debt-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 15px;
  height: 14px;
  margin-left: 5px;
  padding: 0 3px;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  line-height: 1;
  color: var(--accent-danger);
  background: transparent;
  border: 1px solid var(--accent-danger);
  border-radius: 2px;
  vertical-align: 1px;
}
</style>
