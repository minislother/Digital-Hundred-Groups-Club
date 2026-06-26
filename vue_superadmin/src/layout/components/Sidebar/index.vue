<template>
  <div :class="{'has-logo':showLogo}">
    <logo v-if="showLogo" :collapse="isCollapse" />
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :background-color="variables.menuBg"
        :text-color="variables.menuText"
        :unique-opened="false"
        :active-text-color="variables.menuActiveText"
        :collapse-transition="false"
        mode="vertical"
      >
        <sidebar-item v-for="route in routes" :key="route.path" :item="route" :base-path="route.path" />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Logo from './Logo'
import SidebarItem from './SidebarItem'
import * as variables from '@/styles/variables.scss'

export default {
  components: { SidebarItem, Logo },
  computed: {
    ...mapGetters([
      'sidebar'
    ]),
    // 处理当前组件的业务逻辑或用户交互。
    routes() {
      return this.$router.options.routes
    },
    // 处理当前组件的业务逻辑或用户交互。
    activeMenu() {
      const route = this.$route
      const { meta, path } = route
      // if set path, the sidebar will highlight the path you set
      if (meta.activeMenu) {
        return meta.activeMenu
      }
      return path
    },
    // 展示弹窗、详情或提示内容。
    showLogo() {
      return this.$store.state.settings.sidebarLogo
    },
    // 处理当前组件的业务逻辑或用户交互。
    variables() {
      return variables
    },
    // 处理当前组件的业务逻辑或用户交互。
    isCollapse() {
      return !this.sidebar.opened
    }
  }
}
</script>
