package com.skylink.backend.controller

import com.skylink.backend.dto.auth.AuthResponse
import com.skylink.backend.dto.auth.CompleteGoogleRequest
import com.skylink.backend.dto.auth.GoogleAuthRequest
import com.skylink.backend.dto.auth.GoogleCallbackResponse
import com.skylink.backend.dto.auth.LoginRequest
import com.skylink.backend.dto.auth.RegisterRequest
import com.skylink.backend.dto.celestial.ConstellationCultureResponse
import com.skylink.backend.dto.celestial.ConstellationDetailResponse
import com.skylink.backend.dto.celestial.ConstellationResponse
import com.skylink.backend.dto.celestial.PlanetResponse
import com.skylink.backend.dto.celestial.SpaceObjectSummary
import com.skylink.backend.dto.celestial.WikiResponse
import com.skylink.backend.dto.chat.ChatMessageResponse
import com.skylink.backend.dto.chat.ChatRoomResponse
import com.skylink.backend.dto.chat.ChatSubscriptionResponse
import com.skylink.backend.dto.chat.CreateChatRoomRequest
import com.skylink.backend.dto.chat.UpdateChatRoomRequest
import com.skylink.backend.dto.chat.UpdateReadStateRequest
import com.skylink.backend.dto.event.CreateEventRequest
import com.skylink.backend.dto.event.EventDetailsResponse
import com.skylink.backend.dto.event.EventResponse
import com.skylink.backend.dto.favorite.FavoriteRequest
import com.skylink.backend.dto.favorite.FavoriteResponse
import com.skylink.backend.dto.favorite.FavoriteUpdateRequest
import com.skylink.backend.dto.user.ChangePasswordRequest
import com.skylink.backend.dto.user.FcmTokenRequest
import com.skylink.backend.dto.user.UpdateProfileRequest
import com.skylink.backend.dto.user.UserProfileResponse
import com.skylink.backend.dto.weather.CurrentVisibilityResponseDto
import com.skylink.backend.model.enums.AuthProvider
import com.skylink.backend.model.enums.ChatRoomType
import com.skylink.backend.model.enums.EventType
import com.skylink.backend.model.enums.UserRole
import com.skylink.backend.service.AuthServiceInterface
import com.skylink.backend.service.CelestialServiceInterface
import com.skylink.backend.service.ChatRoomServiceInterface
import com.skylink.backend.service.EventServiceInterface
import com.skylink.backend.service.FavoriteService
import com.skylink.backend.service.WeatherServiceInterface
import com.skylink.backend.service.user.PfpFile
import com.skylink.backend.service.user.UserProfileService
import com.skylink.backend.service.user.UserSecurityService
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.Authentication
import java.security.Principal
import java.time.Instant
import java.time.LocalDate

class ControllerDelegationTest {

    private val now = Instant.parse("2026-05-23T08:00:00Z")
    private val principal = Principal { "user@test.com" }
    private val authentication: Authentication = mock {
        on { name }.thenReturn("user@test.com")
    }

    @Test
    fun `auth controller delegates all auth endpoints`() {
        val service: AuthServiceInterface = mock()
        val controller = AuthController(service)
        val register = RegisterRequest("user@test.com", "User", "password123", UserRole.OBSERVER)
        val login = LoginRequest("user@test.com", "password123")
        val google = GoogleAuthRequest("id-token")
        val complete = CompleteGoogleRequest("pending", "User", UserRole.CONTRIBUTOR)
        whenever(service.register(register)).thenReturn(AuthResponse("token", UserRole.OBSERVER, "User", 1L))
        whenever(service.login(login)).thenReturn(AuthResponse("login-token", UserRole.OBSERVER, "User", 1L))
        whenever(service.googleAuth(google)).thenReturn(GoogleCallbackResponse("pending", true, "User"))
        whenever(service.completeGoogleRegistration(complete)).thenReturn(AuthResponse("done", UserRole.CONTRIBUTOR, "User", 1L))

        assertEquals("token", controller.register(register).token)
        assertEquals("login-token", controller.login(login).token)
        assertEquals(true, controller.googleAuth(google).isPending)
        assertEquals(UserRole.CONTRIBUTOR, controller.completeGoogle(complete).role)
    }

