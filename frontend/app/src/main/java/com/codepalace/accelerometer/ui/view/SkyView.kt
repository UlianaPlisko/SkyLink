package com.codepalace.accelerometer.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.codepalace.accelerometer.data.model.Star
import kotlin.math.abs
import kotlin.math.tan

class SkyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val debugPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(70, 255, 255, 255)
        strokeWidth = 2f
    }

    var stars: List<Star> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var rotationMatrix: FloatArray? = null
        set(value) {
            field = value
            invalidate()
        }

    // Keep these only for debug if you want
    var phoneAzimuth: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var phoneAltitude: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    // Horizontal camera FOV in degrees
    var fovHorizontal: Float = 90f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        if (width == 0 || height == 0) return

        val centerX = width / 2f
        val centerY = height / 2f

        // debug crosshair
        canvas.drawLine(centerX - 20f, centerY, centerX + 20f, centerY, debugPaint)
        canvas.drawLine(centerX, centerY - 20f, centerX, centerY + 20f, debugPaint)

        val matrix = rotationMatrix ?: return

        val aspect = height.toFloat() / width.toFloat()
        val fovHorizontalRad = Math.toRadians(fovHorizontal.toDouble()).toFloat()
        val fovVerticalRad = 2f * kotlin.math.atan(tan(fovHorizontalRad / 2f) * aspect)

        val tanHalfHFov = tan(fovHorizontalRad / 2f)
        val tanHalfVFov = tan(fovVerticalRad / 2f)

        for (star in stars) {
            // world vector in local ENU coordinates
            val world = floatArrayOf(
                star.east.toFloat(),
                star.north.toFloat(),
                star.up.toFloat()
            )

            val device = multiplyTransposeMatVec(matrix, world)

            val xDev = device[0]
            val yDev = device[1]
            val zDev = device[2]

            // Correct camera convention for a phone screen:
            // +X = right on screen
            // +Y = up on screen
            // +Z = out of the screen toward the user
            // so looking "through" the phone means forward = -Z
            val right = xDev
            val up = yDev
            val forward = -zDev

            // behind the camera
            if (forward <= 0f) continue

            // normalized perspective coordinates
            val ndcX = right / (forward * tanHalfHFov)
            val ndcY = up / (forward * tanHalfVFov)

            // outside visible frustum
            if (abs(ndcX) > 1f || abs(ndcY) > 1f) continue

            val screenX = centerX + ndcX * centerX
            val screenY = centerY - ndcY * centerY

            val radius = (6f - star.magnitude.toFloat()).coerceIn(1.5f, 10f)
            starPaint.color = if (star.magnitude < 1f) Color.YELLOW else Color.WHITE
            canvas.drawCircle(screenX, screenY, radius, starPaint)

            if (star.magnitude < 2.5f) {
                textPaint.color = Color.argb(220, 200, 200, 200)
                canvas.drawText(star.name, screenX, screenY - radius - 8f, textPaint)
            }
        }

        // optional debug text
        textPaint.color = Color.argb(140, 180, 180, 180)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 26f
        canvas.drawText(
            "Az ${phoneAzimuth.toInt()}°  Alt ${phoneAltitude.toInt()}°",
            24f,
            40f,
            textPaint
        )
        textPaint.textAlign = Paint.Align.CENTER
    }

    private fun multiplyMatVec(m: FloatArray, v: FloatArray): FloatArray {
        return floatArrayOf(
            m[0] * v[0] + m[1] * v[1] + m[2] * v[2],
            m[3] * v[0] + m[4] * v[1] + m[5] * v[2],
            m[6] * v[0] + m[7] * v[1] + m[8] * v[2]
        )
    }

    @Suppress("unused")
    private fun multiplyTransposeMatVec(m: FloatArray, v: FloatArray): FloatArray {
        return floatArrayOf(
            m[0] * v[0] + m[3] * v[1] + m[6] * v[2],
            m[1] * v[0] + m[4] * v[1] + m[7] * v[2],
            m[2] * v[0] + m[5] * v[1] + m[8] * v[2]
        )
    }
}