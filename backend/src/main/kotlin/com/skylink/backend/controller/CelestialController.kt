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

    @GetMapping("/cultures")
    fun getAllCultures(): List<ConstellationCultureResponse> {
        return celestialService.getAllCultures()
    }

    @PutMapping("/cultures/{id}/current")
    fun setCurrentCulture(@PathVariable id: Long): ResponseEntity<Void> {
        val success = celestialService.setCurrentCulture(id)
        return if (success) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/constellations/current")
    fun getAllConstellationsForCurrentCulture(): List<ConstellationResponse> {
        return celestialService.getAllConstellationsForCurrentCulture()
    }

    @GetMapping("/constellations/current/{id}")
    fun getConstellationDetailForCurrentCulture(@PathVariable id: Long): ResponseEntity<ConstellationDetailResponse> {
        val detail = celestialService.getConstellationDetailForCurrentCulture(id)
        return if (detail != null) ResponseEntity.ok(detail) else ResponseEntity.notFound().build()
    }
}