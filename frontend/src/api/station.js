import request from '@/utils/request'

export function getStationNames(keyword) {
  return request({
    url: '/station/names',
    method: 'get',
    params: { keyword }
  })
}

export function getStationZones() {
  return request({
    url: '/station/zones',
    method: 'get'
  })
}

export function getStationPage(params) {
  return request({
    url: '/station/page',
    method: 'get',
    params
  })
}

export function getStationById(id) {
  return request({
    url: `/station/${id}`,
    method: 'get'
  })
}

export function getStationByName(stationName) {
  return request({
    url: `/station/name/${encodeURIComponent(stationName)}`,
    method: 'get'
  })
}

export function addStation(data) {
  return request({
    url: '/station',
    method: 'post',
    data
  })
}

export function ensureStation(stationName) {
  return request({
    url: '/station/ensure',
    method: 'post',
    params: { stationName }
  })
}

export function updateStation(data) {
  return request({
    url: '/station',
    method: 'put',
    data
  })
}

export function deleteStation(id) {
  return request({
    url: `/station/${id}`,
    method: 'delete'
  })
}
