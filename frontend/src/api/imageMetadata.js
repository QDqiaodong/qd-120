import request from '@/utils/request'

export function getImageMetadataPage(params) {
  return request({
    url: '/image-metadata/page',
    method: 'get',
    params
  })
}

export function getImageMetadataByStopperNo(stopperNo) {
  return request({
    url: `/image-metadata/stopper/${stopperNo}`,
    method: 'get'
  })
}

export function getImageMetadataById(id) {
  return request({
    url: `/image-metadata/${id}`,
    method: 'get'
  })
}

export function addImageMetadata(data) {
  return request({
    url: '/image-metadata',
    method: 'post',
    data
  })
}

export function updateImageMetadata(data) {
  return request({
    url: '/image-metadata',
    method: 'put',
    data
  })
}

export function deleteImageMetadata(id) {
  return request({
    url: `/image-metadata/${id}`,
    method: 'delete'
  })
}
