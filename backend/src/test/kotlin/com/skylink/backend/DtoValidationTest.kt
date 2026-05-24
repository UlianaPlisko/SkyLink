package com.skylink.backend

import com.skylink.backend.dto.auth.CompleteGoogleRequest
import com.skylink.backend.dto.auth.RegisterRequest
import com.skylink.backend.dto.chat.ChatMessageSendRequest
import com.skylink.backend.dto.chat.CreateChatRoomRequest
import com.skylink.backend.dto.chat.UpdateChatRoomRequest
import com.skylink.backend.dto.event.CreateEventRequest
import com.skylink.backend.dto.user.ChangePasswordRequest
import com.skylink.backend.dto.user.UpdateProfileRequest
import com.skylink.backend.model.enums.ChatRoomType
import com.skylink.backend.model.enums.EventType
import com.skylink.backend.model.enums.UserRole
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class DtoValidationTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `registration and google completion validate user supplied auth fields`() {
        assertEquals(
            emptySet<String>(),
            violations(RegisterRequest("user@test.com", "User Name", "password123", UserRole.OBSERVER))
        )
        assertEquals(
            setOf("email", "displayName", "password"),
            violations(RegisterRequest("not-email", "ab", "short", UserRole.OBSERVER))
        )
        assertEquals(
            setOf("displayName"),
            violations(CompleteGoogleRequest("pending-token", "ab", UserRole.CONTRIBUTOR))
        )
    }

    @Test
    fun `chat request validation catches blank and oversized input`() {
        assertEquals(emptySet<String>(), violations(ChatMessageSendRequest("hello")))
        assertEquals(setOf("content"), violations(ChatMessageSendRequest("")))
        assertEquals(setOf("content"), violations(ChatMessageSendRequest("x".repeat(5001))))

        assertEquals(setOf("name"), violations(CreateChatRoomRequest("", ChatRoomType.GLOBAL)))
        assertEquals(setOf("name"), violations(UpdateChatRoomRequest("x".repeat(256), ChatRoomType.GLOBAL)))
    }

    @Test
    fun `profile password and event requests validate editable fields`() {
        assertEquals(emptySet<String>(), violations(UpdateProfileRequest(displayName = null)))
        assertEquals(setOf("displayName"), violations(UpdateProfileRequest(displayName = "ab")))
        assertEquals(setOf("oldPassword", "newPassword"), violations(ChangePasswordRequest("", "short")))

        assertEquals(
            emptySet<String>(),
            violations(
                CreateEventRequest(
                    title = "Meteor watch",
                    description = null,
                    eventType = EventType.METEOR_SHOWER,
                    startAt = Instant.now().plusSeconds(3600),
                    endAt = null
                )
            )
        )
        assertEquals(
            setOf("title", "startAt"),
            violations(
                CreateEventRequest(
                    title = "",
                    description = null,
                    eventType = EventType.OTHER,
                    startAt = Instant.now().minusSeconds(3600),
                    endAt = null
                )
            )
        )
    }

    private fun violations(value: Any): Set<String> {
        return validator.validate(value)
            .map { it.propertyPath.toString() }
            .toSet()
    }
}
