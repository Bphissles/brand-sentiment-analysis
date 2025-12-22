package sentiment

import java.time.Instant

/**
 * User account for dashboard authentication
 * Supports JWT-based authentication via Spring Security REST
 */
class User {

    String email
    String passwordHash
    String role               // admin, viewer

    Boolean enabled = true
    Boolean accountExpired = false
    Boolean accountLocked = false
    Boolean passwordExpired = false

    Instant lastLoginAt
    Instant dateCreated
    Instant lastUpdated

    static constraints = {
        email nullable: false, blank: false, email: true, unique: true, maxSize: 255
        passwordHash nullable: false, blank: false, maxSize: 255
        role nullable: false, inList: ['admin', 'viewer']
        enabled nullable: false
        accountExpired nullable: false
        accountLocked nullable: false
        passwordExpired nullable: false
        lastLoginAt nullable: true
    }

    static mapping = {
        password column: 'password_hash'
    }
}
