package com.codepalace.accelerometer.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager

class OrientationHelper(private val context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /**
     * Prefer Earth-referenced sensors for astronomy:
     *
     * 1) TYPE_ROTATION_VECTOR - best general choice
     * 2) TYPE_GEOMAGNETIC_ROTATION_VECTOR - still Earth/magnetic referenced
     * 3) TYPE_GAME_ROTATION_VECTOR - last fallback only, not ideal for world azimuth
     */
    private val activeSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    var onMatrixChanged: ((rotationMatrix: FloatArray) -> Unit)? = null
    var onOrientationChanged: ((azimuth: Float, pitch: Float, roll: Float) -> Unit)? = null

    /**
     * device->world rotation matrix from sensor
     */
    private val rotationMatrix = FloatArray(9)

    /**
     * device->world matrix remapped to current screen rotation
     */
    private val remappedMatrix = FloatArray(9)

    private val orientationAngles = FloatArray(3)

    fun start() {
        val sensor = activeSensor ?: return
        sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val type = event.sensor.type
        if (type != Sensor.TYPE_ROTATION_VECTOR &&
            type != Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR &&
            type != Sensor.TYPE_GAME_ROTATION_VECTOR
        ) {
            return
        }

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        when (getDisplayRotation()) {
            Surface.ROTATION_0 -> {
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Y,
                    remappedMatrix
                )
            }

            Surface.ROTATION_90 -> {
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_Y,
                    SensorManager.AXIS_MINUS_X,
                    remappedMatrix
                )
            }

            Surface.ROTATION_180 -> {
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_MINUS_X,
                    SensorManager.AXIS_MINUS_Y,
                    remappedMatrix
                )
            }

            Surface.ROTATION_270 -> {
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_MINUS_Y,
                    SensorManager.AXIS_X,
                    remappedMatrix
                )
            }

            else -> {
                System.arraycopy(rotationMatrix, 0, remappedMatrix, 0, 9)
            }
        }

        SensorManager.getOrientation(remappedMatrix, orientationAngles)

        var azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        if (azimuthDeg < 0f) azimuthDeg += 360f

        val pitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        onOrientationChanged?.invoke(azimuthDeg, pitchDeg, rollDeg)
        onMatrixChanged?.invoke(remappedMatrix.copyOf())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    @Suppress("DEPRECATION")
    private fun getDisplayRotation(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation ?: Surface.ROTATION_0
        } else {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.rotation
        }
    }
}