<template>
  <div class="club-page app-container">
    <section class="page-toolbar">
      <div>
        <p class="eyebrow">Clubs</p>
        <h1>社团列表</h1>
      </div>
      <div class="search-row">
        <el-input
          v-model="searchInfo"
          placeholder="搜索社团名称"
          clearable
          @keyup.enter="currentPage = 1; handleSearch()"
        />
        <el-button type="primary" icon="el-icon-search" @click="currentPage = 1; handleSearch()">搜索</el-button>
      </div>
    </section>

    <el-row :gutter="20">
      <el-col
        v-for="group in groups"
        :key="group.id"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
      >
        <el-card class="group-card" :body-style="{ padding: '0' }">
          <img :src="group.image" :alt="`${group.name} 封面`" loading="lazy" class="group-image">
          <div class="group-details">
            <div class="group-name">{{ group.name }}</div>
            <el-button size="small" @click="showDescription(group)">查看详情</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" title="社团详情" width="520px">
      <p class="dialog-description">{{ currentGroup.description || '暂无社团介绍' }}</p>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    <el-pagination
      :current-page="currentPage" :page-size="pageSize" :total="totalItems"
      layout="prev, pager, next, total"
      @current-change="page => { currentPage = page; handleSearch() }"
    />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getGroups } from '@/api/user.js'

export default {
  name: 'Clubtable',
  computed: {
    ...mapGetters([
      'userName'
    ])
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      groups: [],
      dialogVisible: false,
      searchInfo: '',
      totalItems: 0,
      currentPage: 1,
      pageSize: 32,
      currentGroup: {
        description: ''
      }
    }
  },
  // 组件创建后加载页面初始数据。
  created() { this.handleSearch() },
  methods: {
    // 展示弹窗、详情或提示内容。
    showDescription(group) {
      this.currentGroup = group
      this.dialogVisible = true
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleSearch() {
      getGroups(this.searchInfo, this.currentPage, this.pageSize)
        .then((response) => {
          this.totalItems = response.data.page.total
          this.groups = response.data.items
        })
        .catch((error) => {
          console.error(error)
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.club-page {
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
}

.search-row {
  display: flex;
  gap: 10px;
  width: min(460px, 100%);
}

.search-row .el-input {
  flex: 1;
}

.group-card {
  margin-bottom: 20px;
  overflow: hidden;
  transition: transform .18s ease, box-shadow .18s ease;
}

.group-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(27, 31, 36, 0.12) !important;
}

.group-image {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 10;
  object-fit: cover;
  background: var(--bt-surface-muted);
}

.group-details {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
}

.group-name {
  color: var(--bt-text);
  font-size: 16px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dialog-description {
  margin: 0;
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
