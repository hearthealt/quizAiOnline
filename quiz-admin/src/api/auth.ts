import request from '@/utils/request'

export function login(username: string, password: string) {
  return request.post('/api/admin/auth/login', { username, password })
}

export function logout() {
  return request.post('/api/admin/auth/logout')
}

export function getInfo() {
  return request.get('/api/admin/auth/info')
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request.put('/api/admin/auth/password', data)
}
