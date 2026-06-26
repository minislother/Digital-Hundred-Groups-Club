<template>
  <div class="application-page app-container">
    <section class="page-heading">
      <p class="eyebrow">Applications</p>
      <h1>入团申请审核</h1>
    </section>

    <el-table :data="applications" style="width: 100%" @row-click="handleRowClick">
      <el-table-column prop="stuName" label="姓名" min-width="160">
        <template #default="scope">
          <span class="strong-text">{{ scope.row.stuName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="stuNumber" label="学号" min-width="180" />
      <el-table-column prop="createTime" label="申请时间" min-width="220">
        <template #default="scope">
          <span>{{ formatDate(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="isAccepted" label="申请状态" min-width="140">
        <template #default="scope">
          <span :class="['status-pill', statusClass(scope.row.isAccepted)]">
            {{ statusText(scope.row.isAccepted) }}
          </span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getSeeApplications } from '@/api/seeapplication.js'

export default {
  name: 'Seeapplication',
  computed: {
    ...mapGetters([
      'name'
    ])
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      applications: []
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    getSeeApplications(this.$store.getters.currentClubName).then((response) => {
      this.applications = response.data.items
    })
      .catch(error => {
        console.error(error)
      })
    if (!this.$store.getters.isClubSelected) {
      this.$message.warning('登录身份失效，请重新登录')
      this.reload()
    }
  },
  methods: {
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleRowClick(row) {
      this.$router.push({
        name: 'Appdetail',
        params: { id: row.applicationId }
      })
    },
    // 格式化展示文本，便于页面直接显示。
    formatDate(date) {
      if (!date) {
        return '-'
      }
      const d = new Date(date)
      return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
    },
    // 处理当前组件的业务逻辑或用户交互。
    statusText(value) {
      if (value === true) return '已批准'
      if (value === false) return '已拒绝'
      return '待审核'
    },
    // 处理当前组件的业务逻辑或用户交互。
    statusClass(value) {
      if (value === true) return 'is-success'
      if (value === false) return 'is-danger'
      return 'is-warning'
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
.application-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-heading {
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

.strong-text {
  font-weight: 700;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.is-success {
  color: var(--bt-success);
  background: #dafbe1;
}

.is-danger {
  color: var(--bt-danger);
  background: #ffebe9;
}

.is-warning {
  color: var(--bt-warning);
  background: #fff8c5;
}
</style>
