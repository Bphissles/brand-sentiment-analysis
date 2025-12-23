package sentiment

import grails.gorm.transactions.Transactional
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import java.security.Key
import java.util.Date

/**
 * Service for user authentication and JWT token management
 */
@Transactional
class AuthService {

    def grailsApplication

    // JWT secret key - should be at least 256 bits for HS256
    private Key getSigningKey() {
        def secret = grailsApplication.config.getProperty('jwt.secret', String) ?: 'default-secret-key-change-in-production-must-be-at-least-256-bits'
        return Keys.hmacShaKeyFor(secret.bytes)
    }

    // Token expiration time (24 hours)
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000

    /**
     * Authenticate user with email and password
     * 
     * @param email User email
     * @param password Plain text password
     * @return User if authenticated, null otherwise
     */
    User authenticate(String email, String password) {
        def user = User.findByEmail(email)
        
        if (!user || !user.enabled) {
            return null
        }

        // Verify password (using BCrypt)
        if (verifyPassword(password, user.passwordHash)) {
            // Update last login
            user.lastLoginAt = java.time.Instant.now()
            user.save(flush: true)
            return user
        }

        return null
    }

    /**
     * Generate JWT token for authenticated user
     * 
     * @param user The authenticated user
     * @return JWT token string
     */
    String generateToken(User user) {
        def now = new Date()
        def expiration = new Date(now.time + EXPIRATION_TIME)

        return Jwts.builder()
            .setSubject(user.email)
            .claim("role", user.role)
            .claim("userId", user.id)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact()
    }

    /**
     * Validate JWT token and extract user info
     * 
     * @param token JWT token string
     * @return Map with user info if valid, null otherwise
     */
    Map validateToken(String token) {
        try {
            def claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()

            return [
                email: claims.getSubject(),
                role: claims.get("role", String),
                userId: claims.get("userId", Long),
                expiration: claims.getExpiration()
            ]
        } catch (Exception e) {
            log.warn("Invalid JWT token: ${e.message}")
            return null
        }
    }

    /**
     * Register a new user
     * 
     * @param email User email
     * @param password Plain text password
     * @param role User role (admin or viewer)
     * @return Created user or null if email exists
     */
    User register(String email, String password, String role = 'viewer') {
        if (User.findByEmail(email)) {
            return null // Email already exists
        }

        def user = new User(
            email: email,
            passwordHash: hashPassword(password),
            role: role,
            enabled: true,
            accountExpired: false,
            accountLocked: false,
            passwordExpired: false
        )

        if (user.validate() && user.save(flush: true)) {
            return user
        }

        log.error("Failed to create user: ${user.errors}")
        return null
    }

    /**
     * Hash password using BCrypt
     */
    private String hashPassword(String password) {
        return org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt())
    }

    /**
     * Verify password against BCrypt hash
     */
    private boolean verifyPassword(String password, String hash) {
        return org.mindrot.jbcrypt.BCrypt.checkpw(password, hash)
    }
}
