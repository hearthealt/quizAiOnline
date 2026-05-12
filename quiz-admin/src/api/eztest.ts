import request from '@/utils/request'
import type { PageResult } from '@/types'

export function getProfile() {
  return request.get('/api/admin/eztest/profile')
}

export function saveProfile(data: Record<string, any>) {
  return request.put('/api/admin/eztest/profile', data)
}

export function listSessions(data: Record<string, any>) {
  return request.post('/api/admin/eztest/sessions', data)
}

export function createJob(data: Record<string, any>) {
  return request.post('/api/admin/eztest/jobs', data)
}

export function importJob(id: number, data: Record<string, any>) {
  return request.post(`/api/admin/eztest/jobs/${id}/import`, data)
}

export function getJob(id: number) {
  return request.get(`/api/admin/eztest/jobs/${id}`)
}

export function getJobFiles(id: number, params: Record<string, any>) {
  return request.get<PageResult<any>>(`/api/admin/eztest/jobs/${id}/files`, { params })
}

export function getJobList(params: Record<string, any>) {
  return request.get<PageResult<any>>('/api/admin/eztest/jobs', { params })
}

export function deleteJob(id: number) {
  return request.delete(`/api/admin/eztest/jobs/${id}`)
}

export function batchDeleteJobs(ids: number[]) {
  return request.delete('/api/admin/eztest/jobs/batch', { data: ids })
}

export function downloadFile(jobId: number, fileId: number) {
  return request.get(`/api/admin/eztest/jobs/${jobId}/files/${fileId}`, { responseType: 'blob' })
}
