<template>
  <div class="activity-page app-container">
    <section class="stats-grid">
      <div class="stat-card">
        <span>全部活动</span>
        <strong>{{ activities.length }}</strong>
      </div>
      <div class="stat-card success">
        <span>本页已通过</span>
        <strong>{{ accepted }}</strong>
      </div>
      <div class="stat-card danger">
        <span>本页已拒绝</span>
        <strong>{{ rejected }}</strong>
      </div>
      <div class="stat-card warning">
        <span>本页待审核</span>
        <strong>{{ toBeAccepted }}</strong>
      </div>
    </section>

    <section class="page-toolbar">
      <div>
        <p class="eyebrow">Activity Review</p>
        <h1>活动审批</h1>
      </div>
      <div class="search-row">
        <el-input v-model="searchInfo" placeholder="搜索活动" clearable @keyup.enter="handleSearch" />
        <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
        <el-button @click="clearSearch">重置</el-button>
      </div>
    </section>

    <el-table :data="activities" style="width: 100%" @row-click="handleRowClick">
      <el-table-column prop="name" label="活动名称" min-width="180">
        <template #default="scope">
          <span class="strong-text">{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="id" label="活动编号" min-width="120" />
      <el-table-column prop="organizer" label="组织者" min-width="150" />
      <el-table-column prop="time" label="申请时间" min-width="220" sortable>
        <template #default="scope">
          <span>{{ formatDate(scope.row.time) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="isAccepted" label="状态与操作" min-width="260" sortable>
        <template #default="scope">
          <div class="action-cell">
            <span :class="['status-pill', statusClass(scope.row.isAccepted)]">
              {{ statusText(scope.row.isAccepted) }}
            </span>
            <div class="action-buttons">
              <el-button
                size="small"
                type="primary"
                :disabled="scope.row.isAccepted === true"
                @click.stop="acceptActivity(scope.row.id)"
              >
                通过
              </el-button>
              <el-button
                size="small"
                :disabled="scope.row.isAccepted === false"
                @click.stop="rejectActivity(scope.row.id)"
              >
                拒绝
              </el-button>
            </div>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination :current-page="pageNumber" :page-size="20" :total="pageTotal"
      layout="prev, pager, next, total" @current-change="page => { pageNumber = page; fetchActivities() }" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { searchActivities, getAllApps, acceptActivity, rejectActivity } from '@/api/activities.js'

export default {
  name: 'SeeAllActivity',
  // 提供组件初始响应式数据。
  data() {
    return {
      pageNumber: 1,
      pageTotal: 0,
      activities: [],
      searchInfo: ''
    }
  },
  computed: {
    ...mapGetters([
      'name'
    ]),
    // 处理当前组件的业务逻辑或用户交互。
    toBeAccepted() {
      return this.activities.filter(a => a.isAccepted === null).length
    },
    // 处理当前组件的业务逻辑或用户交互。
    accepted() {
      return this.activities.filter(a => a.isAccepted === true).length
    },
    // 处理当前组件的业务逻辑或用户交互。
    rejected() {
      return this.activities.filter(a => a.isAccepted === false).length
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    this.fetchActivities()
  },
  methods: {
    // 从后端接口获取业务数据。
    fetchActivities() {
      return searchActivities(this.searchInfo, { pageNum: this.pageNumber, pageSize: 20 }).then((response) => {
        this.pageTotal = response.data.page.total
        this.activities = response.data.items
      })
        .catch(error => {
          console.error(error)
        })
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
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleSearch() {
      this.pageNumber = 1
      searchActivities(this.searchInfo, { pageNum: this.pageNumber, pageSize: 20 })
        .then((response) => {
        this.pageTotal = response.data.page.total
          this.activities = response.data.items
        })
        .catch((error) => {
          console.error(error)
        })
    },
    // 根据筛选条件查询并更新列表结果。
    clearSearch() {
      this.searchInfo = ''
      this.fetchActivities()
    },
    // 处理当前组件的业务逻辑或用户交互。
    statusText(value) {
      if (value === true) return '已通过'
      if (value === false) return '已拒绝'
      return '待审核'
    },
    // 处理当前组件的业务逻辑或用户交互。
    statusClass(value) {
      if (value === true) return 'is-success'
      if (value === false) return 'is-danger'
      return 'is-warning'
    },
    // 处理当前组件的业务逻辑或用户交互。
    acceptActivity(id) {
      acceptActivity(id)
        .then(() => {
          this.$message.success('活动已通过')
          this.fetchActivities()
        })
        .catch((error) => {
          console.error(error)
        })
    },
    // 处理当前组件的业务逻辑或用户交互。
    rejectActivity(id) {
      rejectActivity(id)
        .then(() => {
          this.$message.success('活动已拒绝')
          this.fetchActivities()
        })
        .catch((error) => {
          console.error(error)
        })
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

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 18px;
  border: 1px solid var(--bt-border-muted);
  border-radius: 8px;
  background: #fff;
  box-shadow: var(--bt-shadow);
}

.stat-card span {
  color: var(--bt-text-muted);
  font-size: 13px;
  font-weight: 600;
}

.stat-card strong {
  display: block;
  margin-top: 8px;
  color: var(--bt-text);
  font-size: 28px;
}

.stat-card.success strong {
  color: var(--bt-success);
}

.stat-card.danger strong {
  color: var(--bt-danger);
}

.stat-card.warning strong {
  color: var(--bt-warning);
}

.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
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

.page-toolbar h1 {
  margin: 0;
  color: var(--bt-text);
  font-size: 24px;
}

.search-row {
  display: flex;
  gap: 10px;
  width: min(560px, 100%);
}

.search-row .el-input {
  flex: 1;
}

.strong-text {
  font-weight: 700;
}

.action-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.action-buttons {
  display: flex;
  gap: 8px;
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

@media (max-width: 960px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .page-toolbar,
  .action-cell {
    align-items: stretch;
    flex-direction: column;
  }

  .search-row {
    width: 100%;
  }
}
</style>
