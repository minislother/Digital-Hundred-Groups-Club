<template>
  <main class="bt-auth-page">
    <section class="bt-auth-shell" aria-labelledby="student-login-title">
      <aside class="bt-auth-brand">
        <div>
          <p class="bt-auth-kicker">Student Pass</p>
          <h1>数字百团学生入口</h1>
          <p>浏览校园社团、提交加入申请、报名活动并查看个人申请进度，一个入口完成学生侧常用操作。</p>
        </div>
        <div class="bt-auth-meta" aria-label="学生端能力">
          <span>社团发现</span>
          <span>活动报名</span>
          <span>申请追踪</span>
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
          <h2 id="student-login-title">学生登录</h2>
          <p class="bt-auth-subtitle">使用学号和密码进入数字百团。忘记密码时，可通过绑定邮箱找回。</p>

          <el-form-item label="学号" prop="userName">
            <el-input
              ref="userName"
              v-model="loginForm.userName"
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
              v-model="loginForm.password"
              type="password"
              placeholder="请输入登录密码"
              name="password"
              tabindex="2"
              auto-complete="current-password"
              show-password
              @keyup.enter="handleLogin"
            />
            <p class="bt-auth-helper">密码不少于 6 位，请注意区分大小写。</p>
          </el-form-item>

          <el-button :loading="loading" type="primary" class="bt-auth-submit" @click.prevent="handleLogin">
            登录
          </el-button>
          <div class="bt-auth-actions">
            <el-button text @click="showRegister">注册账号</el-button>
            <el-button text @click="retrievePWD">忘记密码</el-button>
          </div>
        </el-form>
      </div>
    </section>

    <el-dialog v-model="senddialogVisible" title="找回密码" width="420px" :append-to-body="true" :close-on-click-modal="false" @close="reset">
      <p class="bt-auth-dialog-copy">{{ tips }}</p>
      <div class="bt-auth-dialog-grid">
        <el-input v-model="stunum" placeholder="请输入学号" />
        <el-button :loading="retrievepwdloading" type="primary" @click="verify">{{ sendbtn }}</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="verifydialogVisible" title="验证邮箱" width="420px" :append-to-body="true" :close-on-click-modal="false" @close="reset">
      <p class="bt-auth-dialog-copy">{{ tips }}</p>
      <div class="bt-auth-dialog-grid">
        <el-input v-model="vcode" placeholder="请输入邮箱验证码" />
        <el-button type="primary" @click="vercode">验证</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="resetdialogVisible" title="重置密码" width="420px" :append-to-body="true" :close-on-click-modal="false" @close="reset">
      <p class="bt-auth-dialog-copy">{{ tips }}</p>
      <div class="bt-auth-dialog-grid">
        <el-input v-model="pas1" type="password" placeholder="请输入新密码" show-password />
        <el-input v-model="pas2" type="password" placeholder="请再次输入新密码" show-password />
        <el-button type="primary" @click="modifyPassword">修改密码</el-button>
      </div>
    </el-dialog>
  </main>
</template>

<script>
import { validuserName } from '@/utils/validate'
import { verifycode, verifyemail, modifypas } from '@/api/user.js'

export default {
  name: 'Login',
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
      redirect: undefined,
      tips: '请输入学号，系统会向绑定邮箱发送验证码。',
      senddialogVisible: false,
      resetdialogVisible: false,
      verifydialogVisible: false,
      sendbtn: '发送验证码',
      retrievepwdloading: false,
      stunum: '',
      vcode: '',
      pas1: '',
      pas2: '',
      mail: ''
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
    // 展示弹窗、详情或提示内容。
    showRegister() {
      this.$router.push({ path: '/register' })
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleLogin() {
      if (this.loading || Date.now() - this.lastLoginAt < 1500) {
        return
      }
      this.$refs.loginForm.validate(valid => {
        if (!valid) return
        this.lastLoginAt = Date.now()
        this.loading = true
        this.$store.dispatch('user/login', this.loginForm).then(() => {
          this.$router.push({ path: this.redirect || '/' })
          this.loading = false
        }).catch(() => {
          this.loading = false
        })
      })
    },
    // 处理当前组件的业务逻辑或用户交互。
    retrievePWD() {
      this.senddialogVisible = true
    },
    // 处理当前组件的业务逻辑或用户交互。
    verify() {
      if (!this.stunum) {
        this.$message.info('请输入学号')
        return
      }
      this.retrievepwdloading = true
      verifyemail(this.stunum)
        .then((response) => {
          this.mail = response.data.email
          this.senddialogVisible = false
          this.tips = '验证码已发送到绑定邮箱，请输入验证码。'
          this.verifydialogVisible = true
          this.startCountdown()
        })
        .catch(error => {
          console.error(error)
        })
        .finally(() => {
          this.retrievepwdloading = false
        })
    },
    // 启动定时器、流程或页面任务。
    startCountdown() {
      let time = 60
      this.sendbtn = `${time} 秒后重新获取`
      const timer = setInterval(() => {
        time -= 1
        if (time <= 0) {
          this.sendbtn = '重新获取验证码'
          clearInterval(timer)
          return
        }
        this.sendbtn = `${time} 秒后重新获取`
      }, 1000)
    },
    // 处理当前组件的业务逻辑或用户交互。
    vercode() {
      verifycode(this.mail, this.vcode).then(() => {
        this.verifydialogVisible = false
        this.tips = '请设置新的登录密码。'
        this.resetdialogVisible = true
      }).catch(err => {
        console.error(err)
      })
    },
    // 处理当前组件的业务逻辑或用户交互。
    modifyPassword() {
      if (this.pas1 === '' || this.pas2 === '') {
        this.$message.info('请输入新密码')
      } else if (this.pas1 !== this.pas2) {
        this.$message.info('两次密码不一致')
      } else {
        modifypas(this.stunum, this.pas1).then(() => {
          this.$message.success('新密码修改成功')
          this.resetdialogVisible = false
        }).catch(err => {
          console.error(err)
        })
      }
    },
    // 重置表单、状态或本地缓存。
    reset() {
      this.tips = '请输入学号，系统会向绑定邮箱发送验证码。'
    }
  }
}
</script>
