import request from '@/utils/request'
import type { PageResult } from '@/types'

export function getConfig() {
  return request.get('/api/admin/ai/config')
}

export function updateConfig(data: Record<string, any>) {
  return request.put('/api/admin/ai/config', data)
}

export function testConnection(data?: Record<string, any>) {
  return request.post('/api/admin/ai/test', data)
}

export function getModels(data: Record<string, any>) {
  return request.post('/api/admin/ai/models', data)
}

export function generate(data: { questionId: number; mode: string }) {
  return request.post('/api/admin/ai/generate', data)
}

export function batchGenerate(data: Record<string, any>) {
  return request.post('/api/admin/ai/batch-generate', data)
}

export function getLogList(params: Record<string, any>) {
  return request.get<PageResult<any>>('/api/admin/ai/log/list', { params })
}

export function getStats() {
  return request.get('/api/admin/ai/stats')
}
