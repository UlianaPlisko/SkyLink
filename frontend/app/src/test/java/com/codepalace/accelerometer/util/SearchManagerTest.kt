package com.codepalace.accelerometer.util

import com.codepalace.accelerometer.data.local.SpaceObjectEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchManagerTest {

    private val manager = SearchManager()

    @Test
    fun `blank query returns no results`() {
        assertTrue(manager.search("   ", sampleObjects()).isEmpty())
    }

    @Test
    fun `search ranks prefix before substring and fuzzy matches`() {
        val results = manager.search("sir", sampleObjects())

        assertEquals(listOf("Sirius", "Sirus", "Alpha Sirius"), results.map { it.displayName })
    }

    @Test
    fun `search filters dim objects and limits result count`() {
        val manyBrightObjects = (1L..40L).map {
            SpaceObjectEntity(it, "Star $it", "STAR", it.toDouble(), it.toDouble(), 1.0 + it / 100.0)
        }
        val dimObject = SpaceObjectEntity(100L, "Star dim", "STAR", 0.0, 0.0, 5.5)

        val results = manager.search("star", manyBrightObjects + dimObject)

        assertEquals(SearchManager.MAX_RESULTS, results.size)
        assertTrue(results.none { it.displayName == "Star dim" })
        assertEquals("Star 1", results.first().displayName)
    }

    @Test
    fun `short query uses plain case insensitive matching`() {
        val results = manager.search("ve", sampleObjects())

        assertEquals(listOf("Vega"), results.map { it.displayName })
    }

    private fun sampleObjects() = listOf(
        SpaceObjectEntity(1L, "Alpha Sirius", "STAR", 1.0, 2.0, 1.1),
        SpaceObjectEntity(2L, "Sirius", "STAR", 3.0, 4.0, -1.4),
        SpaceObjectEntity(3L, "Sirus", "STAR", 5.0, 6.0, 0.0),
        SpaceObjectEntity(4L, "Vega", "STAR", 7.0, 8.0, 0.03),
        SpaceObjectEntity(5L, "Dim Sirius", "STAR", 9.0, 10.0, 6.0)
    )
}
