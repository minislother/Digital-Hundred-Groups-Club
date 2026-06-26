const AUTH_EXPIRED_CODES = [50008, 50012, 50014]

// 处理当前组件的业务逻辑或用户交互。
function isAuthExpired(status, code) {
  return status === 401 || AUTH_EXPIRED_CODES.includes(code)
}

// 处理文件上传相关逻辑。
function buildUploadAction(baseURL, path) {
  const normalizedBaseURL = baseURL || ''
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${normalizedBaseURL}${normalizedPath}`
}

// 格式化展示文本，便于页面直接显示。
function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}年${month}月${day}日 ${hours}:${minutes}:${seconds}`
}

// 创建业务数据或初始化页面结构。
function createRequest(options) {
  const {
    axios,
    Message,
    MessageBox,
    store,
    getToken,
    baseURL,
    timeout = 5000
  } = options

  // 处理当前组件的业务逻辑或用户交互。
  function confirmRelogin() {
    MessageBox.confirm('登录状态已失效，可以取消停留在当前页面，或重新登录。', '登录已过期', {
      confirmButtonText: '重新登录',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      if (!store || !store.dispatch) {
        location.reload()
        return
      }
      store.dispatch('user/resetToken').then(() => {
        location.reload()
      })
    })
  }

  const service = axios.create({
    baseURL,
    timeout
  })

  service.interceptors.request.use(
    config => {
      const token = getToken ? getToken() : ''
      if (token) {
        config.headers['X-Token'] = token
      }
      return config
    },
    error => Promise.reject(error)
  )

  service.interceptors.response.use(
    response => {
      const res = response.data

      if (res.code !== 20000) {
        Message({
          message: res.message || '请求失败',
          type: 'error',
          duration: 5 * 1000
        })

        if (isAuthExpired(response.status, res.code)) {
          confirmRelogin()
        }
        return Promise.reject(new Error(res.message || '请求失败'))
      }

      return res
    },
    error => {
      const response = error.response || {}
      const data = response.data || {}
      const code = data.code
      const message = data.message || error.message

      Message({
        message,
        type: 'error',
        duration: 5 * 1000
      })

      if (isAuthExpired(response.status, code)) {
        confirmRelogin()
      }

      return Promise.reject(error)
    }
  )

  return service
}

module.exports = {
  buildUploadAction,
  createRequest,
  formatDateTime,
  isAuthExpired
}
