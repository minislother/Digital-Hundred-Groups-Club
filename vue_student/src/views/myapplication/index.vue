<template>
  <div class="app-container application-page">
    <section class="page-heading">
      <span class="eyebrow">Application tracking</span>
      <h1>我的申请</h1>
      <p>查看你提交过的社团加入申请，及时了解审核进度。</p>
    </section>

    <section class="application-grid">
      <el-card class="application-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>申请记录</span>
            <el-tag type="info">{{ myApps.length }} 条</el-tag>
          </div>
        </template>

        <el-table :data="myApps" :row-class-name="tableRowClassName" empty-text="暂无申请记录">
          <el-table-column prop="createTime" label="申请时间" min-width="180">
            <template #default="scope">
              <span class="text">{{ formatDate(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="groupName" label="申请社团" min-width="180">
            <template #default="scope">
              <span class="text">{{ scope.row.groupName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="isAccepted" label="审核状态" min-width="140">
            <template #default="scope">
              <span :class="statusClass(scope.row.isAccepted)">
                {{ statusText(scope.row.isAccepted) }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <aside class="countdown-card">
        <span class="eyebrow">Today matters</span>
        <h2>距离明天还有</h2>
        <div class="time-grid" aria-label="距离明天的倒计时">
          <strong>{{ hours }}</strong>
          <span>小时</span>
          <strong>{{ minutes }}</strong>
          <span>分钟</span>
          <strong>{{ seconds }}</strong>
          <span>秒</span>
        </div>
        <p>今天就去发现一个喜欢的社团，给校园生活多加一点可能性。</p>
        <el-button type="primary" @click="$router.push({ name: 'Dashboard' })">浏览社团</el-button>
      </aside>
    </section>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getApplications } from '@/api/group.js'

export default {
  name: 'Myapplication',
  computed: {
    ...mapGetters([
      'name'
    ])
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      myApps: [],
      hours: 0,
      minutes: 0,
      seconds: 0,
      timer: null
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    getApplications(this.name)
      .then((response) => {
        this.myApps = response.data.items || []
        this.myApps.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
      })
      .catch(error => {
        console.error(error)
      })
  },
  // 组件挂载后初始化页面交互或监听器。
  mounted() {
    this.startCountdown()
  },
  // 组件卸载前释放监听器或定时器。
  beforeUnmount() {
    clearInterval(this.timer)
  },
  methods: {
    // 处理当前组件的业务逻辑或用户交互。
    tableRowClassName({ row }) {
      if (row.isAccepted === true) return 'success-row'
      if (row.isAccepted === false) return 'fail-row'
      return ''
    },
    // 处理当前组件的业务逻辑或用户交互。
    statusClass(value) {
      if (value === true) return 'bt-status is-success'
      if (value === false) return 'bt-status is-danger'
      return 'bt-status is-warning'
    },
    // 处理当前组件的业务逻辑或用户交互。
    statusText(value) {
      if (value === true) return '已通过'
      if (value === false) return '未通过'
      return '待审核'
    },
    // 启动定时器、流程或页面任务。
    startCountdown() {
      // 更新表单字段或业务数据。
      const update = () => {
        const now = new Date()
        const tomorrow = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1)
        const distance = tomorrow - now

        this.hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
        this.minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60))
        this.seconds = Math.floor((distance % (1000 * 60)) / 1000)
      }

      update()
      this.timer = setInterval(update, 1000)
    },
    // 格式化展示文本，便于页面直接显示。
    formatDate(date) {
      if (!date) return '-'
      const d = new Date(date)
      if (Number.isNaN(d.getTime())) return '-'
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
    }
  }
}
</script>

<style lang="scss" scoped>
.application-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
  align-items: start;
}

.application-card,
.countdown-card {
  border: 1px solid var(--bt-border-muted);
  border-radius: var(--bt-radius-lg);
  background: var(--bt-surface);
  box-shadow: var(--bt-shadow);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 700;
}

.countdown-card {
  padding: 24px;
}

.countdown-card h2 {
  margin: 8px 0 16px;
  font-size: 22px;
}

.countdown-card p {
  color: var(--bt-text-muted);
  line-height: 1.7;
}

.time-grid {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 8px 12px;
  margin-bottom: 18px;
}

.time-grid strong {
  color: var(--bt-primary);
  font-family: var(--bt-font-data);
  font-size: 30px;
  line-height: 1;
}

.time-grid span {
  align-self: end;
  color: var(--bt-text-muted);
}

@media (max-width: 980px) {
  .application-grid {
    grid-template-columns: 1fr;
  }
}
</style>
