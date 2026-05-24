package com.skylink.backend.service

import com.skylink.backend.dto.celestial.DeepSkyObjectResponse
import com.skylink.backend.dto.celestial.PlanetResponse
import com.skylink.backend.dto.celestial.StarResponse
import com.skylink.backend.dto.celestial.WikiResponse
import com.skylink.backend.model.ConstStarConnectionId
import com.skylink.backend.model.entity.ConstStarConnection
import com.skylink.backend.model.entity.Constellation
import com.skylink.backend.model.entity.ConstellationCulture
import com.skylink.backend.model.entity.DeepSkyObjectData
import com.skylink.backend.model.entity.PlanetData
import com.skylink.backend.model.entity.SpaceObject
import com.skylink.backend.model.entity.StarData
import com.skylink.backend.model.enums.DeepSkyObjectClass
import com.skylink.backend.model.enums.SpaceObjectType
import com.skylink.backend.repository.ConstStarConnectionRepository
import com.skylink.backend.repository.ConstellationCultureRepository
import com.skylink.backend.repository.ConstellationRepository
import com.skylink.backend.repository.DeepSkyObjectDataRepository
import com.skylink.backend.repository.PlanetDataRepository
import com.skylink.backend.repository.SpaceObjectRepository
import com.skylink.backend.repository.StarDataRepository
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.Optional

class CelestialServiceTest {

    private val spaceObjectRepository: SpaceObjectRepository = mock()
    private val starDataRepository: StarDataRepository = mock()
    private val planetDataRepository: PlanetDataRepository = mock()
    private val deepSkyObjectDataRepository: DeepSkyObjectDataRepository = mock()
    private val constellationRepository: ConstellationRepository = mock()
    private val constStarConnectionRepository: ConstStarConnectionRepository = mock()
    private val constellationCultureRepository: ConstellationCultureRepository = mock()
    private val wikipediaService: WikipediaService = mock()
    private val service = CelestialService(
        spaceObjectRepository,
        starDataRepository,
        planetDataRepository,
        deepSkyObjectDataRepository,
        constellationRepository,
        constStarConnectionRepository,
        constellationCultureRepository,
        wikipediaService
    )

    @Test
    fun `space object lists map summaries`() {
        val star = spaceObject(1L, "Vega", SpaceObjectType.STAR)
        val planet = spaceObject(2L, "Mars", SpaceObjectType.PLANET, magnitude = null)
        whenever(spaceObjectRepository.findAll()).thenReturn(listOf(star, planet))
        whenever(spaceObjectRepository.findBrightStars(2.0)).thenReturn(listOf(star))

        val all = service.getAllSpaceObjects()
        val bright = service.getBrightStars(2.0)

        assertEquals(listOf("Vega", "Mars"), all.map { it.displayName })
        assertEquals(0.03, all.first().magnitude)
        assertEquals("STAR", bright.single().objectType)
    }

    @Test
    fun `space object detail dispatches by object type and handles missing data`() {
        val star = spaceObject(1L, "Vega", SpaceObjectType.STAR)
        val planet = spaceObject(2L, "Mars", SpaceObjectType.PLANET)
        val galaxy = spaceObject(3L, "Andromeda", SpaceObjectType.GALAXY)
        val nebula = spaceObject(4L, "Nebula", SpaceObjectType.NEBULA)
        whenever(spaceObjectRepository.findById(1L)).thenReturn(Optional.of(star))
        whenever(spaceObjectRepository.findById(2L)).thenReturn(Optional.of(planet))
        whenever(spaceObjectRepository.findById(3L)).thenReturn(Optional.of(galaxy))
        whenever(spaceObjectRepository.findById(4L)).thenReturn(Optional.of(nebula))
        whenever(spaceObjectRepository.findById(404L)).thenReturn(Optional.empty())
        whenever(starDataRepository.findBySpaceObjectId(1L)).thenReturn(
            StarData(1L, star, 1.0, constellation = "LYR", spectralType = "A0V", distanceLy = 25.0)
        )
        whenever(planetDataRepository.findBySpaceObjectId(2L)).thenReturn(
            PlanetData(2L, planet, "{}", Instant.parse("2026-05-23T08:00:00Z"))
        )
        whenever(deepSkyObjectDataRepository.findBySpaceObjectId(3L)).thenReturn(
            DeepSkyObjectData(3L, galaxy, "M31", DeepSkyObjectClass.SPIRAL_GALAXY, 3.1)
        )

        assertEquals("LYR", (service.getSpaceObjectDetail(1L) as StarResponse).constellation)
        assertEquals("{}", (service.getSpaceObjectDetail(2L) as PlanetResponse).orbitalModel)
        assertEquals("M31", (service.getSpaceObjectDetail(3L) as DeepSkyObjectResponse).catalogId)
        assertNull(service.getSpaceObjectDetail(4L))
        assertNull(service.getSpaceObjectDetail(404L))

        whenever(starDataRepository.findBySpaceObjectId(1L)).thenReturn(null)
        whenever(planetDataRepository.findBySpaceObjectId(2L)).thenReturn(null)
        whenever(deepSkyObjectDataRepository.findBySpaceObjectId(3L)).thenReturn(null)
        assertNull(service.getSpaceObjectDetail(1L))
        assertNull(service.getSpaceObjectDetail(2L))
        assertNull(service.getSpaceObjectDetail(3L))
    }

