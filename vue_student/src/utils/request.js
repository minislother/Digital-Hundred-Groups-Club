import axios from 'axios'
import { ElMessageBox as MessageBox, ElMessage as Message } from 'element-plus'
import shared from '@baituan/frontend-shared'
import store from '@/store'
import { getToken } from '@/utils/auth'

const service = shared.createRequest({
  axios,
  MessageBox,
  Message,
  store,
  getToken,
  baseURL: process.env.VUE_APP_BASE_API
})

export default service
