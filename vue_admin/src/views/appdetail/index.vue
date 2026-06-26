<template>
  <div class="detail-page app-container">
    <section class="page-heading">
      <div>
        <p class="eyebrow">Application Detail</p>
        <h1>申请详情信息</h1>
      </div>
      <span :class="['status-pill', statusClass(applications.isAccepted)]">
        {{ statusText(applications.isAccepted) }}
      </span>
    </section>

    <el-card class="detail-card">
      <dl class="detail-grid">
        <div>
          <dt>申请编号</dt>
          <dd>{{ applications.applicationId || '-' }}</dd>
        </div>
        <div>
          <dt>社团名称</dt>
          <dd>{{ applications.groupName || '-' }}</dd>
        </div>
        <div>
          <dt>姓名</dt>
          <dd>{{ applications.stuName || '-' }}</dd>
        </div>
        <div>
          <dt>学号</dt>
          <dd>{{ applications.stuNumber || '-' }}</dd>
        </div>
        <div>
          <dt>手机号</dt>
          <dd>{{ applications.phone || '-' }}</dd>
        </div>
        <div>
          <dt>性别</dt>
          <dd>{{ applications.gender || '-' }}</dd>
        </div>
        <div>
          <dt>专业</dt>
          <dd>{{ applications.major || '-' }}</dd>
        </div>
        <div>
          <dt>申请时间</dt>
          <dd>{{ formatDate(applications.createTime) }}</dd>
        </div>
        <div class="full-row">
          <dt>自我介绍</dt>
          <dd>{{ applications.selfIntro || '暂无自我介绍' }}</dd>
        </div>
      </dl>
      <div class="button-row">
        <el-button
          v-if="applications.attachment !== null"
          @click="openAttachment()"
        >
          查看附件
        </el-button>
        <el-button
          type="success"
          :disabled="applications.isAccepted !== null"
          @click="submitAccept()"
        >
          同意申请
        </el-button>
        <el-button
          type="danger"
          :disabled="applications.isAccepted !== null"
          @click="submitReject()"
        >
          拒绝申请
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import shared from '@baituan/frontend-shared'
import { getAppDetails, acceptApplication, rejectApplication } from '@/api/seeapplication.js'

export default {
  name: 'Appdetail',
  computed: {
    ...mapGetters([
      'name'
    ])
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      applications: {}
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    const id = this.$route.params.id
    getAppDetails(id).then((response) => {
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
    // 格式化展示文本，便于页面直接显示。
    formatDate(value) {
      return shared.formatDateTime(value)
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
    // 打开详情、预览或外部资源。
    openAttachment() {
      window.open(this.applications.attachment)
    },
    // 提交表单数据并触发后端业务处理。
    submitAccept() {
      acceptApplication(this.applications.applicationId)
        .then(() => {
          this.$message.success('已同意申请')
          this.applications.isAccepted = true
        })
        .catch((error) => {
          console.error(error)
        })
    },
    // 提交表单数据并触发后端业务处理。
    submitReject() {
      rejectApplication(this.applications.applicationId)
        .then(() => {
          this.$message.success('已拒绝申请')
          this.applications.isAccepted = false
        })
        .catch((error) => {
          console.error(error)
        })
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
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.detail-card {
  max-width: 1040px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin: 0;
}

.detail-grid div {
  padding: 14px;
  border: 1px solid var(--bt-border-muted);
  border-radius: 8px;
  background: var(--bt-surface-muted);
}

.full-row {
  grid-column: 1 / -1;
}

dt {
  color: var(--bt-text-muted);
  font-size: 13px;
  font-weight: 700;
}

dd {
  margin: 8px 0 0;
  color: var(--bt-text);
  line-height: 1.7;
}

.button-row {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.is-success {
  color: var(--bt-success);
  background: #dcfce7;
}

.is-danger {
  color: var(--bt-danger);
  background: #fee2e2;
}

.is-warning {
  color: var(--bt-warning);
  background: #fef3c7;
}

@media (max-width: 720px) {
  .page-heading,
  .button-row {
    align-items: stretch;
    flex-direction: column;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
