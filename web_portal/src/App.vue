<template>
  <main class="portal-shell">
    <section class="hero-panel" aria-labelledby="portal-title">
      <div class="hero-copy">
        <p class="eyebrow">数字百团 Web Portal</p>
        <h1 id="portal-title">请选择你的登录身份</h1>
        <p class="hero-text">
          学生、社团管理员和超级管理员从同一个入口进入，再跳转到各自的网页端登录页。
        </p>
      </div>

      <div class="status-strip" aria-label="系统入口状态">
        <span>统一入口</span>
        <strong>3 个角色</strong>
        <span>Vue 3 Web</span>
      </div>
    </section>

    <section class="role-grid" aria-label="身份选择">
      <article
        v-for="role in roles"
        :key="role.key"
        class="role-card"
        :class="role.accentClass"
      >
        <div class="role-card__top">
          <span class="role-icon" aria-hidden="true">
            <component :is="role.icon" :size="28" :stroke-width="2.2" />
          </span>
          <span class="role-tag">{{ role.tag }}</span>
        </div>

        <h2>{{ role.title }}</h2>
        <p>{{ role.description }}</p>

        <ul class="role-points" :aria-label="`${role.title}常用功能`">
          <li v-for="point in role.points" :key="point">{{ point }}</li>
        </ul>

        <button
          class="enter-button"
          type="button"
          :data-role-target="role.key"
          @click="goToRole(role)"
        >
          <span>进入{{ role.title }}</span>
          <ArrowRight :size="18" :stroke-width="2.4" aria-hidden="true" />
        </button>
      </article>
    </section>
  </main>
</template>

<script>
import {
  ArrowRight,
  GraduationCap,
  ShieldCheck,
  UserCog
} from '@lucide/vue'

const defaultRoleUrls = {
  student: 'http://localhost:9533/#/login',
  admin: 'http://localhost:9531/#/login',
  superadmin: 'http://localhost:9532/#/login'
}

const roleUrls = {
  student: process.env.VUE_APP_STUDENT_URL || defaultRoleUrls.student,
  admin: process.env.VUE_APP_ADMIN_URL || defaultRoleUrls.admin,
  superadmin: process.env.VUE_APP_SUPERADMIN_URL || defaultRoleUrls.superadmin
}

const roles = [
  {
    key: 'student',
    title: '学生端',
    tag: 'Student',
    icon: GraduationCap,
    accentClass: 'role-card--student',
    description: '适合浏览社团、报名活动、提交入团申请和查看个人参与记录。',
    points: ['社团广场', '活动中心', '我的申请'],
    url: roleUrls.student
  },
  {
    key: 'admin',
    title: '管理员端',
    tag: 'Club Admin',
    icon: UserCog,
    accentClass: 'role-card--admin',
    description: '适合社团负责人维护资料、审核申请、管理成员和发布活动。',
    points: ['入团审核', '成员维护', '活动管理'],
    url: roleUrls.admin
  },
  {
    key: 'superadmin',
    title: '超级管理员端',
    tag: 'Super Admin',
    icon: ShieldCheck,
    accentClass: 'role-card--superadmin',
    description: '适合平台管理者处理社团、学生、活动审批和整体数据统计。',
    points: ['平台审核', '批量导入', '数据总览'],
    url: roleUrls.superadmin
  }
]

export default {
  name: 'App',
  components: {
    ArrowRight
  },
  data() {
    return {
      // 三个地址可以通过环境变量覆盖，生产部署时可指向正式域名或 Nginx 子路径。
      roles
    }
  },
  methods: {
    goToRole(role) {
      window.location.assign(role.url)
    }
  }
}
</script>
