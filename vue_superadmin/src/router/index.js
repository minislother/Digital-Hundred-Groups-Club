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
  path: '/knowledge', component: Layout,
  children: [{ path: '', name: 'ClubKnowledge', component: () => import('@/views/knowledge/index'), meta: { title: '社团知识库', icon: 'documentation' } }]
},

  {
    path: '/login',
    component: () => import('@/views/login/index'),
    hidden: true
  },

  {
    path: '/404',
    component: () => import('@/views/404'),
    hidden: true
  },

  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [{
      path: 'dashboard',
      name: 'Dashboard',
      component: () => import('@/views/dashboard/index'),
      meta: { title: '数据总览', icon: 'dashboard' }
    }]
  },

  {
    path: '/',
    component: Layout,
    redirect: '/stuAndClub/table',
    name: 'StuAndClub',
    meta: { title: '学生与社团管理', icon: 'el-icon-s-help' },
    children: [
      {
        path: 'clubtable',
        name: 'Clubtable',
        component: () => import('@/views/clubtable/index'),
        meta: { title: '社团列表', icon: 'el-icon-folder-checked' }
      },
      // {
      //   path: 'clubapp',
      //   name: 'Clubapp',
      //   component: () => import('@/views/clubapp/index'),
      //   meta: { title: '社团申请列表', icon: 'el-icon-folder-add' }
      // },
      {
        path: 'stutree',
        name: 'Stutree',
        component: () => import('@/views/stutree/index'),
        meta: { title: '学生列表', icon: 'tree' }
      }
    ]
  },

  {
    path: '/activity',
    component: Layout,
    children: [
      {
        path: 'index',
        name: 'Activity',
        component: () => import('@/views/activity/index'),
        meta: { title: '活动审批管理', icon: 'el-icon-s-flag' }
      }
    ]
  },

  {
    path: '/admission-dead-letters',
    component: Layout,
    children: [{
      path: 'index',
      name: 'AdmissionDeadLetters',
      component: () => import('@/views/admission-dead-letters/index'),
      meta: { title: '报名异常补偿', icon: 'el-icon-warning-outline' }
    }]
  },

  {
    path: '/activity',
    component: Layout,
    children: [
      {
        path: 'activitydetail/:aid',
        name: 'Activitydetail',
        component: () => import('@/views/activity/activitydetail')
      }
    ]
  },

  // {
  //   path: '/form',
  //   component: Layout,
  //   children: [
  //     {
  //       path: 'index',
  //       name: 'Form',
  //       component: () => import('@/views/form/index'),
  //       meta: { title: 'Form', icon: 'form' }
  //     }
  //   ]
  // },

  {
    path: '/add',
    component: Layout,
    children: [{
      path: 'index',
      name: 'Add',
      meta: { title: '批量导入', icon: 'nested' },
      component: () => import('@/views/add/index')
    }]
  },

  // {
  //   path: '/nested',
  //   component: Layout,
  //   redirect: '/nested/menu1',
  //   name: 'Nested',
  //   meta: {
  //     title: 'Nested',
  //     icon: 'nested'
  //   },
  //   children: [
  //     {
  //       path: 'menu1',
  //       component: () => import('@/views/nested/menu1/index'), // Parent router-view
  //       name: 'Menu1',
  //       meta: { title: 'Menu1' },
  //       children: [
  //         {
  //           path: 'menu1-1',
  //           component: () => import('@/views/nested/menu1/menu1-1'),
  //           name: 'Menu1-1',
  //           meta: { title: 'Menu1-1' }
  //         },
  //         {
  //           path: 'menu1-2',
  //           component: () => import('@/views/nested/menu1/menu1-2'),
  //           name: 'Menu1-2',
  //           meta: { title: 'Menu1-2' },
  //           children: [
  //             {
  //               path: 'menu1-2-1',
  //               component: () => import('@/views/nested/menu1/menu1-2/menu1-2-1'),
  //               name: 'Menu1-2-1',
  //               meta: { title: 'Menu1-2-1' }
  //             },
  //             {
  //               path: 'menu1-2-2',
  //               component: () => import('@/views/nested/menu1/menu1-2/menu1-2-2'),
  //               name: 'Menu1-2-2',
  //               meta: { title: 'Menu1-2-2' }
  //             }
  //           ]
  //         },
  //         {
  //           path: 'menu1-3',
  //           component: () => import('@/views/nested/menu1/menu1-3'),
  //           name: 'Menu1-3',
  //           meta: { title: 'Menu1-3' }
  //         }
  //       ]
  //     },
  //   ]
  // },

  {
    path: '/external-link',
    component: Layout,
    children: [
      {
        path: 'https://sse.sysu.edu.cn/',
        meta: { title: '软件工程学院主页', icon: 'link' }
      }
    ]
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
