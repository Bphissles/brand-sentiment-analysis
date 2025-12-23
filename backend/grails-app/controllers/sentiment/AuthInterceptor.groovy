package sentiment

import grails.converters.JSON

/**
 * Interceptor to enforce JWT authentication on protected endpoints
 * Validates JWT tokens and enforces role-based access control
 */
class AuthInterceptor {

    AuthService authService

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_ENDPOINTS = [
        '/api/auth/login',
        '/api/auth/register',
        '/api/auth/promote',  // Has its own auth logic for bootstrap
        '/api/health'
    ]

    // Admin-only endpoints
    private static final List<String> ADMIN_ENDPOINTS = [
        '/api/analysis/clear',
        '/api/analysis/load-fixtures',
        '/api/ingestion/scrapeAll',
        '/api/ingestion/scrape'
    ]

    AuthInterceptor() {
        matchAll()
    }

    boolean before() {
        // Get request path
        def requestPath = request.forwardURI

        // Allow public endpoints
        if (isPublicEndpoint(requestPath)) {
            return true
        }

        // In development mode, allow unauthenticated access with warning
        def environment = grails.util.Environment.current.name
        if (environment == 'DEVELOPMENT') {
            def authHeader = request.getHeader('Authorization')
            if (!authHeader || !authHeader.startsWith('Bearer ')) {
                log.warn("Unauthenticated request to ${requestPath} - allowed in DEVELOPMENT mode")
                // Set default dev user attributes
                request.setAttribute('userEmail', 'dev@localhost')
                request.setAttribute('userRole', 'admin')
                request.setAttribute('userId', 0L)
                return true
            }
        }

        // Extract and validate JWT token
        def authHeader = request.getHeader('Authorization')
        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            render status: 401, text: [error: 'Missing or invalid Authorization header'] as JSON
            return false
        }

        def token = authHeader.substring(7) // Remove "Bearer " prefix
        def claims = authService.validateToken(token)

        if (!claims) {
            render status: 401, text: [error: 'Invalid or expired token'] as JSON
            return false
        }

        // Store user info in request for controllers to access
        request.setAttribute('userEmail', claims.email)
        request.setAttribute('userRole', claims.role)
        request.setAttribute('userId', claims.userId)

        // Check admin-only endpoints
        if (isAdminEndpoint(requestPath) && claims.role != 'admin') {
            render status: 403, text: [error: 'Admin access required'] as JSON
            return false
        }

        return true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }

    /**
     * Check if endpoint is public (no auth required)
     */
    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.any { path.startsWith(it) }
    }

    /**
     * Check if endpoint requires admin role
     */
    private boolean isAdminEndpoint(String path) {
        return ADMIN_ENDPOINTS.any { path.startsWith(it) }
    }
}
