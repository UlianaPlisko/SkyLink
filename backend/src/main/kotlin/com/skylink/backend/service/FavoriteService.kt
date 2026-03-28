package com.skylink.backend.service

import com.skylink.backend.dto.celestial.SpaceObjectSummary
import com.skylink.backend.dto.favorite.FavoriteRequest
import com.skylink.backend.dto.favorite.FavoriteResponse
import com.skylink.backend.model.entity.Favorite
import com.skylink.backend.repository.FavoriteRepository
import com.skylink.backend.repository.SpaceObjectRepository
import com.skylink.backend.service.user.UserProfileService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FavoriteService(
    private val favoriteRepository: FavoriteRepository,
    private val userService: UserProfileService,
    private val spaceObjectRepository: SpaceObjectRepository
) {

    @Transactional(readOnly = true)
    fun getUserFavorites(userId: Long): List<FavoriteResponse> {
        return favoriteRepository.findByUserId(userId)
            .map { it.toResponse() }
    }

    @Transactional
    fun addFavorite(userId: Long, request: FavoriteRequest): FavoriteResponse {

        val existing = favoriteRepository
            .findByUserIdAndSpaceObjectId(userId, request.spaceObjectId)

        if (existing.isPresent) return existing.get().toResponse()

        val user = userService.getById(userId)

        val spaceObject = spaceObjectRepository.findById(request.spaceObjectId)
            .orElseThrow { IllegalArgumentException("SpaceObject not found") }

        val favorite = Favorite(
            user = user,
            spaceObject = spaceObject,
            note = request.note,
            visibility = request.visibility ?: 1.0
        )

        return favoriteRepository.save(favorite).toResponse()
    }

    @Transactional
    fun removeFavorite(userId: Long, spaceObjectId: Long) {
        favoriteRepository.deleteByUserIdAndSpaceObjectId(userId, spaceObjectId)
    }


    private fun Favorite.toResponse(): FavoriteResponse {
        return FavoriteResponse(
            id = id,
            spaceObject = spaceObject.toSummary(),
            note = note,
            visibility = visibility,
            addedAt = addedAt
        )
    }

    private fun com.skylink.backend.model.entity.SpaceObject.toSummary(): SpaceObjectSummary {
        return SpaceObjectSummary(
            id = id,
            displayName = displayName ?: "Unknown",
            magnitude = magnitude,
            objectType = objectType.name,
            raDeg = raDeg ?: 0.0,
            decDeg = decDeg ?: 0.0,
            description = description
        )
    }
}