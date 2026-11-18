<template>
  <div class="student-dashboard app-container">
    <section class="hero-panel">
      <div class="hero-copy">
        <p class="eyebrow">数字百团</p>
        <h1>发现适合你的校园社团</h1>
        <typed-text
          class="hero-typed"
          :strings="['丰富活动等你参与', '海量社团等你选择', '欢迎登录中山大学社团招新系统']"
        />
      </div>
      <div class="hero-actions">
        <div class="search-row">
          <el-input
            v-model="searchInfo"
            placeholder="搜索社团名称"
            clearable
            @keyup.enter="currentPage = 1; handleSearch()"
          />
          <el-button type="primary" icon="el-icon-search" @click="currentPage = 1; handleSearch()">搜索</el-button>
        </div>
        <div class="tool-row">
          <el-button plain @click="showVideoDialog">播放社团宣传视频</el-button>
          <el-popover
            placement="bottom"
            width="240"
            trigger="click"
            :content="weatherText"
          >
            <template #reference>
              <el-button plain>查看天气</el-button>
            </template>
          </el-popover>
          <el-button
            plain
            :icon="carouselVisible ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"
            @click="carouselVisible = !carouselVisible"
          >
            {{ carouselVisible ? '收起轮播' : '展开轮播' }}
          </el-button>
        </div>
      </div>
    </section>

    <section v-if="carouselVisible" class="carousel-panel">
      <el-carousel height="360px">
        <el-carousel-item v-for="item in imagebox" :key="item.id">
          <a :href="getWebsiteURL(item.id)" target="_blank" rel="noopener noreferrer">
            <img :src="item.idView" :alt="`校园专题轮播图 ${item.id + 1}`" loading="lazy" class="carousel-image">
          </a>
        </el-carousel-item>
      </el-carousel>
    </section>

    <section class="section-heading">
      <div>
        <p class="eyebrow">Club Square</p>
        <h2>社团广场</h2>
      </div>
      <span>共 {{ totalItems }} 个社团</span>
    </section>

    <el-row :gutter="20" class="group-grid">
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
            <div class="group-buttons">
              <el-button type="primary" size="small" @click="redirectToApplication(group.name)">申请加入</el-button>
              <el-button size="small" @click="showDescription(group)">查看详情</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="videoDialogVisible" width="720px" @close="handleVideoDialogClose">
      <div class="video-container">
        <video ref="videoPlayer" controls>
          <source :src="videoUrl" type="video/mp4">
          Your browser does not support the video tag.
        </video>
      </div>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="社团详情" width="520px">
      <p class="dialog-description">{{ currentGroup.description }}</p>
      <template #footer>
        <el-button
          v-if="currentGroup.attachment !== null"
          type="primary"
          @click="downloadAttachment(currentGroup.id)"
        >
          下载社团详情附件
        </el-button>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <div class="pagination-row">
      <el-pagination
        :current-page="currentPage"
        :page-sizes="[4, 8, 16, 32]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="totalItems"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { getGroups, getVideo, getWeather, getAttachmentUrl } from '@/api/group.js'
import TypedText from '@/components/TypedText'

