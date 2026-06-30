import request from '@/utils/request'

// 读取并返回业务数据或页面状态。
export function getActivites(searchInfo) {
  return request({
    url: '/activity/all',
    method: 'get',
    params: { searchInfo }
  })
}

// 读取并返回业务数据或页面状态。
export function getActivitesDetail() {
  return request({
    url: '/activity/studentDetail',
    method: 'get',
    params: { }
  })
}

// 读取并返回业务数据或页面状态。
export function getTopActivites() {
  return request({
    url: '/activity/top',
    method: 'get',
    params: { }
  })
}
