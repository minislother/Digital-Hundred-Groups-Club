<template>
  <div class="app-container group-page">
    <section class="page-heading">
      <span class="eyebrow">My clubs</span>
      <h1>我的社团</h1>
      <p>集中查看你已经加入的社团，快速回顾社团简介与后续活动安排。</p>
    </section>

    <section v-if="groups.length" class="club-grid">
      <article v-for="group in groups" :key="group.id" class="club-card">
        <img :src="group.image" :alt="`${group.name} 封面`" loading="lazy" class="club-cover">
        <div class="club-body">
          <h2>{{ group.name }}</h2>
          <p>{{ group.description || '该社团暂未填写简介。' }}</p>
          <el-button type="primary" @click="showDescription(group)">查看详情</el-button>
        </div>
      </article>
    </section>

    <el-empty v-else description="你还没有加入社团">
      <el-button type="primary" @click="$router.push({ name: 'Dashboard' })">去社团广场看看</el-button>
    </el-empty>

    <el-dialog v-model="dialogVisible" title="社团详情" width="560px">
      <p class="dialog-copy">{{ currentGroup.description || '该社团暂未填写简介。' }}</p>
      <template #footer>
        <el-button type="primary" @click="dialogVisible = false">知道了</el-button>
      </template>
    </el-dialog>
    <el-button v-if="hasNext" @click="loadMore">加载更多社团</el-button>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getMyGroups } from '@/api/group.js'
import { getProfile } from '@/api/user.js'

export default {
  name: 'MyGroup',
  computed: {
    ...mapGetters([
      'name'
    ])
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      groups: [],
      nextCursor: null,
      hasNext: false,
      userID: '',
      dialogVisible: false,
      currentGroup: {
        description: ''
      }
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    getProfile(this.name)
      .then((res) => {
        this.userID = res.data.student.userId
        return getMyGroups(this.userID)
      })
      .then((response) => {
        this.groups = response.data.groupItems || []
        this.hasNext = response.data.hasNext
        this.nextCursor = response.data.nextCursor
      })
      .catch(error => {
        console.error(error)
      })
  },
  methods: {
    async loadMore() {
      const response = await getMyGroups(this.userID, this.nextCursor)
      this.groups.push(...response.data.groupItems)
      this.hasNext = response.data.hasNext
      this.nextCursor = response.data.nextCursor
    },
    // 展示弹窗、详情或提示内容。
    showDescription(group) {
      this.currentGroup = group
      this.dialogVisible = true
    }
  }
}
</script>

<style lang="scss" scoped>
.club-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

.club-card {
  overflow: hidden;
  border: 1px solid var(--bt-border-muted);
  border-radius: var(--bt-radius-lg);
  background: var(--bt-surface);
  box-shadow: var(--bt-shadow);
}

.club-cover {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 10;
  object-fit: cover;
  background: var(--bt-surface-muted);
}

.club-body {
  padding: 18px;
}

.club-body h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.club-body p,
.dialog-copy {
  color: var(--bt-text-muted);
  line-height: 1.7;
}

.club-body p {
  display: -webkit-box;
  min-height: 54px;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>
