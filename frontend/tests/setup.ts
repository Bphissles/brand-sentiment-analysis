import { vi } from 'vitest'
import { ref, computed, readonly, watch } from 'vue'

// Make Vue functions globally available
vi.stubGlobal('ref', ref)
vi.stubGlobal('computed', computed)
vi.stubGlobal('readonly', readonly)
vi.stubGlobal('watch', watch)

// Mock Nuxt runtime config
vi.stubGlobal('useRuntimeConfig', () => ({
  public: {
    apiUrl: 'http://localhost:8080'
  }
}))

// Mock Nuxt useState
vi.stubGlobal('useState', (key: string, init?: any) => {
  return ref(typeof init === 'function' ? init() : init)
})

// Mock Nuxt useRoute
vi.stubGlobal('useRoute', () => ({
  path: '/',
  params: {},
  query: {}
}))

// Mock Nuxt useRouter
vi.stubGlobal('useRouter', () => ({
  push: vi.fn(),
  replace: vi.fn(),
  back: vi.fn()
}))

// Mock Nuxt navigateTo
vi.stubGlobal('navigateTo', vi.fn())

// Mock global $fetch
vi.stubGlobal('$fetch', vi.fn())

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => { store[key] = value },
    removeItem: (key: string) => { delete store[key] },
    clear: () => { store = {} }
  }
})()

Object.defineProperty(global, 'localStorage', {
  value: localStorageMock
})

// Mock process.client for Nuxt SSR checks without clobbering Node's process
if (!(global as any).process) {
  ;(global as any).process = { client: true }
} else {
  ;(global as any).process.client = true
}
