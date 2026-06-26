/**
 * Created by PanJiaChen on 16/11/18.
 */

/**
 * @param {string} path
 * @returns {Boolean}
 */
// 判断链接或图标地址是否为外部资源。
export function isExternal(path) {
  return /^(https?:|mailto:|tel:)/.test(path)
}

/**
 * @param {string} str
 * @returns {Boolean}
 */
// 处理当前组件的业务逻辑或用户交互。
export function validuserName(str) {
  return str.trim().length === 8;
}
