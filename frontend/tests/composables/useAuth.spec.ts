import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useAuth } from '../../composables/useAuth'

describe('useAuth', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    global.$fetch = vi.fn()
  })

  describe('initial state', () => {
    it('should start with unauthenticated state', () => {
      const { isAuthenticated, user, token } = useAuth()
      expect(isAuthenticated.value).toBe(false)
      expect(user.value).toBeNull()
      expect(token.value).toBeNull()
    })
  })

  describe('isTokenExpired', () => {
    it('should return true for malformed tokens', () => {
      const { isTokenExpired } = useAuth()
      expect(isTokenExpired('invalid')).toBe(true)
      expect(isTokenExpired('a.b')).toBe(true)
      expect(isTokenExpired('')).toBe(true)
    })

    it('should return true for expired tokens', () => {
      const { isTokenExpired } = useAuth()
      // Create a token with exp in the past
      const payload = { exp: Math.floor(Date.now() / 1000) - 3600 }
      const token = `header.${btoa(JSON.stringify(payload))}.signature`
      expect(isTokenExpired(token)).toBe(true)
    })

    it('should return false for valid non-expired tokens', () => {
      const { isTokenExpired } = useAuth()
      // Create a token with exp in the future
      const payload = { exp: Math.floor(Date.now() / 1000) + 3600 }
      const token = `header.${btoa(JSON.stringify(payload))}.signature`
      expect(isTokenExpired(token)).toBe(false)
    })
  })

  describe('login', () => {
    it('should set auth state on successful login', async () => {
      const mockUser = { id: 1, email: 'test@example.com', role: 'viewer' as const }
      const mockToken = 'valid.token.here'
      global.$fetch = vi.fn().mockResolvedValue({
        success: true,
        token: mockToken,
        user: mockUser
      })

      const { login, isAuthenticated, user, token } = useAuth()
      const result = await login('test@example.com', 'password')

      expect(result.success).toBe(true)
      expect(isAuthenticated.value).toBe(true)
      expect(user.value).toEqual(mockUser)
      expect(token.value).toBe(mockToken)
    })

    it('should persist token to localStorage on successful login', async () => {
      const mockUser = { id: 1, email: 'test@example.com', role: 'viewer' as const }
      global.$fetch = vi.fn().mockResolvedValue({
        success: true,
        token: 'test-token',
        user: mockUser
      })

      const { login } = useAuth()
      await login('test@example.com', 'password')

      expect(localStorage.getItem('authToken')).toBe('test-token')
      expect(localStorage.getItem('authUser')).toBe(JSON.stringify(mockUser))
    })

    it('should return error on failed login', async () => {
      global.$fetch = vi.fn().mockRejectedValue({
        data: { error: 'Invalid credentials' }
      })

      const { login, isAuthenticated } = useAuth()
      const result = await login('test@example.com', 'wrong')

      expect(result.success).toBe(false)
      expect(result.error).toBe('Invalid credentials')
      expect(isAuthenticated.value).toBe(false)
    })
  })

  describe('logout', () => {
    it('should clear auth state and localStorage', async () => {
      // Setup authenticated state
      localStorage.setItem('authToken', 'test-token')
      localStorage.setItem('authUser', JSON.stringify({ id: 1 }))

      const { logout, isAuthenticated, user, token } = useAuth()
      logout()

      expect(isAuthenticated.value).toBe(false)
      expect(user.value).toBeNull()
      expect(token.value).toBeNull()
      expect(localStorage.getItem('authToken')).toBeNull()
      expect(localStorage.getItem('authUser')).toBeNull()
    })
  })

  describe('getAuthHeader', () => {
    it('should return empty object when not authenticated', () => {
      const { getAuthHeader } = useAuth()
      expect(getAuthHeader()).toEqual({})
    })

    it('should return Authorization header when authenticated', async () => {
      global.$fetch = vi.fn().mockResolvedValue({
        success: true,
        token: 'my-token',
        user: { id: 1, email: 'test@example.com', role: 'viewer' }
      })

      const { login, getAuthHeader } = useAuth()
      await login('test@example.com', 'password')

      expect(getAuthHeader()).toEqual({ Authorization: 'Bearer my-token' })
    })
  })
})
