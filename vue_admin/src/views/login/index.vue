<template>
  <main class="bt-auth-page">
    <section class="bt-auth-shell" aria-labelledby="manager-login-title">
      <aside class="bt-auth-brand">
        <div>
          <p class="bt-auth-kicker">Club Manager</p>
          <h1>社团管理员工作台</h1>
          <p>面向社团负责人，集中处理成员维护、社团资料、活动发布和入团申请审核。</p>
        </div>
        <div class="bt-auth-meta" aria-label="管理员端能力">
          <span>申请审核</span>
          <span>成员维护</span>
          <span>活动管理</span>
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
          <h2 id="manager-login-title">管理员登录</h2>
          <p class="bt-auth-subtitle">登录后请选择要管理的社团，系统会进入对应社团的管理视图。</p>

          <el-form-item label="管理员账号" prop="userName">
            <el-input
              ref="userName"
              v-model="loginForm.userName"
              placeholder="请输入管理员账号"
              name="userName"
              type="text"
              tabindex="1"
              auto-complete="username"
            />
            <p class="bt-auth-helper">请输入社团管理员分配的账号。</p>
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
            登录并选择社团
          </el-button>
        </el-form>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" title="选择要管理的社团" width="460px" :append-to-body="true" @close="reset">
      <p class="bt-auth-dialog-copy">该账号关联多个社团，请选择本次要进入的管理范围。</p>
      <div class="bt-auth-choice-grid">
        <el-button
          v-for="(item, index) in choosebtn"
          :key="'choosebtn' + index"
          type="primary"
          plain
          @click="chooseclub(index)"
        >
          {{ item }}
        </el-button>
      </div>
    </el-dialog>
  </main>
</template>

<script>
import { managerlogin, managedgroup } from '@/api/user.js'

export default {
  name: 'Login',
  // 提供组件初始响应式数据。
  data() {
    // 校验输入内容是否满足业务要求。
    const validateuserName = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入管理员账号'))
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
      dialogVisible: false,
      choosebtn: [],
      token: 0,
      cid: [],
      cname: []
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
        if (!valid) return
        this.lastLoginAt = Date.now()
        this.loading = true
        managerlogin(this.loginForm.userName.trim(), this.loginForm.password)
          .then(response => {
            const { data } = response
            this.token = data.token
            this.$store.dispatch('user/managerlogin', this.token)
            return managedgroup(this.loginForm.userName.trim())
          })
          .then(res => {
            this.choosebtn = []
            this.cid = []
            this.cname = []
            res.data.items.forEach((item, index) => {
              this.addButton(index, item.id, item.name)
            })
            this.dialogVisible = true
          })
          .catch(() => {
            this.loading = false
          })
          .finally(() => {
            this.loading = false
          })
      })
    },
    // 处理当前组件的业务逻辑或用户交互。
    chooseclub(index) {
      const clubid = this.cid[index]
      const clubname = this.cname[index]
      this.$store.commit('chooseclub', clubid)
      this.$store.commit('clubname', clubname)
      this.$router.push({ path: this.redirect || '/' })
    },
    // 新增业务数据或追加界面状态。
    addButton(i, cid, cname) {
      this.cid[i] = cid
      this.cname[i] = cname
      this.choosebtn[i] = `社团 ID：${cid}  ${cname}`
    },
    // 重置表单、状态或本地缓存。
    reset() {
      this.choosebtn = []
    }
  }
}
</script>
