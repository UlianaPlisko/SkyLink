package com.skylink.backend.controller

import com.skylink.backend.dto.favorite.FavoriteRequest
import com.skylink.backend.dto.favorite.FavoriteResponse
import com.skylink.backend.service.FavoriteService
import com.skylink.backend.service.user.UserProfileService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

@Tag(name = "Favorites", description = "User favorites management")
@RestController
@RequestMapping("/api/favorites")
class FavoriteController(
    private val favoriteService: FavoriteService,
    private val userService: UserProfileService
) {

    @Operation(summary = "Get user favorites")
    @GetMapping
    fun getFavorites(principal: Principal): List<FavoriteResponse> {
        val user = userService.getByEmail(principal.name)
        return favoriteService.getUserFavorites(user.id)
    }

    @Operation(summary = "Add item to favorites")
    @PostMapping
    fun addFavorite(
        @RequestBody request: FavoriteRequest,
        principal: Principal
    ): FavoriteResponse {
        val user = userService.getByEmail(principal.name)
        return favoriteService.addFavorite(user.id, request)
    }

    @Operation(summary = "Remove item from favorites")
    @DeleteMapping("/{spaceObjectId}")
    fun removeFavorite(
        @PathVariable spaceObjectId: Long,
        principal: Principal
    ): ResponseEntity<Void> {
        val user = userService.getByEmail(principal.name)

        return if (favoriteService.removeFavorite(user.id, spaceObjectId)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}