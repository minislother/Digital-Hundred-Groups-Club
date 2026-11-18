<template>
  <div class="app-container dead-letter-page">
    <div class="page-heading">
      <h1>报名异常补偿</h1>
      <p>查看 Kafka 多次投递失败的报名事件，并执行重试或人工确认。</p>
    </div>
    <el-table v-loading="loading" :data="items" border>
      <el-table-column prop="id" label="事件编号" min-width="250" />
      <el-table-column prop="attempts" label="失败次数" width="100" />
      <el-table-column prop="deadAt" label="进入死信时间" min-width="180" />
      <el-table-column label="事件内容" min-width="320">
        <template #default="scope"><code>{{ scope.row.payload }}</code></template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" @click="retry(scope.row)">重新投递</el-button>
          <el-button size="small" type="warning" @click="resolve(scope.row)">已人工处理</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { listAdmissionDeadLetters, retryAdmissionDeadLetter, resolveAdmissionDeadLetter } from '@/api/activities'

export default {
  name: 'AdmissionDeadLetters',
  data() { return { loading: false, items: [] } },
  created() { this.load() },
  methods: {
    async load() {
      this.loading = true
      try {
        const response = await listAdmissionDeadLetters()
        this.items = response.items || response.data || []
      } finally { this.loading = false }
    },
    async retry(row) {
      await retryAdmissionDeadLetter(row.id)
      this.$message.success('已重新加入投递队列')
      this.load()
    },
    async resolve(row) {
      await this.$confirm('确认已完成该报名的人工补偿？', '确认操作')
      await resolveAdmissionDeadLetter(row.id)
      this.$message.success('已标记为人工处理')
      this.load()
    }
  }
}
</script>

<style scoped>
.dead-letter-page { padding: 24px; }
.page-heading { margin-bottom: 20px; }
.page-heading h1 { margin: 0 0 8px; }
.page-heading p { color: #6b7280; margin: 0; }
code { white-space: pre-wrap; word-break: break-all; }
</style>
