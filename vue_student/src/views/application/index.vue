<template>
  <div class="app-container apply-page">
    <section class="page-heading">
      <span class="eyebrow">Club application</span>
      <h1>申请加入社团</h1>
      <p>确认个人信息后，补充一段自我介绍。附件仅支持 zip，大小不超过 10MB。</p>
    </section>

    <el-form ref="ruleForm" :model="ruleForm" :rules="rules" label-width="112px" class="apply-form">
      <div class="form-section">
        <h2>基础信息</h2>
        <el-form-item label="申请社团">
          <el-input v-model="ruleForm.groupName" disabled />
        </el-form-item>
        <el-form-item label="姓名" prop="stuName">
          <el-input v-model="stuName" disabled />
        </el-form-item>
        <el-form-item label="学号" prop="stuNumber">
          <el-input v-model="stuNumber" disabled />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="ruleForm.phone" disabled />
        </el-form-item>
        <el-form-item label="专业" prop="major">
          <el-input v-model="ruleForm.major" disabled />
        </el-form-item>
      </div>

      <div class="form-section">
        <h2>申请说明</h2>
        <el-form-item label="自我介绍" prop="selfIntro">
          <el-input
            v-model="ruleForm.selfIntro"
            type="textarea"
            :rows="6"
            maxlength="500"
            show-word-limit
            placeholder="可以写下你对社团的兴趣、相关经历或希望参与的方向"
          />
        </el-form-item>
        <el-form-item label="申请附件">
          <el-upload
            class="upload-demo"
            :action="uploadAction('/application/uploadZip')"
            :on-remove="handleRemove"
            :before-remove="beforeRemove"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            multiple
            :limit="1"
            :on-exceed="handleExceed"
            :file-list="fileList"
            accept=".zip"
          >
            <el-button type="primary">上传 zip 附件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 1 个 zip 文件，大小不超过 10MB。</div>
            </template>
          </el-upload>
        </el-form-item>
      </div>

      <div class="form-actions">
        <el-button type="primary" @click="submitForm('ruleForm')">提交申请</el-button>
        <el-button @click="resetForm('ruleForm')">重置填写</el-button>
      </div>
    </el-form>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { postform } from '@/api/group.js'
import { getProfile } from '@/api/user.js'
import { buildUploadAction } from '@/utils/upload'

export default {
  name: 'Application',
  computed: {
    ...mapGetters([
      'name'
    ])
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      fileList: [],
      ruleForm: {
        groupName: '',
        phone: '',
        gender: '',
        major: '',
        stuName: '',
        stuNumber: '',
        selfIntro: '',
        attachment: ''
      },
      stuName: '',
      stuNumber: '',
      majors: [],
      rules: {
        phone: [
          { required: true, message: '请输入手机号', trigger: 'change' },
          { pattern: /^1[0-9]{10}$/, message: '请输入有效的手机号', trigger: 'blur' }
        ],
        selfIntro: [
          { required: true, message: '请填写自我介绍', trigger: 'blur' }
        ]
      }
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    this.stuNumber = this.name
    getProfile(this.name)
      .then((response) => {
        this.stuName = response.data.student.userName
        this.ruleForm.major = response.data.student.major
        this.ruleForm.phone = response.data.student.phone
      })
      .catch((error) => {
        console.error(error)
      })
  },
  // 组件挂载后初始化页面交互或监听器。
  mounted() {
    this.ruleForm.groupName = this.$route.params.groupName
    this.majors = this.loadAll()
  },
  methods: {
    // 处理文件上传相关逻辑。
    uploadAction(path) {
      return buildUploadAction(path)
    },
    // 提交表单数据并触发后端业务处理。
    submitForm(formName) {
      this.$refs[formName].validate((valid) => {
        if (!valid) return false

        this.ruleForm.stuName = this.stuName
        this.ruleForm.stuNumber = this.stuNumber
        postform(this.ruleForm).then(() => {
          this.$message.success('申请已提交')
          this.$router.push({ name: 'Dashboard' })
        })
          .catch(error => {
            console.error(error)
          })
      })
    },
    // 根据筛选条件查询并更新列表结果。
    querySearch(queryString, cb) {
      const results = queryString ? this.majors.filter(this.createFilter(queryString)) : this.majors
      cb(results)
    },
    // 创建业务数据或初始化页面结构。
    createFilter(queryString) {
      return (major) => major.value.toLowerCase().indexOf(queryString.toLowerCase()) === 0
    },
    // 加载页面所需数据，并同步到当前状态。
    loadAll() {
      return [
        { value: '软件工程学院' },
        { value: '人工智能学院' },
        { value: '微电子科学与技术学院' },
        { value: '测绘与遥感技术学院' },
        { value: '土木工程学院' },
        { value: '大气科学学院' },
        { value: '岭南学院' },
        { value: '中国语言文学学院' },
        { value: '历史学院' },
        { value: '哲学学院' },
        { value: '政治与公共事务管理学院' },
        { value: '管理学院' },
        { value: '外国语学院' },
        { value: '化学学院' },
        { value: '数学学院' },
        { value: '马克思主义学院' },
        { value: '药学院' },
        { value: '护理学院' },
        { value: '计算机学院' },
        { value: '物理学院' },
        { value: '电子与信息工程学院' },
        { value: '国际金融学院' },
        { value: '海洋科学学院' },
        { value: '物理与天文学院' },
        { value: '国际关系学院' },
        { value: '商学院' },
        { value: '理学院' },
        { value: '材料学院' },
        { value: '生态学院' },
        { value: '农学院' },
        { value: '智能工程学院' }
      ]
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleSelect() {},
    // 重置表单、状态或本地缓存。
    resetForm(formName) {
      this.$refs[formName].resetFields()
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleRemove() {},
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleExceed(files, fileList) {
      this.$message.warning(`最多上传 1 个文件，本次选择了 ${files.length} 个，当前共 ${files.length + fileList.length} 个。`)
    },
    // 移除指定业务数据或上传文件。
    beforeRemove(file) {
      return this.$confirm(`确定移除 ${file.name}？`)
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleUploadSuccess(response) {
      this.ruleForm.attachment = response.data?.fileUrl || response.fileUrl
      this.$message.success('附件上传成功')
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleUploadError() {
      this.$message.error('附件上传失败，请稍后重试')
    },
    // 处理文件上传相关逻辑。
    beforeUpload(file) {
      if (file.size / 1024 / 1024 > 10) {
        this.$message.error('文件大小不能超过 10MB')
        return false
      }
      return true
    }
  }
}
</script>

<style lang="scss" scoped>
.apply-form {
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

.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
