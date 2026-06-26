<template>
  <div class="app-container ranking-page">
    <section class="page-heading">
      <span class="eyebrow">Club ranking</span>
      <h1>社团热度榜</h1>
      <p>按近期关注度展示社团，帮你快速发现校园里正在升温的组织。</p>
    </section>

    <section v-if="groups.length" class="ranking-list">
      <article v-for="(group, index) in groups" :key="group.id" class="ranking-card">
        <div class="rank-badge">第 {{ index + 1 }} 名</div>
        <img :src="group.image" :alt="`${group.name} 封面`" loading="lazy" class="ranking-cover">
        <div class="ranking-body">
          <h2>{{ group.name }}</h2>
          <p>{{ group.description || '该社团暂未填写简介。' }}</p>
          <div class="card-actions">
            <el-button type="primary" @click="redirectToApplication(group.name)">申请加入</el-button>
            <el-button @click="showDescription(group)">查看详情</el-button>
          </div>
        </div>
      </article>
    </section>

    <el-empty v-else description="暂无热度榜数据" />

    <el-dialog v-model="dialogVisible" title="社团详情" width="560px">
      <p class="dialog-copy">{{ currentGroup.description || '该社团暂未填写简介。' }}</p>
      <template #footer>
        <el-button v-if="currentGroup.id" @click="downloadAttachment(currentGroup.id)">下载附件</el-button>
        <el-button type="primary" @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { getAttachmentUrl, getTopGroups } from '@/api/group.js'

export default {
  name: 'Top',
  // 提供组件初始响应式数据。
  data() {
    return {
      groups: [],
      dialogVisible: false,
      currentGroup: {
        id: '',
        description: ''
      }
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    getTopGroups().then((response) => {
      this.groups = response.data.item || []
    })
      .catch(error => {
        console.error(error)
      })
  },
  methods: {
    // 展示弹窗、详情或提示内容。
    showDescription(group) {
      this.currentGroup = group
      this.dialogVisible = true
    },
    // 处理当前组件的业务逻辑或用户交互。
    redirectToApplication(groupName) {
      this.$router.push({
        name: 'Application',
        params: {
          groupName
        }
      })
    },
    // 下载后端提供的附件或模板文件。
    async downloadAttachment(groupId) {
      try {
        const response = await getAttachmentUrl(groupId)
        const url = response.attachment || response.data?.attachment
        if (url) {
          window.open(url, '_blank')
        } else {
          this.$message.error('暂未获取到附件地址')
        }
      } catch (error) {
        console.error(error)
        this.$message.error('下载附件时发生错误，请稍后重试')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.ranking-list {
  display: grid;
  gap: 16px;
}

.ranking-card {
  position: relative;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 20px;
  padding: 18px;
  border: 1px solid var(--bt-border-muted);
  border-radius: var(--bt-radius-lg);
  background: var(--bt-surface);
  box-shadow: var(--bt-shadow);
}

.rank-badge {
  position: absolute;
  top: 18px;
  left: 18px;
  z-index: 1;
  min-height: 32px;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--bt-warning-soft);
  color: var(--bt-warning);
  font-weight: 750;
}

.ranking-cover {
  width: 100%;
  aspect-ratio: 16 / 10;
  object-fit: cover;
  border-radius: var(--bt-radius);
  background: var(--bt-surface-muted);
}

.ranking-body h2 {
  margin: 4px 0 10px;
  font-size: 22px;
}

.ranking-body p,
.dialog-copy {
  color: var(--bt-text-muted);
  line-height: 1.7;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}

@media (max-width: 760px) {
  .ranking-card {
    grid-template-columns: 1fr;
  }
}
</style>
