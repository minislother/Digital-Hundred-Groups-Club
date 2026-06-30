<template>
  <div class="dashboard-page app-container">
    <section class="page-heading">
      <div>
        <p class="eyebrow">Overview</p>
        <h1>超级管理员工作台</h1>
        <p>查看社团热度与成员规模，快速判断平台运营情况。</p>
      </div>
      <span>社团运营数据概览</span>
    </section>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>热门社团 Top 5</span>
              <small>按热度排序</small>
            </div>
          </template>
          <div v-if="hotGroups.length" class="rank-list" aria-label="热门社团排行">
            <article v-for="(group, index) in hotGroups" :key="group.name || index" class="rank-item">
              <div class="rank-line">
                <span class="rank-index">{{ index + 1 }}</span>
                <strong>{{ group.name }}</strong>
                <em>{{ group.hot }}</em>
              </div>
              <div class="rank-bar" aria-hidden="true">
                <span :style="{ width: getPercent(group.hot, maxHot) + '%' }" />
              </div>
            </article>
          </div>
          <el-empty v-else description="暂无热度数据" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>成员人数 Top 5</span>
              <small>按成员数排序</small>
            </div>
          </template>
          <div v-if="memberGroups.length" class="rank-list" aria-label="成员人数排行">
            <article v-for="(group, index) in memberGroups" :key="group.groupName || index" class="rank-item">
              <div class="rank-line">
                <span class="rank-index">{{ index + 1 }}</span>
                <strong>{{ group.groupName }}</strong>
                <em>{{ group.count }} 人</em>
              </div>
              <div class="rank-bar is-green" aria-hidden="true">
                <span :style="{ width: getPercent(group.count, maxMemberCount) + '%' }" />
              </div>
            </article>
          </div>
          <el-empty v-else description="暂无成员数据" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { findtop, getcount } from '@/api/user'

export default {
  // 提供组件初始响应式数据。
  data() {
    return {
      hotGroups: [],
      memberGroups: []
    }
  },
  computed: {
    // 处理当前组件的业务逻辑或用户交互。
    maxHot() {
      return Math.max(...this.hotGroups.map(group => Number(group.hot) || 0), 1)
    },
    // 处理当前组件的业务逻辑或用户交互。
    maxMemberCount() {
      return Math.max(...this.memberGroups.map(group => Number(group.count) || 0), 1)
    }
  },
  // 组件挂载后初始化页面交互或监听器。
  mounted() {
    this.loadDashboard()
  },
  methods: {
    // 读取并返回业务数据或页面状态。
    getPercent(value, max) {
      return Math.max(8, Math.round(((Number(value) || 0) / max) * 100))
    },
    // 加载页面所需数据，并同步到当前状态。
    loadDashboard() {
      getcount()
        .then((response) => {
          this.memberGroups = (response.data.items || []).slice(0, 5)
        })
        .catch(error => {
          console.error(error)
        })

      findtop()
        .then((response) => {
          this.hotGroups = (response.data.item || []).slice(0, 5)
        })
        .catch(error => {
          console.error(error)
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.page-heading span {
  color: var(--bt-text-muted);
  font-size: 14px;
}

.chart-card {
  min-height: 430px;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--bt-text);
  font-weight: 700;
}

.card-header small {
  color: var(--bt-text-muted);
  font-size: 13px;
  font-weight: 500;
}

.rank-list {
  display: grid;
  gap: 18px;
  padding: 8px 0;
}

.rank-item {
  display: grid;
  gap: 10px;
}

.rank-line {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
}

.rank-index {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--bt-primary-soft);
  color: var(--bt-primary);
  font-family: var(--bt-font-data);
  font-weight: 800;
}

.rank-line strong {
  overflow: hidden;
  color: var(--bt-text);
  font-size: 15px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-line em {
  color: var(--bt-text-muted);
  font-family: var(--bt-font-data);
  font-size: 14px;
  font-style: normal;
  font-weight: 650;
}

.rank-bar {
  height: 12px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--bt-surface-muted);
}

.rank-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--bt-primary), #22d3ee);
  transition: width .24s ease;
}

.rank-bar.is-green span {
  background: linear-gradient(90deg, var(--bt-campus-green), #22c55e);
}

@media (max-width: 760px) {
  .page-heading,
  .card-header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
