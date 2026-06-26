<template>
  <main class="bt-auth-page">
    <section class="bt-auth-shell" aria-labelledby="register-title">
      <aside class="bt-auth-brand">
        <div>
          <p class="bt-auth-kicker">Create Account</p>
          <h1>创建数字百团账号</h1>
          <p>用学号和常用邮箱完成注册。注册成功后，可申请加入社团、报名活动并查看申请进度。</p>
        </div>
        <div class="bt-auth-meta" aria-label="注册流程">
          <span>填写学号</span>
          <span>邮箱验证</span>
          <span>开始使用</span>
        </div>
      </aside>

      <div class="bt-auth-panel">
        <el-form
          ref="registerForm"
          :model="registerForm"
          :rules="registerRules"
          class="bt-auth-form"
          label-position="top"
          auto-complete="on"
        >
          <h2 id="register-title">学生注册</h2>
          <p class="bt-auth-subtitle">请使用本人学号注册，验证码会发送到你填写的邮箱。</p>

          <el-form-item label="学号" prop="userName">
            <el-input
              ref="userName"
              v-model="registerForm.userName"
              placeholder="请输入 8 位学号"
              name="userName"
              type="text"
              tabindex="1"
              auto-complete="username"
            />
            <p class="bt-auth-helper">学号需为 8 位数字。</p>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              ref="password"
              v-model="registerForm.password"
              type="password"
              placeholder="请输入至少 6 位密码"
              name="password"
              tabindex="2"
              auto-complete="new-password"
              show-password
              @keyup.enter="handleRegister"
            />
            <p class="bt-auth-helper">建议使用字母和数字组合，避免使用过于简单的密码。</p>
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input
              ref="email"
              v-model="registerForm.email"
              placeholder="请输入常用邮箱"
              name="email"
              type="email"
              tabindex="3"
              auto-complete="email"
            />
            <p class="bt-auth-helper">邮箱用于接收验证码和后续找回密码。</p>
          </el-form-item>

          <el-form-item label="验证码" prop="valicode">
            <el-input
              ref="valicode"
              v-model="registerForm.valicode"
              placeholder="请输入邮箱验证码"
              name="valicode"
              type="text"
              tabindex="4"
              auto-complete="one-time-code"
            />
          </el-form-item>

          <el-button :loading="loading" type="primary" class="bt-auth-submit" @click="handleRegister">注册账号</el-button>
          <div class="bt-auth-actions">
            <el-button :loading="emailLoading" plain @click="EmailConfirm">发送验证码</el-button>
            <el-button text @click="returnLogin">返回登录</el-button>
          </div>
        </el-form>
      </div>
    </section>
  </main>
</template>

<script>
import { validuserName } from '@/utils/validate'
import { validateEmail, register } from '@/api/user.js'

export default {
  name: 'Register',
  // 提供组件初始响应式数据。
  data() {
    // 校验输入内容是否满足业务要求。
    const validateuserName = (rule, value, callback) => {
      if (!validuserName(value)) {
        callback(new Error('学号必须为 8 位数字'))
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
      registerForm: {
        userName: '',
        password: '',
        email: '',
        valicode: ''
      },
      registerRules: {
        userName: [{ required: true, trigger: 'blur', validator: validateuserName }],
        password: [{ required: true, trigger: 'blur', validator: validatePassword }],
        email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
        valicode: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }]
      },
      loading: false,
      emailLoading: false,
      lastRegisterAt: 0,
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
    // 处理当前组件的业务逻辑或用户交互。
    returnLogin() {
      this.$router.push({ path: '/login' })
    },
    // 处理当前组件的业务逻辑或用户交互。
    EmailConfirm() {
      if (this.emailLoading) {
        return
      }
      if (!this.registerForm.email) {
        this.$message.info('请先填写邮箱')
        return
      }

      this.emailLoading = true
      validateEmail(this.registerForm.email)
        .then(() => {
          this.$message.success('验证码已发送到邮箱')
        })
        .catch(error => {
          console.error(error)
        })
        .finally(() => {
          this.emailLoading = false
        })
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleRegister() {
      if (this.loading || Date.now() - this.lastRegisterAt < 1500) {
        return
      }
      this.$refs.registerForm.validate(valid => {
        if (!valid) return false

        this.loading = true
        this.lastRegisterAt = Date.now()
        register(this.registerForm)
          .then(() => {
            this.$message.success('注册成功，请登录')
            this.$router.push('/login')
          })
          .catch(error => {
            console.error(error)
          })
          .finally(() => {
            this.loading = false
          })
      })
    }
  }
}
</script>
