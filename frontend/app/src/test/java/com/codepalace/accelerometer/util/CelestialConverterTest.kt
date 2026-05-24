package com.codepalace.accelerometer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.sqrt

class CelestialConverterTest {

    @Test
    fun `az altitude converts cardinal directions to enu vectors`() {
        val north = CelestialConverter.azAltToENU(0.0, 0.0)
        val east = CelestialConverter.azAltToENU(90.0, 0.0)
        val zenith = CelestialConverter.azAltToENU(42.0, 90.0)

        assertClose(0.0, north.x)
        assertClose(1.0, north.y)
        assertClose(0.0, north.z)
        assertClose(1.0, east.x)
        assertClose(0.0, east.y)
        assertClose(1.0, zenith.z)
    }

    @Test
    fun `az altitude vector is unit length`() {
        val vector = CelestialConverter.azAltToENU(123.0, 45.0)
        val length = sqrt(vector.x * vector.x + vector.y * vector.y + vector.z * vector.z)

        assertClose(1.0, length)
    }

    @Test
    fun `smooth azimuth handles wrap around`() {
        val smoothed = CelestialConverter.smoothAzimuth(350f, 10f, 0.5f)

        assertTrue(smoothed < 5f || smoothed > 355f)
    }

    @Test
    fun `ra dec to az alt returns normalized azimuth and valid altitude`() {
        val (azimuth, altitude) = CelestialConverter.raDecToAzAlt(
            raDeg = 120.0,
            decDeg = 30.0,
            latDeg = 48.1486,
            lonDeg = 17.1077,
            referenceInstant = Instant.parse("2026-05-23T20:00:00Z")
        )

        assertTrue(azimuth in 0.0..360.0)
        assertTrue(altitude in -90.0..90.0)
    }

    private fun assertClose(expected: Double, actual: Double) {
        assertEquals(expected, actual, 1e-6)
    }
}
