package com.skylink.backend

import com.skylink.backend.dto.celestial.DeepSkyObjectResponse
import com.skylink.backend.dto.celestial.PlanetResponse
import com.skylink.backend.dto.celestial.StarResponse
import com.skylink.backend.dto.celestial.WikiResponse
import com.skylink.backend.dto.chat.CreateChatRoomRequest
import com.skylink.backend.dto.chat.RawSocketRequest
import com.skylink.backend.dto.chat.RawSocketResponse
import com.skylink.backend.dto.chat.UpdateChatRoomRequest
import com.skylink.backend.config.ChatWebSocketHandler
import com.skylink.backend.model.UserChatSubscriptionId
import com.skylink.backend.model.entity.ChatRoom
import com.skylink.backend.model.entity.DeepSkyObjectData
import com.skylink.backend.model.entity.Favorite
import com.skylink.backend.model.entity.PlanetData
import com.skylink.backend.model.entity.SpaceObject
import com.skylink.backend.model.entity.StarData
import com.skylink.backend.model.entity.User
import com.skylink.backend.model.entity.UserChatSubscription
import com.skylink.backend.model.enums.ChatRoomType
import com.skylink.backend.model.enums.DeepSkyObjectClass
import com.skylink.backend.model.enums.SpaceObjectType
import com.skylink.backend.model.enums.UserRole
import com.skylink.backend.repository.ChatMessageRepository
import com.skylink.backend.repository.ChatRoomRepository
import com.skylink.backend.repository.ConstStarConnectionRepository
import com.skylink.backend.repository.ConstellationCultureRepository
import com.skylink.backend.repository.ConstellationRepository
import com.skylink.backend.repository.DeepSkyObjectDataRepository
import com.skylink.backend.repository.FavoriteRepository
import com.skylink.backend.repository.PlanetDataRepository
import com.skylink.backend.repository.SpaceObjectRepository
import com.skylink.backend.repository.StarDataRepository
import com.skylink.backend.repository.UserChatSubscriptionRepository
import com.skylink.backend.repository.UserRepository
import com.skylink.backend.service.CelestialService
import com.skylink.backend.service.ChatRoomService
import com.skylink.backend.service.FavoriteService
import com.skylink.backend.service.ChatMessagingServiceInterface
import com.skylink.backend.service.JwtService
import com.skylink.backend.service.WikipediaService
import com.skylink.backend.service.user.UserProfileService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.socket.WebSocketSession
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.Optional

class DefensiveBranchTest {

    @Test
    fun `celestial service uses fallback display names for incomplete objects`() {
        val spaceObjectRepository: SpaceObjectRepository = mock()
        val starDataRepository: StarDataRepository = mock()
        val planetDataRepository: PlanetDataRepository = mock()
        val deepSkyObjectDataRepository: DeepSkyObjectDataRepository = mock()
        val service = CelestialService(
            spaceObjectRepository,
            starDataRepository,
            planetDataRepository,
            deepSkyObjectDataRepository,
            mock<ConstellationRepository>(),
            mock<ConstStarConnectionRepository>(),
            mock<ConstellationCultureRepository>(),
            mock<WikipediaService>()
        )
        val star = mockedSpaceObject(1L, SpaceObjectType.STAR, displayName = null)
        val planet = mockedSpaceObject(2L, SpaceObjectType.PLANET, displayName = null)
        val galaxy = mockedSpaceObject(3L, SpaceObjectType.GALAXY, displayName = null)

        whenever(spaceObjectRepository.findAll()).thenReturn(listOf(star))
        whenever(spaceObjectRepository.findById(1L)).thenReturn(Optional.of(star))
        whenever(spaceObjectRepository.findById(2L)).thenReturn(Optional.of(planet))
        whenever(spaceObjectRepository.findById(3L)).thenReturn(Optional.of(galaxy))
        whenever(starDataRepository.findBySpaceObjectId(1L)).thenReturn(StarData(1L, star, 1.0))
        whenever(planetDataRepository.findBySpaceObjectId(2L)).thenReturn(
            PlanetData(2L, planet, "{}", Instant.parse("2026-05-23T08:00:00Z"))
        )
        whenever(deepSkyObjectDataRepository.findBySpaceObjectId(3L)).thenReturn(
            DeepSkyObjectData(3L, galaxy, null, DeepSkyObjectClass.DIFFUSE_NEBULA, null)
        )

        assertEquals("Unknown", service.getAllSpaceObjects().single().displayName)
        assertEquals("Unknown", (service.getSpaceObjectDetail(1L) as StarResponse).displayName)
        assertEquals("Unknown", (service.getSpaceObjectDetail(2L) as PlanetResponse).displayName)
        assertEquals("Unknown", (service.getSpaceObjectDetail(3L) as DeepSkyObjectResponse).displayName)
    }

