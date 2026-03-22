package com.skylink.backend.service

import com.skylink.backend.dto.celestial.*
import com.skylink.backend.model.entity.*
import com.skylink.backend.model.enums.*
import com.skylink.backend.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service implementation for all celestial data operations.
 *
 * This service acts as the single point of truth for retrieving stars, constellations,
 * and related astronomical information from the database. It is optimized for read-heavy
 * usage (star maps, constellation details, real-time sky rendering) and integrates
 * with the mobile app's 2D sky modeling needs.
 *
 * All operations are marked `@Transactional(readOnly = true)` for maximum performance
 * and to avoid accidental writes.
 *
 * @see CelestialServiceInterface
 */
@Service
class CelestialService(
    private val spaceObjectRepository: SpaceObjectRepository,
    private val starDataRepository: StarDataRepository,
    private val planetDataRepository: PlanetDataRepository,
    private val deepSkyObjectDataRepository: DeepSkyObjectDataRepository,
    private val constellationRepository: ConstellationRepository,
    private val constStarConnectionRepository: ConstStarConnectionRepository,
    private val constellationCultureRepository: ConstellationCultureRepository
) : CelestialServiceInterface {

    /**
     * Returns **all** space objects in the database (stars, planets, deep sky objects, etc.).
     *
     * This is the primary method used by the Android app for full 2D sky modeling
     * (rendering the entire star map). Returns lightweight summaries only.
     * Note: For dynamic objects like planets, positions may need separate computation.
     *
     * @return list of all space objects as `SpaceObjectSummary`
     */
    @Transactional(readOnly = true)
    override fun getAllSpaceObjects(): List<SpaceObjectSummary> {
        return spaceObjectRepository.findAll().map { it.toSummary() }
    }

    /**
     * Returns only bright stars (magnitude ≤ `maxMagnitude`).
     *
     * Useful for performance-optimized rendering on mobile devices or when the user
     * filters the sky view to show only visible stars.
     *
     * @param maxMagnitude maximum apparent magnitude (e.g. 6.0 for naked-eye visible)
     * @return filtered list of bright stars as `SpaceObjectSummary`
     */
    @Transactional(readOnly = true)
    override fun getBrightStars(maxMagnitude: Double): List<SpaceObjectSummary> {
        return spaceObjectRepository.findBrightStars(maxMagnitude).map { it.toSummary() }
    }

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
    @Transactional(readOnly = true)
    override fun getSpaceObjectDetail(id: Long): Any? {
        val spaceObject = spaceObjectRepository.findById(id).orElse(null) ?: return null

        return when (spaceObject.objectType) {
            SpaceObjectType.STAR -> getStarDetail(spaceObject)
            SpaceObjectType.PLANET -> getPlanetDetail(spaceObject)
            SpaceObjectType.GALAXY -> getDeepSkyObjectDetail(spaceObject)
            else -> null  // Unsupported type
        }
    }

    /**
     * Private method to build star details.
     */
    private fun getStarDetail(spaceObject: SpaceObject): StarResponse? {
        val starData = starDataRepository.findBySpaceObjectId(spaceObject.id) ?: return null
        return StarResponse(
            id = spaceObject.id,
            displayName = spaceObject.displayName ?: "Unknown",
            magnitude = spaceObject.magnitude,
            raDeg = spaceObject.raDeg ?: 0.0,
            decDeg = spaceObject.decDeg ?: 0.0,
            constellation = starData.constellation,
            spectralType = starData.spectralType,
            distanceLy = starData.distanceLy
        )
    }

    /**
     * Private method to build planet details.
     */
    private fun getPlanetDetail(spaceObject: SpaceObject): PlanetResponse? {
        val planetData = planetDataRepository.findBySpaceObjectId(spaceObject.id) ?: return null
        return PlanetResponse(
            id = spaceObject.id,
            displayName = spaceObject.displayName ?: "Unknown",
            magnitude = spaceObject.magnitude,
            raDeg = spaceObject.raDeg ?: 0.0,
            decDeg = spaceObject.decDeg ?: 0.0,
            orbitalModel = planetData.orbitalModel,
            lastComputed = planetData.lastComputed
        )
    }

    /**
     * Private method to build deep sky object details.
     */
    private fun getDeepSkyObjectDetail(spaceObject: SpaceObject): DeepSkyObjectResponse? {
        val dsoData = deepSkyObjectDataRepository.findBySpaceObjectId(spaceObject.id) ?: return null
        return DeepSkyObjectResponse(
            id = spaceObject.id,
            displayName = spaceObject.displayName ?: "Unknown",
            magnitude = spaceObject.magnitude,
            raDeg = spaceObject.raDeg ?: 0.0,
            decDeg = spaceObject.decDeg ?: 0.0,
            catalogId = dsoData.catalogId,
            objectClass = dsoData.objectClass.name,
            angularSize = dsoData.angularSize
        )
    }

    /**
     * Returns a list of all constellations with basic metadata.
     *
     * Used to populate the constellation browser / list screen in the app.
     * Fetches culture data in a single query to avoid lazy loading issues.
     *
     * @return list of all constellations as `ConstellationResponse`
     */
    @Transactional(readOnly = true)
    override fun getAllConstellations(): List<ConstellationResponse> {
        return constellationRepository.findAllWithCulture().map { it.toResponse() }
    }

    /**
     * Returns full details of a constellation **including** all its member stars
     * and the linked culture information (culture name + region).
     *
     * Member stars are fetched via the `ConstStarConnection` table (unique stars from connections).
     * Perfect for the "Constellation Detail" screen.
     *
     * @param id ID of the constellation
     * @return detailed constellation with its stars or `null` if not found
     */
    @Transactional(readOnly = true)
    override fun getConstellationDetail(id: Long): ConstellationDetailResponse? {
        val constellation = constellationRepository.findByIdWithCulture(id).orElse(null) ?: return null

        val connections = constStarConnectionRepository.findByConstellationId(constellation.id)
        val starIds = connections.flatMap { listOf(it.id.star1Id, it.id.star2Id) }.toSet()
        val stars = spaceObjectRepository.findAllById(starIds).map { it.toSummary() }

        return ConstellationDetailResponse(
            id = constellation.id,
            name = constellation.name,
            cultureName = constellation.culture.name,
            region = constellation.culture.region,
            description = constellation.description,
            stars = stars
        )
    }

    @Transactional(readOnly = true)
    override fun getAllCultures(): List<ConstellationCultureResponse> {
        return constellationCultureRepository.findAll().map { it.toResponse() }
    }

    @Transactional
    override fun setCurrentCulture(cultureId: Long): Boolean {
        if (!constellationCultureRepository.existsById(cultureId)) {
            return false
        }
        constellationCultureRepository.setAllToNotCurrent()
        constellationCultureRepository.setCurrent(cultureId)
        return true
    }

    @Transactional(readOnly = true)
    override fun getAllConstellationsForCurrentCulture(): List<ConstellationResponse> {
        return constellationRepository.findAllWithCurrentCulture().map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    override fun getConstellationDetailForCurrentCulture(id: Long): ConstellationDetailResponse? {
        val constellation = constellationRepository.findByIdWithCurrentCulture(id).orElse(null) ?: return null

        val connections = constStarConnectionRepository.findByConstellationId(constellation.id)
        val starIds = connections.flatMap { listOf(it.id.star1Id, it.id.star2Id) }.toSet()
        val stars = spaceObjectRepository.findAllById(starIds).map { it.toSummary() }

        return ConstellationDetailResponse(
            id = constellation.id,
            name = constellation.name,
            cultureName = constellation.culture.name,
            region = constellation.culture.region,
            description = constellation.description,
            stars = stars
        )
    }

    // ====================== Private mappers ======================

    /**
     * Converts a `SpaceObject` entity to the lightweight `SpaceObjectSummary` DTO.
     */
    private fun SpaceObject.toSummary() = SpaceObjectSummary(
        id = id,
        displayName = displayName ?: "Unknown",
        magnitude = magnitude,
        objectType = objectType.name,  // Enum to String
        raDeg = raDeg ?: 0.0,
        decDeg = decDeg ?: 0.0,
        description = description
    )

    /**
     * Maps Constellation (with joined culture) to DTO.
     */
    private fun Constellation.toResponse() = ConstellationResponse(
        id = id,
        name = name,
        cultureName = culture.name,
        region = culture.region,
        description = description
    )

    private fun ConstellationCulture.toResponse() = ConstellationCultureResponse(
        id = id,
        name = name,
        region = region,
        description = description,
        isCurrent = isCurrent
    )
}