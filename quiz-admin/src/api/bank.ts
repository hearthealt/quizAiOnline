import request from '@/utils/request'
import type { PageResult } from '@/types'

export function getList(params: { categoryId?: number; keyword?: string; pageNum: number; pageSize: number }) {
  return request.get<PageResult<any>>('/api/admin/bank/list', { params })
}

export function getDetail(id: number) {
  return request.get(`/api/admin/bank/${id}`)
}

export function create(data: Record<string, any>) {
  return request.post('/api/admin/bank', data)
}

export function update(id: number, data: Record<string, any>) {
  return request.put(`/api/admin/bank/${id}`, data)
}

export function remove(id: number) {
  return request.delete(`/api/admin/bank/${id}`)
}