    @Test
    fun `wiki lookup rejects bad summaries and proxies accepted images`() {
        val sol = spaceObject(1L, "Sol", SpaceObjectType.STAR)
        whenever(spaceObjectRepository.findById(1L)).thenReturn(Optional.of(sol))
        whenever(spaceObjectRepository.findById(404L)).thenReturn(Optional.empty())
        whenever(wikipediaService.fetchSummary("Sun")).thenReturn(null)
        whenever(wikipediaService.fetchSummary("Sun star")).thenReturn(WikiResponse("Sun", "may refer to:", "url", "image"))
        whenever(wikipediaService.fetchSummary("Sun astronomy")).thenReturn(WikiResponse("Sun", "A star", "url", "http://image"))
        whenever(wikipediaService.downloadImage("http://image")).thenReturn(byteArrayOf(1, 2, 3))
        whenever(wikipediaService.guessContentType("http://image")).thenReturn("image/png")

        val wiki = service.getSpaceObjectWiki(1L)

        assertEquals("/api/celestial/space-objects/1/wiki-image", wiki?.imageUrl)
        assertArrayEquals(byteArrayOf(1, 2, 3), service.getSpaceObjectWikiImage(1L))
        assertEquals("image/png", service.getSpaceObjectWikiImageContentType(1L))
        assertNull(service.getSpaceObjectWiki(404L))
        assertNull(service.getSpaceObjectWikiImage(404L))
        assertNull(service.getSpaceObjectWikiImageContentType(404L))
        verify(wikipediaService, times(3)).fetchSummary("Sun")
        verify(wikipediaService, times(3)).fetchSummary("Sun star")
        verify(wikipediaService, times(3)).fetchSummary("Sun astronomy")
    }

    @Test
    fun `wiki lookup tries type specific candidates until one is accepted`() {
        val mars = spaceObject(2L, "Mars", SpaceObjectType.PLANET)
        whenever(spaceObjectRepository.findById(2L)).thenReturn(Optional.of(mars))
        whenever(wikipediaService.fetchSummary("Mars")).thenReturn(null)
        whenever(wikipediaService.fetchSummary("Mars planet")).thenReturn(WikiResponse("Mars", "Planet summary", "url", null))

        val wiki = service.getSpaceObjectWiki(2L)

        assertEquals("Mars", wiki?.title)
        assertEquals("/api/celestial/space-objects/2/wiki-image", wiki?.imageUrl)
        verify(wikipediaService).fetchSummary("Mars")
        verify(wikipediaService).fetchSummary("Mars planet")
        verify(wikipediaService, never()).fetchSummary("Mars astronomy")
    }

    @Test
    fun `wiki lookup handles blank summaries no images and type specific candidates`() {
        val planet = spaceObject(2L, "Mars", SpaceObjectType.PLANET)
        val galaxy = spaceObject(3L, "Andromeda", SpaceObjectType.GALAXY)
        val nebula = spaceObject(4L, "Nebula", SpaceObjectType.NEBULA)
        val luna = spaceObject(5L, "Luna", SpaceObjectType.PLANET)
        val terra = spaceObject(6L, "Terra", SpaceObjectType.PLANET)
        whenever(spaceObjectRepository.findById(2L)).thenReturn(Optional.of(planet))
        whenever(spaceObjectRepository.findById(3L)).thenReturn(Optional.of(galaxy))
        whenever(spaceObjectRepository.findById(4L)).thenReturn(Optional.of(nebula))
        whenever(spaceObjectRepository.findById(5L)).thenReturn(Optional.of(luna))
        whenever(spaceObjectRepository.findById(6L)).thenReturn(Optional.of(terra))
        whenever(wikipediaService.fetchSummary(any())).thenReturn(WikiResponse("Any", "", "url", null))

        assertNull(service.getSpaceObjectWiki(2L))
        assertNull(service.getSpaceObjectWikiImage(2L))
        assertNull(service.getSpaceObjectWikiImageContentType(2L))

        whenever(wikipediaService.fetchSummary(any())).thenReturn(WikiResponse("Any", "Summary", "url", null))
        assertNull(service.getSpaceObjectWikiImage(3L))
        assertNull(service.getSpaceObjectWikiImageContentType(4L))
        assertEquals("Any", service.getSpaceObjectWiki(5L)?.title)
        assertEquals("Any", service.getSpaceObjectWiki(6L)?.title)
    }

