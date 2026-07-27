import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
// 注：响应拦截器解包 response.data 为 ApiResponse 后直接返回，
// 下游调用方拿到的不再是 AxiosResponse 而是 ApiResponse 本身。
// 因此返回值需 cast 为 AxiosResponse 以满足 axios 拦截器类型签名。
import { getToken, removeToken } from './auth'
import { ElMessage } from 'element-plus'
import router from '@/router'
import type { ApiResponse } from '@/types'

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data as ApiResponse
    if (res.code !== undefined && res.code !== 0 && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res as unknown as AxiosResponse
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        ElMessage.error('登录已过期，请重新登录')
        removeToken()
        localStorage.removeItem('user')
        // 防止多个 401 并发触发重复导航（NavigationCancelled）
        if (router.currentRoute.value.path !== '/login') {
          router.push('/login').catch(() => {})
        }
      } else if (status === 403) {
        ElMessage.error('没有权限执行此操作')
      } else if (status === 500) {
        ElMessage.error('服务器错误，请稍后重试')
      } else {
        ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default request