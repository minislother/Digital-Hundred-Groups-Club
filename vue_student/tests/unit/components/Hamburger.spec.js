import { shallowMount } from '@vue/test-utils'
import Hamburger from '@/components/Hamburger/index.vue'

describe('Hamburger.vue', () => {
  it('点击按钮发出切换事件', async () => {
    const wrapper = shallowMount(Hamburger)
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('toggleClick')).toHaveLength(1)
    wrapper.unmount()
  })
  it('激活状态同步到图标和无障碍属性', async () => {
    const wrapper = shallowMount(Hamburger)
    await wrapper.setProps({ isActive: true })
    expect(wrapper.find('.is-active').exists()).toBe(true)
    expect(wrapper.find('button').attributes('aria-pressed')).toBe('true')
    await wrapper.setProps({ isActive: false })
    expect(wrapper.find('.is-active').exists()).toBe(false)
    wrapper.unmount()
  })
})
