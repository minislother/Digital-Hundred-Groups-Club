import store from '@/store'

const { body } = document
const WIDTH = 992 // refer to Bootstrap's responsive design

export default {
  watch: {
    // 处理当前组件的业务逻辑或用户交互。
    $route(route) {
      if (this.device === 'mobile' && this.sidebar.opened) {
        store.dispatch('app/closeSideBar', { withoutAnimation: false })
      }
    }
  },
  // 组件挂载前注册页面级监听器。
  beforeMount() {
    window.addEventListener('resize', this.$_resizeHandler)
  },
  // 组件卸载前释放监听器或定时器。
  beforeUnmount() {
    window.removeEventListener('resize', this.$_resizeHandler)
  },
  // 组件挂载后初始化页面交互或监听器。
  mounted() {
    const isMobile = this.$_isMobile()
    if (isMobile) {
      store.dispatch('app/toggleDevice', 'mobile')
      store.dispatch('app/closeSideBar', { withoutAnimation: true })
    }
  },
  methods: {
    // use $_ for mixins properties
    // https://vuejs.org/v2/style-guide/index.html#Private-property-names-essential
    // 处理当前组件的业务逻辑或用户交互。
    $_isMobile() {
      const rect = body.getBoundingClientRect()
      return rect.width - 1 < WIDTH
    },
    // 处理当前组件的业务逻辑或用户交互。
    $_resizeHandler() {
      if (!document.hidden) {
        const isMobile = this.$_isMobile()
        store.dispatch('app/toggleDevice', isMobile ? 'mobile' : 'desktop')

        if (isMobile) {
          store.dispatch('app/closeSideBar', { withoutAnimation: true })
        }
      }
    }
  }
}
