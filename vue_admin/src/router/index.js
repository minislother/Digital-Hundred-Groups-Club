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
        name: 'Login',
        component: () =>
            import ('@/views/login/index'),
        hidden: true
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
        redirect: '/seeapplication',
        children: [{
            path: 'seeapplication',
            name: 'Seeapplication',
            component: () =>
                import ('@/views/seeapplication/index'),
            meta: { title: '入团申请', icon: 'form' }
        }]
    },

    {
        path: '/',
        component: Layout,
        redirect: '/manage',
        children: [{
            path: 'manage',
            name: 'Manage',
            component: () =>
                import ('@/views/manage/index'),
            // meta: { title: '我的社团', icon: 'table' }
        }]
    },

    {
        path: '/',
        component: Layout,
        redirect: '/appdetail/:id',
        children: [{
          path: 'appdetail/:id',
          name: 'Appdetail',
          component: () =>
            import('@/views/appdetail/index'),
        //   meta: { title: 'appdetail', icon: 'table' }
        }]
    },

    {
        path: '/',
        component: Layout,
        redirect: '/student',
        children: [{
          path: 'student',
          name: 'Student',
          component: () =>
            import('@/views/student/index'),
           meta: { title: '社团成员', icon: 'table' }
        }]
    },

    {
        path: '/',
        component: Layout,
        redirect: '/studentdetail/:id',
        children: [{
          path: 'studentdetail/:id',
          name: 'Studentdetail',
          component: () =>
            import('@/views/studentdetail/index'),
        //   meta: { title: 'studentdetail', icon: 'table' }
        }]
    },

    {
        path: '/',
        component: Layout,
        redirect: '/groupdetail',
        children: [{
            path: 'groupdetail',
            name: 'Groupdetail',
            component: () =>
                import ('@/views/groupdetail/index'),
            meta: { title: '社团资料', icon: 'table' }
        }]
    },


    {
        path: '/',
        component: Layout,
        redirect: '/groupactivity',
        children: [{
            path: 'groupactivity',
            name: 'groupactivity',
            component: () =>
                import ('@/views/groupactivity/index'),
            meta: { title: '社团活动', icon: 'el-icon-s-flag' }
        }]
    },

    {
        path: '/',
        component: Layout,
        redirect: '/groupactivity/activitydetail/:aid',
        children: [{
          path: 'groupactivity/activitydetail/:aid',
          name: 'Activitydetail',
          component: () =>
            import('@/views/groupactivity/activitydetail/index'),
        }]
    },

    {
        path: '/',
        component: Layout,
        redirect: '/groupactivity/addactivity',
        children: [{
          path: 'groupactivity/addactivity',
          name: 'Addactivity',
          component: () =>
            import('@/views/groupactivity/addactivity/index'),
            meta: { title: '发布活动申请', icon: 'el-icon-plus' }
        }]
    },

    {
        path: '/',
        component: Layout,
        redirect: '/groupactivity/pendingctivity',
        children: [{
          path: 'groupactivity/pendingctivity',
          name: 'PendingActivity',
          component: () =>
            import('@/views/groupactivity/pendingctivity/index'),
            meta: { title: '活动变更', icon: 'el-icon-s-check' }
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
