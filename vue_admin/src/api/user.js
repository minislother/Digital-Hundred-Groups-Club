import request from '@/utils/request'


// 处理当前组件的业务逻辑或用户交互。
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

// 读取并返回业务数据或页面状态。
export function getInfo() {
  return request({
    url: '/manager/info',
    method: 'get'
  })
}

// 处理当前组件的业务逻辑或用户交互。
export function logout() {
  return request({
    url: '/manager/logout',
    method: 'post'
  })
}

  // 处理当前组件的业务逻辑或用户交互。
  export function managedgroup(managerId) {
    return request({
      url: '/individualGroup/allManagedGroups',
      method: 'get',
      params: { managerId }
    })
}


// 读取并返回业务数据或页面状态。
export function getStudentDetail(userId) {
  return request({
    url: '/manager/profile',
    method: 'post',
    params: { userId }
  })
}

// 根据筛选条件查询并更新列表结果。
export function searchStudent(studentId) {
  return request({
    url: '/individualGroup/allGroups',
    method: 'post',
    params: { studentId }
  })
}