    @Test
    fun `celestial service skips wiki details when summary data is incomplete`() {
        val spaceObjectRepository: SpaceObjectRepository = mock()
        val wikipediaService: WikipediaService = mock()
        val service = CelestialService(
            spaceObjectRepository,
            mock<StarDataRepository>(),
            mock<PlanetDataRepository>(),
            mock<DeepSkyObjectDataRepository>(),
            mock<ConstellationRepository>(),
            mock<ConstStarConnectionRepository>(),
            mock<ConstellationCultureRepository>(),
            wikipediaService
        )
        val unnamed = mockedSpaceObject(4L, SpaceObjectType.STAR, displayName = null)
        val named = mockedSpaceObject(5L, SpaceObjectType.STAR, displayName = "Edge")
        val nullish = mockedSpaceObject(6L, SpaceObjectType.STAR, displayName = "Nullish")
        val wiki = mock<WikiResponse>()
        val nullishWiki = mock<WikiResponse>()
        Mockito.doReturn("Edge").`when`(wiki).title
        Mockito.doReturn("url").`when`(wiki).url
        Mockito.doReturn(null).`when`(wiki).imageUrl
        Mockito.doReturn("Summary", "Summary", "").`when`(wiki).summary
        Mockito.doReturn("Nullish").`when`(nullishWiki).title
        Mockito.doReturn("url").`when`(nullishWiki).url
        Mockito.doReturn(null).`when`(nullishWiki).imageUrl
        Mockito.doReturn("Summary", "Summary", null).`when`(nullishWiki).summary

        whenever(spaceObjectRepository.findById(4L)).thenReturn(Optional.of(unnamed))
        whenever(spaceObjectRepository.findById(5L)).thenReturn(Optional.of(named))
        whenever(spaceObjectRepository.findById(6L)).thenReturn(Optional.of(nullish))
        whenever(wikipediaService.fetchSummary("Edge")).thenReturn(wiki)
        whenever(wikipediaService.fetchSummary("Nullish")).thenReturn(nullishWiki)

        assertNull(service.getSpaceObjectWiki(4L))
        assertNull(service.getSpaceObjectWiki(5L))
        assertNull(service.getSpaceObjectWiki(6L))
    }

    @Test
    fun `chat room service keeps event data consistent for create and update requests`() {
        val chatRoomRepository: ChatRoomRepository = mock()
        val userRepository: UserRepository = mock()
        val subscriptionRepository: UserChatSubscriptionRepository = mock()
        val service = ChatRoomService(
            chatRoomRepository,
            mock<ChatMessageRepository>(),
            subscriptionRepository,
            userRepository
        )
        val user = user()
        val room = ChatRoom(id = 2L, name = "Room", type = ChatRoomType.EVENT, eventId = 2L, createdBy = user.id)
        val createRequest = mock<CreateChatRoomRequest>()
        val updateRequest = mock<UpdateChatRoomRequest>()

        Mockito.doReturn("Edge").`when`(createRequest).name
        Mockito.doReturn(ChatRoomType.EVENT).`when`(createRequest).type
        Mockito.doReturn(null).`when`(createRequest).regionGeom
        Mockito.doReturn(9L, null).`when`(createRequest).eventId
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(chatRoomRepository.save(any())).thenAnswer { invocation ->
            (invocation.arguments[0] as ChatRoom).copy(id = 4L)
        }
        whenever(subscriptionRepository.save(any())).thenAnswer { it.arguments[0] as UserChatSubscription }

        assertEquals(4L, service.createRoom(user.email, createRequest).id)

        Mockito.doReturn("Edge Updated").`when`(updateRequest).name
        Mockito.doReturn(ChatRoomType.EVENT).`when`(updateRequest).type
        Mockito.doReturn(null).`when`(updateRequest).regionGeom
        Mockito.doReturn(10L, null, null).`when`(updateRequest).eventId
        whenever(chatRoomRepository.findById(room.id)).thenReturn(Optional.of(room))
        whenever(chatRoomRepository.save(room)).thenReturn(room)

        assertNull(service.updateRoom(room.id, updateRequest).eventId)
    }