    @Test
    fun `weather controller delegates visibility lookup`() {
        val service: WeatherServiceInterface = mock()
        val controller = WeatherController(service)
        whenever(service.getCurrentVisibility("Bratislava")).thenReturn(
            CurrentVisibilityResponseDto("Bratislava", null, 10.0, 5, 60, 0.0, true, "Clear", 1000)
        )

        assertEquals("Bratislava", controller.getCurrentVisibility("Bratislava").location)
    }

    @Test
    fun `favorite controller maps principal user and response status`() {
        val favoriteService: FavoriteService = mock()
        val userService: UserProfileService = mock()
        val controller = FavoriteController(favoriteService, userService)
        val user = user()
        val favorite = favoriteResponse()
        whenever(userService.getByEmail("user@test.com")).thenReturn(user)
        whenever(favoriteService.getUserFavorites(1L)).thenReturn(listOf(favorite))
        whenever(favoriteService.addFavorite(1L, FavoriteRequest(10L, "note", 1.0))).thenReturn(favorite)
        whenever(favoriteService.updateFavorite(1L, 10L, FavoriteUpdateRequest("new", 0.5))).thenReturn(favorite)
        whenever(favoriteService.removeFavorite(1L, 10L)).thenReturn(true)
        whenever(favoriteService.removeFavorite(1L, 11L)).thenReturn(false)

        assertEquals(listOf(favorite), controller.getFavorites(principal))
        assertEquals(favorite, controller.addFavorite(FavoriteRequest(10L, "note", 1.0), principal))
        assertEquals(favorite, controller.updateFavorite(10L, FavoriteUpdateRequest("new", 0.5), principal))
        assertEquals(HttpStatus.NO_CONTENT, controller.removeFavorite(10L, principal).statusCode)
        assertEquals(HttpStatus.NOT_FOUND, controller.removeFavorite(11L, principal).statusCode)
    }

    @Test
    fun `user controller delegates profile operations and pfp status`() {
        val profileService: UserProfileService = mock()
        val securityService: UserSecurityService = mock()
        val controller = UserController(profileService, securityService)
        val profile = profileResponse()
        val update = UpdateProfileRequest("New Name")
        val password = ChangePasswordRequest("old-password", "new-password")
        val file = MockMultipartFile("file", "pfp.png", "image/png", byteArrayOf(1, 2, 3))
        whenever(profileService.getProfile("user@test.com")).thenReturn(profile)
        whenever(profileService.updateProfile("user@test.com", update)).thenReturn(profile.copy(displayName = "New Name"))
        whenever(profileService.getPfp("user@test.com")).thenReturn(PfpFile(byteArrayOf(4, 5), "image/png"), null)
        whenever(profileService.deletePfp("user@test.com")).thenReturn(true, false)

        assertEquals(profile, controller.getProfile(principal))
        assertEquals("New Name", controller.updateProfile(principal, update).displayName)
        controller.changePassword(principal, password)
        controller.uploadPfp(principal, file)
        assertArrayEquals(byteArrayOf(4, 5), controller.getPfp(principal).body)
        assertEquals(HttpStatus.NOT_FOUND, controller.getPfp(principal).statusCode)
        assertEquals(HttpStatus.NO_CONTENT, controller.deletePfp(principal).statusCode)
        assertEquals(HttpStatus.NOT_FOUND, controller.deletePfp(principal).statusCode)
        controller.registerFcmToken(FcmTokenRequest("token"), principal)

        verify(securityService).changePassword("user@test.com", password)
        verify(profileService).updatePfp("user@test.com", file)
        verify(profileService).registerFcmToken("user@test.com", "token")
    }

