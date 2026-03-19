import request from '@/utils/request'

export function getUserGrowth(params: Record<string, any>) {
  return request.get('/api/admin/stat/user-growth', { params })
}

export function getAnswerTrend(params: Record<string, any>) {
  return request.get('/api/admin/stat/answer-trend', { params })
}

export function getBankHot(limit?: number) {
  return request.get('/api/admin/stat/bank-hot', { params: { limit } })
}

export function getWrongRank(params: Record<string, any>) {
  return request.get('/api/admin/stat/wrong-rank', { params })
}

export function getAccuracyDist(bankId?: number) {
  return request.get('/api/admin/stat/accuracy-dist', { params: { bankId } })
}

export function getFavoriteRank(limit?: number) {
  return request.get('/api/admin/stat/favorite-rank', { params: { limit } })
}
