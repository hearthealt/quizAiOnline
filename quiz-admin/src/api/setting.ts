import request from '@/utils/request'

let pendingSettingRequest: Promise<any> | null = null

export function getSetting() {
  if (pendingSettingRequest) {
    return pendingSettingRequest
  }

  pendingSettingRequest = request.get('/api/admin/system/setting')
    .finally(() => {
      pendingSettingRequest = null
    })

  return pendingSettingRequest
}

export function updateSetting(data: Record<string, string>) {
  pendingSettingRequest = null
  return request.put('/api/admin/system/setting', data)
}
