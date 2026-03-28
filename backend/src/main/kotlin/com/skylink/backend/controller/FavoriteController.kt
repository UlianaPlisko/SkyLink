package com.skylink.backend.controller

import com.skylink.backend.dto.favorite.FavoriteRequest
import com.skylink.backend.dto.favorite.FavoriteResponse
import com.skylink.backend.service.FavoriteService
import com.skylink.backend.service.user.UserProfileService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/favorites")
class FavoriteController(
    private val favoriteService: FavoriteService,
    private val userService: UserProfileService
) {

    @GetMapping
    fun getFavorites(@AuthenticationPrincipal email: String): List<FavoriteResponse> {
        val user = userService.getByEmail(email)
        return favoriteService.getUserFavorites(user.id)
    }

    @PostMapping
    fun addFavorite(
        @RequestBody request: FavoriteRequest,
        @AuthenticationPrincipal email: String
    ): FavoriteResponse {
        val user = userService.getByEmail(email)
        return favoriteService.addFavorite(user.id, request)
    }

    @DeleteMapping("/{spaceObjectId}")
    fun removeFavorite(
        @PathVariable spaceObjectId: Long,
        @AuthenticationPrincipal email: String
    ): ResponseEntity<String> {
        val user = userService.getByEmail(email)
        favoriteService.removeFavorite(user.id, spaceObjectId)
        return ResponseEntity.ok("Removed from favorites")
    }
}