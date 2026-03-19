import request from '@/utils/request'
import type { PageResult } from '@/types'

export function getAdminList(params: Record<string, any>) {
  return request.get<PageResult<any>>('/api/admin/system/admin/list', { params })
}

export function createAdmin(data: Record<string, any>) {
  return request.post('/api/admin/system/admin', data)
}

export function updateAdmin(id: number, data: Record<string, any>) {
  return request.put(`/api/admin/system/admin/${id}`, data)
}

export function deleteAdmin(id: number) {
  return request.delete(`/api/admin/system/admin/${id}`)
}
