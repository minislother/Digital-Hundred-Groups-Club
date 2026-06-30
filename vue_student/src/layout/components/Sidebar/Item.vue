<template>
  <span class="menu-item-content">
    <i v-if="isElementIcon" :class="[icon, 'sub-el-icon']" />
    <svg-icon v-else-if="icon" :icon-class="icon" />
    <span v-if="title">{{ title }}</span>
  </span>
</template>

<script>
export default {
  name: 'MenuItem',
  props: {
    icon: {
      type: String,
      default: ''
    },
    title: {
      type: String,
      default: ''
    }
  },
  computed: {
    // 处理当前组件的业务逻辑或用户交互。
    isElementIcon() {
      return this.icon.includes('el-icon')
    }
  }
}
</script>

<style scoped>
.sub-el-icon {
  color: currentColor;
  width: 1em;
  height: 1em;
}

.menu-item-content {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
</style>
