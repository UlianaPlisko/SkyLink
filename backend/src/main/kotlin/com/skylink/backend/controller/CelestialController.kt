package com.skylink.backend.controller

import com.skylink.backend.dto.celestial.*
import com.skylink.backend.service.CelestialServiceInterface
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

@Tag(
    name = "Celestial Data",
    description = "Public operations related to space objects, constellations and constellation cultures"
)
@RestController
@RequestMapping("/api/celestial")
class CelestialController(
    private val celestialService: CelestialServiceInterface
) {

    @Operation(summary = "Get all space objects")
    @GetMapping("/space-objects")
    fun getAllSpaceObjects(): List<SpaceObjectSummary> {
        return celestialService.getAllSpaceObjects()
    }

    @Operation(summary = "Get bright stars filtered by maximum magnitude")
    @GetMapping("/bright-stars")
    fun getBrightStars(@RequestParam maxMagnitude: Double): List<SpaceObjectSummary> {
        return celestialService.getBrightStars(maxMagnitude)
    }

    @Operation(summary = "Get detailed information about a space object by ID")
    @GetMapping("/space-objects/{id}")
    fun getSpaceObjectDetail(@PathVariable id: Long): ResponseEntity<SpaceObjectDetailResponse> {
        val detail = celestialService.getSpaceObjectDetail(id)
        return if (detail != null) ResponseEntity.ok(detail) else ResponseEntity.notFound().build()
    }

    @Operation(summary = "Get Wikipedia information about a space object by ID")
    @GetMapping("/space-objects/{id}/wiki")
    fun getSpaceObjectWiki(@PathVariable id: Long): ResponseEntity<WikiResponse> {
        val wiki = celestialService.getSpaceObjectWiki(id)
        return if (wiki != null) ResponseEntity.ok(wiki) else ResponseEntity.notFound().build()
    }

    @Operation(summary = "Get proxied Wikipedia image for a space object by ID")
    @GetMapping("/space-objects/{id}/wiki-image")
    fun getSpaceObjectWikiImage(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val imageBytes = celestialService.getSpaceObjectWikiImage(id)
            ?: return ResponseEntity.notFound().build()

        val contentType = celestialService.getSpaceObjectWikiImageContentType(id) ?: "image/jpeg"

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .body(imageBytes)
    }

    @Operation(summary = "Get all constellations")
    @GetMapping("/constellations")
    fun getAllConstellations(): List<ConstellationResponse> {
        return celestialService.getAllConstellations()
    }

    @Operation(summary = "Get detailed information about a constellation by ID")
    @GetMapping("/constellations/{id}")
    fun getConstellationDetail(@PathVariable id: Long): ResponseEntity<ConstellationDetailResponse> {
        val detail = celestialService.getConstellationDetail(id)
        return if (detail != null) ResponseEntity.ok(detail) else ResponseEntity.notFound().build()
    }

    @Operation(summary = "Get all available constellation cultures")
    @GetMapping("/cultures")
    fun getAllCultures(): List<ConstellationCultureResponse> {
        return celestialService.getAllCultures()
    }

    @Operation(summary = "Set the current active constellation culture")
    @PutMapping("/cultures/{id}/current")
    fun setCurrentCulture(@PathVariable id: Long): ResponseEntity<Void> {
        val success = celestialService.setCurrentCulture(id)
        return if (success) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(summary = "Get all constellations for the current culture")
    @GetMapping("/constellations/current")
    fun getAllConstellationsForCurrentCulture(): List<ConstellationResponse> {
        return celestialService.getAllConstellationsForCurrentCulture()
    }

    @Operation(summary = "Get detailed information about a constellation by ID for the current culture")
    @GetMapping("/constellations/current/{id}")
    fun getConstellationDetailForCurrentCulture(@PathVariable id: Long): ResponseEntity<ConstellationDetailResponse> {
        val detail = celestialService.getConstellationDetailForCurrentCulture(id)
        return if (detail != null) ResponseEntity.ok(detail) else ResponseEntity.notFound().build()
    }
}