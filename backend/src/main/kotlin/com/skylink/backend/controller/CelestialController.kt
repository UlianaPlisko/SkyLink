package com.skylink.backend.controller

import com.skylink.backend.dto.celestial.*
import com.skylink.backend.service.CelestialServiceInterface
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller for celestial data endpoints.
 * No authentication required—public access for star/constellation data.
 */
@RestController
@RequestMapping("/api/celestial")
class CelestialController(
    private val celestialService: CelestialServiceInterface
) {

    @GetMapping("/space-objects")
    fun getAllSpaceObjects(): List<SpaceObjectSummary> {
        return celestialService.getAllSpaceObjects()
    }

    @GetMapping("/bright-stars")
    fun getBrightStars(@RequestParam maxMagnitude: Double): List<SpaceObjectSummary> {
        return celestialService.getBrightStars(maxMagnitude)
    }

    /**
     * Retrieves detailed information for any space object (star, planet, deep sky object).
     * Returns the appropriate DTO based on the object's type.
     */
    @GetMapping("/space-objects/{id}")
    fun getSpaceObjectDetail(@PathVariable id: Long): ResponseEntity<Any> {
        val detail = celestialService.getSpaceObjectDetail(id)
        return if (detail != null) ResponseEntity.ok(detail) else ResponseEntity.notFound().build()
    }

    @GetMapping("/constellations")
    fun getAllConstellations(): List<ConstellationResponse> {
        return celestialService.getAllConstellations()
    }

    @GetMapping("/constellations/{id}")
    fun getConstellationDetail(@PathVariable id: Long): ResponseEntity<ConstellationDetailResponse> {
        val detail = celestialService.getConstellationDetail(id)
        return if (detail != null) ResponseEntity.ok(detail) else ResponseEntity.notFound().build()
    }
}