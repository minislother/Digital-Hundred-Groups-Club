<template>
  <main class="bt-auth-page">
    <section class="bt-auth-shell" aria-labelledby="super-login-title">
      <aside class="bt-auth-brand">
        <div>
          <p class="bt-auth-kicker">Super Admin</p>
          <h1>数字百团总控台</h1>
          <p>用于统一维护学生、社团、活动审核、批量导入和平台运营数据，适合系统管理员集中管理。</p>
        </div>
        <div class="bt-auth-meta" aria-label="超级管理员端能力">
          <span>数据总览</span>
          <span>批量维护</span>
          <span>审核管理</span>
        </div>
      </aside>

      <div class="bt-auth-panel">
        <el-form
          ref="loginForm"
          :model="loginForm"
          :rules="loginRules"
          class="bt-auth-form"
          auto-complete="on"
          label-position="top"
        >
          <h2 id="super-login-title">超级管理员登录</h2>
          <p class="bt-auth-subtitle">进入平台级管理后台，请确认账号具备超级管理员权限。</p>

          <el-form-item label="账号" prop="userName">
            <el-input
              ref="userName"
              v-model="loginForm.userName"
              placeholder="请输入管理员账号"
              name="userName"
              type="text"
              tabindex="1"
              auto-complete="username"
            />
            <p class="bt-auth-helper">请输入后台管理员账号。</p>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              ref="password"
              v-model="loginForm.password"
              type="password"
              placeholder="请输入登录密码"
              name="password"
              tabindex="2"
              auto-complete="current-password"
              show-password
              @keyup.enter="handleLogin"
            />
            <p class="bt-auth-helper">密码不少于 6 位。</p>
          </el-form-item>

          <el-button :loading="loading" type="primary" class="bt-auth-submit" @click.prevent="handleLogin">
            进入总控台
          </el-button>
        </el-form>
      </div>
    </section>
  </main>
</template>

<script>
export default {
  name: 'Login',
  // 提供组件初始响应式数据。
  data() {
    // 校验输入内容是否满足业务要求。
    const validateuserName = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入账号'))
      } else {
        callback()
      }
    }
    // 校验输入内容是否满足业务要求。
    const validatePassword = (rule, value, callback) => {
      if (value.length < 6) {
        callback(new Error('密码长度不能少于 6 位'))
      } else {
        callback()
      }
    }
    return {
      loginForm: {
        userName: '',
        password: ''
      },
      loginRules: {
        userName: [{ required: true, trigger: 'blur', validator: validateuserName }],
        password: [{ required: true, trigger: 'blur', validator: validatePassword }]
      },
      loading: false,
      lastLoginAt: 0,
      redirect: undefined
    }
  },
  watch: {
    $route: {
      // 处理用户操作或组件事件，连接界面交互和业务请求。
      handler(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  methods: {
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleLogin() {
      if (this.loading || Date.now() - this.lastLoginAt < 1500) {
        return
      }
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.lastLoginAt = Date.now()
          this.loading = true
          this.$store.dispatch('user/login', this.loginForm).then(() => {
            this.$router.push({ path: this.redirect || '/' })
            this.loading = false
          }).catch(() => {
            this.loading = false
          })
        }
      })
    }
  }
}
</script>
