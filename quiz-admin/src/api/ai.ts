import request from '@/utils/request'
import type { PageResult } from '@/types'

export function getConfig() {
  return request.get('/api/admin/ai/config')
}

export function updateConfig(data: Record<string, any>) {
  return request.put('/api/admin/ai/config', data)
}

export function testConnection() {
  return request.post('/api/admin/ai/test')
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

// iFlow API Key 管理
export function getIflowStatus() {
  return request.get('/api/admin/ai/iflow/status')
}

export function refreshIflowKey() {
  return request.post('/api/admin/ai/iflow/refresh')
}

export function getIflowModels() {
  return request.get('/api/admin/ai/iflow/models')
}

// 文件转换
export function parseFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/admin/ai/convert/parse', formData)
}

export function ruleParse(content: string) {
  return request.post('/api/admin/ai/convert/rule-parse', content, {
    headers: { 'Content-Type': 'text/plain' }
  })
}
