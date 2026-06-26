import { login, logout, getInfo, register, validateEmail } from '@/api/user'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { resetRouter } from '@/router'

// 创建学生端用户模块的默认状态，确保退出登录后可以恢复到初始值。
const getDefaultState = () => {
  return {
    token: getToken(),
    name: '',
    avatar: ''
  }
}

const state = getDefaultState()

const mutations = {
  // 重置 Vuex 用户状态，避免上一个登录用户的数据残留。
  RESET_STATE: (state) => {
    Object.assign(state, getDefaultState())
  },
  // 更新当前会话令牌。
  SET_TOKEN: (state, token) => {
    state.token = token
  },
  // 更新导航栏或用户信息区域展示的姓名。
  SET_NAME: (state, name) => {
    state.name = name
  },
  // 更新用户头像地址。
  SET_AVATAR: (state, avatar) => {
    state.avatar = avatar
  }
}

const actions = {
  // 学生登录成功后，将后端返回的 token 同步到 Vuex 和 Cookie。
  login({ commit }, userInfo) {
    const { userName, password } = userInfo
    return new Promise((resolve, reject) => {
      login(userName.trim(), password).then(response => {
        const { data } = response
        commit('SET_TOKEN', data.token)
        setToken(data.token)
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 注册学生账号，参数名保持与后端 RegisterUser 对象一致。
  register(context, { stuNumber, password, email, valicode }) {
    return new Promise((resolve, reject) => {
      register({ stuNumber, password, email, valicode }).then((res) => {
        resolve(res)
      }).catch((err) => {
        reject(err)
      })
    })
  },

  // 请求后端向指定邮箱发送验证码。
  validateEmail(context, { email }) {
    return new Promise((resolve, reject) => {
      validateEmail({ email }).then((res) => {
        resolve(res)
      }).catch((err) => {
        reject(err)
      })
    })
  },

  // 拉取当前学生的基础信息，用于页面顶部和个人信息展示。
  getInfo({ commit, state }) {
    return new Promise((resolve, reject) => {
      getInfo(state.token).then(response => {
        const { data } = response

        if (!data) {
          return reject('Verification failed, please Login again.')
        }

        const { name, avatar } = data

        commit('SET_NAME', name)
        commit('SET_AVATAR', avatar)
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 退出登录并清空本地登录状态。
  logout({ commit, state }) {
    return new Promise((resolve, reject) => {
      logout(state.token).then(() => {
        removeToken()
        resetRouter()
        commit('RESET_STATE')
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  // 仅清理本地 token，常用于登录过期或请求被后端拒绝时。
  resetToken({ commit }) {
    return new Promise(resolve => {
      removeToken()
      commit('RESET_STATE')
      resolve()
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
