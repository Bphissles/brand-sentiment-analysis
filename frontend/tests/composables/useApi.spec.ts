import { describe, it, expect, beforeEach, vi } from 'vitest'

describe('useApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    global.$fetch = vi.fn()
  })

  it('should have correct base URL from config', () => {
    const config = useRuntimeConfig()
    expect(config.public.apiUrl).toBe('http://localhost:8080')
  })

  it('should be able to mock fetch calls', async () => {
    const mockData = { success: true }
    global.$fetch = vi.fn().mockResolvedValue(mockData)
    
    const result = await $fetch('/test')
    expect(result).toEqual(mockData)
    expect(global.$fetch).toHaveBeenCalledWith('/test')
  })
})
