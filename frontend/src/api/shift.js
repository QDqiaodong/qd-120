import request from '@/utils/request'

export function getShiftList() {
  return request({
    url: '/shift/list',
    method: 'get'
  })
}

export function getShiftByStopper(stopperId) {
  return request({
    url: `/shift/stopper/${stopperId}`,
    method: 'get'
  })
}

export function addShift(data) {
  return request({
    url: '/shift',
    method: 'post',
    data
  })
}
