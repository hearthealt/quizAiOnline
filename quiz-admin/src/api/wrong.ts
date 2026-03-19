import request from '@/utils/request'

export function getAdminWrongList(params: Record<string, any>) {
  return request.get('/api/admin/wrong/list', { params })
}
