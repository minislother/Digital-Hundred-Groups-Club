<template>
  <div :class="classObj" class="app-wrapper">
    <button
      v-if="device==='mobile'&&sidebar.opened"
      type="button"
      class="drawer-bg"
      aria-label="关闭侧边栏"
      @click="handleClickOutside"
    />
    <sidebar class="sidebar-container" />
    <div class="main-container">
      <div :class="{'fixed-header':fixedHeader}">
        <navbar />
      </div>
      <app-main />
    </div>
  </div>
</template>

<script>
import { Navbar, Sidebar, AppMain } from './components'
import ResizeMixin from './mixin/ResizeHandler'

export default {
  name: 'Layout',
  components: {
    Navbar,
    Sidebar,
    AppMain
  },
  mixins: [ResizeMixin],
  computed: {
    // 处理当前组件的业务逻辑或用户交互。
    sidebar() {
      return this.$store.state.app.sidebar
    },
    // 处理当前组件的业务逻辑或用户交互。
    device() {
      return this.$store.state.app.device
    },
    // 处理当前组件的业务逻辑或用户交互。
    fixedHeader() {
      return this.$store.state.settings.fixedHeader
    },
    // 处理当前组件的业务逻辑或用户交互。
    classObj() {
      return {
        hideSidebar: !this.sidebar.opened,
        openSidebar: this.sidebar.opened,
        withoutAnimation: this.sidebar.withoutAnimation,
        mobile: this.device === 'mobile'
      }
    }
  },
  methods: {
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleClickOutside() {
      this.$store.dispatch('app/closeSideBar', { withoutAnimation: false })
    }
  }
}
</script>

<style lang="scss" scoped>
  @import "~@/styles/mixin.scss";
  @import "~@/styles/variables.scss";

  .app-wrapper {
    @include clearfix;
    position: relative;
    height: 100%;
    width: 100%;
    &.mobile.openSidebar{
      position: fixed;
      top: 0;
    }
  }
  .drawer-bg {
    border: 0;
    background: #000;
    cursor: pointer;
    opacity: 0.3;
    padding: 0;
    width: 100%;
    top: 0;
    height: 100%;
    position: absolute;
    z-index: 999;
  }

  .fixed-header {
    position: fixed;
    top: 0;
    right: 0;
    z-index: 9;
    width: calc(100% - #{$sideBarWidth});
    transition: width 0.28s;
  }

  .hideSidebar .fixed-header {
    width: calc(100% - 54px)
  }

  .mobile .fixed-header {
    width: 100%;
  }
</style>
