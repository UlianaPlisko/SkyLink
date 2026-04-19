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

    private val skyBackgroundColor = Color.rgb(14, 26, 43)

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

    /**
     * device -> world matrix from OrientationHelper
     * In rendering we use transpose(matrix) to get world -> device.
     */
    var rotationMatrix: FloatArray? = null
        set(value) {
            field = value
            invalidate()
        }

    var phoneAzimuth: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var phonePitch: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var phoneRoll: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    /**
     * Horizontal visible sky angle in degrees.
     */
    var fovHorizontal: Float = 90f
        set(value) {
            field = value.coerceIn(30f, 120f)
            invalidate()
        }

    var showLabels: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(skyBackgroundColor)

        if (width <= 0 || height <= 0) return

        val matrix = rotationMatrix ?: run {
            drawCenteredDebug(canvas, "Waiting for orientation...")
            return
        }

        val centerX = width / 2f
        val centerY = height / 2f

        val aspect = height.toFloat() / width.toFloat()
        val fovHorizontalRad = Math.toRadians(fovHorizontal.toDouble())
        val fovVerticalRad = 2.0 * kotlin.math.atan(tan(fovHorizontalRad / 2.0) * aspect)

        // Gnomonic / tangent-plane limits (your current projection)
        val gnomonicLimitX = tan(fovHorizontalRad / 2.0)
        val gnomonicLimitY = tan(fovVerticalRad / 2.0)

        // Stereographic limits
        val stereoLimitX = 2.0 * tan(fovHorizontalRad / 4.0)
        val stereoLimitY = 2.0 * tan(fovVerticalRad / 4.0)

        drawCrosshair(canvas, centerX, centerY)

        /**
         * Compute how "table-like" the current view is.
         *
         * Device forward direction is -Z in device coordinates.
         * Convert that to world coordinates using device->world matrix.
         *
         * forwardWorld = matrix * (0, 0, -1)
         *              = (-m[2], -m[5], -m[8])
         *
         * In ENU world:
         * z = Up
         *
         * If |forwardWorld.z| is near 1 => phone points almost vertical
         * (good to keep your old tangent-plane behavior)
         *
         * If |forwardWorld.z| is near 0 => phone points more toward horizon
         * (better to use stereographic to reduce edge sliding)
         */
        val forwardWorldUp = -matrix[8].toDouble()
        val verticalLook = abs(forwardWorldUp).coerceIn(0.0, 1.0)

        // 0 -> stereographic, 1 -> gnomonic
        val gnomonicWeight = smoothStep(0.65, 0.92, verticalLook)
        val stereoWeight = 1.0 - gnomonicWeight

        for (star in stars) {
            val world = floatArrayOf(
                star.east.toFloat(),
                star.north.toFloat(),
                star.up.toFloat()
            )

            val device = multiplyTransposeMatVec(matrix, world)

            val xDev = device[0].toDouble()
            val yDev = device[1].toDouble()
            val zDev = device[2].toDouble()

            /**
             * Device coordinates:
             * +X = screen right
             * +Y = screen top
             * +Z = out of screen toward user
             *
             * Looking through the phone means forward = -Z
             */
            val forward = -zDev
            if (forward <= 0.0) continue

            // -----------------------------
            // 1) Your current projection
            // -----------------------------
            val gnomonicX = xDev / forward
            val gnomonicY = yDev / forward

            val gnomonicNx = gnomonicX / gnomonicLimitX
            val gnomonicNy = gnomonicY / gnomonicLimitY

            // -----------------------------
            // 2) Stereographic projection
            // -----------------------------
            val k = 2.0 / (1.0 + forward)
            val stereoX = xDev * k
            val stereoY = yDev * k

            val stereoNx = stereoX / stereoLimitX
            val stereoNy = stereoY / stereoLimitY

            // If star is far outside both projections, skip it
            if ((abs(gnomonicNx) > 1.3 && abs(stereoNx) > 1.3) ||
                (abs(gnomonicNy) > 1.3 && abs(stereoNy) > 1.3)
            ) {
                continue
            }

            // -----------------------------
            // 3) Blend them
            // -----------------------------
            val nx = (gnomonicNx * gnomonicWeight + stereoNx * stereoWeight).toFloat()
            val ny = (gnomonicNy * gnomonicWeight + stereoNy * stereoWeight).toFloat()

            if (abs(nx) > 1f || abs(ny) > 1f) continue

            val screenX = centerX + nx * centerX
            val screenY = centerY - ny * centerY

            val radius = starRadiusForMagnitude(star.magnitude.toFloat())
            starPaint.color = if (star.magnitude < 1f) {
                Color.rgb(255, 235, 170)
            } else {
                Color.WHITE
            }

            canvas.drawCircle(screenX, screenY, radius, starPaint)

            if (showLabels && star.magnitude < 2.5f && star.name.isNotBlank()) {
                textPaint.color = Color.argb(220, 210, 210, 210)
                canvas.drawText(star.name, screenX, screenY - radius - 8f, textPaint)
            }
        }

        //drawDebugText(canvas, gnomonicWeight, stereoWeight)
    }

    private fun starRadiusForMagnitude(magnitude: Float): Float {
        return when {
            magnitude < 0f -> 9f
            magnitude < 1f -> 7f
            magnitude < 2f -> 5.5f
            magnitude < 3f -> 4.5f
            magnitude < 4f -> 3.5f
            else -> 2.5f
        }
    }

    private fun drawCrosshair(canvas: Canvas, centerX: Float, centerY: Float) {
        canvas.drawLine(centerX - 20f, centerY, centerX + 20f, centerY, debugPaint)
        canvas.drawLine(centerX, centerY - 20f, centerX, centerY + 20f, debugPaint)
    }

    private fun drawDebugText(canvas: Canvas, gnomonicWeight: Double, stereoWeight: Double) {
        textPaint.color = Color.argb(150, 180, 180, 180)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 26f

        canvas.drawText(
            "Az ${phoneAzimuth.toInt()}°  Pitch ${phonePitch.toInt()}°  Roll ${phoneRoll.toInt()}°",
            24f,
            40f,
            textPaint
        )

        canvas.drawText(
            "G ${(gnomonicWeight * 100).toInt()}%  S ${(stereoWeight * 100).toInt()}%",
            24f,
            72f,
            textPaint
        )

        textPaint.textAlign = Paint.Align.CENTER
    }

    private fun drawCenteredDebug(canvas: Canvas, message: String) {
        textPaint.color = Color.argb(180, 200, 200, 200)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 34f
        canvas.drawText(message, width / 2f, height / 2f, textPaint)
    }

    private fun multiplyTransposeMatVec(m: FloatArray, v: FloatArray): FloatArray {
        return floatArrayOf(
            m[0] * v[0] + m[3] * v[1] + m[6] * v[2],
            m[1] * v[0] + m[4] * v[1] + m[7] * v[2],
            m[2] * v[0] + m[5] * v[1] + m[8] * v[2]
        )
    }

    private fun smoothStep(edge0: Double, edge1: Double, x: Double): Double {
        if (edge0 == edge1) return if (x < edge0) 0.0 else 1.0
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)
    }
}