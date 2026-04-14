package com.codepalace.accelerometer.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import kotlin.math.*

data class Vec3(
    val x: Double,
    val y: Double,
    val z: Double
)

object CelestialConverter {
    /**
     * Returns Pair(azimuthDegrees, altitudeDegrees) - 0° = North, positive East
     * IMPORTANT: call this with a fixed referenceInstant (set once when you load stars)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun raDecToAzAlt(
        raDeg: Double,          // RA in degrees (if RA in hours => raHours*15)
        decDeg: Double,
        latDeg: Double,
        lonDeg: Double,
        referenceInstant: Instant
    ): Pair<Double, Double> {
        val jd = calculateJulianDate(referenceInstant)
        val gmst = greenwichMeanSiderealTimeDeg(jd)         // degrees
        // LST in degrees: GMST + longitude (east positive)
        var lst = gmst + lonDeg
        lst = (lst % 360.0 + 360.0) % 360.0

        // Hour Angle in degrees: HA = LST - RA  -> normalize to -180..180
        var ha = lst - raDeg
        ha = ((ha + 540.0) % 360.0) - 180.0

        // convert to radians for trig
        val haRad = Math.toRadians(ha)
        val decRad = Math.toRadians(decDeg)
        val latRad = Math.toRadians(latDeg)

        // altitude
        val sinAlt = sin(decRad) * sin(latRad) + cos(decRad) * cos(latRad) * cos(haRad)
        val altRad = asin(sinAlt)

        // azimuth using atan2 for correct quadrant
        val y = sin(haRad)
        val x = cos(haRad) * sin(latRad) - tan(decRad) * cos(latRad)
        var azRad = atan2(y, x)
        var azDeg = Math.toDegrees(azRad)
        // Convert to 0..360 and make 0 = North, East = 90
        azDeg = (azDeg + 360.0) % 360.0

        val altDeg = Math.toDegrees(altRad)
        return azDeg to altDeg
    }

    private fun greenwichMeanSiderealTimeDeg(jd: Double): Double {
        // Robust and commonly used formula (degrees)
        val T = (jd - 2451545.0) / 36525.0
        var gmst = 280.46061837 +
                360.98564736629 * (jd - 2451545.0) +
                0.000387933 * T * T - T * T * T / 38710000.0
        gmst = (gmst % 360.0 + 360.0) % 360.0
        return gmst
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateJulianDate(instant: Instant): Double {
        val seconds = instant.epochSecond.toDouble() + instant.nano.toDouble() / 1_000_000_000.0
        return 2440587.5 + seconds / 86400.0
    }

    fun smoothAzimuth(prev: Float, measured: Float, alpha: Float): Float {
        val a1 = Math.toRadians(prev.toDouble())
        val a2 = Math.toRadians(measured.toDouble())
        val x = alpha * cos(a1) + (1 - alpha) * cos(a2)
        val y = alpha * sin(a1) + (1 - alpha) * sin(a2)
        var sm = Math.toDegrees(atan2(y, x)).toFloat()
        if (sm < 0) sm += 360f
        return sm
    }

    fun azAltToENU(azDeg: Double, altDeg: Double): Vec3 {
        val az = Math.toRadians(azDeg)
        val alt = Math.toRadians(altDeg)

        val east = cos(alt) * sin(az)
        val north = cos(alt) * cos(az)
        val up = sin(alt)

        return Vec3(east, north, up)
    }
}