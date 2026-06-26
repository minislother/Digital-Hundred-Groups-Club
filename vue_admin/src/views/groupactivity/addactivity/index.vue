<template>
  <div class="app-container activity-form-page">
    <section class="page-heading">
      <span class="eyebrow">Activity proposal</span>
      <h1>发布活动申请</h1>
      <p>填写活动基本信息后提交给超级管理员审核。信息越完整，审核沟通成本越低。</p>
    </section>

    <el-form ref="ruleForm" :model="ruleForm" :rules="rules" label-width="112px" class="activity-form">
      <div class="form-section">
        <h2>活动信息</h2>
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="ruleForm.name" placeholder="例如：春季招新分享会" />
        </el-form-item>
        <el-form-item label="组织者" prop="organizer">
          <el-input v-model="ruleForm.organizer" placeholder="填写负责人或组织部门" />
        </el-form-item>
        <el-form-item label="活动地点" prop="place">
          <el-input v-model="ruleForm.place" placeholder="例如：教学楼 A101" />
        </el-form-item>
        <el-form-item label="活动时间" prop="time">
          <el-date-picker
            v-model="ruleForm.time"
            type="datetime"
            placeholder="选择活动日期和时间"
            class="full-control"
          />
        </el-form-item>
        <el-form-item label="活动安排" prop="arrange">
          <el-input v-model="ruleForm.arrange" placeholder="简要说明流程安排" />
        </el-form-item>
        <el-form-item label="活动描述" prop="desc">
          <el-input
            v-model="ruleForm.desc"
            type="textarea"
            :rows="6"
            maxlength="800"
            show-word-limit
            placeholder="说明活动目的、面向对象、报名要求和注意事项"
          />
        </el-form-item>
      </div>

      <div class="form-actions">
        <el-button type="primary" @click="submitActivity('ruleForm')">提交活动申请</el-button>
        <el-button @click="resetForm('ruleForm')">重置</el-button>
      </div>
    </el-form>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { addActivity } from '@/api/activities.js'

export default {
  name: 'AddActivity',
  computed: {
    ...mapGetters([
      'name'
    ])
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      ruleForm: {
        name: '',
        organizer: '',
        type: 0,
        place: '',
        time: '',
        arrange: '',
        desc: '',
        groupName: this.$store.getters.currentClubName
      },
      rules: {
        name: [
          { required: true, message: '请输入活动名称', trigger: 'blur' },
          { min: 1, max: 15, message: '活动名称长度为 1 到 15 个字符', trigger: 'blur' }
        ],
        organizer: [
          { required: true, message: '请输入组织者', trigger: 'blur' },
          { min: 1, max: 15, message: '组织者长度为 1 到 15 个字符', trigger: 'blur' }
        ],
        place: [
          { required: true, message: '请输入活动地点', trigger: 'blur' },
          { min: 1, max: 31, message: '活动地点长度为 1 到 31 个字符', trigger: 'blur' }
        ],
        time: [
          { type: 'date', required: true, message: '请选择活动时间', trigger: 'change' }
        ],
        arrange: [
          { min: 1, max: 31, message: '活动安排长度为 1 到 31 个字符', trigger: 'blur' }
        ]
      }
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    if (!this.$store.getters.isClubSelected) {
      this.$confirm('登录身份已失效，请重新登录', '需要重新登录', {
        confirmButtonText: '重新登录',
        showCancelButton: false,
        type: 'warning'
      }).finally(() => this.reload())
    }
  },
  methods: {
    // 提交表单数据并触发后端业务处理。
    submitActivity(formName) {
      this.$refs[formName].validate((valid) => {
        if (!valid) {
          this.$message.error('请检查活动信息后再提交')
          return false
        }

        addActivity(this.ruleForm).then(() => {
          this.$message.success('活动申请已提交')
          this.resetForm('ruleForm')
        }).catch(error => {
          console.error(error)
        })
      })
    },
    // 重置表单、状态或本地缓存。
    resetForm(formName) {
      this.$refs[formName].resetFields()
    },
    // 重新加载当前页面数据，保持界面内容最新。
    async reload() {
      await this.$store.dispatch('user/logout')
      this.$router.push(`/login?redirect=${this.$route.fullPath}`)
    }
  }
}
</script>

<style lang="scss" scoped>
.activity-form {
  max-width: 960px;
}

.form-section {
  padding: 24px;
  margin-bottom: 18px;
  border: 1px solid var(--bt-border-muted);
  border-radius: var(--bt-radius-lg);
  background: var(--bt-surface);
  box-shadow: var(--bt-shadow);
}

.form-section h2 {
  margin: 0 0 18px;
  font-size: 20px;
}

.full-control {
  width: 100%;
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