    @Test
    fun `chat room controller delegates all room operations`() {
        val service: ChatRoomServiceInterface = mock()
        val controller = ChatRoomController(service)
        val create = CreateChatRoomRequest(" Global ", ChatRoomType.GLOBAL, null, null)
        val update = UpdateChatRoomRequest("Regional", ChatRoomType.REGIONAL, "{}", null)
        val read = UpdateReadStateRequest(99L)
        val room = chatRoomResponse()
        val sub = ChatSubscriptionResponse(1L, 2L, now, 99L)
        val message = ChatMessageResponse(99L, 2L, 1L, "user@test.com", "hello", now, null)
        whenever(service.createRoom("user@test.com", create)).thenReturn(room)
        whenever(service.getAllRooms(ChatRoomType.GLOBAL)).thenReturn(listOf(room))
        whenever(service.getRoomById(2L)).thenReturn(room)
        whenever(service.updateRoom(2L, update)).thenReturn(room.copy(name = "Regional"))
        whenever(service.subscribeCurrentUser("user@test.com", 2L)).thenReturn(sub)
        whenever(service.getRoomSubscriptions(2L)).thenReturn(listOf(sub))
        whenever(service.getUserSubscribedRooms("user@test.com")).thenReturn(listOf(room))
        whenever(service.getRoomMessages(2L)).thenReturn(listOf(message))
        whenever(service.getLatestMessage(2L)).thenReturn(message)
        whenever(service.updateReadState("user@test.com", 2L, read)).thenReturn(sub)

        assertEquals(room, controller.createRoom(authentication, create))
        assertEquals(listOf(room), controller.getRooms(ChatRoomType.GLOBAL))
        assertEquals(room, controller.getRoomById(2L))
        assertEquals("Regional", controller.updateRoom(2L, update).name)
        controller.deleteRoom(authentication, 2L)
        assertEquals(sub, controller.subscribeCurrentUser(authentication, 2L))
        controller.unsubscribeCurrentUser(authentication, 2L)
        assertEquals(listOf(sub), controller.getRoomSubscriptions(2L))
        assertEquals(listOf(room), controller.getMySubscribedRooms(authentication))
        assertEquals(listOf(message), controller.getRoomMessages(2L))
        assertEquals(message, controller.getLatestMessage(2L))
        assertEquals(sub, controller.updateReadState(authentication, 2L, read))
        verify(service).deleteRoom("user@test.com", 2L)
        verify(service).unsubscribeCurrentUser("user@test.com", 2L)
    }

    @Test
    fun `event controller delegates event operations`() {
        val service: EventServiceInterface = mock()
        val controller = EventController(service)
        val create = CreateEventRequest("Star party", "desc", EventType.OTHER, now.plusSeconds(3600), null)
        val response = EventResponse(3L, "Star party", "OTHER", now, "User")
        val detail = EventDetailsResponse(3L, "Star party", "desc", "OTHER", now, null, 1L, 2, true, 10)
        val date = LocalDate.of(2026, 5, 23)
        whenever(service.createEvent(create, "user@test.com")).thenReturn(response)
        whenever(service.listAllEvents("user@test.com")).thenReturn(listOf(detail))
        whenever(service.listEventsForDate(date, "user@test.com")).thenReturn(listOf(detail))
        whenever(service.listUserFutureEvents("user@test.com")).thenReturn(listOf(detail))
        whenever(service.isUserParticipant(3L, "user@test.com")).thenReturn(true)

        assertEquals(response, controller.createEvent(create, "user@test.com"))
        controller.enrollToEvent(3L, "user@test.com")
        controller.signOutFromEvent(3L, "user@test.com")
        assertEquals(listOf(detail), controller.listAllEvents("user@test.com"))
        assertEquals(listOf(detail), controller.listEventsForDate(date, "user@test.com"))
        assertEquals(listOf(detail), controller.listMyEvents("user@test.com"))
        assertEquals(true, controller.isParticipant(3L, "user@test.com").isParticipant)
        verify(service).enrollToEvent(3L, "user@test.com")
        verify(service).signOutFromEvent(3L, "user@test.com")
    }

