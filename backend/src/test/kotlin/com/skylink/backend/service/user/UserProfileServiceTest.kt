package com.skylink.backend.service.user

import com.skylink.backend.dto.user.UpdateProfileRequest
import com.skylink.backend.model.entity.User
import com.skylink.backend.model.entity.UserPfp
import com.skylink.backend.model.enums.UserRole
import com.skylink.backend.repository.UserPfpRepository
import com.skylink.backend.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

class UserProfileServiceTest {

    private val userRepository: UserRepository = mock()
    private val userPfpRepository: UserPfpRepository = mock()
    private val service = UserProfileService(userRepository, userPfpRepository)

    @Test
    fun `get by id and email return users or throw`() {
        val user = user()
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(userRepository.findById(2L)).thenReturn(Optional.empty())
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty())

        assertEquals(user, service.getById(1L))
        assertThrows(ResponseStatusException::class.java) { service.getById(2L) }
        assertEquals(user, service.getByEmail(user.email))
        assertThrows(ResponseStatusException::class.java) { service.getByEmail("missing@test.com") }
    }

    @Test
    fun `exists by email delegates to repository`() {
        whenever(userRepository.existsByEmail("user@test.com")).thenReturn(true)

        assertTrue(service.existsByEmail("user@test.com"))
    }

    @Test
    fun `get and update profile map pfp url`() {
        val user = user()
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(userPfpRepository.existsById(1L)).thenReturn(true, false)
        whenever(userRepository.save(user)).thenAnswer { it.arguments[0] }

        val profile = service.getProfile(user.email)
        val updated = service.updateProfile(user.email, UpdateProfileRequest("Updated"))

        assertEquals("/api/profile/pfp", profile.pfpUrl)
        assertEquals("Updated", updated.displayName)
        assertNull(updated.pfpUrl)
    }

    @Test
    fun `update profile keeps display name when request omits it`() {
        val user = user()
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(userRepository.save(user)).thenAnswer { it.arguments[0] }
        whenever(userPfpRepository.existsById(1L)).thenReturn(false)

        val updated = service.updateProfile(user.email, UpdateProfileRequest(displayName = null))

        assertEquals("User", updated.displayName)
    }

    @Test
    fun `update pfp validates files and creates or updates records`() {
        val user = user()
        val existing = UserPfp(userId = 1L, data = byteArrayOf(1), contentType = "image/png")
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(userPfpRepository.findById(1L)).thenReturn(Optional.empty(), Optional.of(existing))
        whenever(userPfpRepository.save(any())).thenAnswer { it.arguments[0] }

        service.updatePfp(user.email, MockMultipartFile("file", "pfp.png", "IMAGE/PNG", byteArrayOf(1, 2)))
        service.updatePfp(user.email, MockMultipartFile("file", "pfp.webp", "image/webp", byteArrayOf(3, 4)))

        assertArrayEquals(byteArrayOf(3, 4), existing.data)
        assertEquals("image/webp", existing.contentType)
        verify(userPfpRepository).save(existing)
    }

    @Test
    fun `update pfp rejects invalid files`() {
        val user = user()
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))

        assertThrows(ResponseStatusException::class.java) {
            service.updatePfp(user.email, MockMultipartFile("file", "empty.png", "image/png", byteArrayOf()))
        }
        assertThrows(ResponseStatusException::class.java) {
            service.updatePfp(user.email, MockMultipartFile("file", "missing", null, byteArrayOf(1)))
        }
        assertThrows(ResponseStatusException::class.java) {
            service.updatePfp(user.email, MockMultipartFile("file", "text.txt", "text/plain", byteArrayOf(1)))
        }
        assertThrows(ResponseStatusException::class.java) {
            service.updatePfp(user.email, MockMultipartFile("file", "huge.png", "image/png", ByteArray(5 * 1024 * 1024 + 1)))
        }
    }

    @Test
    fun `get and delete pfp handle missing and existing pfp`() {
        val user = user()
        val pfp = UserPfp(userId = 1L, data = byteArrayOf(7), contentType = "image/jpeg")
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(userPfpRepository.findById(1L)).thenReturn(Optional.of(pfp), Optional.empty(), Optional.of(pfp), Optional.empty())

        val found = service.getPfp(user.email)
        val missing = service.getPfp(user.email)
        val deleted = service.deletePfp(user.email)
        val notDeleted = service.deletePfp(user.email)

        assertArrayEquals(byteArrayOf(7), found?.bytes)
        assertEquals("image/jpeg", found?.contentType)
        assertNull(missing)
        assertTrue(deleted)
        assertFalse(notDeleted)
        verify(userPfpRepository).delete(pfp)
    }

    @Test
    fun `register fcm token skips anonymous and saves real users`() {
        val user = user()
        service.registerFcmToken("anonymousUser", "skip")
        verifyNoInteractions(userRepository)

        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        service.registerFcmToken(user.email, "token")

        assertEquals("token", user.fcmToken)
        verify(userRepository).save(user)
    }

    private fun user() = User(
        id = 1L,
        email = "user@test.com",
        displayName = "User",
        role = UserRole.OBSERVER
    )
}
