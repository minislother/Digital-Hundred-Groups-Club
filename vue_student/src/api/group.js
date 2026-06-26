import request from '@/utils/request'

// 读取并返回业务数据或页面状态。
export function getGroups(searchInfo) {
  return request({
    url: '/group/all',
    method: 'get',
    params: { searchInfo }
  })
}

// 读取并返回业务数据或页面状态。
export function getVideo() {
  return request({
    url: '/group/getVideo',
    method: 'get',
    params: { }
  })
}

// 读取并返回业务数据或页面状态。
export function getWeather() {
  return request({
    url: '/getWeather',
    method: 'get',
    params: { }
  })
}

// 读取并返回业务数据或页面状态。
export function getTopGroups() {
  return request({
    url: '/group/top',
    method: 'get',
    params: { }
  })
}

// 读取并返回业务数据或页面状态。
export function getApplications(stuNumber) {
  return request({
    url: '/application/myApps',
    method: 'post',
    params: { stuNumber }
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function postform(form) {
  return request({
    url: '/application/submit',
    method: 'post',
    data: form
  })
}

// 读取并返回业务数据或页面状态。
export function getAttachmentUrl(groupId) {
  return request({
    url: '/group/getAttachment',
    method: 'post',
    params: { id: groupId }
  })
}

// 展示弹窗、详情或提示内容。
export function showGroups(searchInfo) {
  return request({
    url: '/group/all',
    method: 'post',
    params: { searchInfo }
  })
}

// 读取并返回业务数据或页面状态。
export function getMyGroups(studentId) {
  return request({
    url: '/individualGroup/allGroups',
    method: 'post',
    params: { studentId }
  })
}

