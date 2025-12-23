package sentiment

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

/**
 * Unit tests for AuthService
 * Tests authentication, token generation, and user registration
 */
class AuthServiceSpec extends Specification implements ServiceUnitTest<AuthService> {

    def setup() {
        // Mock grailsApplication config
        service.grailsApplication = [
            config: [
                getProperty: { String key, Class type ->
                    if (key == 'jwt.secret') {
                        return 'test-secret-key-for-unit-tests-must-be-at-least-256-bits-long'
                    }
                    return null
                }
            ]
        ]
    }

    void "test password hashing and verification"() {
        given: "a plain text password"
        def password = "testPassword123"

        when: "password is hashed"
        def hash = service.hashPassword(password)

        then: "hash is generated and can be verified"
        hash != null
        hash != password
        service.verifyPassword(password, hash)
        !service.verifyPassword("wrongPassword", hash)
    }

    void "test token generation"() {
        given: "a user"
        def user = new User(
            id: 1L,
            email: "test@example.com",
            role: "viewer"
        )

        when: "token is generated"
        def token = service.generateToken(user)

        then: "token is valid JWT"
        token != null
        token.split('\\.').length == 3 // JWT has 3 parts
    }

    void "test token validation with valid token"() {
        given: "a user and generated token"
        def user = new User(
            id: 1L,
            email: "test@example.com",
            role: "admin"
        )
        def token = service.generateToken(user)

        when: "token is validated"
        def claims = service.validateToken(token)

        then: "claims are extracted correctly"
        claims != null
        claims.email == "test@example.com"
        claims.role == "admin"
        claims.userId == 1L
    }

    void "test token validation with invalid token"() {
        when: "invalid token is validated"
        def claims = service.validateToken("invalid.token.here")

        then: "validation fails"
        claims == null
    }

    void "test user registration with new email"() {
        given: "a new email and password"
        def email = "newuser@example.com"
        def password = "securePassword123"

        and: "mock User domain"
        mockDomain(User)

        when: "user is registered"
        def user = service.register(email, password, "viewer")

        then: "user is created successfully"
        user != null
        user.email == email
        user.role == "viewer"
        user.enabled == true
        service.verifyPassword(password, user.passwordHash)
    }

    void "test user registration with existing email"() {
        given: "an existing user"
        def email = "existing@example.com"
        mockDomain(User, [
            new User(email: email, passwordHash: "hash", role: "viewer", enabled: true)
        ])

        when: "registration attempted with same email"
        def user = service.register(email, "password", "viewer")

        then: "registration fails"
        user == null
    }

    void "test authentication with valid credentials"() {
        given: "an existing user"
        def email = "user@example.com"
        def password = "correctPassword"
        def hash = service.hashPassword(password)
        
        mockDomain(User, [
            new User(
                id: 1L,
                email: email,
                passwordHash: hash,
                role: "viewer",
                enabled: true
            )
        ])

        when: "authentication is attempted"
        def user = service.authenticate(email, password)

        then: "authentication succeeds"
        user != null
        user.email == email
    }

    void "test authentication with invalid password"() {
        given: "an existing user"
        def email = "user@example.com"
        def hash = service.hashPassword("correctPassword")
        
        mockDomain(User, [
            new User(
                email: email,
                passwordHash: hash,
                role: "viewer",
                enabled: true
            )
        ])

        when: "authentication with wrong password"
        def user = service.authenticate(email, "wrongPassword")

        then: "authentication fails"
        user == null
    }

    void "test authentication with disabled user"() {
        given: "a disabled user"
        def email = "disabled@example.com"
        def hash = service.hashPassword("password")
        
        mockDomain(User, [
            new User(
                email: email,
                passwordHash: hash,
                role: "viewer",
                enabled: false
            )
        ])

        when: "authentication is attempted"
        def user = service.authenticate(email, "password")

        then: "authentication fails"
        user == null
    }
}
