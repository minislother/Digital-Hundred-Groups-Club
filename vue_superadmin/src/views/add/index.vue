<template>
  <div class="upload-page app-container">
    <section class="page-heading">
      <div><p class="eyebrow">Import</p><h1>批量导入学生</h1></div>
      <el-button @click="downloadexcel">下载模板文件</el-button>
    </section>
    <el-alert title="按模板填写学号、姓名、密码。重复账号跳过；每批独立提交，中途失败会保留已完成批次。" type="info" :closable="false" />
    <el-card class="upload-card">
      <el-upload
        ref="uploader"
        action="#"
        accept=".xls,.xlsx"
        :limit="1"
        :auto-upload="false"
        :disabled="uploading"
        :before-upload="beforeFileUpload"
        :http-request="UploadRequest"
        :on-exceed="handleExceed"
      >
        <el-button :disabled="uploading">选择 Excel 文件</el-button>
      </el-upload>
      <el-button class="submit-button" type="primary" :loading="uploading" @click="$refs.uploader.submit()">确认上传</el-button>
      <p>大文件建议使用 xlsx。排队及处理进度保存在服务器，可稍后返回查看。</p>
    </el-card>
    <section aria-live="polite" :aria-busy="uploading">
      <h2>最近的导入任务</h2>
      <p v-if="loadError" role="alert">{{ loadError }} <el-button @click="refresh">重试</el-button></p>
      <el-empty v-if="!tasks.length" description="暂无导入任务" />
      <el-card v-for="task in tasks" :key="task.id" class="task-card">
        <strong>{{ statusText(task.status) }}</strong>
        <p>任务：{{ task.id }}</p>
        <p>已处理 {{ task.processed }} 行 · 成功 {{ task.succeeded }} · 重复 {{ task.duplicates }} · 无效 {{ task.invalid }}</p>
        <el-progress v-if="task.total" :percentage="Math.min(100, Math.round(task.processed / task.total * 100))" />
        <p v-if="task.errorMessage" role="alert">{{ task.errorMessage }}</p>
        <el-button v-if="task.duplicates + task.invalid > 0" @click="showErrors(task.id)">查看处理明细</el-button>
      </el-card>
    </section>
    <el-dialog v-model="errorsVisible" title="处理明细（行号为跨工作表的数据序号，不含表头和空行）" width="90%">
      <el-table :data="errors"><el-table-column prop="row_no" label="数据序号" /><el-table-column prop="reason" label="原因" /></el-table>
      <el-button :disabled="!hasMoreErrors || errorsLoading" @click="moreErrors">加载更多</el-button>
    </el-dialog>
  </div>
</template>
<script>
import { uploadfile1, download1, listImportTasks, getImportErrors } from '@/api/user'
export default {
  data() {
    return { tasks: [], uploading: false, loadError: '', timer: null, disposed: false,
      errors: [], errorsVisible: false, errorsLoading: false, hasMoreErrors: false, errorTaskId: '' }
  },
  mounted() { this.refresh() },
  beforeUnmount() { this.disposed = true; clearTimeout(this.timer) },
  methods: {
    statusText(status) {
      return ({ PENDING: '等待处理', PROCESSING: '正在导入', SUCCESS: '导入完成', PARTIAL_SUCCESS: '完成，部分数据已跳过', FAILED: '导入中断，已提交批次保留' })[status] || status
    },
    beforeFileUpload(file) {
      if (!/\.xlsx?$/i.test(file.name)) { this.$message.error('请选择 xls 或 xlsx 文件'); return false }
      return true
    },
    handleExceed() { this.$message.warning('每次选择一个文件，请移除已选文件后重新选择') },
    async UploadRequest(item) {
      this.uploading = true
      try {
        const body = new FormData(); body.append('file', item.file)
        const result = await uploadfile1(body)
        this.$message.success('文件已接收，等待导入')
        this.$refs.uploader.clearFiles()
        await this.refresh()
        return result
      } finally { this.uploading = false }
    },
    async refresh() {
      clearTimeout(this.timer)
      try {
        const result = await listImportTasks()
        if (this.disposed) return
        this.tasks = result.data.tasks; this.loadError = ''
      } catch (e) { this.loadError = '暂时无法获取进度，请稍后重试' } finally {
        if (!this.disposed && (this.loadError || this.tasks.some(task => ['PENDING', 'PROCESSING'].includes(task.status)))) {
          this.timer = setTimeout(() => this.refresh(), 3000)
        }
      }
    },
    async showErrors(id) {
      this.errorTaskId = id; this.errors = []; this.hasMoreErrors = true; this.errorsVisible = true
      await this.moreErrors()
    },
    async moreErrors() {
      this.errorsLoading = true
      try {
        const id = this.errorTaskId
        const after = this.errors.length ? this.errors[this.errors.length - 1].row_no : 0
        const result = await getImportErrors(id, after)
        if (this.errorTaskId !== id) return
        this.errors.push(...result.data.errors); this.hasMoreErrors = result.data.errors.length === 100
      } finally { this.errorsLoading = false }
    },
    downloadexcel() { download1('模板.xlsx').catch(() => {}) }
  }
}
</script>
<style scoped>
.task-card { margin: 16px 0; overflow-wrap: anywhere; }
</style>
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
