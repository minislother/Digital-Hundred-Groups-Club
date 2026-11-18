import request from '@/utils/request'

// 本社未审核的活动
export function getMyApps(searchInfo, groupName, pagination = { pageNum: 1, pageSize: 20 }) {
  const data = new URLSearchParams()
  data.append('searchInfo', searchInfo)
  data.append('groupName', groupName)
  return request({
    url: '/activity/myApps',
    method: 'post',
    params: pagination,
    data: data
  })
}

// 本社审核通过和不通过的活动
export function getMyActivity(groupName, pagination = { pageNum: 1, pageSize: 20 }) {
  return request({
    url: '/activity/myActivity',
    method: 'post',
    params: { groupName , ...pagination }
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

// TODO: modifyInfo, addActivity, deleteActivity with param(activity)
// the hell who can fix this
// 处理当前组件的业务逻辑或用户交互。
export function modifyInfo(activity) {
  // let data = new URLSearchParams();
  // data.append("name", activity.name)
  // data.append("organizer", activity.organizer,)
  // data.append("type", activity.type,)
  // data.append("place", activity.place,)
  // data.append("time", activity.time,)
  // data.append("arrange", activity.arrange,)
  // data.append("desc", activity.desc,)
  // data.append("groupName", activity.groupName)
  return request({
    url: '/activity/modifyInfo',
    method: 'post',
    data: activity
  })
}

// 新增业务数据或追加界面状态。
export function addActivity(activity) {
  return request({
    url: '/activity/addActivity',
    method: 'post',
    data: activity
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

// super admin
// 读取并返回业务数据或页面状态。
export function getAllApps(searchInfo, pagination = { pageNum: 1, pageSize: 20 }) {
  return request({
    url: '/activity/allApps',
    method: 'post',
    params: { searchInfo , ...pagination }
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
