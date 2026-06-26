<template>
  <div class="activity-page app-container">
    <section class="page-toolbar">
      <div>
        <p class="eyebrow">Activities</p>
        <h1>活动列表</h1>
      </div>
      <div class="search-row">
        <el-input
          v-model="searchInfo"
          placeholder="搜索活动名称"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
      </div>
    </section>

    <el-row :gutter="20">
      <el-col
        v-for="activity in activites"
        :key="activity.id"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
      >
        <el-card class="activity-card" :body-style="{ padding: '0' }">
          <img :src="activity.image" :alt="`${activity.name} 活动封面`" loading="lazy" class="activity-image">
          <div class="activity-details">
            <div class="activity-title">{{ activity.name }}</div>
            <div class="activity-organizer">{{ activity.organizer }}</div>
            <div class="activity-buttons">
              <el-button type="primary" size="small" @click="redirectToApplication(activity.id)">申请加入</el-button>
              <el-button size="small" @click="showDescription(activity)">查看详情</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" title="活动详情" width="520px">
      <div class="detail-list">
        <div>
          <span>活动介绍</span>
          <p>{{ currentActivity.description || '暂无介绍' }}</p>
        </div>
        <div>
          <span>举办社团</span>
          <p>{{ currentActivity.groupName || currentActivity.organizer || '暂无信息' }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getActivites } from '@/api/activity.js'

export default {
  name: 'activity',
  computed: {
    ...mapGetters([
      'userName'
    ])
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      activites: [],
      dialogVisible: false,
      currentActivity: {
        organizer: ''
      },
      totalItems: 0,
      pageSize: 32,
      currentPage: 1,
      searchInfo: ''
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    this.handleSearch()
  },
  methods: {
    // 展示弹窗、详情或提示内容。
    showDescription(activity) {
      this.currentActivity = activity
      this.dialogVisible = true
    },
    // 处理当前组件的业务逻辑或用户交互。
    redirectToApplication(activityid) {
      this.$router.push({
        name: 'ActivityApplication',
        params: {
          activityid: activityid
        }
      })
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleSearch() {
      getActivites(this.searchInfo)
        .then((response) => {
          this.totalItems = response.data.items.length
          this.activites = response.data.items.slice((this.currentPage - 1) * this.pageSize, this.currentPage * this.pageSize)
        })
        .catch((error) => {
          console.error(error)
        })
    },
    // 处理当前组件的业务逻辑或用户交互。
    redirectToShowactivitydetails(activityId) {
      this.$router.push({
        name: 'Showactivitydetails',
        params: {
          activityId: activityId
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.activity-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
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
  line-height: 1.25;
}

.search-row {
  display: flex;
  gap: 10px;
  width: min(480px, 100%);
}

.search-row .el-input {
  flex: 1;
}

.activity-card {
  margin-bottom: 20px;
  overflow: hidden;
  transition: transform .18s ease, box-shadow .18s ease;
}

.activity-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(27, 31, 36, 0.12) !important;
}

.activity-image {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 10;
  object-fit: cover;
  background: var(--bt-surface-muted);
}

.activity-details {
  padding: 14px;
}

.activity-title {
  color: var(--bt-text);
  font-size: 16px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-organizer {
  min-height: 20px;
  margin-top: 4px;
  color: var(--bt-text-muted);
  font-size: 13px;
}

.activity-buttons {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.activity-buttons .el-button {
  flex: 1;
  margin-left: 0;
}

.detail-list {
  display: grid;
  gap: 16px;
}

.detail-list span {
  color: var(--bt-text-muted);
  font-size: 13px;
  font-weight: 600;
}

.detail-list p {
  margin: 6px 0 0;
  color: var(--bt-text);
  line-height: 1.7;
}

@media (max-width: 760px) {
  .page-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-row {
    width: 100%;
  }
}
</style>
