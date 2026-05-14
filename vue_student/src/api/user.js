import request from '@/utils/request'

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

export function register(data) {
  return request({
    url: '/student/register',
    method: 'post',
    data: data
  })
}

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

export function modifyPhone(stuNumber, phone) {
  return request({
      url: '/student/modifyPhone',
      method: 'post',
      params: { stuNumber, phone }
  })
}

export function modifyDescription(stuNumber, description) {
  return request({
      url: '/student/modifyDescription',
      method: 'post',
      params: { stuNumber, description }
  })
}

export function modifyNickname(stuNumber, nickname) {
  return request({
      url: '/student/modifyNickname',
      method: 'post',
      params: { stuNumber, nickname }
  })
}

export function validateEmail(email) {
  return request({
      url: '/student/validateEmail',
      method: 'post',
      params: { email }
  })
}

export function getProfile(stuNumber) {
  return request({
    url: '/student/profile',
    method: 'post',
    params: { stuNumber }
  })
}


export function getInfo() {
  return request({
    url: '/student/info',
    method: 'get'
  })
}

export function logout() {
  return request({
    url: '/student/logout',
    method: 'post'
  })
}

export function verifyemail(stuNumber) {
  return request({
    url: '/student/getEmail',
    method: 'post',
    params: { stuNumber }
  })
}

export function verifycode(email, validateCode) {
  return request({
    url: '/student/getValidate',
    method: 'post',
    params: { email, validateCode }
  })
}

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
