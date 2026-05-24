package com.skylink.backend.service.user

import com.skylink.backend.dto.user.ChangePasswordRequest
import com.skylink.backend.model.entity.User
import com.skylink.backend.model.entity.UserCredentials
import com.skylink.backend.model.enums.AuthProvider
import com.skylink.backend.model.enums.UserRole
import com.skylink.backend.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

class UserSecurityServiceTest {

    private val userRepository: UserRepository = mock()
    private val passwordEncoder: PasswordEncoder = mock()
    private val service = UserSecurityService(userRepository, passwordEncoder)

    @Test
    fun `change password validates account and saves new hash`() {
        val user = localUser()
        user.localCredential = UserCredentials(user = user, passwordHash = "old-hash")
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches("old", "old-hash")).thenReturn(true)
        whenever(passwordEncoder.encode("new")).thenReturn("new-hash")

        service.changePassword(user.email, ChangePasswordRequest("old", "new"))

        assertEquals("new-hash", user.localCredential?.passwordHash)
        verify(userRepository).save(user)
    }

    @Test
    fun `change password rejects null encoded password`() {
        val user = localUser(email = "null-encoded@test.com")
        user.localCredential = UserCredentials(user = user, passwordHash = "old-hash")
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches("old", "old-hash")).thenReturn(true)
        whenever(passwordEncoder.encode("new")).thenReturn(null)

        assertThrows(IllegalArgumentException::class.java) {
            service.changePassword(user.email, ChangePasswordRequest("old", "new"))
        }
    }

    @Test
    fun `change password rejects missing user google account missing credential wrong old and same password`() {
        val local = localUser()
        val google = localUser(provider = AuthProvider.GOOGLE)
        val missingCredential = localUser(email = "missing-credential@test.com")
        val wrongOld = localUser(email = "wrong@test.com").also {
            it.localCredential = UserCredentials(user = it, passwordHash = "hash")
        }
        val samePassword = localUser(email = "same@test.com").also {
            it.localCredential = UserCredentials(user = it, passwordHash = "hash")
        }
        whenever(userRepository.findByEmail("absent@test.com")).thenReturn(Optional.empty())
        whenever(userRepository.findByEmail(google.email)).thenReturn(Optional.of(google))
        whenever(userRepository.findByEmail(missingCredential.email)).thenReturn(Optional.of(missingCredential))
        whenever(userRepository.findByEmail(wrongOld.email)).thenReturn(Optional.of(wrongOld))
        whenever(userRepository.findByEmail(samePassword.email)).thenReturn(Optional.of(samePassword))
        whenever(passwordEncoder.matches("old", "hash")).thenReturn(false, true)

        assertThrows(IllegalArgumentException::class.java) {
            service.changePassword("absent@test.com", ChangePasswordRequest("old", "new"))
        }
        assertThrows(ResponseStatusException::class.java) {
            service.changePassword(google.email, ChangePasswordRequest("old", "new"))
        }
        assertThrows(ResponseStatusException::class.java) {
            service.changePassword(missingCredential.email, ChangePasswordRequest("old", "new"))
        }
        assertThrows(ResponseStatusException::class.java) {
            service.changePassword(wrongOld.email, ChangePasswordRequest("old", "new"))
        }
        assertThrows(ResponseStatusException::class.java) {
            service.changePassword(samePassword.email, ChangePasswordRequest("old", "old"))
        }
    }

    private fun localUser(
        email: String = "user@test.com",
        provider: AuthProvider = AuthProvider.LOCAL
    ) = User(
        id = 1L,
        email = email,
        displayName = "User",
        provider = provider,
        role = UserRole.OBSERVER
    )
}
