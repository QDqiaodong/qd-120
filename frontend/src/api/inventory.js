import request from '@/utils/request'

export function getInventoryList() {
  return request({
    url: '/inventory/list',
    method: 'get'
  })
}

export function getInventoryById(id) {
  return request({
    url: `/inventory/${id}`,
    method: 'get'
  })
}

export function getInventoryDetail(inventoryId) {
  return request({
    url: `/inventory/detail/${inventoryId}`,
    method: 'get'
  })
}

export function startInventory(data) {
  return request({
    url: '/inventory/start',
    method: 'post',
    data
  })
}

export function markInventoryItem(data) {
  return request({
    url: '/inventory/mark',
    method: 'post',
    data
  })
}

export function completeInventory(data) {
  return request({
    url: '/inventory/complete',
    method: 'post',
    data
  })
}
