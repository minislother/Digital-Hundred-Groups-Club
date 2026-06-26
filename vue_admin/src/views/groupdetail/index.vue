<template>
  <div class="app-container group-detail-page">
    <section class="page-heading">
      <span class="eyebrow">Club profile</span>
      <h1>社团资料</h1>
      <p>维护社团介绍、附件和封面图。社团资料会影响学生端展示，请保持信息准确清晰。</p>
    </section>

    <section class="detail-layout">
      <el-form :model="staticForm" label-width="96px" class="info-card">
        <h2>基础信息</h2>
        <el-form-item label="社团名称" prop="name">
          <el-input v-model="staticForm.name" disabled />
        </el-form-item>
        <el-form-item label="社长" prop="leader">
          <el-input v-model="staticForm.leader" disabled />
        </el-form-item>
      </el-form>

      <el-form ref="newForm" :model="newForm" status-icon :rules="rules" label-width="96px" class="edit-card">
        <h2>展示资料</h2>
        <el-form-item label="详细介绍" prop="description">
          <el-input
            v-model="newForm.description"
            type="textarea"
            :rows="6"
            maxlength="800"
            show-word-limit
            placeholder="介绍社团定位、日常活动、招新要求等"
          />
        </el-form-item>
        <el-form-item label="介绍附件">
          <el-upload
            :before-upload="beforeUpload"
            class="upload-demo"
            :action="uploadAction('/group/uploadZip')"
            :on-remove="handleRemove"
            :before-remove="beforeRemove"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            multiple
            :limit="1"
            :on-exceed="handleExceed"
            :file-list="zipList"
            accept=".zip"
          >
            <el-button type="primary">上传 zip 附件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 1 个 zip 文件，大小不超过 10MB。</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="社团封面">
          <el-upload
            class="upload-demo"
            drag
            :on-success="handlePhotoUploadSuccess"
            :on-preview="handlePictureCardPreview"
            :on-remove="handleRemovePhoto"
            accept=".jpg,.png"
            :limit="1"
            :action="uploadAction('/group/uploadPhoto')"
            multiple
            :on-exceed="handlePhotoExceed"
          >
            <div class="upload-placeholder">
              <strong>拖拽图片到这里，或点击上传</strong>
              <span>支持 jpg/png，建议使用横向封面图。</span>
            </div>
            <template #tip>
              <div class="el-upload__tip">上传后会用于学生端社团卡片展示。</div>
            </template>
          </el-upload>
        </el-form-item>
        <div class="form-actions">
          <el-button type="primary" @click="updateDescription('newForm')">保存社团资料</el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getDetails, updateForm } from '@/api/manage.js'
import { buildUploadAction } from '@/utils/upload'

export default {
  name: 'Groupdetail',
  computed: {
    ...mapGetters([
      'name'
    ])
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      zipList: [],
      fileList: [],
      dialogImageUrl: '',
      dialogVisible: false,
      staticForm: {
        name: '',
        leader: ''
      },
      newForm: {
        description: '',
        attachment: '',
        image: ''
      },
      rules: {
        description: [
          { required: true, message: '请填写社团详细介绍', trigger: 'change' }
        ]
      }
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    this.staticForm.name = this.name

    getDetails(this.$store.getters.currentClubName)
      .then((response) => {
        this.staticForm.name = response.data.group.name
        this.staticForm.leader = response.data.group.leader
        this.newForm.description = response.data.group.description
        this.newForm.image = response.data.group.image
        this.newForm.attachment = response.data.group.attachment
      })
      .catch((error) => {
        console.error(error)
      })

    if (!this.$store.getters.isClubSelected) {
      this.$confirm('登录身份已失效，请重新登录', '需要重新登录', {
        confirmButtonText: '重新登录',
        showCancelButton: false,
        type: 'warning'
      }).finally(() => this.reload())
    }
  },
  methods: {
    // 处理文件上传相关逻辑。
    uploadAction(path) {
      return buildUploadAction(path)
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handlePhotoExceed(files, fileList) {
      this.$message.warning(`最多上传 1 张图片，本次选择了 ${files.length} 张，当前共 ${files.length + fileList.length} 张。`)
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handlePhotoUploadSuccess(response) {
      this.newForm.image = response.data?.fileUrl || response.fileUrl
      this.$message.success('封面上传成功')
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleRemovePhoto(file) {
      this.fileList = this.fileList.filter((item) => item.name !== file.name)
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handlePictureCardPreview(file) {
      this.dialogImageUrl = file.url
      this.dialogVisible = true
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleRemove() {},
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleExceed(files, zipList) {
      this.$message.warning(`最多上传 1 个文件，本次选择了 ${files.length} 个，当前共 ${files.length + zipList.length} 个。`)
    },
    // 移除指定业务数据或上传文件。
    beforeRemove(file) {
      return this.$confirm(`确定移除 ${file.name}？`)
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleUploadSuccess(response) {
      this.newForm.attachment = response.data?.fileUrl || response.fileUrl
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
    },
    // 更新表单字段或业务数据。
    updateDescription(formName) {
      this.$refs[formName].validate((valid) => {
        if (!valid) return false

        updateForm(this.staticForm.name, this.newForm.description, this.newForm.attachment, this.newForm.image)
          .then(() => {
            this.$message.success('社团资料已保存')
            this.$router.push({ name: 'Groupdetail' })
          })
          .catch(error => {
            console.error(error)
          })
      })
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
.detail-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.info-card,
.edit-card {
  padding: 24px;
  border: 1px solid var(--bt-border-muted);
  border-radius: var(--bt-radius-lg);
  background: var(--bt-surface);
  box-shadow: var(--bt-shadow);
}

.info-card h2,
.edit-card h2 {
  margin: 0 0 18px;
  font-size: 20px;
}

.upload-placeholder {
  display: grid;
  gap: 6px;
  color: var(--bt-text);
}

.upload-placeholder span {
  color: var(--bt-text-muted);
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding-left: 96px;
}

@media (max-width: 1080px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .form-actions {
    padding-left: 0;
  }
}
</style>
