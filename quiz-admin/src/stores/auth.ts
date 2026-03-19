import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'
import router from '@/router'
import type { Admin } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('admin_token') || '')
  const adminInfo = ref<Admin | null>(null)

  // 初始化时从 localStorage 恢复 adminInfo
  const savedInfo = localStorage.getItem('admin_info')
  if (savedInfo) {
    try {
      adminInfo.value = JSON.parse(savedInfo)
    } catch {}
  }

  function setToken(t: string) {
    token.value = t
    localStorage.setItem('admin_token', t)
  }

  function setAdminInfo(info: Admin | null) {
    adminInfo.value = info
    if (info) {
      localStorage.setItem('admin_info', JSON.stringify(info))
    } else {
      localStorage.removeItem('admin_info')
    }
  }

  async function login(username: string, password: string) {
    const data = await request.post('/api/admin/auth/login', { username, password }) as any
    // 后端返回的是 AdminInfoVO，直接包含 token 和用户信息
    setToken(data.token)
    setAdminInfo({
      id: data.id,
      username: data.username,
      nickname: data.nickname,
      avatar: data.avatar,
      role: data.role,
      status: 1,
      createTime: ''
    })
  }

  // 获取当前用户信息（用于刷新或验证）
  async function fetchInfo() {
    if (!token.value) return
    try {
      const data = await request.get('/api/admin/auth/info') as any
      setAdminInfo({
        id: data.id,
        username: data.username,
        nickname: data.nickname,
        avatar: data.avatar,
        role: data.role,
        status: 1,
        createTime: ''
      })
    } catch {
      // token 失效，登出
      logout()
    }
  }

  function logout() {
    token.value = ''
    adminInfo.value = null
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_info')
    router.push('/login')
  }

  return { token, adminInfo, setToken, setAdminInfo, login, fetchInfo, logout }
})
