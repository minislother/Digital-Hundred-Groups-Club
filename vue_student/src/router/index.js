import { createRouter, createWebHashHistory } from 'vue-router'

/* Layout */
import Layout from '@/layout'

/**
 * Note: sub-menu only appear when route children.length >= 1
 *
 * hidden: true                   if set true, item will not show in the sidebar(default is false)
 * alwaysShow: true               if set true, will always show the root menu
 *                                if not set alwaysShow, when item has more than one children route,
 *                                it will becomes nested mode, otherwise not show the root menu
 * redirect: noRedirect           if set noRedirect will no redirect in the breadcrumb
 * name:'router-name'             the name is used by <keep-alive> (must set!!!)
 * meta : {
    roles: ['admin','editor']    control the page roles (you can set multiple roles)
    title: 'title'               the name show in sidebar and breadcrumb (recommend set)
    icon: 'svg-name'/'el-icon-x' the icon show in the sidebar
    breadcrumb: false            if set false, the item will hidden in breadcrumb(default is true)
    activeMenu: '/example/list'  if set path, the sidebar will highlight the path you set
  }
 */

/**
 * constantRoutes
 * a base page that does not have permission requirements
 * all roles can be accessed
 */
export const constantRoutes = [{
  path: '/login',
  component: () =>
    import ('@/views/login/index'),
  hidden: true
},

{
  path: '/register',
  component: () =>
    import ('@/views/register/index')
},

{
  path: '/404',
  component: () =>
    import ('@/views/404'),
  hidden: true
},

{
  path: '/',
  component: Layout,
  redirect: '/dashboard',
  children: [{
    path: 'dashboard',
    name: 'Dashboard',
    component: () =>
      import ('@/views/dashboard/index'),
    meta: { title: '社团广场', icon: 'dashboard' }
  }]
},

{
  path: '/',
  component: Layout,
  redirect: '/application',
  children: [{
    path: 'application/:groupName',
    name: 'Application',
    component: () =>
      import ('@/views/application/index')
  }]
},

{
  path: '/',
  component: Layout,
  redirect: '/showgroupdetails',
  children: [{
    path: 'showgroupdetails/:groupId',
    name: 'Showgroupdetails',
    component: () =>
      import ('@/views/showgroupdetails/index')
  }]
},

{
  path: '/',
  component: Layout,
  redirect: '/activityapplication',
  children: [{
    path: 'activityapplication/:activityname',
    name: 'ActivityApplication',
    component: () =>
      import ('@/views/activityapplication/index')
  }]
},

{
  path: '/myapplication',
  component: Layout,
  redirect: '/myapplication',
  children: [{
    path: 'myapplication',
    name: 'Myapplication',
    component: () =>
      import ('@/views/myapplication/index'),
    meta: { title: '我的申请', icon: 'table' }
  }]
},

{
  path: '/top',
  component: Layout,
  redirect: '/top',
  children: [{
    path: 'top',
    name: 'Top',
    component: () =>
      import ('@/views/top/index'),
    meta: { title: '社团热度榜', icon: 'form' }
  }]
},

{
  path: '/profile',
  component: Layout,
  redirect: '/profile',
  children: [{
    path: 'profile',
    name: 'Profile',
    component: () =>
      import ('@/views/profile/index'),
    meta: { title: '个人信息', icon: 'el-icon-s-custom' }
  }]
},

{
  path: '/activity',
  component: Layout,
  redirect: '/activity',
  children: [{
    path: 'activity',
    name: 'activity',
    component: () =>
      import ('@/views/activity/index'),
    meta: { title: '活动中心', icon: 'el-icon-date' }
  }]
},

{
  path: '/mygroup',
  component: Layout,
  redirect: '/mygroup',
  children: [{
    path: 'mygroup',
    name: 'mygroup',
    component: () =>
      import ('@/views/mygroup/index'),
    meta: { title: '我的社团', icon: 'el-icon-basketball' }
  }]
},

// 404 page must be placed at the end !!!
{ path: '/:pathMatch(.*)*', redirect: '/404', hidden: true }
]

// 处理当前组件的业务逻辑或用户交互。
const buildRouter = () => createRouter({
  // mode: 'history', // require service support
  history: createWebHashHistory(),
  // 处理当前组件的业务逻辑或用户交互。
  scrollBehavior: () => ({ top: 0 }),
  routes: constantRoutes
})

const router = buildRouter()
router.options = { routes: constantRoutes }

// 重置表单、状态或本地缓存。
export function resetRouter() {
  // Routes are static in this app. Keep the exported API for existing callers.
}

export default router