    @Test
    fun `favorite service maps incomplete saved objects with fallback display names`() {
        val favoriteRepository: FavoriteRepository = mock()
        val service = FavoriteService(
            favoriteRepository,
            mock<UserProfileService>(),
            mock<SpaceObjectRepository>()
        )
        val favorite = Favorite(
            id = 1L,
            user = user(),
            spaceObject = mockedSpaceObject(1L, SpaceObjectType.STAR, displayName = null),
            note = null,
            visibility = 1.0
        )
        whenever(favoriteRepository.findByUserId(1L)).thenReturn(listOf(favorite))

        assertEquals("Unknown", service.getUserFavorites(1L).single().spaceObject.displayName)
    }

    @Test
    fun `profile picture upload rejects files without a content type`() {
        val userRepository: UserRepository = mock()
        val service = UserProfileService(userRepository, mock())
        val file = mock<MultipartFile>()
        val user = user()
        whenever(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        whenever(file.isEmpty).thenReturn(false)
        Mockito.doReturn(null).`when`(file).contentType

        org.junit.jupiter.api.assertThrows<ResponseStatusException> {
            service.updatePfp(user.email, file)
        }
    }

    @Test
    fun `websocket auth rejects authentication messages without a token`() {
        val objectMapper: ObjectMapper = mock()
        val handler = ChatWebSocketHandler(
            objectMapper,
            mock<JwtService>(),
            mock<UserRepository>(),
            mock<UserChatSubscriptionRepository>(),
            mock<ChatMessagingServiceInterface>()
        )
        val session: WebSocketSession = mock {
            on { id }.thenReturn("session-edge")
            on { isOpen }.thenReturn(true)
        }
        val request = mock<RawSocketRequest>()
        Mockito.doReturn("AUTH").`when`(request).type
        Mockito.doReturn(null).`when`(request).token
        whenever(objectMapper.writeValueAsString(any<RawSocketResponse>())).thenReturn("json")
        val method = ChatWebSocketHandler::class.java.getDeclaredMethod(
            "handleAuth",
            WebSocketSession::class.java,
            RawSocketRequest::class.java
        )

        method.isAccessible = true
        org.junit.jupiter.api.assertThrows<java.lang.reflect.InvocationTargetException> {
            method.invoke(handler, session, request)
        }
    }

    private fun mockedSpaceObject(
        id: Long,
        objectType: SpaceObjectType,
        displayName: String?
    ): SpaceObject {
        val spaceObject = mock<SpaceObject>()
        Mockito.doReturn(id).`when`(spaceObject).id
        Mockito.doReturn(objectType).`when`(spaceObject).objectType
        Mockito.doReturn(1.0).`when`(spaceObject).raDeg
        Mockito.doReturn(2.0).`when`(spaceObject).decDeg
        Mockito.doReturn(0.3).`when`(spaceObject).magnitude
        Mockito.doReturn(displayName).`when`(spaceObject).displayName
        Mockito.doReturn("description").`when`(spaceObject).description
        return spaceObject
    }

    private fun user() = User(
        id = 1L,
        email = "user@test.com",
        displayName = "User",
        role = UserRole.OBSERVER
    )
}
