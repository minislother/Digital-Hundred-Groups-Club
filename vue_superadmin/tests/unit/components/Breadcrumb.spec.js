import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import Breadcrumb from '@/components/Breadcrumb/index.vue'

it('导航变化后展示标题并排除无标题路由', async () => {
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/dashboard', name: 'dashboard', component: { template: '<div />' }, meta: { title: '首页' } },
    { path: '/group', component: { template: '<router-view />' }, meta: { title: '社团' }, children: [
      { path: 'detail', component: { template: '<div />' }, meta: { title: '详情' } },
      { path: 'hidden', component: { template: '<div />' } }
    ] }
  ] })
  await router.push('/dashboard')
  await router.isReady()
  const wrapper = mount(Breadcrumb, { global: { plugins: [router, ElementPlus] } })
  expect(wrapper.findAll('.el-breadcrumb__inner')).toHaveLength(1)
  await router.push('/group/detail'); await flushPromises()
  expect(wrapper.findAll('.el-breadcrumb__inner')).toHaveLength(3)
  const last = wrapper.findAll('.el-breadcrumb__inner').at(-1)
  expect(last.text()).toBe('详情'); expect(last.find('a').exists()).toBe(false)
  await router.push('/group/hidden'); await flushPromises()
  expect(wrapper.findAll('.el-breadcrumb__inner')).toHaveLength(2)
  wrapper.unmount()
})
