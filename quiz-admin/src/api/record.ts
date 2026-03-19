import request from '@/utils/request'
import type { PageResult } from '@/types'

export function getPracticeList(params: Record<string, any>) {
  return request.get<PageResult<any>>('/api/admin/record/practice/list', { params })
}

export function getPracticeDetail(id: number) {
  return request.get(`/api/admin/record/practice/${id}`)
}

export function getExamList(params: Record<string, any>) {
  return request.get<PageResult<any>>('/api/admin/record/exam/list', { params })
}

export function getExamDetail(id: number) {
  return request.get(`/api/admin/record/exam/${id}`)
}
