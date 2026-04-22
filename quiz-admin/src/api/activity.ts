import request from '@/utils/request'
import type { PageResult } from '@/types'

export function getActivityOverview(date?: string) {
  return request.get('/api/admin/activity/overview', { params: { date } })
}

export function getActiveUsers(params: Record<string, any>) {
  return request.get<PageResult<any>>('/api/admin/activity/users', { params })
}

export function getActivityUserDetail(userId: number, params?: Record<string, any>) {
  return request.get(`/api/admin/activity/users/${userId}`, { params })
}

export function getAnswerDetails(params: Record<string, any>) {
  return request.get<PageResult<any>>('/api/admin/activity/answers', { params })
}
