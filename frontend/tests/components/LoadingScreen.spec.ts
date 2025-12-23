import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import LoadingScreen from '~/components/LoadingScreen.vue'

describe('LoadingScreen', () => {
  it('should render with default message', () => {
    const wrapper = mount(LoadingScreen)
    
    expect(wrapper.text()).toContain('Peterbilt Sentiment Analyzer')
    expect(wrapper.text()).toContain('Initializing services...')
  })

  it('should render with custom message', () => {
    const wrapper = mount(LoadingScreen, {
      props: {
        message: 'Loading data...'
      }
    })
    
    expect(wrapper.text()).toContain('Loading data...')
  })

  it('should show service status when enabled', () => {
    const wrapper = mount(LoadingScreen, {
      props: {
        showStatus: true,
        backendReady: true,
        mlReady: false
      }
    })
    
    expect(wrapper.text()).toContain('Backend API')
    expect(wrapper.text()).toContain('ML Engine')
  })

  it('should not show service status by default', () => {
    const wrapper = mount(LoadingScreen, {
      props: {
        showStatus: false
      }
    })
    
    expect(wrapper.text()).not.toContain('Backend API')
  })

  it('should display correct status indicators', () => {
    const wrapper = mount(LoadingScreen, {
      props: {
        showStatus: true,
        backendReady: true,
        mlReady: false
      }
    })
    
    const statusDivs = wrapper.findAll('[class*="rounded-full"]')
    expect(statusDivs.length).toBeGreaterThan(0)
  })
})
