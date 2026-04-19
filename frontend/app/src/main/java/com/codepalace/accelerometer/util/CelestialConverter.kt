package com.codepalace.accelerometer.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

data class Vec3(
    val x: Double,
    val y: Double,
    val z: Double
)

object CelestialConverter {

    /**
     * Converts equatorial coordinates (RA/Dec) to local horizontal coordinates (Az/Alt).
     *
     * Returns:
     * Pair(azimuthDegrees, altitudeDegrees)
     *
     * Convention:
     * - Azimuth: 0° = North, 90° = East, 180° = South, 270° = West
     * - Altitude: 0° = horizon, +90° = zenith
     *
     * IMPORTANT:
     * use one fixed referenceInstant for the whole loaded star catalog
     * so all stars are computed for the same sky moment.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun raDecToAzAlt(
        raDeg: Double,
        decDeg: Double,
        latDeg: Double,
        lonDeg: Double,
        referenceInstant: Instant
    ): Pair<Double, Double> {
        val jd = calculateJulianDate(referenceInstant)
        val gmstDeg = greenwichMeanSiderealTimeDeg(jd)

        var localSiderealTimeDeg = gmstDeg + lonDeg
        localSiderealTimeDeg = normalizeDegrees360(localSiderealTimeDeg)

        var hourAngleDeg = localSiderealTimeDeg - raDeg
        hourAngleDeg = normalizeDegrees180(hourAngleDeg)

        val haRad = Math.toRadians(hourAngleDeg)
        val decRad = Math.toRadians(decDeg)
        val latRad = Math.toRadians(latDeg)

        val sinAlt = sin(decRad) * sin(latRad) +
                cos(decRad) * cos(latRad) * cos(haRad)
        val altRad = asin(sinAlt)

        val y = sin(haRad)
        val x = cos(haRad) * sin(latRad) - tan(decRad) * cos(latRad)

        var azDeg = Math.toDegrees(atan2(y, x))
        azDeg = normalizeDegrees360(azDeg)

        val altDeg = Math.toDegrees(altRad)

        return azDeg to altDeg
    }

    /**
     * Converts azimuth/altitude to local ENU unit vector.
     *
     * ENU axes:
     * - x = East
     * - y = North
     * - z = Up
     */
    fun azAltToENU(azDeg: Double, altDeg: Double): Vec3 {
        val azRad = Math.toRadians(azDeg)
        val altRad = Math.toRadians(altDeg)

        val east = cos(altRad) * sin(azRad)
        val north = cos(altRad) * cos(azRad)
        val up = sin(altRad)

        return Vec3(east, north, up)
    }

    /**
     * Circular smoothing for azimuth angles in degrees.
     */
    fun smoothAzimuth(prev: Float, measured: Float, alpha: Float): Float {
        val a1 = Math.toRadians(prev.toDouble())
        val a2 = Math.toRadians(measured.toDouble())

        val x = alpha * cos(a1) + (1f - alpha) * cos(a2)
        val y = alpha * sin(a1) + (1f - alpha) * sin(a2)

        var result = Math.toDegrees(atan2(y, x)).toFloat()
        if (result < 0f) result += 360f
        return result
    }

    private fun normalizeDegrees360(value: Double): Double {
        return (value % 360.0 + 360.0) % 360.0
    }

    private fun normalizeDegrees180(value: Double): Double {
        return ((value + 540.0) % 360.0) - 180.0
    }

    private fun greenwichMeanSiderealTimeDeg(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0

        var gmst = 280.46061837 +
                360.98564736629 * (jd - 2451545.0) +
                0.000387933 * t * t -
                (t * t * t) / 38710000.0

        gmst = normalizeDegrees360(gmst)
        return gmst
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateJulianDate(instant: Instant): Double {
        val seconds = instant.epochSecond.toDouble() +
                instant.nano.toDouble() / 1_000_000_000.0
        return 2440587.5 + seconds / 86400.0
    }
}