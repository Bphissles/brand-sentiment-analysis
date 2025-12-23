import { vi } from 'vitest'

// Mock Nuxt composables
global.useRuntimeConfig = vi.fn(() => ({
  public: {
    apiUrl: 'http://localhost:8080'
  }
}))

global.useState = vi.fn((key: string, init: any) => {
  const state = ref(typeof init === 'function' ? init() : init)
  return state
})

global.useRoute = vi.fn(() => ({
  params: {},
  query: {},
  path: '/'
}))

global.useRouter = vi.fn(() => ({
  push: vi.fn(),
  replace: vi.fn(),
  back: vi.fn()
}))

global.navigateTo = vi.fn()

// Mock $fetch
global.$fetch = vi.fn()
