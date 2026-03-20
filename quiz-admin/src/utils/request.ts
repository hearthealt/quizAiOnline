import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse, AxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

// 请求取消管理
const pendingRequests = new Map<string, AbortController>()

const generateRequestKey = (config: AxiosRequestConfig): string => {
  return `${config.method}:${config.url}`
}

const removePendingRequest = (config: AxiosRequestConfig) => {
  const key = generateRequestKey(config)
  if (pendingRequests.has(key)) {
    pendingRequests.get(key)?.abort()
    pendingRequests.delete(key)
  }
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000,
})

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 取消重复请求
    removePendingRequest(config)
    const controller = new AbortController()
    config.signal = controller.signal
    pendingRequests.set(generateRequestKey(config), controller)

    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = authStore.token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    pendingRequests.delete(generateRequestKey(response.config))
    
    // Blob 响应直接返回，不走 JSON 解析
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const res = response.data
    if (res.code !== 0 && res.code !== 200) {
      window.$message?.error(res.message || '请求失败')
      if (res.code === 401) {
        const authStore = useAuthStore()
        authStore.logout()
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  (error) => {
    if (error.config) {
      pendingRequests.delete(generateRequestKey(error.config))
    }
    
    // 请求被取消时不显示错误
    if (axios.isCancel(error)) {
      return Promise.reject(error)
    }
    
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      router.push('/login')
    }
    const message = error.response?.data?.message || error.message || '网络错误'
    window.$message?.error(message)
    return Promise.reject(error)
  }
)

/**
 * 取消所有进行中的请求
 */
export const cancelAllRequests = () => {
  pendingRequests.forEach((controller) => controller.abort())
  pendingRequests.clear()
}

/**
 * 取消指定请求
 */
export const cancelRequest = (method: string, url: string) => {
  const key = `${method}:${url}`
  pendingRequests.get(key)?.abort()
  pendingRequests.delete(key)
}

export default service
