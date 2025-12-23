import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useAuth } from '~/composables/useAuth'

describe('useAuth', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock localStorage
    global.localStorage = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn(),
      length: 0,
      key: vi.fn()
    } as any
    
    // Mock process.client
    global.process = { client: true } as any
  })

  it('should initialize with unauthenticated state', () => {
    const { isAuthenticated, user, token } = useAuth()
    
    expect(isAuthenticated.value).toBe(false)
    expect(user.value).toBeNull()
    expect(token.value).toBeNull()
  })

  it('should login successfully with valid credentials', async () => {
    const mockResponse = {
      success: true,
      token: 'mock-jwt-token',
      user: {
        id: 1,
        email: 'test@example.com',
        role: 'viewer'
      }
    }
    
    global.$fetch = vi.fn().mockResolvedValue(mockResponse)
    
    const { login, isAuthenticated, user, token } = useAuth()
    const result = await login('test@example.com', 'password')
    
    expect(result.success).toBe(true)
    expect(isAuthenticated.value).toBe(true)
    expect(user.value?.email).toBe('test@example.com')
    expect(token.value).toBe('mock-jwt-token')
    expect(localStorage.setItem).toHaveBeenCalledWith('authToken', 'mock-jwt-token')
  })

  it('should handle login failure', async () => {
    global.$fetch = vi.fn().mockRejectedValue({
      data: { error: 'Invalid credentials' }
    })
    
    const { login, isAuthenticated } = useAuth()
    const result = await login('test@example.com', 'wrongpassword')
    
    expect(result.success).toBe(false)
    expect(result.error).toBe('Invalid credentials')
    expect(isAuthenticated.value).toBe(false)
  })

  it('should register new user successfully', async () => {
    const mockResponse = {
      success: true,
      token: 'mock-jwt-token',
      user: {
        id: 1,
        email: 'newuser@example.com',
        role: 'viewer'
      }
    }
    
    global.$fetch = vi.fn().mockResolvedValue(mockResponse)
    
    const { register, isAuthenticated } = useAuth()
    const result = await register('newuser@example.com', 'password')
    
    expect(result.success).toBe(true)
    expect(isAuthenticated.value).toBe(true)
  })

  it('should logout and clear state', () => {
    const { logout, isAuthenticated, user, token } = useAuth()
    
    // Manually set authenticated state
    const auth = useAuth()
    auth.authState.value = {
      user: { id: 1, email: 'test@example.com', role: 'viewer' },
      token: 'mock-token',
      isAuthenticated: true
    }
    
    logout()
    
    expect(isAuthenticated.value).toBe(false)
    expect(user.value).toBeNull()
    expect(token.value).toBeNull()
    expect(localStorage.removeItem).toHaveBeenCalledWith('authToken')
    expect(localStorage.removeItem).toHaveBeenCalledWith('authUser')
  })

  it('should return auth header when authenticated', () => {
    const { getAuthHeader } = useAuth()
    const auth = useAuth()
    
    auth.authState.value = {
      user: { id: 1, email: 'test@example.com', role: 'viewer' },
      token: 'mock-token',
      isAuthenticated: true
    }
    
    const header = getAuthHeader()
    
    expect(header).toEqual({ Authorization: 'Bearer mock-token' })
  })

  it('should return empty object when not authenticated', () => {
    const { getAuthHeader } = useAuth()
    
    const header = getAuthHeader()
    
    expect(header).toEqual({})
  })

  it('should initialize from localStorage', () => {
    const mockUser = { id: 1, email: 'test@example.com', role: 'viewer' }
    
    global.localStorage.getItem = vi.fn((key: string) => {
      if (key === 'authToken') return 'saved-token'
      if (key === 'authUser') return JSON.stringify(mockUser)
      return null
    })
    
    const { initAuth, isAuthenticated, user, token } = useAuth()
    initAuth()
    
    expect(isAuthenticated.value).toBe(true)
    expect(user.value?.email).toBe('test@example.com')
    expect(token.value).toBe('saved-token')
  })
})
