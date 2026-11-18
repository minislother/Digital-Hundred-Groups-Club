import axios from 'axios'
import { ElMessageBox as MessageBox, ElMessage as Message } from 'element-plus'
import shared from '@baituan/frontend-shared'
import store from '@/store'
import { getToken } from '@/utils/auth'

const service = shared.createRequest({
  axios,
  MessageBox,
  Message,
  // 延迟读取状态容器，避免 ESM 下 store → API → request 的循环初始化。
  store: { dispatch: (...args) => store.dispatch(...args) },
  getToken,
  baseURL: process.env.VUE_APP_BASE_API
})

export default service
