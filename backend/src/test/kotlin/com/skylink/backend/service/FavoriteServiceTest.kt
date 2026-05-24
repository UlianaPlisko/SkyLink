package com.skylink.backend.service

import com.skylink.backend.dto.favorite.FavoriteRequest
import com.skylink.backend.dto.favorite.FavoriteUpdateRequest
import com.skylink.backend.model.entity.Favorite
import com.skylink.backend.model.entity.SpaceObject
import com.skylink.backend.model.entity.User
import com.skylink.backend.model.enums.SpaceObjectType
import com.skylink.backend.model.enums.UserRole
import com.skylink.backend.repository.FavoriteRepository
import com.skylink.backend.repository.SpaceObjectRepository
import com.skylink.backend.service.user.UserProfileService
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
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

class FavoriteServiceTest {

    private val favoriteRepository: FavoriteRepository = mock()
    private val userProfileService: UserProfileService = mock()
    private val spaceObjectRepository: SpaceObjectRepository = mock()
    private val service = FavoriteService(favoriteRepository, userProfileService, spaceObjectRepository)

    @Test
    fun `get user favorites maps repository results`() {
        whenever(favoriteRepository.findByUserId(1L)).thenReturn(listOf(favorite(note = "watch", visibility = 0.8)))

        val responses = service.getUserFavorites(1L)

        assertEquals(1, responses.size)
        assertEquals("Vega", responses.single().spaceObject.displayName)
        assertEquals("STAR", responses.single().spaceObject.objectType)
        assertEquals("watch", responses.single().note)
        assertEquals(0.8, responses.single().visibility)
    }

    @Test
    fun `add favorite returns existing favorite without loading dependencies`() {
        val existing = favorite(note = "already")
        whenever(favoriteRepository.findByUserIdAndSpaceObjectId(1L, 10L)).thenReturn(Optional.of(existing))

        val response = service.addFavorite(1L, FavoriteRequest(spaceObjectId = 10L))

        assertEquals("already", response.note)
        verifyNoInteractions(userProfileService, spaceObjectRepository)
    }

    @Test
    fun `add favorite saves new favorite with default visibility`() {
        val user = user()
        val spaceObject = spaceObject()
        whenever(favoriteRepository.findByUserIdAndSpaceObjectId(1L, 10L)).thenReturn(Optional.empty())
        whenever(userProfileService.getById(1L)).thenReturn(user)
        whenever(spaceObjectRepository.findById(10L)).thenReturn(Optional.of(spaceObject))
        whenever(favoriteRepository.save(any())).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as Favorite
            Favorite(id = 99L, user = saved.user, spaceObject = saved.spaceObject, note = saved.note, visibility = saved.visibility)
        }

        val response = service.addFavorite(1L, FavoriteRequest(spaceObjectId = 10L, note = "new", visibility = null))

        assertEquals(99L, response.id)
        assertEquals("new", response.note)
        assertEquals(1.0, response.visibility)
    }

    @Test
    fun `add favorite saves explicit visibility`() {
        val user = user()
        val spaceObject = spaceObject()
        whenever(favoriteRepository.findByUserIdAndSpaceObjectId(1L, 10L)).thenReturn(Optional.empty())
        whenever(userProfileService.getById(1L)).thenReturn(user)
        whenever(spaceObjectRepository.findById(10L)).thenReturn(Optional.of(spaceObject))
        whenever(favoriteRepository.save(any())).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as Favorite
            Favorite(id = 100L, user = saved.user, spaceObject = saved.spaceObject, note = saved.note, visibility = saved.visibility)
        }

        val response = service.addFavorite(1L, FavoriteRequest(spaceObjectId = 10L, note = "new", visibility = 0.3))

        assertEquals("Vega", response.spaceObject.displayName)
        assertEquals(0.3, response.visibility)
    }

    @Test
    fun `add favorite throws when space object is missing`() {
        whenever(favoriteRepository.findByUserIdAndSpaceObjectId(1L, 10L)).thenReturn(Optional.empty())
        whenever(userProfileService.getById(1L)).thenReturn(user())
        whenever(spaceObjectRepository.findById(10L)).thenReturn(Optional.empty())

        assertThrows(ResponseStatusException::class.java) {
            service.addFavorite(1L, FavoriteRequest(spaceObjectId = 10L))
        }
    }

    @Test
    fun `update favorite trims notes and updates visibility`() {
        val favorite = favorite(note = "old", visibility = 0.4)
        whenever(favoriteRepository.findByUserIdAndSpaceObjectId(1L, 10L)).thenReturn(Optional.of(favorite))
        whenever(favoriteRepository.save(any())).thenAnswer { it.arguments[0] }

        val response = service.updateFavorite(1L, 10L, FavoriteUpdateRequest(note = "  better  ", visibility = 0.9))

        assertEquals("better", response.note)
        assertEquals(0.9, response.visibility)
    }

    @Test
    fun `update favorite clears blank note and keeps visibility when omitted`() {
        val favorite = favorite(note = "old", visibility = 0.4)
        whenever(favoriteRepository.findByUserIdAndSpaceObjectId(1L, 10L)).thenReturn(Optional.of(favorite))
        whenever(favoriteRepository.save(any())).thenAnswer { it.arguments[0] }

        val response = service.updateFavorite(1L, 10L, FavoriteUpdateRequest(note = "   "))

        assertNull(response.note)
        assertEquals(0.4, response.visibility)
    }

    @Test
    fun `update favorite accepts null note`() {
        val favorite = favorite(
            note = "old",
            visibility = 0.4
        )
        whenever(favoriteRepository.findByUserIdAndSpaceObjectId(1L, 10L)).thenReturn(Optional.of(favorite))
        whenever(favoriteRepository.save(any())).thenAnswer { it.arguments[0] }

        val response = service.updateFavorite(1L, 10L, FavoriteUpdateRequest(note = null, visibility = null))

        assertNull(response.note)
        assertEquals(0.4, response.visibility)
    }

    @Test
    fun `update favorite throws when missing`() {
        whenever(favoriteRepository.findByUserIdAndSpaceObjectId(1L, 10L)).thenReturn(Optional.empty())

        assertThrows(ResponseStatusException::class.java) {
            service.updateFavorite(1L, 10L, FavoriteUpdateRequest(note = "x"))
        }
    }

    @Test
    fun `remove favorite deletes existing favorite`() {
        val favorite = favorite()
        whenever(favoriteRepository.findByUserIdAndSpaceObjectId(1L, 10L)).thenReturn(Optional.of(favorite))

        assertTrue(service.removeFavorite(1L, 10L))
        verify(favoriteRepository).delete(favorite)
    }

    @Test
    fun `remove favorite returns false when favorite is missing`() {
        whenever(favoriteRepository.findByUserIdAndSpaceObjectId(1L, 10L)).thenReturn(Optional.empty())

        assertFalse(service.removeFavorite(1L, 10L))
    }

    private fun user() = User(
        id = 1L,
        email = "user@test.com",
        displayName = "User",
        role = UserRole.OBSERVER
    )

    private fun spaceObject() = SpaceObject(
        id = 10L,
        objectType = SpaceObjectType.STAR,
        raDeg = 279.2,
        decDeg = 38.8,
        magnitude = 0.03,
        displayName = "Vega",
        description = "Bright star"
    )

    private fun favorite(
        note: String? = null,
        visibility: Double = 1.0,
        spaceObject: SpaceObject = spaceObject()
    ) = Favorite(
        id = 5L,
        user = user(),
        spaceObject = spaceObject,
        note = note,
        visibility = visibility
    )
}
