import request from '@/utils/request'

// 处理当前组件的业务逻辑或用户交互。
export function login(userId, password) {
  const data = new URLSearchParams()
  data.append('userId', userId)
  data.append('password', password)
  return request({
    url: '/topManager/login',
    method: 'post',
    data
  })
}

// 读取并返回业务数据或页面状态。
export function getInfo() {
  return request({
    url: '/topManager/info',
    method: 'get'
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function logout() {
  return request({
    url: '/topManager/logout',
    method: 'post'
  })
}
// 读取并返回业务数据或页面状态。
export function getAllStudent(searchInfo, pagination = { pageNum: 1, pageSize: 20 }) {
  return request({
    url: '/individualGroup/getAllStudents',
    method: 'post',
    params: { searchInfo, ...pagination }
  })
}
// 展示弹窗、详情或提示内容。
export function showGroups(searchInfo, pageNum = 1, pageSize = 32) {
  return request({
    url: '/group/all',
    method: 'post',
    params: { searchInfo, pageNum, pageSize }
  })
}

// 读取并返回业务数据或页面状态。
export function getGroups(searchInfo, pageNum = 1, pageSize = 32) {
  return request({
    url: '/group/all',
    method: 'get',
    params: { searchInfo, pageNum, pageSize }
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function findtop() {
  return request({
    url: '/group/top',
    method: 'get',
    params: { }
  })
}

// 读取并返回业务数据或页面状态。
export function getcount() {
  return request({
    url: '/individualGroup/getGroupMembers',
    method: 'get',
    params: { }
  })
}

// 处理文件上传相关逻辑。
export function uploadfile1(file) {
  return request({
    url: '/topManager/uploadExcel',
    method: 'post',
    data: file
  })
}

// 下载后端提供的附件或模板文件。
export function download1(fileName) {
  return request({
    url: '/topManager/downloadExcel',
    method: 'get',
    params: { fileName }
  })
}

// 导入任务保存在服务器，页面刷新后仍可查询。
export function listImportTasks() {
  return request({ url: '/topManager/importTasks', method: 'get' })
}
export function getImportErrors(id, after = 0) {
  return request({ url: `/topManager/importTasks/${id}/errors`, method: 'get', params: { after }})
}
