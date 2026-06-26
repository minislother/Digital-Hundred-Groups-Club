import request from '@/utils/request'

// 读取并返回业务数据或页面状态。
export function getList(searchInfo) {
  return request({
    url: '/group/all',
    method: 'get',
    params: { searchInfo }
  })
}
