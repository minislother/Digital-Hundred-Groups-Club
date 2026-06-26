<template>
  <section class="app-main">
    <transition name="fade-transform" mode="out-in">
      <router-view :key="key" />
    </transition>
  </section>
</template>

<script>
export default {
  name: 'AppMain',
  computed: {
    // 生成路由视图缓存 key，确保页面正确刷新。
    key() {
      return this.$route.path
    }
  }
}
</script>

<style scoped>
.app-main {
  min-height: calc(100vh - 56px);
  width: 100%;
  position: relative;
  overflow-x: hidden;
  overflow-y: auto;
  background: #f6f8fa;
}
.fixed-header+.app-main {
  padding-top: 56px;
}
</style>

<style lang="scss">
// fix css style bug in open el-dialog
.el-popup-parent--hidden {
  .fixed-header {
    padding-right: 15px;
  }
}
</style>
