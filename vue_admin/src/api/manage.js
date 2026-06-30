import request from '@/utils/request'

// 读取并返回业务数据或页面状态。
export function getManages(groupName) {
  return request({
    url: '/group/myclub',
    method: 'post',
    params: { groupName }
  })
}

// 读取并返回业务数据或页面状态。
export function getDetails(groupname) {
  return request({
    url: '/group/managerDetail',
    method: 'post',
    params: { groupname }
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function redirectToGroupDetail(groupName) {
  return request({
    url: '/group/myclub',
    method: 'post',
    params: { groupName }
  })
}

// 更新表单字段或业务数据。
export function updateForm(groupname, description, attachment, image) {
  return request({
    url: '/group/modifydescription',
    method: 'post',
    params: { groupname, description, attachment, image }
  })
}

// 更新表单字段或业务数据。
export function updatePassword(groupName, password) {
  const data = new URLSearchParams()
  data.append('groupName', groupName)
  data.append('password', password)
  return request({
    url: '/group/modifyPassword',
    method: 'post',
    data
  })
}
