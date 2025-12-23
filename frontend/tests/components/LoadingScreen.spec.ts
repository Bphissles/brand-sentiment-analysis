import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import LoadingScreen from '../../components/LoadingScreen.vue'

describe('LoadingScreen', () => {
  it('should render component', () => {
    const wrapper = mount(LoadingScreen)
    expect(wrapper.exists()).toBe(true)
  })

  it('should display default message', () => {
    const wrapper = mount(LoadingScreen)
    const text = wrapper.text()
    expect(text).toContain('Initializing')
  })

  it('should accept custom message prop', () => {
    const wrapper = mount(LoadingScreen, {
      props: {
        message: 'Custom Loading'
      }
    })
    expect(wrapper.text()).toContain('Custom Loading')
  })

  it('should show backend status when showStatus is true', () => {
    const wrapper = mount(LoadingScreen, {
      props: {
        showStatus: true,
        backendReady: true
      }
    })
    expect(wrapper.html()).toContain('Backend')
  })

  it('should show ML engine status when showStatus is true', () => {
    const wrapper = mount(LoadingScreen, {
      props: {
        showStatus: true,
        mlReady: true
      }
    })
    expect(wrapper.html()).toContain('ML Engine')
  })
})
