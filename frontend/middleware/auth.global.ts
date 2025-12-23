/**
 * Global auth middleware - automatically protects all routes
 * 
 * Runs on every route navigation to ensure authentication
 */
export default defineNuxtRouteMiddleware((to, from) => {
  // Skip middleware on server-side (auth state is client-only with localStorage)
  if (process.server) return

  // Public routes that don't require authentication
  const publicRoutes = ['/login', '/register']
  
  // Check if current route is public
  const isPublicRoute = publicRoutes.some(route => to.path === route || to.path.startsWith(route + '/'))
  
  // Get auth state from useState
  const authState = useState<{ isAuthenticated: boolean; token: string | null }>('auth')
  
  // Check localStorage as fallback (handles page refresh before state is hydrated)
  let isAuthenticated = authState.value?.isAuthenticated || false
  
  if (!isAuthenticated && typeof localStorage !== 'undefined') {
    const savedToken = localStorage.getItem('authToken')
    if (savedToken) {
      isAuthenticated = true
    }
  }

  // Handle public routes
  if (isPublicRoute) {
    // Redirect authenticated users away from login page
    if (isAuthenticated && to.path === '/login') {
      return navigateTo('/')
    }
    // Allow access to public routes
    return
  }

  // Protected route - redirect to login if not authenticated
  if (!isAuthenticated) {
    // Store the intended destination for redirect after login
    if (process.client) {
      sessionStorage.setItem('redirectAfterLogin', to.fullPath)
    }
    return navigateTo('/login')
  }
})
