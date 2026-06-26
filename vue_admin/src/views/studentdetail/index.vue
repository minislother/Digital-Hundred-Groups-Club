<template>
  <div class="detail-page app-container">
    <section class="page-heading">
      <div>
        <p class="eyebrow">Member Detail</p>
        <h1>成员详情信息</h1>
      </div>
    </section>

    <el-card class="detail-card">
      <dl class="detail-grid">
        <div>
          <dt>学号</dt>
          <dd>{{ student.userId || '-' }}</dd>
        </div>
        <div>
          <dt>姓名</dt>
          <dd>{{ student.userName || '-' }}</dd>
        </div>
        <div>
          <dt>邮箱</dt>
          <dd>{{ student.email || '-' }}</dd>
        </div>
        <div>
          <dt>手机号</dt>
          <dd>{{ student.phone || '-' }}</dd>
        </div>
        <div>
          <dt>校区</dt>
          <dd>{{ student.campus || '-' }}</dd>
        </div>
        <div>
          <dt>专业</dt>
          <dd>{{ student.major || '-' }}</dd>
        </div>
        <div class="full-row">
          <dt>自我介绍</dt>
          <dd>{{ student.description || '暂无自我介绍' }}</dd>
        </div>
      </dl>
    </el-card>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getStudentDetail } from '@/api/user.js'

export default {
  name: 'Studentdetail',
  computed: {
    ...mapGetters([
      'name'
    ])
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      student: {}
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    const id = this.$route.params.id
    getStudentDetail(id).then((response) => {
      this.student = response.data.user
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
  max-width: 960px;
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

@media (max-width: 720px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
