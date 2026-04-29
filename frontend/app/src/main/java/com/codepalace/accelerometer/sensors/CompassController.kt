package com.codepalace.accelerometer.sensors

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs

class CompassController(
    context: Context
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val rotationVector: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val magnetometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val rotationMatrix = FloatArray(9)
    private val adjustedMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var accelValues: FloatArray? = null
    private var magnetValues: FloatArray? = null

    private var lastDeliveredAzimuth = 0f
    private var initialized = false

    var useTrueNorth: Boolean = true
    var smoothingAlpha: Float = 0.15f
    var headingOffsetDeg: Float = 0f

    var onHeadingChanged: ((Float) -> Unit)? = null
    var onRawHeadingChanged: ((Float) -> Unit)? = null

    private var lastLocation: Location? = null

    fun updateLocation(location: Location?) {
        lastLocation = location
    }

    fun start() {
        rotationVector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        } ?: run {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
            magnetometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                computeAdjustedOrientation(rotationMatrix)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                accelValues = event.values.clone()
                updateFromAccelMag()
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                magnetValues = event.values.clone()
                updateFromAccelMag()
            }
        }
    }

    private fun updateFromAccelMag() {
        val acc = accelValues ?: return
        val mag = magnetValues ?: return

        val ok = SensorManager.getRotationMatrix(rotationMatrix, null, acc, mag)
        if (!ok) return

        computeAdjustedOrientation(rotationMatrix)
    }

    @Suppress("DEPRECATION")
    private fun computeAdjustedOrientation(baseMatrix: FloatArray) {
        val rotation = windowManager.defaultDisplay.rotation

        val (axisX, axisY) = when (rotation) {
            Surface.ROTATION_0 -> SensorManager.AXIS_X to SensorManager.AXIS_Y
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }

        SensorManager.remapCoordinateSystem(
            baseMatrix,
            axisX,
            axisY,
            adjustedMatrix
        )

        SensorManager.getOrientation(adjustedMatrix, orientation)
        deliverAzimuth(orientation[0])
    }

    private fun deliverAzimuth(azimuthRad: Float) {
        var magneticAzimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
        magneticAzimuthDeg = normalize360(magneticAzimuthDeg)

        onRawHeadingChanged?.invoke(magneticAzimuthDeg)

        val corrected = if (useTrueNorth) {
            val loc = lastLocation
            if (loc != null) {
                val field = GeomagneticField(
                    loc.latitude.toFloat(),
                    loc.longitude.toFloat(),
                    loc.altitude.toFloat(),
                    System.currentTimeMillis()
                )
                normalize360(magneticAzimuthDeg + field.declination)
            } else {
                magneticAzimuthDeg
            }
        } else {
            magneticAzimuthDeg
        }

        val withOffset = normalize360(corrected + headingOffsetDeg)
        val smoothed = smoothAngle(lastDeliveredAzimuth, withOffset, smoothingAlpha)

        if (!initialized || angularDistance(lastDeliveredAzimuth, smoothed) > 0.1f) {
            lastDeliveredAzimuth = smoothed
            initialized = true
            onHeadingChanged?.invoke(smoothed)
        }
    }

    private fun smoothAngle(current: Float, target: Float, alpha: Float): Float {
        val delta = shortestSignedAngleDelta(current, target)
        return normalize360(current + alpha * delta)
    }

    private fun shortestSignedAngleDelta(from: Float, to: Float): Float {
        var delta = (to - from + 540f) % 360f - 180f
        if (delta < -180f) delta += 360f
        return delta
    }

    private fun angularDistance(a: Float, b: Float): Float {
        return abs(shortestSignedAngleDelta(a, b))
    }

    private fun normalize360(value: Float): Float {
        var v = value % 360f
        if (v < 0f) v += 360f
        return v
    }
}
