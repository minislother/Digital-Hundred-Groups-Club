import { createStore } from 'vuex'
import getters from './getters'
import app from './modules/app'
import settings from './modules/settings'
import user from './modules/user'

const defaultClubState = {
  clubid: 0,
  clubname: ''
}

// 加载页面所需数据，并同步到当前状态。
function loadClubState() {
  try {
    return JSON.parse(sessionStorage.getItem('clubState')) || defaultClubState
  } catch (error) {
    return defaultClubState
  }
}

// 保存业务数据或本地状态。
function saveClubState(state) {
  sessionStorage.setItem('clubState', JSON.stringify({
    clubid: state.clubid,
    clubname: state.clubname
  }))
}

const store = createStore({
  modules: {
    app,
    settings,
    user
  },
  getters,
  state: loadClubState(),
  mutations: {
    // 处理当前组件的业务逻辑或用户交互。
    chooseclub(state, cid) {
      state.clubid = cid
      saveClubState(state)
    },
    // 处理当前组件的业务逻辑或用户交互。
    clubname(state, cname) {
      state.clubname = cname
      saveClubState(state)
    },
    // 清空表单、状态或本地缓存。
    clearClubState(state) {
      state.clubid = defaultClubState.clubid
      state.clubname = defaultClubState.clubname
      sessionStorage.removeItem('clubState')
    }
  }
})

export default store
