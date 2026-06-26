import request from '@/utils/request'

// 处理当前组件的业务逻辑或用户交互。
export function login(stuNumber,password) {
  const data = new URLSearchParams()
  data.append('stuNumber', stuNumber)
  data.append('password', password)
  return request({
    url: '/student/login',
    method: 'post',
    data
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function register(data) {
  return request({
    url: '/student/register',
    method: 'post',
    data: data
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function modifyPassword(stuNumber, password) {
  const data = new URLSearchParams()
  data.append('stuNumber', stuNumber)
  data.append('password', password)
  return request({
      url: '/student/modifyPass',
      method: 'post',
      data
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function modifyPhone(stuNumber, phone) {
  return request({
      url: '/student/modifyPhone',
      method: 'post',
      params: { stuNumber, phone }
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function modifyDescription(stuNumber, description) {
  return request({
      url: '/student/modifyDescription',
      method: 'post',
      params: { stuNumber, description }
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function modifyNickname(stuNumber, nickname) {
  return request({
      url: '/student/modifyNickname',
      method: 'post',
      params: { stuNumber, nickname }
  })
}

// 校验输入内容是否满足业务要求。
export function validateEmail(email) {
  return request({
      url: '/student/validateEmail',
      method: 'post',
      params: { email }
  })
}

// 读取并返回业务数据或页面状态。
export function getProfile(stuNumber) {
  return request({
    url: '/student/profile',
    method: 'post',
    params: { stuNumber }
  })
}


// 读取并返回业务数据或页面状态。
export function getInfo() {
  return request({
    url: '/student/info',
    method: 'get'
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function logout() {
  return request({
    url: '/student/logout',
    method: 'post'
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function verifyemail(stuNumber) {
  return request({
    url: '/student/getEmail',
    method: 'post',
    params: { stuNumber }
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function verifycode(email, validateCode) {
  return request({
    url: '/student/getValidate',
    method: 'post',
    params: { email, validateCode }
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function modifypas(stuNumber,password) {
  const data = new URLSearchParams()
  data.append('stuNumber', stuNumber)
  data.append('password', password)
  return request({
    url: '/student/modifyPass',
    method: 'post',
    data
  })
}