    @Test
    fun `celestial controller maps nullable service responses to status codes`() {
        val service: CelestialServiceInterface = mock()
        val controller = CelestialController(service)
        val summary = SpaceObjectSummary(1L, "Mars", 1.0, "PLANET", 1.0, 2.0, "red")
        val detail = PlanetResponse(1L, "Mars", 1.0, 1.0, 2.0, "red", orbitalModel = "model", lastComputed = now)
        val wiki = WikiResponse("Mars", "summary", "url", "image")
        val constellation = ConstellationResponse(4L, "Orion", "Western", "World", "hunter")
        val constellationDetail = ConstellationDetailResponse(4L, "Orion", "Western", "World", "hunter", listOf(summary))
        val culture = ConstellationCultureResponse(5L, "Western", "World", "desc", true)
        whenever(service.getAllSpaceObjects()).thenReturn(listOf(summary))
        whenever(service.getBrightStars(2.0)).thenReturn(listOf(summary))
        whenever(service.getSpaceObjectDetail(1L)).thenReturn(detail)
        whenever(service.getSpaceObjectDetail(2L)).thenReturn(null)
        whenever(service.getSpaceObjectWiki(1L)).thenReturn(wiki)
        whenever(service.getSpaceObjectWiki(2L)).thenReturn(null)
        whenever(service.getSpaceObjectWikiImage(1L)).thenReturn(byteArrayOf(9))
        whenever(service.getSpaceObjectWikiImageContentType(1L)).thenReturn("image/png")
        whenever(service.getSpaceObjectWikiImage(3L)).thenReturn(byteArrayOf(8))
        whenever(service.getSpaceObjectWikiImageContentType(3L)).thenReturn(null)
        whenever(service.getSpaceObjectWikiImage(2L)).thenReturn(null)
        whenever(service.getAllConstellations()).thenReturn(listOf(constellation))
        whenever(service.getConstellationDetail(4L)).thenReturn(constellationDetail)
        whenever(service.getConstellationDetail(6L)).thenReturn(null)
        whenever(service.getAllCultures()).thenReturn(listOf(culture))
        whenever(service.setCurrentCulture(5L)).thenReturn(true)
        whenever(service.setCurrentCulture(6L)).thenReturn(false)
        whenever(service.getAllConstellationsForCurrentCulture()).thenReturn(listOf(constellation))
        whenever(service.getConstellationDetailForCurrentCulture(4L)).thenReturn(constellationDetail)
        whenever(service.getConstellationDetailForCurrentCulture(6L)).thenReturn(null)

        assertEquals(listOf(summary), controller.getAllSpaceObjects())
        assertEquals(listOf(summary), controller.getBrightStars(2.0))
        assertEquals(HttpStatus.OK, controller.getSpaceObjectDetail(1L).statusCode)
        assertEquals(HttpStatus.NOT_FOUND, controller.getSpaceObjectDetail(2L).statusCode)
        assertEquals(HttpStatus.OK, controller.getSpaceObjectWiki(1L).statusCode)
        assertEquals(HttpStatus.NOT_FOUND, controller.getSpaceObjectWiki(2L).statusCode)
        assertArrayEquals(byteArrayOf(9), controller.getSpaceObjectWikiImage(1L).body)
        assertEquals("image/jpeg", controller.getSpaceObjectWikiImage(3L).headers.contentType.toString())
        assertEquals(HttpStatus.NOT_FOUND, controller.getSpaceObjectWikiImage(2L).statusCode)
        assertEquals(listOf(constellation), controller.getAllConstellations())
        assertEquals(HttpStatus.OK, controller.getConstellationDetail(4L).statusCode)
        assertEquals(HttpStatus.NOT_FOUND, controller.getConstellationDetail(6L).statusCode)
        assertEquals(listOf(culture), controller.getAllCultures())
        assertEquals(HttpStatus.OK, controller.setCurrentCulture(5L).statusCode)
        assertEquals(HttpStatus.NOT_FOUND, controller.setCurrentCulture(6L).statusCode)
        assertEquals(listOf(constellation), controller.getAllConstellationsForCurrentCulture())
        assertEquals(HttpStatus.OK, controller.getConstellationDetailForCurrentCulture(4L).statusCode)
        assertEquals(HttpStatus.NOT_FOUND, controller.getConstellationDetailForCurrentCulture(6L).statusCode)
    }

    private fun user() = com.skylink.backend.model.entity.User(
        id = 1L,
        email = "user@test.com",
        displayName = "User",
        role = UserRole.OBSERVER
    )

    private fun profileResponse() = UserProfileResponse(
        id = 1L,
        email = "user@test.com",
        displayName = "User",
        provider = AuthProvider.LOCAL,
        role = UserRole.OBSERVER,
        createdAt = now,
        lastUsedAt = null,
        pfpUrl = null
    )

    private fun favoriteResponse() = FavoriteResponse(
        id = 1L,
        spaceObject = SpaceObjectSummary(10L, "Vega", 0.03, "STAR", 1.0, 2.0, "bright"),
        note = "note",
        visibility = 1.0,
        addedAt = now
    )

    private fun chatRoomResponse() = ChatRoomResponse(
        id = 2L,
        name = "Global",
        type = ChatRoomType.GLOBAL,
        regionGeom = null,
        eventId = null,
        createdBy = 1L,
        createdAt = now
    )
}
