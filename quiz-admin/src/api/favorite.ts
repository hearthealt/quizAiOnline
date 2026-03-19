import request from '@/utils/request'

export function getAdminFavoriteList(params: Record<string, any>) {
  return request.get('/api/admin/favorite/list', { params })
}
