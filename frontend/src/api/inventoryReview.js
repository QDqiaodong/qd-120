import request from '@/utils/request'

export function getReviewSummary() {
  return request({
    url: '/inventory-review/summary',
    method: 'get'
  })
}

export function getPendingReviews(inventoryId) {
  return request({
    url: '/inventory-review/pending',
    method: 'get',
    params: inventoryId ? { inventoryId } : {}
  })
}

export function getReviewedList(inventoryId) {
  return request({
    url: '/inventory-review/reviewed',
    method: 'get',
    params: inventoryId ? { inventoryId } : {}
  })
}

export function getReviewHistory(detailId) {
  return request({
    url: `/inventory-review/history/${detailId}`,
    method: 'get'
  })
}

export function secondConfirm(data) {
  return request({
    url: '/inventory-review/second-confirm',
    method: 'post',
    data
  })
}

export function correctStation(data) {
  return request({
    url: '/inventory-review/correct-station',
    method: 'post',
    data
  })
}

export function markFound(data) {
  return request({
    url: '/inventory-review/mark-found',
    method: 'post',
    data
  })
}

export function processScrap(data) {
  return request({
    url: '/inventory-review/process-scrap',
    method: 'post',
    data
  })
}
