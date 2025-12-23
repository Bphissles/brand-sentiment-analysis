/**
 * Composable to check health of backend services
 */
export const useServiceHealth = () => {
  const config = useRuntimeConfig()
  const baseUrl = config.public.apiUrl || 'http://localhost:8080'

  const isReady = ref(false)
  const backendReady = ref(false)
  const mlReady = ref(false)
  const error = ref<string | null>(null)

  /**
   * Check if backend API is healthy
   */
  const checkBackendHealth = async (): Promise<boolean> => {
    try {
      const response = await $fetch<any>(`${baseUrl}/api/health`, {
        timeout: 5000
      })
      return response.status === 'healthy' || response.status === 'ok'
    } catch (e) {
      return false
    }
  }

  /**
   * Wait for services to be ready
   */
  const waitForServices = async (maxAttempts = 30, delayMs = 1000) => {
    let attempts = 0
    
    while (attempts < maxAttempts) {
      try {
        // Check backend health
        const response = await $fetch<any>(`${baseUrl}/api/health`, {
          timeout: 5000
        })
        
        backendReady.value = response.status === 'healthy' || response.status === 'ok'
        mlReady.value = response.mlEngineStatus === 'healthy'
        
        // Both services ready
        if (backendReady.value && mlReady.value) {
          isReady.value = true
          return true
        }
        
        // Backend ready but ML engine not yet
        if (backendReady.value && !mlReady.value) {
          // Continue waiting for ML engine
        }
        
      } catch (e) {
        // Services not ready yet, continue waiting
      }
      
      attempts++
      await new Promise(resolve => setTimeout(resolve, delayMs))
    }
    
    // Timeout - proceed anyway but set error
    if (!backendReady.value) {
      error.value = 'Backend API is not responding. Please check if services are running.'
    } else if (!mlReady.value) {
      error.value = 'ML Engine is not responding. Some features may be limited.'
      // Allow proceeding if backend is ready
      isReady.value = true
      return true
    }
    
    return false
  }

  return {
    isReady: readonly(isReady),
    backendReady: readonly(backendReady),
    mlReady: readonly(mlReady),
    error: readonly(error),
    checkBackendHealth,
    waitForServices
  }
}
