import { createApp } from 'vue'

import 'normalize.css/normalize.css' // A modern alternative to CSS resets

import ElementPlus, { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import '@/styles/index.scss' // global css

import App from './App'
import store from './store'
import router from './router'

import { registerSvgIcons } from '@/icons' // icon
import '@/permission' // permission control

/**
 * If you don't want to use mock-server
 * you want to use MockJs for mock api
 * you can execute: mockXHR()
 *
 * Currently MockJs will be used in the production environment,
 * please remove it before going online ! ! !
 */
if (process.env.NODE_ENV === 'production') {
  const { mockXHR } = require('../mock')
  mockXHR()
}

const app = createApp(App)

registerSvgIcons(app)
app.use(store)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.config.globalProperties.$message = ElMessage
app.config.globalProperties.$confirm = ElMessageBox.confirm
app.mount('#app')
