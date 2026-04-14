package com.codepalace.accelerometer.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import kotlin.math.*

class OrientationHelper(private val context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationVectorSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    var onMatrixChanged: ((rotationMatrix: FloatArray) -> Unit)? = null
    var onOrientationChanged: ((azimuth: Float, pitch: Float, roll: Float) -> Unit)? = null

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun start() {
        val sensor = rotationVectorSensor
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR &&
            event.sensor.type != Sensor.TYPE_GAME_ROTATION_VECTOR
        ) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = wm.defaultDisplay.rotation

        when (rotation) {
            Surface.ROTATION_0 ->
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Y,
                    remappedMatrix
                )

            Surface.ROTATION_90 ->
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_Y,
                    SensorManager.AXIS_MINUS_X,
                    remappedMatrix
                )

            Surface.ROTATION_180 ->
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_MINUS_X,
                    SensorManager.AXIS_MINUS_Y,
                    remappedMatrix
                )

            Surface.ROTATION_270 ->
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_MINUS_Y,
                    SensorManager.AXIS_X,
                    remappedMatrix
                )

            else -> System.arraycopy(rotationMatrix, 0, remappedMatrix, 0, 9)
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
}