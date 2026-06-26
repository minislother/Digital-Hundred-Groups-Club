export default {
  computed: {
    // 处理当前组件的业务逻辑或用户交互。
    device() {
      return this.$store.state.app.device
    }
  },
  // 组件挂载后初始化页面交互或监听器。
  mounted() {
    // In order to fix the click on menu on the ios device will trigger the mouseleave bug
    // https://github.com/PanJiaChen/vue-element-admin/issues/1135
    this.fixBugIniOS()
  },
  methods: {
    // 处理当前组件的业务逻辑或用户交互。
    fixBugIniOS() {
      const $subMenu = this.$refs.subMenu
      if ($subMenu) {
        const handleMouseleave = $subMenu.handleMouseleave
        $subMenu.handleMouseleave = (e) => {
          if (this.device === 'mobile') {
            return
          }
          handleMouseleave(e)
        }
      }
    }
  }
}
