<template>
  <div class="activity-page app-container">
    <section class="page-heading">
      <p class="eyebrow">Club Activities</p>
      <h1>社团活动管理</h1>
    </section>

    <el-table :data="activities" style="width: 100%" @row-click="handleRowClick">
      <el-table-column prop="name" label="活动名称" min-width="180">
        <template #default="scope">
          <span class="strong-text">{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="id" label="活动编号" min-width="140" />
      <el-table-column prop="organizer" label="组织者" min-width="160" />
      <el-table-column prop="time" label="申请时间" min-width="220" sortable>
        <template #default="scope">
          <span>{{ formatDate(scope.row.time) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="isAccepted" label="申请状态" min-width="140" sortable>
        <template #default="scope">
          <span :class="['status-pill', statusClass(scope.row.isAccepted)]">
            {{ statusText(scope.row.isAccepted) }}
          </span>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination :current-page="pageNumber" :page-size="20" :total="pageTotal"
      layout="prev, pager, next, total" @current-change="page => { pageNumber = page; fetchPage() }" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getMyActivity } from '@/api/activities.js'

export default {
  name: 'SeeActivity',
  computed: {
    ...mapGetters([
      'name'
    ])
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      pageNumber: 1,
      pageTotal: 0,
      activities: []
    }
  },
  // 组件创建后加载页面初始数据。
  created() { this.fetchPage() },
  methods: {
    fetchPage() {
getMyActivity(this.$store.getters.currentClubName, { pageNum: this.pageNumber, pageSize: 20 }).then((response) => {
        this.pageTotal = response.data.page.total
      this.activities = response.data.items
    })
      .catch(error => {
        console.error(error)
      })
    if (!this.$store.getters.isClubSelected) {
      this.$message.warning('登录身份失效，请重新登录')
      this.reload()
    }
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleRowClick(row) {
      const aid = row.id
      const activity = this.activities.filter(i => i.id === aid)[0]
      this.$router.push({
        name: 'Activitydetail',
        params: { aid: aid, activity: activity }
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
.activity-page {
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
