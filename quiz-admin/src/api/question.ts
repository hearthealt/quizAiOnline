import request from '@/utils/request'
import type { PageResult } from '@/types'

export function getList(params: { bankId?: number; type?: number; keyword?: string; pageNum: number; pageSize: number }) {
  return request.get<PageResult<any>>('/api/admin/question/list', { params })
}

export function getDetail(id: number) {
  return request.get(`/api/admin/question/${id}`)
}

export function create(data: Record<string, any>) {
  return request.post('/api/admin/question', data)
}

export function update(id: number, data: Record<string, any>) {
  return request.put(`/api/admin/question/${id}`, data)
}

export function remove(id: number) {
  return request.delete(`/api/admin/question/${id}`)
}

export function batchDelete(ids: number[]) {
  return request.delete('/api/admin/question/batch', { data: ids })
}

export function importExcel(bankId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('bankId', String(bankId))
  return request.post('/api/admin/question/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function downloadTemplate() {
  return request.get('/api/admin/question/template', { responseType: 'blob' })
}
