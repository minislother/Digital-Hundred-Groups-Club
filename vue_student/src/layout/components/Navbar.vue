<template>
  <div class="navbar">
    <hamburger :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />
    <breadcrumb class="breadcrumb-container" />

    <div class="right-menu">
      <el-dropdown class="avatar-container" trigger="click">
        <div class="avatar-wrapper">
          <img :src="avatar + '?imageView2/1/w/80/h/80'" class="user-avatar">
          <i class="el-icon-caret-bottom" />
        </div>
        <template #dropdown>
          <el-dropdown-menu class="user-dropdown">
            <router-link to="/">
              <el-dropdown-item>首页</el-dropdown-item>
            </router-link>
            <a target="_blank" href="https://www.sysu.edu.cn/">
              <el-dropdown-item>中大首页</el-dropdown-item>
            </a>
            <a target="_blank" href="https://sse.sysu.edu.cn/">
              <el-dropdown-item>软件工程学院首页</el-dropdown-item>
            </a>
            <el-dropdown-item divided @click="logout">
              <span style="display:block;">退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import Hamburger from '@/components/Hamburger'

export default {
  components: {
    Breadcrumb,
    Hamburger
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'avatar'
    ])
  },
  methods: {
    // 切换界面状态或显示模式。
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    // 处理当前组件的业务逻辑或用户交互。
    async logout() {
      await this.$store.dispatch('user/logout')
      this.$router.push(`/login?redirect=${this.$route.fullPath}`)
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 56px;
  display: flex;
  align-items: center;
  overflow: visible;
  position: sticky;
  top: 0;
  background: #fff;
  border-bottom: 1px solid #eaeef2;
  box-shadow: none;
  z-index: 10;

  .hamburger-container {
    line-height: 56px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background .3s;
    -webkit-tap-highlight-color: transparent;
    border-right: 1px solid #eaeef2;

    &:hover {
      background: #f6f8fa;
    }
  }

  .breadcrumb-container {
    float: left;
    flex: 1;
    min-width: 0;
    padding-left: 18px;
  }

  .right-menu {
    margin-left: auto;
    float: right;
    height: 100%;
    line-height: 56px;
    padding-right: 18px;

    &:focus {
      outline: none;
    }

    .avatar-container {
      margin-right: 0;

      .avatar-wrapper {
        height: 56px;
        display: flex;
        align-items: center;
        gap: 8px;
        margin-top: 0;
        position: relative;

        .user-avatar {
          cursor: pointer;
          width: 34px;
          height: 34px;
          border-radius: 50%;
          border: 1px solid #d0d7de;
          background: #f6f8fa;
        }

        .el-icon-caret-bottom {
          cursor: pointer;
          position: static;
          font-size: 12px;
          color: #57606a;
        }
      }
    }
  }
}
</style>
