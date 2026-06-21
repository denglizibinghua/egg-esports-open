import Vue from 'vue'
import VueRouter from 'vue-router'
import { getToken, getUser } from '@/utils/auth'

Vue.use(VueRouter)

// 修复 Vue Router v3 重复导航 & 取消导航报错
const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch(function (err) {
    if (err.name !== 'NavigationDuplicated' && err.name !== 'NavigationCancelled') throw err
  })
}
const originalReplace = VueRouter.prototype.replace
VueRouter.prototype.replace = function replace(location) {
  return originalReplace.call(this, location).catch(function (err) {
    if (err.name !== 'NavigationDuplicated' && err.name !== 'NavigationCancelled') throw err
  })
}

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import(/* webpackChunkName: "home" */ '@/views/Home.vue')
  },
  {
    path: '/matches',
    name: 'MatchList',
    component: () => import(/* webpackChunkName: "match-list" */ '@/views/MatchList.vue')
  },
  {
    path: '/match/:id',
    name: 'MatchDetail',
    component: () => import(/* webpackChunkName: "match-detail" */ '@/views/MatchDetail.vue')
  },
  {
    path: '/leaderboard',
    name: 'Leaderboard',
    component: () => import(/* webpackChunkName: "leaderboard" */ '@/views/Leaderboard.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import(/* webpackChunkName: "login" */ '@/views/Login.vue')
  },
  {
    path: '/bank',
    name: 'Bank',
    component: () => import(/* webpackChunkName: "bank" */ '@/views/Bank.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/user',
    name: 'UserCenter',
    component: () => import(/* webpackChunkName: "user-center" */ '@/views/UserCenter.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import(/* webpackChunkName: "notifications" */ '@/views/Notifications.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile/:id',
    name: 'PublicProfile',
    component: () => import(/* webpackChunkName: "public-profile" */ '@/views/PublicProfile.vue')
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import(/* webpackChunkName: "admin-dashboard" */ '@/views/admin/AdminDashboard.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/match/create',
    name: 'MatchCreate',
    component: () => import(/* webpackChunkName: "admin-match-create" */ '@/views/admin/MatchCreate.vue'),
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
    component: () => import(/* webpackChunkName: "about" */ '@/views/About.vue')
  },
  {
    path: '*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
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