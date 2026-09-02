// Vue Router 4 入口：createRouter + createWebHistory
import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getUser } from '@/utils/auth'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/matches',
    name: 'MatchList',
    component: () => import('@/views/MatchList.vue')
  },
  {
    path: '/match/:id',
    name: 'MatchDetail',
    component: () => import('@/views/MatchDetail.vue')
  },
  {
    path: '/leaderboard',
    name: 'Leaderboard',
    component: () => import('@/views/Leaderboard.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/bank',
    name: 'Bank',
    component: () => import('@/views/Bank.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/user',
    name: 'UserCenter',
    component: () => import('@/views/UserCenter.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/Notifications.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile/:id',
    name: 'PublicProfile',
    component: () => import('@/views/PublicProfile.vue')
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('@/views/admin/AdminDashboard.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/match/create',
    name: 'MatchCreate',
    component: () => import('@/views/admin/MatchCreate.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/match/:id',
    name: 'MatchEdit',
    component: () => import('@/views/admin/MatchEdit.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('@/views/About.vue')
  },
  {
    path: '/showcase',
    name: 'Showcase',
    component: () => import('@/views/Showcase.vue')
  },
  {
    path: '/pricing',
    name: 'Pricing',
    component: () => import('@/views/Pricing.vue')
  },
  {
    // 隐私政策占位页面（法律合规要求）
    path: '/privacy',
    name: 'Privacy',
    component: () => import('@/views/Privacy.vue')
  },
  {
    // 服务条款占位页面（法律合规要求）
    path: '/terms',
    name: 'Terms',
    component: () => import('@/views/Terms.vue')
  },
  {
    // Vue Router 4 中 404 通配符使用动态参数语法
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 切换路由时滚动到顶部
  scrollBehavior() {
    return { top: 0 }
  }
})

// 全局前置守卫：登录与权限校验
router.beforeEach((to, from, next) => {
  const token = getToken()
  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.requiresAdmin) {
    const user = getUser() || {}
    if (user.role !== 1) {
      next({ name: 'MatchList' })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
