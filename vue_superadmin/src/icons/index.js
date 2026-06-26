import SvgIcon from '@/components/SvgIcon'// svg component

const req = require.context('./svg', false, /\.svg$/)
const requireAll = requireContext => requireContext.keys().map(requireContext)
requireAll(req)

// 处理当前组件的业务逻辑或用户交互。
export function registerSvgIcons(app) {
  app.component('svg-icon', SvgIcon)
}
