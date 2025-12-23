package sentiment

import grails.converters.JSON

/**
 * Controller for authentication endpoints
 * Handles login, registration, and token validation
 */
class AuthController {

    static responseFormats = ['json']
    static allowedMethods = [
        login: 'POST',
        register: 'POST',
        me: 'GET',
        logout: 'POST',
        promoteToAdmin: 'POST'
    ]

    AuthService authService

    /**
     * POST /api/auth/login
     * Authenticate user and return JWT token
     */
    def login() {
        def json = request.JSON

        if (!json?.email || !json?.password) {
            render status: 400, text: [error: 'Email and password required'] as JSON
            return
        }

        def user = authService.authenticate(json.email, json.password)

        if (!user) {
            render status: 401, text: [error: 'Invalid credentials'] as JSON
            return
        }

        def token = authService.generateToken(user)

        respond([
            success: true,
            token: token,
            user: [
                id: user.id,
                email: user.email,
                role: user.role
            ]
        ])
    }

    /**
     * POST /api/auth/register
     * Register a new user account
     */
    def register() {
        def json = request.JSON

        if (!json?.email || !json?.password) {
            render status: 400, text: [error: 'Email and password required'] as JSON
            return
        }

        // Validate email format
        if (!json.email.matches(/^[\w.+-]+@[\w.-]+\.\w{2,}$/)) {
            render status: 400, text: [error: 'Invalid email format'] as JSON
            return
        }

        // Validate password strength
        if (json.password.length() < 8) {
            render status: 400, text: [error: 'Password must be at least 8 characters'] as JSON
            return
        }

        def user = authService.register(json.email, json.password, json.role ?: 'viewer')

        if (!user) {
            render status: 409, text: [error: 'Email already registered'] as JSON
            return
        }

        def token = authService.generateToken(user)

        respond([
            success: true,
            token: token,
            user: [
                id: user.id,
                email: user.email,
                role: user.role
            ]
        ])
    }

    /**
     * GET /api/auth/me
     * Get current user info from JWT token
     */
    def me() {
        def authHeader = request.getHeader('Authorization')

        if (!authHeader?.startsWith('Bearer ')) {
            render status: 401, text: [error: 'No token provided'] as JSON
            return
        }

        def token = authHeader.substring(7)
        def claims = authService.validateToken(token)

        if (!claims) {
            render status: 401, text: [error: 'Invalid or expired token'] as JSON
            return
        }

        def user = User.findByEmail(claims.email)

        if (!user || !user.enabled) {
            render status: 401, text: [error: 'User not found or disabled'] as JSON
            return
        }

        respond([
            user: [
                id: user.id,
                email: user.email,
                role: user.role,
                lastLoginAt: user.lastLoginAt?.toString()
            ]
        ])
    }

    /**
     * POST /api/auth/logout
     * Logout (client-side token removal, server acknowledges)
     */
    def logout() {
        respond([success: true, message: 'Logged out successfully'])
    }

    /**
     * POST /api/auth/promote
     * Promote a user to admin role (admin only, or first user becomes admin)
     */
    def promoteToAdmin() {
        def json = request.JSON
        def email = json?.email

        if (!email) {
            render status: 400, text: [error: 'Email required'] as JSON
            return
        }

        def user = User.findByEmail(email)
        if (!user) {
            render status: 404, text: [error: 'User not found'] as JSON
            return
        }

        // Check if this is the first admin (bootstrap scenario)
        def adminCount = User.countByRole('admin')
        def requestingUserRole = request.getAttribute('userRole')

        // Allow promotion if: no admins exist OR requester is admin
        if (adminCount == 0 || requestingUserRole == 'admin') {
            user.role = 'admin'
            user.save(flush: true)

            respond([
                success: true,
                message: "User ${email} promoted to admin",
                user: [
                    id: user.id,
                    email: user.email,
                    role: user.role
                ]
            ])
        } else {
            render status: 403, text: [error: 'Admin access required'] as JSON
        }
    }
}
