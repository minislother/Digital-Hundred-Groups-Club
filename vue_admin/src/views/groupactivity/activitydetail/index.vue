<template>
  <div>
    <el-header>
      <h1>活动详情信息</h1>
    </el-header>
    <el-row :gutter="20">
      <el-col :span="4">
        <div class="grid-content bg-purple"><el-main class="el-icon-wind-power"> 活动名</el-main></div>
      </el-col>
      <el-col :span="4">
        <div class="grid-content bg-purple-light"><el-main>{{ activity.name }}</el-main></div>
      </el-col>
      <el-col :span="4">
        <div class="grid-content bg-purple"><el-main class="el-icon-location"> 地点</el-main></div>
      </el-col>
      <el-col :span="4">
        <div class="grid-content bg-purple-light"><el-main>{{ activity.place }}</el-main></div>
      </el-col>
      <el-col :span="4">
        <div class="grid-content bg-purple"><el-main class="el-icon-s-custom"> 组织者</el-main></div>
      </el-col>
      <el-col :span="4">
        <div class="grid-content bg-purple-light"><el-main>{{ activity.organizer }}</el-main></div>
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="4">
        <div class="grid-content bg-purple"><el-main class="el-icon-time"> 申请时间</el-main></div>
      </el-col>
      <el-col :span="4">
        <div class="grid-content bg-purple-light"><el-main>{{ formatDate(activity.time) }}</el-main></div>
      </el-col>
      <el-col :span="4">
        <div class="grid-content bg-purple"><el-main class="el-icon-set-up"> 事项安排</el-main></div>
      </el-col>
      <el-col :span="4">
        <div class="grid-content bg-purple-light"><el-main>{{ activity.arrange }}</el-main></div>
      </el-col>
      <el-col :span="4">
        <div class="grid-content bg-purple"><el-main class="el-icon-open"> 活动状态</el-main></div>
      </el-col>
      <el-col :span="4">
        <div class="grid-content bg-purple-light success-text" v-if="activity.isAccepted === true"><el-main>已批准</el-main>
        </div>
        <div class="grid-content bg-purple-light fail-text" v-else-if="activity.isAccepted === false">
          <el-main>已拒绝</el-main></div>
        <div class="grid-content bg-purple-light pending-text" v-else><el-main>待审核</el-main></div>
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="4">
        <div class="grid-content bg-purple"><el-main class="el-icon-notebook-2
"> 描述</el-main></div>
      </el-col>
      <el-col :span="20">
        <div class="grid-content bg-purple-light"><el-main>{{ activity.description }}</el-main></div>
      </el-col>
    </el-row>
    <div class="button-container">
      <el-form>
        <el-button v-if="activity.attachment !== null" type="primary" class="el-icon-folder edit-button left-button"
          @click="openAttachment()">查看附件</el-button>
        <el-button v-else type="primary" class="el-icon-folder-delete left-button" disabled> 查看附件</el-button>
        <el-button v-if="activity.image !== null" type="primary" class="el-icon-picture edit-button left-button"
          @click="openImage()">查看图片</el-button>
        <el-button v-else type="primary" class="el-icon-picture-outline left-button" disabled> 查看图片</el-button>
      </el-form>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import shared from '@baituan/frontend-shared'

export default {
  name: 'ActivityList',
  computed: {
    ...mapGetters([
      'name'
    ])
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      activity: []
    }
  },
  // 组件创建后加载页面初始数据。
  created: function () {
    this.activity = this.$route.params.activity // 获取路由参数activity
    if(!this.$store.getters.isClubSelected) {
      if(confirm("登陆身份失效，请重新登陆"))
      {
        this.reload()
      }
      else
      {
        window.alert("取消也得重新登陆")
        this.reload()
      }
    }
  },
  methods: {
    // 格式化展示文本，便于页面直接显示。
    formatDate(value) {
      return shared.formatDateTime(value)
    },
    // 打开详情、预览或外部资源。
    openAttachment() {
      window.open(activity.attachment)
    },
    // 打开详情、预览或外部资源。
    openImage() {
      window.open(activity.image)
    },
    // 重新加载当前页面数据，保持界面内容最新。
    async reload() {
      await this.$store.dispatch('user/logout')
      this.$router.push(`/login?redirect=${this.$route.fullPath}`)
    },
    // submitAccept() {
    //   this.$message({
    //     message: '批准申请',
    //     type: 'success'
    //   })
    //   setTimeout(() => {
    //     acceptActivity(activity.id)
    //       .then((response) => {
    //       })
    //       .catch((error) => {
    //         console.error(error)
    //       })
    //   }, 100) // 延迟
    // },
    // submitReject() {
    //   this.$message({
    //     message: '拒绝申请',
    //     type: 'success'
    //   })
    //   setTimeout(() => {
    //     rejectActivity(activity.id)
    //       .then((response) => {
    //       })
    //       .catch((error) => {
    //         console.error(error)
    //       })
    //   }, 100)
    // }
  }
}
</script>

<style scoped>
.el-row {
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
}

.el-col {
  border-radius: 4px;
}

.bg-purple-dark {
  background: #99a9bf;
}

.bg-purple {
  background: #d3dce6;
}

.bg-purple-light {
  background: #e5e9f2;
}

.grid-content {
  border-radius: 4px;
  min-height: 36px;
}

.row-bg {
  padding: 10px 0;
  background-color: #f9fafc;
}

.button-container {
  text-align: center;
  margin-top: 50px;
}

.edit-button {
  margin-left: 30px;
}

.left-button {
  margin-right: 120px;
}

.success-text {
  color: #4caf50;
}

.fail-text {
  color: #f44336;
}

.pending-text {
  color: #ff9800;
}
</style>

