<template>
  <div class="upload-page app-container">
    <section class="page-heading">
      <div>
        <p class="eyebrow">Import</p>
        <h1>批量导入</h1>
      </div>
      <el-button type="text" @click="downloadexcel">下载模板文件</el-button>
    </section>

    <el-row :gutter="20">
      <el-col v-for="item in uploadItems" :key="item.ref" :xs="24" :md="8">
        <el-card class="upload-card">
          <div class="upload-icon">
            <i class="el-icon-upload" />
          </div>
          <h2>{{ item.title }}</h2>
          <p>{{ item.description }}</p>
          <el-upload
            class="upload-demo"
            :ref="item.ref"
            :action="uploadAction('/topManager/uploadExcel')"
            :limit="1"
            :on-exceed="handleExceed"
            :before-upload="beforeFileUpload"
            :auto-upload="false"
            :on-success="onSuccess"
            :on-error="onError"
            :http-request="UploadRequest"
          >
            <el-button type="primary">选择文件</el-button>
          </el-upload>
          <el-button class="submit-button" @click="submitUpload(item.ref)">确认上传</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { uploadfile1, download1 } from '@/api/user'
import { buildUploadAction } from '@/utils/upload'

export default {
  // 提供组件初始响应式数据。
  data() {
    return {
      uploadItems: [
        { ref: 'uploadFile', title: '批量添加学生', description: '导入学生基础信息 Excel 文件。' },
        { ref: 'uploadFile1', title: '批量添加管理员', description: '导入社团管理员账号 Excel 文件。' },
        { ref: 'uploadFile2', title: '批量添加社团', description: '导入社团资料 Excel 文件。' }
      ]
    }
  },
  methods: {
    // 处理文件上传相关逻辑。
    uploadAction(path) {
      return buildUploadAction(path)
    },
    // 提交表单数据并触发后端业务处理。
    submitUpload(refName) {
      const uploadRef = this.$refs[refName]
      const uploader = Array.isArray(uploadRef) ? uploadRef[0] : uploadRef
      if (uploader) {
        uploader.submit()
      }
    },
    // 处理文件上传相关逻辑。
    beforeFileUpload(file) {
      const fileType = file.name.replace(/.+\./, '')
      if (['xlsx', 'xls'].indexOf(fileType.toLowerCase()) === -1) {
        this.$message.error('仅支持上传 xlsx、xls 文件')
        this.clearUploadFiles()
        return false
      }
      return true
    },
    // 处理当前组件的业务逻辑或用户交互。
    onSuccess() {
      this.$message.success('文件上传成功')
    },
    // 处理当前组件的业务逻辑或用户交互。
    onError() {
      this.clearUploadFiles()
      this.$message.error('文件上传失败，请联系管理员')
    },
    // 处理文件上传相关逻辑。
    clearUploadFiles() {
      ['uploadFile', 'uploadFile1', 'uploadFile2'].forEach(refName => {
        const uploadRef = this.$refs[refName]
        const uploader = Array.isArray(uploadRef) ? uploadRef[0] : uploadRef
        if (uploader) {
          uploader.clearFiles()
        }
      })
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleExceed() {
      this.$message.warning('每次最多上传 1 个文件')
    },
    // 处理文件上传相关逻辑。
    UploadRequest(item) {
      const formData = new FormData()
      formData.append('file', item.file)
      uploadfile1(formData)
    },
    // 下载后端提供的附件或模板文件。
    downloadexcel() {
      download1('模板.xlsx').catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.upload-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px;
  border: 1px solid var(--bt-border-muted);
  border-radius: 8px;
  background: #fff;
  box-shadow: var(--bt-shadow);
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--bt-primary);
  font-size: 13px;
  font-weight: 700;
}

.page-heading h1 {
  margin: 0;
  color: var(--bt-text);
  font-size: 24px;
}

.upload-card {
  min-height: 320px;
  text-align: center;
}

.upload-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  margin-bottom: 12px;
  border-radius: 50%;
  background: #ddf4ff;
  color: var(--bt-primary);
  font-size: 34px;
}

.upload-card h2 {
  margin: 0;
  color: var(--bt-text);
  font-size: 20px;
}

.upload-card p {
  min-height: 40px;
  margin: 10px 0 18px;
  color: var(--bt-text-muted);
  line-height: 1.6;
}

.submit-button {
  margin-top: 14px;
}

@media (max-width: 760px) {
  .page-heading {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
