import request from '@/utils/request'
import type { PageResult } from '@/types'

export function getList(params: { keyword?: string; pageNum: number; pageSize: number }) {
  return request.get<PageResult<any>>('/api/admin/category/list', { params })
}

export function getAll() {
  return request.get('/api/admin/category/all')
}

export function create(data: Record<string, any>) {
  return request.post('/api/admin/category', data)
}

export function update(id: number, data: Record<string, any>) {
  return request.put(`/api/admin/category/${id}`, data)
}

export function remove(id: number) {
  return request.delete(`/api/admin/category/${id}`)
}

export function toggleStatus(id: number, status: number) {
  return request.put(`/api/admin/category/${id}/status`, { status })
}
