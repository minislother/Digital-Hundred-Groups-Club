import request from '@/utils/request'

// 读取并返回业务数据或页面状态。
export function getActivites(searchInfo, pageNum = 1, pageSize = 32) {
  return request({
    url: '/activity/all',
    method: 'get',
    params: { searchInfo, pageNum, pageSize }
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


// 游标搜索支持 ES PIT 与 MySQL 降级，不统计全部命中数。
export function searchActivitySlice(keyword, cursor = null, pageSize = 32) {
  return request({ url: '/activity/searchSlice', method: 'get', params: { keyword, cursor, pageSize } })
}
