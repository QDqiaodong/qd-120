import request from '@/utils/request'

export function getMaintenanceList() {
  return request({
    url: '/maintenance/list',
    method: 'get'
  })
}

export function getActiveMaintenance() {
  return request({
    url: '/maintenance/active',
    method: 'get'
  })
}

export function getMaintenanceById(id) {
  return request({
    url: `/maintenance/${id}`,
    method: 'get'
  })
}

export function getMaintenanceByStopper(stopperId) {
  return request({
    url: `/maintenance/stopper/${stopperId}`,
    method: 'get'
  })
}

export function sendMaintenance(data) {
  return request({
    url: '/maintenance',
    method: 'post',
    data
  })
}

export function completeMaintenance(id, data) {
  return request({
    url: `/maintenance/${id}/complete`,
    method: 'post',
    data
  })
}
