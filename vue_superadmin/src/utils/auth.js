import Cookies from 'js-cookie'

const TokenKey = 'baituan_superadmin_token'
const cookieOptions = {
  sameSite: 'strict',
  secure: window.location.protocol === 'https:',
  expires: 7
}

export function getToken() {
  return Cookies.get(TokenKey)
}

export function setToken(token) {
  return Cookies.set(TokenKey, token, cookieOptions)
}

export function removeToken() {
  return Cookies.remove(TokenKey)
}
