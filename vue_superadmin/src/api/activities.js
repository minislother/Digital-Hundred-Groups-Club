import request from '@/utils/request'

// 根据筛选条件查询并更新列表结果。
export function searchActivities(searchInfo, pagination = { pageNum: 1, pageSize: 20 }) {
  return request({
    url: '/activity/allApps',
    method: 'post',
    params: { searchinfo: searchInfo, ...pagination }
  })
}

// 读取并返回业务数据或页面状态。
export function getManangerDetails(groupName, activityName) {
  return request({
    url: '/activity/managerDetail',
    method: 'post',
    params: { groupName, activityName }
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function modDesc(groupName, activityName, description, attachment, image) {
  return request({
    url: '/activity/modifyDescription',
    method: 'post',
    params: { groupName, activityName, description, attachment, image }
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function modifyInfo(activity) {
  return request({
    url: '/activity/modifyInfo',
    method: 'post',
    params: { activity }
  })
}

// 新增业务数据或追加界面状态。
export function addActivity(activity) {
  return request({
    url: '/activity/addActivity',
    method: 'post',
    params: { activity }
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function acceptActivity(activityId) {
  return request({
    url: '/activity/accept',
    method: 'post',
    params: { activityId }
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function rejectActivity(activityId) {
  return request({
    url: '/activity/reject',
    method: 'post',
    params: { activityId }
  })
}

export function listAdmissionDeadLetters() {
  return request({ url: '/admin/activity-admission/dead-letters', method: 'get' })
}

export function retryAdmissionDeadLetter(id) {
  return request({ url: `/admin/activity-admission/dead-letters/${id}/retry`, method: 'post' })
}

export function resolveAdmissionDeadLetter(id) {
  return request({ url: `/admin/activity-admission/dead-letters/${id}/resolve`, method: 'post' })
}

// super admin
// 读取并返回业务数据或页面状态。
export function getAllApps(pagination = { pageNum: 1, pageSize: 20 }) {
  return request({
    url: '/activity/allApps',
    method: 'post',
    params: pagination
  })
}

// 读取并返回业务数据或页面状态。
export function getAppDetail(groupName, activityName) {
  return request({
    url: '/activity/appDetail',
    method: 'post',
    params: { groupName, activityName }
  })
}
