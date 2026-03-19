import request from '@/utils/request'
import type { PageResult } from '@/types'

export function getPlanList() {
  return request.get('/api/admin/vip/plan/list')
}

export function createPlan(data: Record<string, any>) {
  return request.post('/api/admin/vip/plan', data)
}

export function updatePlan(id: number, data: Record<string, any>) {
  return request.put(`/api/admin/vip/plan/${id}`, data)
}

export function deletePlan(id: number) {
  return request.delete(`/api/admin/vip/plan/${id}`)
}

export function togglePlanStatus(id: number, status: number) {
  return request.put(`/api/admin/vip/plan/${id}/status`, { status })
}

export function getOrderList(params: Record<string, any>) {
  return request.get<PageResult<any>>('/api/admin/vip/order/list', { params })
}

export function approveOrder(id: number) {
  return request.put(`/api/admin/vip/order/${id}/approve`)
}

export function rejectOrder(id: number) {
  return request.put(`/api/admin/vip/order/${id}/reject`)
}

export function getStats() {
  return request.get('/api/admin/vip/stats')
}
