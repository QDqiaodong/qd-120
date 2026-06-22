import request from '@/utils/request'

export function getStopperList() {
  return request({
    url: '/stopper/list',
    method: 'get'
  })
}

export function getStopperPage(params) {
  return request({
    url: '/stopper/page',
    method: 'get',
    params
  })
}

export function getStopperGroupByStation() {
  return request({
    url: '/stopper/group-by-station',
    method: 'get'
  })
}

export function getStopperGroupByEquipment() {
  return request({
    url: '/stopper/group-by-equipment',
    method: 'get'
  })
}

export function getStopperById(id) {
  return request({
    url: `/stopper/${id}`,
    method: 'get'
  })
}

export function getStopperByNo(stopperNo) {
  return request({
    url: `/stopper/no/${stopperNo}`,
    method: 'get'
  })
}

export function getAllSpecs() {
  return request({
    url: '/stopper/specs',
    method: 'get'
  })
}

export function getAllStations() {
  return request({
    url: '/stopper/stations',
    method: 'get'
  })
}

export function getAllEquipments() {
  return request({
    url: '/stopper/equipments',
    method: 'get'
  })
}

export function generateStopperNo(spec) {
  return request({
    url: '/stopper/generate-no',
    method: 'get',
    params: { spec }
  })
}

export function addStopper(data) {
  return request({
    url: '/stopper',
    method: 'post',
    data
  })
}

export function updateStopper(data) {
  return request({
    url: '/stopper',
    method: 'put',
    data
  })
}

export function deleteStopper(id) {
  return request({
    url: `/stopper/${id}`,
    method: 'delete'
  })
}

export function getNameplates(ids) {
  return request({
    url: '/stopper/nameplates',
    method: 'post',
    data: ids
  })
}

export function getEquipmentHistory(id) {
  return request({
    url: `/stopper/${id}/equipment-history`,
    method: 'get'
  })
}
