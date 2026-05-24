package com.codepalace.accelerometer.data

import com.codepalace.accelerometer.api.dto.AuthResponse
import com.codepalace.accelerometer.api.dto.ChangePasswordRequest
import com.codepalace.accelerometer.api.dto.CompleteGoogleRequest
import com.codepalace.accelerometer.api.dto.FavoriteRequest
import com.codepalace.accelerometer.api.dto.FavoriteResponse
import com.codepalace.accelerometer.api.dto.FavoriteUpdateRequest
import com.codepalace.accelerometer.api.dto.GoogleAuthRequest
import com.codepalace.accelerometer.api.dto.GoogleCallbackResponse
import com.codepalace.accelerometer.api.dto.LoginRequest
import com.codepalace.accelerometer.api.dto.RegisterRequest
import com.codepalace.accelerometer.api.dto.UpdateProfileRequest
import com.codepalace.accelerometer.api.dto.UserProfileResponse
import com.codepalace.accelerometer.data.model.SpaceObjectSummary
import com.codepalace.accelerometer.data.model.dto.ChatRoomResponse
import com.codepalace.accelerometer.data.model.dto.ChatRoomUi
import com.codepalace.accelerometer.data.model.dto.ConstellationCultureResponse
import com.codepalace.accelerometer.data.model.dto.CreateChatRoomRequest
import com.codepalace.accelerometer.data.model.dto.CreateEventRequest
import com.codepalace.accelerometer.data.model.dto.CurrentVisibilityResponse
import com.codepalace.accelerometer.data.model.dto.FcmTokenRequest
import com.codepalace.accelerometer.data.model.dto.WikiResponse
import com.codepalace.accelerometer.data.model.enums.ChatRoomType
import com.codepalace.accelerometer.data.model.enums.EventType
import com.codepalace.accelerometer.data.model.enums.UserRole
import com.codepalace.accelerometer.ui.MessageKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoModelTest {

    @Test
    fun `auth dto models preserve constructor values and defaults`() {
        val auth = AuthResponse("token", UserRole.ADMIN, "Ada", 1L)
        val login = LoginRequest("a@example.com", "secret")
        val register = RegisterRequest("b@example.com", "Bob", "pass", UserRole.OBSERVER)
        val profile = UserProfileResponse(1L, "a@example.com", "Ada", UserRole.ADMIN)
        val googlePending = GoogleCallbackResponse("pending", isPending = true)
        val googleComplete = CompleteGoogleRequest("pending", "Ada", UserRole.CONTRIBUTOR)

        assertEquals("token", auth.token)
        assertEquals("secret", login.password)
        assertEquals(UserRole.OBSERVER, register.role)
        assertNull(profile.provider)
        assertTrue(googlePending.isPending)
        assertNull(googlePending.role)
        assertEquals("Ada", googleComplete.displayName)
        assertEquals("id-token", GoogleAuthRequest("id-token").idToken)
        assertEquals("new", ChangePasswordRequest("old", "new").newPassword)
        assertEquals("Updated", UpdateProfileRequest("Updated").displayName)
    }

    @Test
    fun `favorite dto models preserve optional fields and defaults`() {
        val summary = SpaceObjectSummary(10L, "Vega", 0.03, "STAR", 1.0, 2.0)
        val response = FavoriteResponse(1L, summary, visibility = 0.9)
        val request = FavoriteRequest(10L)
        val update = FavoriteUpdateRequest(note = "watch", visibility = 0.7)

        assertEquals(summary, response.spaceObject)
        assertNull(response.note)
        assertEquals(1.0, request.visibility!!, 0.0001)
        assertNull(request.note)
        assertEquals("watch", update.note)
        assertEquals(0.7, update.visibility!!, 0.0001)
    }

    @Test
    fun `chat and event dto models preserve defaults`() {
        val room = ChatRoomResponse(1L, "Global", "GLOBAL")
        val roomUi = ChatRoomUi(1L, "Global", isSubscribed = true)
        val createRoom = CreateChatRoomRequest("Event room", ChatRoomType.EVENT, eventId = 3L)
        val createEvent = CreateEventRequest(
            title = "Meteor shower",
            description = "Watch",
            eventType = EventType.METEOR_SHOWER,
            startAt = "2026-05-23T10:00:00Z"
        )

        assertNull(room.regionGeom)
        assertEquals(0, roomUi.unreadCount)
        assertEquals(3L, createRoom.eventId)
        assertNull(createEvent.endAt)
        assertNull(createEvent.chatRoomName)
    }

    @Test
    fun `weather culture firebase wiki and message enums expose expected values`() {
        val weather = CurrentVisibilityResponse(
            location = "Bratislava",
            localTime = "12:00",
            visibilityKm = 10.0,
            cloudPercent = 25,
            humidity = 30,
            precipitationMm = 0.0,
            conditionText = "Sunny",
            conditionCode = 1000,
            isDay = true
        )
        val culture = ConstellationCultureResponse(1L, "Western", "EU", null, current = true)
        val wiki = WikiResponse("Mars", null, null, null)

        assertTrue(weather.isDay)
        assertEquals(25, weather.cloudPercent)
        assertTrue(culture.current)
        assertNull(culture.description)
        assertEquals("abc", FcmTokenRequest("abc").fcmToken)
        assertNull(wiki.imageUrl)
        assertEquals(listOf(MessageKind.INFO, MessageKind.SUCCESS, MessageKind.ERROR), MessageKind.entries)
        assertFalse(MessageKind.ERROR == MessageKind.SUCCESS)
    }
}
