import Cookies from 'js-cookie'

const TokenKey = 'baituan_admin_token'
const cookieOptions = {
  sameSite: 'strict',
  secure: window.location.protocol === 'https:',
  expires: 7
}

export function getToken() {
  return Cookies.get(TokenKey)
}

export function setToken(token) {
  // 存储用户信息到浏览器的cookie
  return Cookies.set(TokenKey, token, cookieOptions)
}

export function removeToken() {
  return Cookies.remove(TokenKey)
}
