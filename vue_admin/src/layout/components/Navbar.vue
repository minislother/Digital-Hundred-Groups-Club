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
            <el-dropdown-item divided @click="logout">
              <span style="display:block;">退出登录</span>
            </el-dropdown-item>
            <el-dropdown-item divided @click="showDescription">
              <span style="display:block;">修改密码</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-dialog v-model="dialogVisible" title="修改密码" width="420px">
      <el-form ref="ruleForm" :model="ruleForm" status-icon :rules="rules" label-width="90px">
        <el-form-item label="新密码" prop="pass">
          <el-input v-model="ruleForm.pass" type="password" autocomplete="off" />
        </el-form-item>
        <el-form-item label="确认密码" prop="checkPass">
          <el-input v-model="ruleForm.checkPass" type="password" autocomplete="off" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitPassword('ruleForm')">提交</el-button>
          <el-button @click="resetForm('ruleForm')">重置</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
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
  // 提供组件初始响应式数据。
  data() {
    // 校验输入内容是否满足业务要求。
    const validatePass = (rule, value, callback) => {
      if (value.length < 6) {
        callback(new Error('密码长度不能少于 6 位'))
      } else {
        callback()
      }
    }
    // 校验输入内容是否满足业务要求。
    const validatePass2 = (rule, value, callback) => {
      if (value !== this.ruleForm.pass) {
        callback(new Error('两次输入密码不一致'))
      } else {
        callback()
      }
    }
    return {
      dialogVisible: false,
      ruleForm: {
        pass: '',
        checkPass: ''
      },
      rules: {
        pass: [
          { required: true, validator: validatePass, trigger: 'blur' }
        ],
        checkPass: [
          { required: true, validator: validatePass2, trigger: 'blur' }
        ]
      }
    }
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
    },
    // 展示弹窗、详情或提示内容。
    showDescription() {
      this.dialogVisible = true
    },
    // 提交表单数据并触发后端业务处理。
    submitPassword(formName) {
      this.$refs[formName].validate(valid => {
        if (valid) {
          this.$message.success('密码修改功能请在社团资料页完成')
          this.dialogVisible = false
        }
      })
    },
    // 重置表单、状态或本地缓存。
    resetForm(formName) {
      this.$refs[formName].resetFields()
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
