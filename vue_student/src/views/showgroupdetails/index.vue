<template>
  <div class="app-container group-detail-page">
    <section class="page-heading">
      <span class="eyebrow">Club attachment</span>
      <h1>社团详情附件</h1>
      <p>下载社团上传的介绍材料，进一步了解社团方向、活动安排和招新要求。</p>
    </section>

    <section class="detail-card">
      <div>
        <h2>附件材料</h2>
        <p>如果社团提供了附件，可以在这里打开查看。打不开时请稍后重试或联系社团管理员。</p>
      </div>
      <el-button type="primary" :disabled="!group.id" @click="downloadAttachment(group.id)">
        下载附件
      </el-button>
    </section>
  </div>
</template>

<script>
import { getAttachmentUrl } from '@/api/group.js'

export default {
  name: 'Showgroupdetails',
  // 提供组件初始响应式数据。
  data() {
    return {
      group: {
        id: '',
        attachment: '',
        description: ''
      }
    }
  },
  // 组件挂载后初始化页面交互或监听器。
  mounted() {
    this.group.id = this.$route.params.groupId
    this.group.description = this.$route.params.description
  },
  methods: {
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
.detail-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px;
  border: 1px solid var(--bt-border-muted);
  border-radius: var(--bt-radius-lg);
  background: var(--bt-surface);
  box-shadow: var(--bt-shadow);
}

.detail-card h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.detail-card p {
  max-width: 680px;
  margin: 0;
  color: var(--bt-text-muted);
}

@media (max-width: 720px) {
  .detail-card {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
