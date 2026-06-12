import Vue from 'vue'
import VueRouter from 'vue-router'
import { getToken, getUser } from '@/utils/auth'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
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
    path: '/user',
    name: 'UserCenter',
    component: () => import('@/views/UserCenter.vue'),
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
  }
]

const router = new VueRouter({
  mode: 'history',
  routes
})

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