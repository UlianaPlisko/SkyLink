package com.skylink.backend.service

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.skylink.backend.dto.auth.*
import com.skylink.backend.model.entity.User
import com.skylink.backend.model.entity.UserCredentials
import com.skylink.backend.model.enums.AuthProvider
import com.skylink.backend.model.enums.UserRole
import com.skylink.backend.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

class AuthServiceTest {

    private val userRepository: UserRepository = mock()
    private val passwordEncoder: PasswordEncoder = mock()
    private val jwtService: JwtService = mock()
    private val googleVerifier: GoogleIdTokenVerifier = mock()

    private val authService = AuthService(
        userRepository,
        passwordEncoder,
        jwtService,
        googleVerifier
    )

    @Test
    fun `register should create user successfully`() {
        val request = RegisterRequest(
            email = "test@test.com",
            displayName = "Tester",
            password = "password123",
            role = UserRole.OBSERVER
        )

        whenever(userRepository.existsByEmail(request.email)).thenReturn(false)
        whenever(passwordEncoder.encode(request.password)).thenReturn("hashed")
        whenever(jwtService.generateToken(request.email)).thenReturn("jwt-token")

        whenever(userRepository.save(any())).thenAnswer {
            val user = it.arguments[0] as User
            user.copy(id = 1L).also { saved ->
                saved.localCredential = user.localCredential
            }
        }

        val response = authService.register(request)

        assertEquals("jwt-token", response.token)
        assertEquals(UserRole.OBSERVER, response.role)
        assertEquals("Tester", response.displayName)

        verify(userRepository).save(any())
    }

    @Test
    fun `register should throw if email exists`() {
        val request = RegisterRequest(
            email = "test@test.com",
            displayName = "Tester",
            password = "password123",
            role = UserRole.OBSERVER
        )

        whenever(userRepository.existsByEmail(request.email)).thenReturn(true)

        assertThrows<IllegalArgumentException> {
            authService.register(request)
        }
    }

    @Test
    fun `register should throw if password encoder returns null`() {
        val request = RegisterRequest(
            email = "null@test.com",
            displayName = "Tester",
            password = "password123",
            role = UserRole.OBSERVER
        )
        whenever(userRepository.existsByEmail(request.email)).thenReturn(false)
        whenever(passwordEncoder.encode(request.password)).thenReturn(null)

        assertThrows<IllegalArgumentException> {
            authService.register(request)
        }
    }

    @Test
    fun `login should throw if user not found`() {
        val request = LoginRequest(
            email = "missing@test.com",
            password = "password"
        )

        whenever(userRepository.findByEmail(request.email))
            .thenReturn(Optional.empty())

        assertThrows<BadCredentialsException> {
            authService.login(request)
        }
    }

    @Test
    fun `login should throw if provider is google`() {
        val user = User(
            email = "google@test.com",
            displayName = "Google User",
            role = UserRole.OBSERVER,
            provider = AuthProvider.GOOGLE
        )

        whenever(userRepository.findByEmail(user.email))
            .thenReturn(Optional.of(user))

        val request = LoginRequest(user.email, "password")

        assertThrows<BadCredentialsException> {
            authService.login(request)
        }
    }

    @Test
    fun `login should throw if password invalid`() {
        val user = User(
            email = "user@test.com",
            displayName = "User",
            role = UserRole.OBSERVER,
            provider = AuthProvider.LOCAL
        )

        user.localCredential = UserCredentials(
            user = user,
            passwordHash = "hashed"
        )

        whenever(userRepository.findByEmail(user.email))
            .thenReturn(Optional.of(user))

        whenever(passwordEncoder.matches("wrongpass", "hashed"))
            .thenReturn(false)

        val request = LoginRequest(user.email, "wrongpass")

        assertThrows<BadCredentialsException> {
            authService.login(request)
        }
    }

    @Test
    fun `login should succeed`() {
        val user = User(
            id = 1L,
            email = "user@test.com",
            displayName = "User",
            role = UserRole.OBSERVER,
            provider = AuthProvider.LOCAL
        )

        user.localCredential = UserCredentials(
            user = user,
            passwordHash = "hashed"
        )

        whenever(userRepository.findByEmail(user.email))
            .thenReturn(Optional.of(user))

        whenever(passwordEncoder.matches("password", "hashed"))
            .thenReturn(true)

        whenever(jwtService.generateToken(user.email))
            .thenReturn("jwt-token")

        val response = authService.login(
            LoginRequest(user.email, "password")
        )

        assertEquals("jwt-token", response.token)

        verify(userRepository).save(user)
    }

    @Test
    fun `complete google registration should fail if user exists`() {
        val request = CompleteGoogleRequest(
            pendingToken = "token",
            displayName = "Google User",
            role = UserRole.OBSERVER
        )

        whenever(jwtService.validatePendingToken("token"))
            .thenReturn(
                PendingGoogleClaims(
                    email = "google@test.com",
                    displayName = "Google User"
                )
            )

        whenever(userRepository.existsByEmail("google@test.com"))
            .thenReturn(true)

        assertThrows<IllegalStateException> {
            authService.completeGoogleRegistration(request)
        }
    }

