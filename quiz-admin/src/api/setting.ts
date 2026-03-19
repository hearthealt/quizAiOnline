import request from '@/utils/request'

export function getSetting() {
  return request.get('/api/admin/system/setting')
}

export function updateSetting(data: Record<string, string>) {
  return request.put('/api/admin/system/setting', data)
}
