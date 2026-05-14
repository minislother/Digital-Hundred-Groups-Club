import request from '@/utils/request'


export function managerlogin(userId,password) {
  const data = new URLSearchParams()
  data.append('userId', userId)
  data.append('password', password)
  return request({
    url: '/manager/login',
    method: 'post',
    data
  })
}

export function getInfo() {
  return request({
    url: '/manager/info',
    method: 'get'
  })
}

export function logout() {
  return request({
    url: '/manager/logout',
    method: 'post'
  })
}

  export function managedgroup(managerId) {
    return request({
      url: '/individualGroup/allManagedGroups',
      method: 'get',
      params: { managerId }
    })
}


export function getStudentDetail(userId) {
  return request({
    url: '/manager/profile',
    method: 'post',
    params: { userId }
  })
}

export function searchStudent(studentId) {
  return request({
    url: '/individualGroup/allGroups',
    method: 'post',
    params: { studentId }
  })
}
