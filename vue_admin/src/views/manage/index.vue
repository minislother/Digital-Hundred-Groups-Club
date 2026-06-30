<template>
  <div class="manage-page app-container">
    <section class="page-heading">
      <div>
        <p class="eyebrow">Club Workspace</p>
        <h1>社团概览</h1>
      </div>
      <el-button type="primary" icon="el-icon-edit" @click="redirectToGroupDetail(currentClubName)">编辑资料</el-button>
    </section>

    <el-row :gutter="20">
      <el-col v-for="manage in manages" :key="manage.id || manage.name" :xs="24" :md="12">
        <el-card class="club-card" :body-style="{ padding: '0' }">
          <img v-if="manage.image" :src="manage.image" :alt="`${manage.name} 封面`" loading="lazy" class="club-image">
          <div v-else class="club-image placeholder">暂无社团图片</div>
          <div class="club-content">
            <div class="club-title-row">
              <h2>{{ manage.name }}</h2>
              <span>{{ manage.leader }}</span>
            </div>
            <p class="club-description">{{ manage.description || '暂无社团介绍' }}</p>
            <div class="meta-list">
              <div>
                <span>图片地址</span>
                <p>{{ manage.image || '未上传' }}</p>
              </div>
              <div>
                <span>附件地址</span>
                <p>{{ manage.attachment || '未上传' }}</p>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getManages } from '@/api/manage.js'

export default {
  name: 'Manage',
  computed: {
    ...mapGetters([
      'name'
    ]),
    // 处理当前组件的业务逻辑或用户交互。
    currentClubName() {
      return this.$store.getters.currentClubName || this.name
    }
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      manages: []
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    getManages(this.name).then((response) => {
      this.manages = response.data.items
    })
    if (!this.$store.getters.isClubSelected) {
      this.$message.warning('登录身份失效，请重新登录')
      this.reload()
    }
  },
  methods: {
    // 处理当前组件的业务逻辑或用户交互。
    redirectToGroupDetail(groupName) {
      this.$router.push({
        name: 'Groupdetail',
        params: {
          groupName: groupName
        }
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
.manage-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
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

.club-card {
  overflow: hidden;
}

.club-image {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 7;
  object-fit: cover;
  background: var(--bt-surface-muted);
}

.club-image.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--bt-text-muted);
}

.club-content {
  padding: 18px;
}

.club-title-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.club-title-row h2 {
  margin: 0;
  color: var(--bt-text);
  font-size: 22px;
}

.club-title-row span {
  color: var(--bt-text-muted);
  font-size: 14px;
}

.club-description {
  margin: 14px 0 18px;
  color: var(--bt-text);
  line-height: 1.7;
}

.meta-list {
  display: grid;
  gap: 12px;
}

.meta-list span {
  color: var(--bt-text-muted);
  font-size: 13px;
  font-weight: 600;
}

.meta-list p {
  margin: 6px 0 0;
  color: var(--bt-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 720px) {
  .page-heading {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
