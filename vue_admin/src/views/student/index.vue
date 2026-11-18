<template>
  <div class="student-page app-container">
    <section class="page-toolbar">
      <div>
        <p class="eyebrow">Members</p>
        <h1>成员管理</h1>
      </div>
      <div class="search-row">
        <el-input
          v-model="searchInfo"
          placeholder="搜索成员姓名或学号"
          clearable
          @keyup.enter="pageNumber = 1; handleSearch()"
        />
        <el-button type="primary" icon="el-icon-search" @click="pageNumber = 1; handleSearch()">搜索</el-button>
      </div>
    </section>

    <el-table :data="studentlist" style="width: 100%" @row-click="handleRowClick">
      <el-table-column prop="userName" label="姓名" min-width="180">
        <template #default="scope">
          <span class="strong-text">{{ scope.row.userName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="userId" label="学号" min-width="220">
        <template #default="scope">
          <span>{{ scope.row.userId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="right">
        <template>
          <el-button type="text">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination :current-page="pageNumber" :page-size="20" :total="totalItems"
      layout="prev, pager, next, total" @current-change="page => { pageNumber = page; handleSearch() }" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getStudentList } from '@/api/table.js'

export default {
  name: 'StudentList',
  computed: {
    ...mapGetters([
      'name'
    ])
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      studentlist: [],
      totalItems: 0,
      pageNumber: 1,
      searchInfo: ''
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    this.handleSearch()
    if (!this.$store.getters.isClubSelected) {
      this.$message.warning('登录身份失效，请重新登录')
      this.reload()
    }
  },
  methods: {
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleRowClick(row) {
      this.$router.push({
        name: 'Studentdetail',
        params: { id: row.userId }
      })
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleSearch() {
      getStudentList(this.$store.getters.currentClubId, this.searchInfo, this.pageNumber, 20)
        .then((response) => {
          this.studentlist = response.data.items
          this.totalItems = response.data.page.total
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
.student-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.strong-text {
  color: var(--bt-text);
  font-weight: 700;
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
