import request from '@/utils/request'

// 读取并返回业务数据或页面状态。
export function getList(params) {
  return request({
    url: '/vue-admin-template/table/list',
    method: 'get',
    params
  })
}

// 读取并返回业务数据或页面状态。
export function getStudentList(groupId) {
  return request({
    url: '/individualGroup/getStudentsByGroup',
    method: 'post',
    params: { groupId }
  })
}
