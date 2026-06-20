import request from '@/utils/request'

export function getScrapList() {
  return request({
    url: '/scrap/list',
    method: 'get'
  })
}

export function getScrapById(id) {
  return request({
    url: `/scrap/${id}`,
    method: 'get'
  })
}

export function addScrap(data) {
  return request({
    url: '/scrap',
    method: 'post',
    data
  })
}

export function getScrapByStopperId(stopperId) {
  return request({
    url: `/scrap/stopper/${stopperId}`,
    method: 'get'
  })
}