export default {
  name: 'Dashboard',
  components: {
    TypedText
  },
  computed: {
    ...mapGetters([
      'name'
    ]),
    // 处理当前组件的业务逻辑或用户交互。
    weatherText() {
      if (!this.weather) {
        return '暂无天气信息'
      }
      return `天气：${this.weather.weather || '-'}   温度：${this.weather.temperature || '-'}℃   ${this.weather.remark || ''}`
    }
  },
  // 提供组件初始响应式数据。
  data() {
    return {
      totalItems: 0,
      pageSize: 32,
      currentPage: 1,
      groups: [],
      weather: '',
      dialogVisible: false,
      currentGroup: {
        description: '',
        attachment: null
      },
      searchInfo: '',
      videoDialogVisible: false,
      videoUrl: '',
      imagebox: [
        { id: 0, idView: new URL('../../assets/caraousel/走马灯_05.webp', import.meta.url).href },
        { id: 1, idView: new URL('../../assets/caraousel/走马灯_06.webp', import.meta.url).href },
        { id: 2, idView: new URL('../../assets/caraousel/走马灯_07.webp', import.meta.url).href },
        { id: 3, idView: new URL('../../assets/caraousel/走马灯_08.webp', import.meta.url).href }
      ],
      carouselVisible: true
    }
  },
  // 组件创建后加载页面初始数据。
  created() {
    this.searchInfo = this.$route.query.searchInfo
    this.handleSearch()
    this.getWeather()
  },
  methods: {
    // 读取并返回业务数据或页面状态。
    getWebsiteURL(id) {
      if (id === 0) {
        return 'https://ztjy.sysu.edu.cn/'
      }
      if (id === 1) {
        return 'https://www.sysu.edu.cn/news/ztjj2/gdzt/zt2023ncjgzhy.htm'
      }
      if (id === 2) {
        return 'https://www.sysu.edu.cn/xxg/zdjj1.htm'
      }
      return 'https://ddhzt.sysu.edu.cn/'
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleSizeChange(val) {
      this.pageSize = val
      this.handleSearch()
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleCurrentChange(val) {
      this.currentPage = val
      this.handleSearch()
    },
    // 展示弹窗、详情或提示内容。
    showDescription(group) {
      this.currentGroup = group
      this.dialogVisible = true
    },
    // 展示弹窗、详情或提示内容。
    showVideoDialog() {
      getVideo()
        .then(response => {
          this.videoUrl = response.data.url
          this.videoDialogVisible = true
          this.$nextTick(() => {
            const videoElement = this.$refs.videoPlayer
            if (videoElement) {
              videoElement.play()
            }
          })
        })
        .catch(error => {
          console.error('获取视频失败:', error)
        })
    },
    // 处理用户操作或组件事件，连接界面交互和业务请求。
    handleVideoDialogClose() {
      const videoElement = this.$refs.videoPlayer
      if (videoElement) {
        videoElement.pause()
        videoElement.currentTime = 0
      }
      this.videoDialogVisible = false
    },
    // 处理当前组件的业务逻辑或用户交互。
    redirectToApplication(groupName) {
      this.$router.push({
        name: 'Application',
        params: {
          groupName: groupName
        }
      })
    },
    // 下载后端提供的附件或模板文件。
    async downloadAttachment(groupId) {
      try {
        const response = await getAttachmentUrl(groupId)
        const attachment = response.data?.attachment || response.attachment
        if (attachment) {
          window.open(attachment, '_blank')
        } else {
          this.$message.error('无法获取附件 URL')
        }
      } catch (error) {
        console.error(error)
        this.$message.error('下载附件时发生错误')
      }
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
    },
    // 读取并返回业务数据或页面状态。
    getWeather() {
      getWeather()
        .then((response) => {
          this.weather = response.data
        })
        .catch((error) => {
          console.error(error)
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.student-dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-panel,
.carousel-panel {
  border: 1px solid var(--bt-border-muted);
  border-radius: 8px;
  background: #fff;
  box-shadow: var(--bt-shadow);
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 520px);
  gap: 24px;
  padding: 24px;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--bt-primary);
  font-size: 13px;
  font-weight: 700;
}

.hero-copy h1,
.section-heading h2 {
  margin: 0;
  color: var(--bt-text);
  font-weight: 700;
}

.hero-copy h1 {
  font-size: 30px;
  line-height: 1.25;
}

.hero-typed {
  display: block;
  margin-top: 14px;
  color: var(--bt-text-muted);
  font-size: 16px;
}

.hero-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search-row,
.tool-row {
  display: flex;
  gap: 10px;
}

.search-row .el-input {
  flex: 1;
}

.tool-row {
  flex-wrap: wrap;
}

.carousel-panel {
  overflow: hidden;
}

.carousel-image {
  width: 100%;
  height: 360px;
  object-fit: cover;
  display: block;
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-top: 4px;
}

.section-heading span {
  color: var(--bt-text-muted);
  font-size: 14px;
}

.group-grid {
  margin-top: -4px;
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
  padding: 14px;
}

.group-name {
  min-height: 24px;
  color: var(--bt-text);
  font-size: 16px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-buttons {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.group-buttons .el-button {
  flex: 1;
  margin-left: 0;
}

.video-container video {
  width: 100%;
  max-height: 70vh;
  border-radius: 8px;
  background: #000;
}

.dialog-description {
  margin: 0;
  color: var(--bt-text);
  font-size: 15px;
  line-height: 1.7;
}

.pagination-row {
  display: flex;
  justify-content: center;
  padding: 4px 0 24px;
}

@media (max-width: 900px) {
  .hero-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .search-row,
  .tool-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
