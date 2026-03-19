import request from '@/utils/request'

export function getOverview() {
  return request.get('/api/admin/dashboard/overview')
}

export function getTrend(days?: number) {
  return request.get('/api/admin/dashboard/trend', { params: { days } })
}

export function getRank(limit?: number) {
  return request.get('/api/admin/dashboard/rank', { params: { limit } })
}

// 复用其他模块 API
export function getVipStats() {
  return request.get('/api/admin/vip/stats')
}

export function getAiStats() {
  return request.get('/api/admin/ai/stats')
}

export function getPendingOrders() {
  return request.get('/api/admin/vip/order/list', { params: { status: 0, pageNum: 1, pageSize: 5 } })
}

export function getRecentUsers() {
  return request.get('/api/admin/user/list', { params: { pageNum: 1, pageSize: 5 } })
}
