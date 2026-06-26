<template>
  <div class="tree-page app-container">
    <section class="page-toolbar">
      <div>
        <p class="eyebrow">Student Tree</p>
        <h1>社团成员树</h1>
      </div>
      <el-input
        v-model="filterText"
        placeholder="输入社团、姓名或学号筛选"
        clearable
      />
    </section>

    <el-card class="tree-card" :body-style="{ padding: '16px' }">
      <el-tree
        ref="tree2"
        class="filter-tree"
        :data="data2"
        :render-content="renderNode"
        :filter-node-method="filterNode"
        default-expand-all
        @node-click="handleNodeClick"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" title="学生详情" width="420px">
      <div class="student-detail">
        <div>
          <span>学号</span>
          <p>{{ data1.userId }}</p>
        </div>
        <div>
          <span>姓名</span>
          <p>{{ data1.userName }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import arrayToTree from 'array-to-tree'
import { getAllStudent } from '@/api/user.js'
import { getList } from '@/api/table.js'

export default {
  // 提供组件初始响应式数据。
  data() {
    return {
      filterText: '',
      studentlist: [],
      grouplist: [],
      searchInfo: '',
      index: 1,
      data1: {},
      data2: [],
      dialogVisible: false
    }
  },
  watch: {
    // 处理当前组件的业务逻辑或用户交互。
    filterText(val) {
      this.$refs.tree2.filter(val)
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    getAllStudent(this.searchInfo).then((response) => {
      this.studentlist = response.data.items
      this.createtree()
    })
      .catch(error => {
        console.error(error)
      })

    getList(this.searchInfo).then((response) => {
      this.grouplist = response.data.items
      this.createtree()
    })
      .catch(error => {
        console.error(error)
      })
  },
  methods: {
    // 处理当前组件的业务逻辑或用户交互。
    filterNode(value, data) {
      if (!value) return true
      const label = `${data.name || ''}${data.userName || ''}${data.userId || ''}`
      return label.indexOf(value) !== -1
    },
    // 创建业务数据或初始化页面结构。
    createtree() {
      if (!this.studentlist.length || !this.grouplist.length) {
        return
      }
      this.index = 1
      this.grouplist.forEach((item, index) => {
        item.index = this.grouplist[index].id
        this.index = this.grouplist[index].id
      })
      this.index = this.index + 1
      this.studentlist.forEach(item => {
        item.index = this.index++
      })
      const tree = arrayToTree([...this.grouplist, ...this.studentlist], {
        parentProperty: 'groupId',
        customID: 'index'
      })
      this.data2 = [{
        index: 0,
        name: '全部社团',
        children: tree
      }]
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleNodeClick(data) {
      if ('userName' in data) {
        this.data1 = data
        this.dialogVisible = true
      }
    },
    // 处理当前组件的业务逻辑或用户交互。
    renderNode(h, { data }) {
      if ('userName' in data) {
        return h('span', { class: 'tree-node student-node' }, [
          h('span', { class: 'node-title' }, data.userName),
          h('span', { class: 'node-meta' }, data.userId)
        ])
      }
      return h('span', { class: 'tree-node club-node' }, [
        h('span', { class: 'node-title' }, data.name)
      ])
    }
  }
}
</script>

<style lang="scss" scoped>
.tree-page {
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

.page-toolbar .el-input {
  width: min(480px, 100%);
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

.tree-card {
  min-height: 520px;
}

.student-detail {
  display: grid;
  gap: 14px;
}

.student-detail span {
  color: var(--bt-text-muted);
  font-size: 13px;
  font-weight: 600;
}

.student-detail p {
  margin: 6px 0 0;
  color: var(--bt-text);
  font-size: 16px;
}

:deep(.tree-node) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

:deep(.node-title) {
  color: var(--bt-text);
  font-weight: 600;
}

:deep(.node-meta) {
  color: var(--bt-text-muted);
  font-size: 12px;
}

@media (max-width: 760px) {
  .page-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .page-toolbar .el-input {
    width: 100%;
  }
}
</style>
