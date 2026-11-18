<template>
  <div class="app-container pending-page">
    <section class="page-heading">
      <span class="eyebrow">Approved activities</span>
      <h1>活动变更</h1>
      <p>查看已通过的活动，并在活动信息需要调整时提交变更。</p>
    </section>

    <div class="page-toolbar">
      <el-input v-model="searchInfo" placeholder="搜索活动名称或组织者" clearable @keyup.enter="handleSearch" />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="clearSearch">清空</el-button>
    </div>

    <el-table :data="searcchRes" empty-text="暂无可变更活动">
      <el-table-column prop="name" label="活动名称" min-width="160" />
      <el-table-column prop="id" label="活动编号" min-width="110" />
      <el-table-column prop="organizer" label="组织者" min-width="140" />
      <el-table-column prop="time" label="申请时间" min-width="170" sortable>
        <template #default="scope">
          {{ formatDate(scope.row.time) }}
        </template>
      </el-table-column>
      <el-table-column prop="isAccepted" label="状态" min-width="120">
        <template #default>
          <span class="bt-status is-success">已通过</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="scope">
          <el-button type="primary" text @click.prevent="handleEdit(scope.$index)">修改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="修改活动" width="640px" :before-close="handleClose">
      <el-form :model="ruleForm" label-width="88px">
        <el-form-item label="名称" required>
          <el-input v-model="ruleForm.name" />
        </el-form-item>
        <el-form-item label="组织者" prop="organizer" required>
          <el-input v-model="ruleForm.organizer" />
        </el-form-item>
        <el-form-item label="时间" prop="time" required>
          <el-date-picker v-model="ruleForm.time" type="datetime" placeholder="选择活动日期和时间" class="full-control" />
        </el-form-item>
        <el-form-item label="地点" prop="place">
          <el-input v-model="ruleForm.place" />
        </el-form-item>
        <el-form-item label="安排" prop="arrange">
          <el-input v-model="ruleForm.arrange" />
        </el-form-item>
        <el-form-item label="描述" prop="desc">
          <el-input v-model="ruleForm.description" type="textarea" :rows="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存修改</el-button>
      </template>
    </el-dialog>
    <el-pagination :current-page="pageNumber" :page-size="20" :total="pageTotal"
      layout="prev, pager, next, total" @current-change="page => { pageNumber = page; fetchActivities(searchInfo) }" />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getMyApps, modifyInfo } from '@/api/activities.js'

export default {
  name: 'PendingActivity',
  computed: {
    ...mapGetters([
      'name'
    ])
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      pageNumber: 1,
      pageTotal: 0,
      searchInfo: '',
      activities: [],
      searcchRes: [],
      dialogVisible: false,
      ruleForm: {
        name: '',
        organizer: '',
        type: 0,
        place: '',
        time: '',
        arrange: '',
        desc: '',
        attachment: '',
        image: '',
        number: 0,
        id: 0,
        groupName: this.$store.getters.currentClubName
      }
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    this.fetchActivities()
  },
  methods: {
    // 从后端接口获取业务数据。
    fetchActivities(keyword = '') {
      return getMyApps(keyword, this.$store.getters.currentClubName, { pageNum: this.pageNumber, pageSize: 20 }).then((response) => {
        this.pageTotal = response.data.page.total
        this.searcchRes = response.data.items || []
        this.activities = response.data.items || []
      })
        .catch(error => {
          console.error(error)
        })
    },
    // 格式化展示文本，便于页面直接显示。
    formatDate(date) {
      if (!date) return '-'
      const d = new Date(date)
      if (Number.isNaN(d.getTime())) return '-'
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleSearch() {
      this.pageNumber = 1
      this.fetchActivities(this.searchInfo)
    },
    // 根据筛选条件查询并更新列表结果。
    clearSearch() {
      this.searchInfo = ''
      this.pageNumber = 1
      this.fetchActivities()
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleEdit(index) {
      const a = this.searcchRes[index]
      this.ruleForm = {
        ...this.ruleForm,
        name: a.name,
        organizer: a.organizer,
        place: a.place,
        time: a.time,
        arrange: a.arrange,
        desc: a.desc,
        description: a.description,
        attachment: a.attachment,
        image: a.image,
        number: a.number,
        id: a.id
      }
      this.dialogVisible = true
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleClose(done) {
      this.$confirm('关闭后未保存的修改会丢失，确认关闭？')
        .then(() => done())
        .catch(() => {})
    },
    // 提交表单数据并触发后端业务处理。
    submitForm() {
      modifyInfo(this.ruleForm).then(() => {
        this.dialogVisible = false
        this.$message.success('活动信息已修改')
        this.fetchActivities(this.searchInfo)
      }).catch(error => {
        this.$message({
          message: error,
          type: 'info'
        })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.full-control {
  width: 100%;
}
</style>