    @Test
    fun `wiki lookup rejects every disambiguation wording`() {
        val nullSummary = spaceObject(7L, "Null Summary", SpaceObjectType.STAR)
        val mayAlsoRefer = spaceObject(8L, "Also Refer", SpaceObjectType.STAR)
        val canReferTo = spaceObject(9L, "Can Refer", SpaceObjectType.STAR)
        whenever(spaceObjectRepository.findById(7L)).thenReturn(Optional.of(nullSummary))
        whenever(spaceObjectRepository.findById(8L)).thenReturn(Optional.of(mayAlsoRefer))
        whenever(spaceObjectRepository.findById(9L)).thenReturn(Optional.of(canReferTo))
        whenever(wikipediaService.fetchSummary("Null Summary")).thenReturn(WikiResponse("Null Summary", null, "url", null))
        whenever(wikipediaService.fetchSummary("Also Refer")).thenReturn(WikiResponse("Also Refer", "This may also refer to another thing", "url", null))
        whenever(wikipediaService.fetchSummary("Can Refer")).thenReturn(WikiResponse("Can Refer", "This can refer to another thing", "url", null))

        assertNull(service.getSpaceObjectWiki(7L))
        assertNull(service.getSpaceObjectWiki(8L))
        assertNull(service.getSpaceObjectWiki(9L))
    }

    @Test
    fun `constellation and culture operations map repository data`() {
        val culture = culture()
        val constellation = constellation(culture)
        val star1 = spaceObject(1L, "Star 1", SpaceObjectType.STAR)
        val star2 = spaceObject(2L, "Star 2", SpaceObjectType.STAR)
        val connection = ConstStarConnection(
            id = ConstStarConnectionId(constellation.id, star1.id, star2.id),
            constellation = constellation,
            star1 = star1,
            star2 = star2
        )
        whenever(constellationRepository.findAllWithCulture()).thenReturn(listOf(constellation))
        whenever(constellationRepository.findByIdWithCulture(10L)).thenReturn(Optional.of(constellation))
        whenever(constellationRepository.findByIdWithCulture(404L)).thenReturn(Optional.empty())
        whenever(constStarConnectionRepository.findByConstellationId(10L)).thenReturn(listOf(connection))
        whenever(spaceObjectRepository.findAllById(setOf(1L, 2L))).thenReturn(listOf(star1, star2))
        whenever(constellationCultureRepository.findAll()).thenReturn(listOf(culture))
        whenever(constellationCultureRepository.existsById(7L)).thenReturn(true)
        whenever(constellationCultureRepository.existsById(8L)).thenReturn(false)
        whenever(constellationRepository.findAllWithCurrentCulture()).thenReturn(listOf(constellation))
        whenever(constellationRepository.findByIdWithCurrentCulture(10L)).thenReturn(Optional.of(constellation))
        whenever(constellationRepository.findByIdWithCurrentCulture(404L)).thenReturn(Optional.empty())

        assertEquals("Orion", service.getAllConstellations().single().name)
        assertEquals(2, service.getConstellationDetail(10L)?.stars?.size)
        assertNull(service.getConstellationDetail(404L))
        assertEquals("Western", service.getAllCultures().single().name)
        assertTrue(service.setCurrentCulture(7L))
        assertFalse(service.setCurrentCulture(8L))
        verify(constellationCultureRepository).setAllToNotCurrent()
        verify(constellationCultureRepository).setCurrent(7L)
        assertEquals("Orion", service.getAllConstellationsForCurrentCulture().single().name)
        assertEquals(2, service.getConstellationDetailForCurrentCulture(10L)?.stars?.size)
        assertNull(service.getConstellationDetailForCurrentCulture(404L))
    }

    private fun spaceObject(
        id: Long,
        displayName: String,
        type: SpaceObjectType,
        magnitude: Double? = 0.03
    ) = SpaceObject(
        id = id,
        objectType = type,
        raDeg = 1.0,
        decDeg = 2.0,
        magnitude = magnitude,
        displayName = displayName,
        description = "description"
    )

    private fun culture() = ConstellationCulture(
        id = 7L,
        name = "Western",
        region = "World",
        description = "desc",
        isCurrent = true
    )

    private fun constellation(culture: ConstellationCulture) = Constellation(
        id = 10L,
        culture = culture,
        name = "Orion",
        description = "hunter"
    )
}
