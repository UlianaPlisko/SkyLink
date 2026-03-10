package com.skylink.backend.service

import com.skylink.backend.dto.celestial.*

interface CelestialServiceInterface {

    /** All space objects – perfect for 2D sky modeling / star map */
    fun getAllSpaceObjects(): List<SpaceObjectSummary>

    /** Bright stars filtered by magnitude (reuses your existing repo method) */
    fun getBrightStars(maxMagnitude: Double): List<SpaceObjectSummary>

    /** List of all constellations */
    fun getAllConstellations(): List<ConstellationResponse>

    /**
     * Retrieves detailed information about a space object, dispatching based on its type.
     *
     * This method fetches the core `SpaceObject` and then retrieves type-specific details
     * (e.g., StarData, PlanetData, DeepSkyObjectData). Returns the appropriate response DTO
     * or `null` if the object doesn't exist or has an unsupported type.
     *
     * @param id ID of the space object
     * @return type-specific detail response or `null`
     */
    fun getSpaceObjectDetail(id: Long): Any?

    /** Constellation detail with its stars (this is the connection you asked for) */
    fun getConstellationDetail(id: Long): ConstellationDetailResponse?
}