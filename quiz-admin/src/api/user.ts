import request from '@/utils/request'
import type { PageResult } from '@/types'

export function getList(params: Record<string, any>) {
  return request.get<PageResult<any>>('/api/admin/user/list', { params })
}

export function getDetail(id: number) {
  return request.get(`/api/admin/user/${id}`)
}

export function updateStatus(id: number, status: number) {
  return request.put(`/api/admin/user/${id}/status`, null, { params: { status } })
}

export function getRecords(id: number, params: Record<string, any>) {
  return request.get<PageResult<any>>(`/api/admin/user/${id}/records`, { params })
}

export function getChatHistory(id: number) {
  return request.get<any[]>(`/api/admin/user/${id}/chat-history`)
}

export function setVip(id: number, isVip: number, expireTime: Date | null) {
  return request.put(`/api/admin/user/${id}/vip`, null, {
    params: { isVip, expireTime: expireTime ? new Date(expireTime).toISOString() : null }
  })
}