    @Test
    fun `complete google registration should handle db conflict`() {
        val request = CompleteGoogleRequest(
            pendingToken = "token",
            displayName = "Google User",
            role = UserRole.OBSERVER
        )

        whenever(jwtService.validatePendingToken("token"))
            .thenReturn(
                PendingGoogleClaims(
                    email = "google@test.com",
                    displayName = "Google User"
                )
            )

        whenever(userRepository.existsByEmail(any()))
            .thenReturn(false)

        whenever(userRepository.save(any()))
            .thenThrow(DataIntegrityViolationException("duplicate"))

        assertThrows<IllegalStateException> {
            authService.completeGoogleRegistration(request)
        }
    }

    @Test
    fun `complete google registration should create google user`() {
        val request = CompleteGoogleRequest(
            pendingToken = "token",
            displayName = "  Google User  ",
            role = UserRole.CONTRIBUTOR
        )

        whenever(jwtService.validatePendingToken("token"))
            .thenReturn(
                PendingGoogleClaims(
                    email = "google@test.com",
                    displayName = "Ignored Claim Name"
                )
            )

        whenever(userRepository.existsByEmail("google@test.com"))
            .thenReturn(false)

        whenever(userRepository.save(any())).thenAnswer {
            val user = it.arguments[0] as User
            user.copy(id = 7L)
        }

        whenever(jwtService.generateToken("google@test.com"))
            .thenReturn("jwt-token")

        val response = authService.completeGoogleRegistration(request)

        assertEquals("jwt-token", response.token)
        assertEquals(UserRole.CONTRIBUTOR, response.role)
        assertEquals("Google User", response.displayName)
        assertEquals(0L, response.userId)
    }

    @Test
    fun `login should throw if local credential missing`() {
        val user = User(
            email = "user@test.com",
            displayName = "User",
            role = UserRole.OBSERVER,
            provider = AuthProvider.LOCAL
        )

        whenever(userRepository.findByEmail(user.email))
            .thenReturn(Optional.of(user))

        assertThrows<IllegalArgumentException> {
            authService.login(LoginRequest(user.email, "password"))
        }
    }

    @Test
    fun `google auth rejects invalid token missing email and local account`() {
        whenever(googleVerifier.verify("invalid")).thenReturn(null)
        assertThrows<BadCredentialsException> {
            authService.googleAuth(GoogleAuthRequest("invalid"))
        }

        val noEmailToken: GoogleIdToken = mock()
        val noEmailPayload = GoogleIdToken.Payload()
        whenever(noEmailToken.payload).thenReturn(noEmailPayload)
        whenever(googleVerifier.verify("no-email")).thenReturn(noEmailToken)
        assertThrows<BadCredentialsException> {
            authService.googleAuth(GoogleAuthRequest("no-email"))
        }

        val noPayloadToken: GoogleIdToken = mock()
        whenever(noPayloadToken.payload).thenReturn(null)
        whenever(googleVerifier.verify("no-payload")).thenReturn(noPayloadToken)
        assertThrows<BadCredentialsException> {
            authService.googleAuth(GoogleAuthRequest("no-payload"))
        }

        val localToken: GoogleIdToken = mock()
        val localPayload = GoogleIdToken.Payload().setEmail("local@test.com")
        whenever(localToken.payload).thenReturn(localPayload)
        whenever(googleVerifier.verify("local")).thenReturn(localToken)
        whenever(userRepository.findByEmail("local@test.com")).thenReturn(
            Optional.of(
                User(
                    id = 3L,
                    email = "local@test.com",
                    displayName = "Local",
                    role = UserRole.OBSERVER,
                    provider = AuthProvider.LOCAL
                )
            )
        )
        assertThrows<BadCredentialsException> {
            authService.googleAuth(GoogleAuthRequest("local"))
        }
    }

    @Test
    fun `google auth logs in existing google user or returns pending token for new user`() {
        val existing = User(
            id = 4L,
            email = "google@test.com",
            displayName = "Google",
            role = UserRole.CONTRIBUTOR,
            provider = AuthProvider.GOOGLE
        )
        val existingToken: GoogleIdToken = mock()
        val existingPayload = GoogleIdToken.Payload()
            .setEmail(existing.email)
            .set("name", "Ignored") as GoogleIdToken.Payload
        whenever(existingToken.payload).thenReturn(existingPayload)
        whenever(googleVerifier.verify("existing")).thenReturn(existingToken)
        whenever(userRepository.findByEmail(existing.email)).thenReturn(Optional.of(existing))
        whenever(jwtService.generateToken(existing.email)).thenReturn("auth-token")

        val existingResponse = authService.googleAuth(GoogleAuthRequest("existing"))

        assertEquals("auth-token", existingResponse.token)
        assertFalse(existingResponse.isPending)
        assertEquals(existing.id, existingResponse.userId)
        verify(userRepository).save(existing)

        val newToken: GoogleIdToken = mock()
        val newPayload = GoogleIdToken.Payload()
            .setEmail("new@test.com")
            .set("name", "New User") as GoogleIdToken.Payload
        whenever(newToken.payload).thenReturn(newPayload)
        whenever(googleVerifier.verify("new")).thenReturn(newToken)
        whenever(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty())
        whenever(jwtService.generatePendingGoogleToken("new@test.com", "New User")).thenReturn("pending-token")

        val newResponse = authService.googleAuth(GoogleAuthRequest("new"))

        assertEquals("pending-token", newResponse.token)
        assertTrue(newResponse.isPending)
        assertEquals("New User", newResponse.displayName)
    }
}
