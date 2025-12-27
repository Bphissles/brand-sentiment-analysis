import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useServiceHealth } from '../../composables/useServiceHealth'

describe('useServiceHealth', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    global.$fetch = vi.fn()
  })

  describe('initial state', () => {
    it('should start with not ready state', () => {
      const { isReady, backendReady, mlReady, error } = useServiceHealth()
      expect(isReady.value).toBe(false)
      expect(backendReady.value).toBe(false)
      expect(mlReady.value).toBe(false)
      expect(error.value).toBeNull()
    })
  })

  describe('checkBackendHealth', () => {
    it('should return true when backend is healthy', async () => {
      global.$fetch = vi.fn().mockResolvedValue({ status: 'healthy' })

      const { checkBackendHealth } = useServiceHealth()
      const result = await checkBackendHealth()

      expect(result).toBe(true)
    })

    it('should return true when backend status is ok', async () => {
      global.$fetch = vi.fn().mockResolvedValue({ status: 'ok' })

      const { checkBackendHealth } = useServiceHealth()
      const result = await checkBackendHealth()

      expect(result).toBe(true)
    })

    it('should return false when backend is unhealthy', async () => {
      global.$fetch = vi.fn().mockResolvedValue({ status: 'unhealthy' })

      const { checkBackendHealth } = useServiceHealth()
      const result = await checkBackendHealth()

      expect(result).toBe(false)
    })

    it('should return false when fetch fails', async () => {
      global.$fetch = vi.fn().mockRejectedValue(new Error('Network error'))

      const { checkBackendHealth } = useServiceHealth()
      const result = await checkBackendHealth()

      expect(result).toBe(false)
    })
  })

  describe('waitForServices', () => {
    it('should set isReady when both services are healthy', async () => {
      global.$fetch = vi.fn().mockResolvedValue({
        status: 'healthy',
        mlEngineStatus: 'healthy'
      })

      const { waitForServices, isReady, backendReady, mlReady } = useServiceHealth()
      const result = await waitForServices(1, 10)

      expect(result).toBe(true)
      expect(isReady.value).toBe(true)
      expect(backendReady.value).toBe(true)
      expect(mlReady.value).toBe(true)
    })

    it('should allow proceeding when backend ready but ML not ready', async () => {
      let attempts = 0
      global.$fetch = vi.fn().mockImplementation(() => {
        attempts++
        return Promise.resolve({
          status: 'healthy',
          mlEngineStatus: 'unhealthy'
        })
      })

      const { waitForServices, isReady, backendReady, mlReady, error } = useServiceHealth()
      const result = await waitForServices(2, 10)

      expect(result).toBe(true)
      expect(isReady.value).toBe(true)
      expect(backendReady.value).toBe(true)
      expect(mlReady.value).toBe(false)
      expect(error.value).toContain('ML Engine')
    })

    it('should set error when backend not responding', async () => {
      global.$fetch = vi.fn().mockRejectedValue(new Error('Network error'))

      const { waitForServices, isReady, error } = useServiceHealth()
      const result = await waitForServices(2, 10)

      expect(result).toBe(false)
      expect(isReady.value).toBe(false)
      expect(error.value).toContain('Backend API')
    })
  })
})
