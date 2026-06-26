import Cookies from 'js-cookie'

const TokenKey = 'baituan_superadmin_token'
const cookieOptions = {
  sameSite: 'strict',
  secure: window.location.protocol === 'https:',
  expires: 7
}

// 读取超级管理员端登录令牌，供路由守卫和请求拦截器判断登录状态。
export function getToken() {
  return Cookies.get(TokenKey)
}

// 保存超级管理员端登录令牌，后续接口会通过请求拦截器自动携带。
export function setToken(token) {
  return Cookies.set(TokenKey, token, cookieOptions)
}

// 清除超级管理员端登录令牌，用于退出登录或登录失效后的状态重置。
export function removeToken() {
  return Cookies.remove(TokenKey)
}
