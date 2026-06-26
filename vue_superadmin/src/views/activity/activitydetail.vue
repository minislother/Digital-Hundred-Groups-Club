<template>
  <div class="app-container activity-detail-page">
    <section class="page-heading">
      <span class="eyebrow">Activity review</span>
      <h1>活动详情</h1>
      <p>核对活动信息、附件与图片后，对活动申请进行审批。</p>
    </section>

    <section class="detail-card">
      <div class="detail-grid">
        <div class="detail-item">
          <span>活动名称</span>
          <strong>{{ activity.name || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span>地点</span>
          <strong>{{ activity.place || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span>组织者</span>
          <strong>{{ activity.organizer || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span>申请时间</span>
          <strong>{{ formatDate(activity.time) }}</strong>
        </div>
        <div class="detail-item">
          <span>事项安排</span>
          <strong>{{ activity.arrange || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span>活动状态</span>
          <span :class="statusClass(activity.isAccepted)">{{ statusText(activity.isAccepted) }}</span>
        </div>
      </div>

      <div class="description-block">
        <span>活动描述</span>
        <p>{{ activity.description || '暂无活动描述。' }}</p>
      </div>

      <div class="action-row">
        <el-button :disabled="!activity.attachment" @click="openAttachment">查看附件</el-button>
        <el-button :disabled="!activity.image" @click="openImage">查看图片</el-button>
        <el-button type="primary" :disabled="activity.isAccepted === true" @click="acceptActivity">批准申请</el-button>
        <el-button type="danger" :disabled="activity.isAccepted === false" @click="rejectActivity">拒绝申请</el-button>
      </div>
    </section>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import shared from '@baituan/frontend-shared'
import { acceptActivity, rejectActivity } from '@/api/activities.js'

export default {
  name: 'ActivityDetail',
  // 提供组件初始响应式数据。
  data() {
    return {
      activity: {}
    }
  },
  computed: {
    ...mapGetters([
      'name'
    ])
  },
  // 组件创建后加载页面初始数据。
  created() {
    this.activity = this.$route.params.activity || {}
  },
  methods: {
    // 格式化展示文本，便于页面直接显示。
    formatDate(value) {
      return shared.formatDateTime(value)
    },
    // 处理当前组件的业务逻辑或用户交互。
    statusClass(value) {
      if (value === true) return 'bt-status is-success'
      if (value === false) return 'bt-status is-danger'
      return 'bt-status is-warning'
    },
    // 处理当前组件的业务逻辑或用户交互。
    statusText(value) {
      if (value === true) return '已批准'
      if (value === false) return '已拒绝'
      return '待审核'
    },
    // 打开详情、预览或外部资源。
    openAttachment() {
      if (this.activity.attachment) window.open(this.activity.attachment)
    },
    // 打开详情、预览或外部资源。
    openImage() {
      if (this.activity.image) window.open(this.activity.image)
    },
    // 处理当前组件的业务逻辑或用户交互。
    acceptActivity() {
      acceptActivity(this.activity.id)
        .then(() => {
          this.activity.isAccepted = true
          this.$message.success('活动申请已批准')
        })
        .catch((error) => {
          console.error(error)
        })
    },
    // 处理当前组件的业务逻辑或用户交互。
    rejectActivity() {
      rejectActivity(this.activity.id)
        .then(() => {
          this.activity.isAccepted = false
          this.$message.success('活动申请已拒绝')
        })
        .catch((error) => {
          console.error(error)
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.detail-card {
  padding: 24px;
  border: 1px solid var(--bt-border-muted);
  border-radius: var(--bt-radius-lg);
  background: var(--bt-surface);
  box-shadow: var(--bt-shadow);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.detail-item {
  display: grid;
  gap: 6px;
  padding: 16px;
  border-radius: var(--bt-radius);
  background: var(--bt-surface-soft);
}

.detail-item span:first-child,
.description-block span {
  color: var(--bt-text-muted);
  font-size: 14px;
}

.detail-item strong {
  font-size: 17px;
}

.description-block {
  margin-top: 20px;
  padding: 18px;
  border: 1px solid var(--bt-border-muted);
  border-radius: var(--bt-radius);
}

.description-block p {
  margin: 8px 0 0;
  color: var(--bt-text);
  line-height: 1.7;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
}
</style>
