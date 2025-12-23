/**
 * Composable for authentication state and operations
 */

interface User {
  id: number
  email: string
  role: 'admin' | 'viewer'
  lastLoginAt?: string
}

interface AuthState {
  user: User | null
  token: string | null
  isAuthenticated: boolean
}

export const useAuth = () => {
  const config = useRuntimeConfig()
  const baseUrl = config.public.apiUrl || 'http://localhost:8080'

  // Global auth state
  const authState = useState<AuthState>('auth', () => ({
    user: null,
    token: null,
    isAuthenticated: false
  }))

  /**
   * Initialize auth state from localStorage
   */
  const initAuth = () => {
    if (process.client) {
      const savedToken = localStorage.getItem('authToken')
      const savedUser = localStorage.getItem('authUser')
      
      if (savedToken && savedUser) {
        authState.value = {
          token: savedToken,
          user: JSON.parse(savedUser),
          isAuthenticated: true
        }
      }
    }
  }

  /**
   * Login with email and password
   */
  const login = async (email: string, password: string): Promise<{ success: boolean; error?: string }> => {
    try {
      const response = await $fetch<{ success: boolean; token: string; user: User }>(`${baseUrl}/api/auth/login`, {
        method: 'POST',
        body: { email, password }
      })

      if (response.success && response.token) {
        authState.value = {
          token: response.token,
          user: response.user,
          isAuthenticated: true
        }

        // Persist to localStorage
        if (process.client) {
          localStorage.setItem('authToken', response.token)
          localStorage.setItem('authUser', JSON.stringify(response.user))
        }

        return { success: true }
      }

      return { success: false, error: 'Login failed' }
    } catch (e: any) {
      return { success: false, error: e.data?.error || 'Invalid credentials' }
    }
  }

  /**
   * Register new user
   */
  const register = async (email: string, password: string): Promise<{ success: boolean; error?: string }> => {
    try {
      const response = await $fetch<{ success: boolean; token: string; user: User }>(`${baseUrl}/api/auth/register`, {
        method: 'POST',
        body: { email, password, role: 'viewer' }
      })

      if (response.success && response.token) {
        authState.value = {
          token: response.token,
          user: response.user,
          isAuthenticated: true
        }

        // Persist to localStorage
        if (process.client) {
          localStorage.setItem('authToken', response.token)
          localStorage.setItem('authUser', JSON.stringify(response.user))
        }

        return { success: true }
      }

      return { success: false, error: 'Registration failed' }
    } catch (e: any) {
      return { success: false, error: e.data?.error || 'Registration failed' }
    }
  }

  /**
   * Logout and clear state
   */
  const logout = () => {
    authState.value = {
      user: null,
      token: null,
      isAuthenticated: false
    }

    if (process.client) {
      localStorage.removeItem('authToken')
      localStorage.removeItem('authUser')
    }
  }

  /**
   * Get current user from token
   */
  const fetchCurrentUser = async (): Promise<User | null> => {
    if (!authState.value.token) return null

    try {
      const response = await $fetch<{ user: User }>(`${baseUrl}/api/auth/me`, {
        headers: {
          Authorization: `Bearer ${authState.value.token}`
        }
      })

      if (response.user) {
        authState.value.user = response.user
        return response.user
      }

      return null
    } catch (e) {
      // Token invalid, logout
      logout()
      return null
    }
  }

  /**
   * Get auth header for API requests
   */
  const getAuthHeader = (): Record<string, string> => {
    if (authState.value.token) {
      return { Authorization: `Bearer ${authState.value.token}` }
    }
    return {}
  }

  return {
    authState: readonly(authState),
    user: computed(() => authState.value.user),
    isAuthenticated: computed(() => authState.value.isAuthenticated),
    token: computed(() => authState.value.token),
    initAuth,
    login,
    register,
    logout,
    fetchCurrentUser,
    getAuthHeader
  }
}
