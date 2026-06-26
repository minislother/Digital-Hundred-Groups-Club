<template>
  <div class="app-container profile-page">
    <section class="page-heading">
      <span class="eyebrow">Student profile</span>
      <h1>个人信息</h1>
      <p>维护你的联系方式、昵称和个人简介。修改密码后需要重新登录。</p>
    </section>

    <section class="profile-grid">
      <el-form ref="newForm" :model="newForm" status-icon :rules="rules" label-width="96px" class="profile-card">
        <h2>资料维护</h2>
        <el-form-item label="学号">
          <el-input v-model="profile.stuNumber" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="profile.stuName" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="newForm.nickname" placeholder="设置一个更容易被同学记住的昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profile.email" disabled />
        </el-form-item>
        <el-form-item label="校区">
          <el-input v-model="profile.campus" disabled />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="profile.major" disabled />
        </el-form-item>
        <el-form-item label="简介">
          <el-input
            v-model="newForm.description"
            type="textarea"
            maxlength="200"
            show-word-limit
            :rows="4"
            placeholder="写一点你希望社团成员了解的信息"
          />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="newForm.phone" type="tel" maxlength="11" autocomplete="tel" />
        </el-form-item>
        <div class="form-actions">
          <el-button type="primary" @click="submit('newForm')">保存资料</el-button>
        </div>
      </el-form>

      <el-form ref="ruleForm" :model="ruleForm" status-icon :rules="rules" label-width="96px" class="profile-card">
        <h2>修改密码</h2>
        <p class="section-copy">为了账号安全，新密码至少 6 位。保存成功后会退出当前登录。</p>
        <el-form-item label="新密码" prop="pass">
          <el-input v-model="ruleForm.pass" type="password" autocomplete="new-password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="checkPass">
          <el-input v-model="ruleForm.checkPass" type="password" autocomplete="new-password" show-password />
        </el-form-item>
        <div class="form-actions">
          <el-button type="primary" @click="submitPassword('ruleForm')">更新密码</el-button>
          <el-button @click="resetForm('ruleForm')">重置</el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { modifyPassword, getProfile, modifyPhone, modifyNickname, modifyDescription } from '@/api/user.js'

export default {
  name: 'Profile',
  computed: {
    ...mapGetters([
      'name'
    ])
  },
  // 提供组件初始响应式数据。
  data() {
    // 校验输入内容是否满足业务要求。
    const validatePass = (rule, value, callback) => {
      if (!value || value.length < 6) {
        callback(new Error('密码长度不能少于 6 位'))
      } else {
        callback()
      }
    }
    // 校验输入内容是否满足业务要求。
    const validatePass2 = (rule, value, callback) => {
      if (value !== this.ruleForm.pass) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
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
        ],
        phone: [
          { required: true, message: '请输入新的手机号', trigger: 'change' },
          { pattern: /^1[0-9]{10}$/, message: '请输入有效的手机号', trigger: 'blur' }
        ]
      },
      profile: {
        stuNumber: '',
        stuName: '',
        email: '',
        campus: '',
        major: '',
        position: ''
      },
      newForm: {
        phone: '',
        description: '',
        nickname: ''
      }
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    this.loadProfile()
  },
  methods: {
    // 加载页面所需数据，并同步到当前状态。
    loadProfile() {
      this.profile.stuNumber = this.name
      return getProfile(this.name)
        .then((response) => {
          this.profile.stuName = response.data.student.userName
          this.profile.email = response.data.student.email
          this.newForm.phone = response.data.student.phone
          this.profile.campus = response.data.student.campus
          this.profile.major = response.data.student.major
          this.profile.position = response.data.student.position
          this.newForm.nickname = response.data.student.nickname
          this.newForm.description = response.data.student.description
        })
        .catch((error) => {
          console.error(error)
        })
    },
    // 提交表单数据并触发后端业务处理。
    submitPassword(formName) {
      this.$refs[formName].validate((valid) => {
        if (!valid) return false

        modifyPassword(this.name, this.ruleForm.pass).then(() => {
          this.$message.success('密码修改成功，请重新登录')
          this.logout()
        })
          .catch(error => {
            console.error(error)
          })
      })
    },
    // 处理当前组件的业务逻辑或用户交互。
    async logout() {
      await this.$store.dispatch('user/logout')
      this.$router.push(`/login?redirect=${this.$route.fullPath}`)
    },
    // 重置表单、状态或本地缓存。
    resetForm(formName) {
      this.$refs[formName].resetFields()
    },
    // 提交表单数据并触发后端业务处理。
    submit(formName) {
      this.$refs[formName].validate((valid) => {
        if (!valid) return false

        Promise.all([
          modifyDescription(this.name, this.newForm.description),
          modifyNickname(this.name, this.newForm.nickname),
          modifyPhone(this.name, this.newForm.phone)
        ])
          .then(() => {
            this.$message.success('资料已保存')
            this.loadProfile()
          })
          .catch((error) => {
            console.error(error)
          })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 24px;
  align-items: start;
}

.profile-card {
  padding: 24px;
  border: 1px solid var(--bt-border-muted);
  border-radius: var(--bt-radius-lg);
  background: var(--bt-surface);
  box-shadow: var(--bt-shadow);
}

.profile-card h2 {
  margin: 0 0 18px;
  font-size: 20px;
}

.section-copy {
  margin: -6px 0 18px;
  color: var(--bt-text-muted);
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding-left: 96px;
}

@media (max-width: 1080px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .form-actions {
    padding-left: 0;
  }
}
</style>
