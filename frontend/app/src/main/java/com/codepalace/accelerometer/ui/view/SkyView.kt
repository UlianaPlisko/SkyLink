package com.codepalace.accelerometer.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.codepalace.accelerometer.data.model.Star
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.tan

class SkyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 210, 210, 210)
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    private val skyBackgroundColor = Color.rgb(14, 26, 43)
    private val redSkyBackgroundColor = Color.rgb(68, 12, 24)

    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val debugPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(70, 255, 255, 255)
        strokeWidth = 2f
    }

    private val labelAnchorCache = mutableMapOf<String, Int>()

    var onStarClick: ((Star) -> Unit)? = null
    var onZoom: ((Float) -> Unit)? = null
    var onManualViewChanged: ((Float, Float) -> Unit)? = null

    private data class ClickableStar(
        val star: Star,
        val screenX: Float,
        val screenY: Float,
        val radius: Float
    )

    private var clickableStars: List<ClickableStar> = emptyList()
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var totalTouchDistance = 0f
    private var touchHadMultiplePointers = false

    var redModeEnabled: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var manualControlEnabled: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var manualAzimuth: Float = 0f
        private set

    var manualAltitude: Float = 20f
        private set

    private val scaleGestureDetector =
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                onZoom?.invoke(detector.scaleFactor)
                return true
            }
        })

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

    var phoneAzimuth: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var onEmptySpaceClick: (() -> Unit)? = null

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
        canvas.drawColor(if (redModeEnabled) redSkyBackgroundColor else skyBackgroundColor)

        if (width <= 0 || height <= 0) return

        val matrix = if (manualControlEnabled) {
            manualRotationMatrix()
        } else {
            rotationMatrix
        } ?: run {
            clickableStars = emptyList()
            drawCenteredDebug(canvas, "Waiting for orientation...")
            return
        }

        val centerX = width / 2f
        val centerY = height / 2f
        val aspect = height.toFloat() / width.toFloat()

        val fovHorizontalRad = Math.toRadians(fovHorizontal.toDouble())
        val fovVerticalRad = 2.0 * kotlin.math.atan(tan(fovHorizontalRad / 2.0) * aspect)

        val gnomonicLimitX = tan(fovHorizontalRad / 2.0)
        val gnomonicLimitY = tan(fovVerticalRad / 2.0)

        val stereoLimitX = 2.0 * tan(fovHorizontalRad / 4.0)
        val stereoLimitY = 2.0 * tan(fovVerticalRad / 4.0)

        drawCrosshair(canvas, centerX, centerY)

        val forwardWorldUp = -matrix[8].toDouble()
        val verticalLook = abs(forwardWorldUp).coerceIn(0.0, 1.0)

        val gnomonicWeight = smoothStep(0.65, 0.92, verticalLook)
        val stereoWeight = 1.0 - gnomonicWeight

        val usedLabelRects = mutableListOf<RectF>()
        val visibleStarRects = mutableListOf<RectF>()

        data class ProjectedStar(
            val star: Star,
            val screenX: Float,
            val screenY: Float,
            val radius: Float
        )

        val projectedStars = mutableListOf<ProjectedStar>()
        val newClickableStars = mutableListOf<ClickableStar>()

        for (star in stars) {
            val world = floatArrayOf(
                -star.east.toFloat(),
                -star.north.toFloat(),
                star.up.toFloat()
            )

            val device = multiplyTransposeMatVec(matrix, world)

            val xDev = device[0].toDouble()
            val yDev = device[1].toDouble()
            val zDev = device[2].toDouble()

            val forward = -zDev
            if (forward <= 0.0) continue

            val gnomonicX = xDev / forward
            val gnomonicY = yDev / forward
            val gnomonicNx = gnomonicX / gnomonicLimitX
            val gnomonicNy = gnomonicY / gnomonicLimitY

            val k = 2.0 / (1.0 + forward)
            val stereoX = xDev * k
            val stereoY = yDev * k
            val stereoNx = stereoX / stereoLimitX
            val stereoNy = stereoY / stereoLimitY

            if ((abs(gnomonicNx) > 1.3 && abs(stereoNx) > 1.3) ||
                (abs(gnomonicNy) > 1.3 && abs(stereoNy) > 1.3)
            ) {
                continue
            }

            val nx = (gnomonicNx * gnomonicWeight + stereoNx * stereoWeight).toFloat()
            val ny = (gnomonicNy * gnomonicWeight + stereoNy * stereoWeight).toFloat()

            if (abs(nx) > 1f || abs(ny) > 1f) continue

            val screenX = centerX + nx * centerX
            val screenY = centerY - ny * centerY
            val radius = starRadiusForMagnitude(star.magnitude.toFloat())

            projectedStars += ProjectedStar(star, screenX, screenY, radius)

            newClickableStars += ClickableStar(
                star = star,
                screenX = screenX,
                screenY = screenY,
                radius = radius + 24f
            )

            visibleStarRects += RectF(
                screenX - radius - 8f,
                screenY - radius - 8f,
                screenX + radius + 8f,
                screenY + radius + 8f
            )

            starPaint.color = if (star.magnitude < 1f) {
                Color.rgb(255, 235, 170)
            } else {
                Color.WHITE
            }

            canvas.drawCircle(screenX, screenY, radius, starPaint)
        }

        clickableStars = newClickableStars

        if (showLabels) {
            val maxLabels = 20
            var labelsDrawn = 0

            for (ps in projectedStars.sortedBy { it.star.magnitude }) {
                if (labelsDrawn >= maxLabels) break

                val star = ps.star
                if (star.name.isBlank()) continue

                val labelRect = findStableLabelPosition(
                    text = star.name,
                    starX = ps.screenX,
                    starY = ps.screenY,
                    starRadius = ps.radius,
                    usedLabels = usedLabelRects,
                    starRects = visibleStarRects
                )

                if (labelRect != null) {
                    val fm = textPaint.fontMetrics
                    val baselineY = labelRect.top - fm.top
                    canvas.drawText(star.name, labelRect.centerX(), baselineY, textPaint)
                    usedLabelRects += labelRect
                }
                labelsDrawn++
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                totalTouchDistance = 0f
                touchHadMultiplePointers = false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                touchHadMultiplePointers = true
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount > 1) {
                    touchHadMultiplePointers = true
                }

                if (manualControlEnabled && event.pointerCount == 1 && !scaleGestureDetector.isInProgress) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    totalTouchDistance += hypot(dx.toDouble(), dy.toDouble()).toFloat()

                    manualAzimuth = normalizeDegrees(manualAzimuth - dx * 0.18f)
                    manualAltitude = (manualAltitude - dy * 0.12f).coerceIn(-20f, 90f)
                    onManualViewChanged?.invoke(manualAzimuth, manualAltitude)

                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
            }
        }

        if (!scaleGestureDetector.isInProgress &&
            event.actionMasked == MotionEvent.ACTION_UP &&
            totalTouchDistance < 20f &&
            !touchHadMultiplePointers
        ) {
            val tapped = clickableStars
                .filter {
                    val distance = hypot(
                        (event.x - it.screenX).toDouble(),
                        (event.y - it.screenY).toDouble()
                    )
                    distance <= it.radius
                }
                .minByOrNull {
                    hypot(
                        (event.x - it.screenX).toDouble(),
                        (event.y - it.screenY).toDouble()
                    )
                }

            if (tapped != null) {
                onStarClick?.invoke(tapped.star)
            } else {
                onEmptySpaceClick?.invoke()   // 👈 ADD THIS
            }

            performClick()
            return true
        }

        return true
    }

    fun setManualLook(azimuth: Float, altitude: Float) {
        manualAzimuth = normalizeDegrees(azimuth)
        manualAltitude = altitude.coerceIn(-20f, 90f)
        onManualViewChanged?.invoke(manualAzimuth, manualAltitude)
        invalidate()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun findStableLabelPosition(
        text: String,
        starX: Float,
        starY: Float,
        starRadius: Float,
        usedLabels: List<RectF>,
        starRects: List<RectF>
    ): RectF? {
        val preferredAnchor = labelAnchorCache[text] ?: preferredAnchorForStar(text)

        val candidateOrder = buildList {
            add(preferredAnchor)
            for (i in 0..5) {
                if (i != preferredAnchor) add(i)
            }
        }

        for (anchor in candidateOrder) {
            val rect = buildLabelRect(text, starX, starY, starRadius, anchor)
            if (!isInsideScreen(rect)) continue
            if (usedLabels.any { RectF.intersects(it, rect) }) continue
            if (starRects.any { RectF.intersects(it, rect) }) continue

            labelAnchorCache[text] = anchor
            return rect
        }

        return null
    }

    private fun preferredAnchorForStar(name: String): Int {
        return name.hashCode().absoluteValue % 6
    }

    private fun buildLabelRect(
        text: String,
        starX: Float,
        starY: Float,
        starRadius: Float,
        anchor: Int
    ): RectF {
        val paddingX = 10f
        val paddingY = 4f
        val margin = 10f

        val textWidth = textPaint.measureText(text)
        val fm = textPaint.fontMetrics
        val textHeight = fm.bottom - fm.top

        val boxWidth = textWidth + paddingX * 2f
        val boxHeight = textHeight + paddingY * 2f

        return when (anchor) {
            0 -> RectF(
                starX - boxWidth / 2f,
                starY - starRadius - margin - boxHeight,
                starX + boxWidth / 2f,
                starY - starRadius - margin
            )

            1 -> RectF(
                starX + starRadius + margin,
                starY - boxHeight / 2f,
                starX + starRadius + margin + boxWidth,
                starY + boxHeight / 2f
            )

            2 -> RectF(
                starX - starRadius - margin - boxWidth,
                starY - boxHeight / 2f,
                starX - starRadius - margin,
                starY + boxHeight / 2f
            )

            3 -> RectF(
                starX - boxWidth / 2f,
                starY + starRadius + margin,
                starX + boxWidth / 2f,
                starY + starRadius + margin + boxHeight
            )

            4 -> RectF(
                starX + starRadius + margin,
                starY - starRadius - margin - boxHeight,
                starX + starRadius + margin + boxWidth,
                starY - starRadius - margin
            )

            else -> RectF(
                starX - starRadius - margin - boxWidth,
                starY - starRadius - margin - boxHeight,
                starX - starRadius - margin,
                starY - starRadius - margin
            )
        }
    }

    private fun isInsideScreen(rect: RectF): Boolean {
        return rect.left >= 0f &&
                rect.top >= 0f &&
                rect.right <= width.toFloat() &&
                rect.bottom <= height.toFloat()
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

    private fun drawCenteredDebug(canvas: Canvas, message: String) {
        textPaint.color = Color.argb(180, 200, 200, 200)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 34f
        canvas.drawText(message, width / 2f, height / 2f, textPaint)
        textPaint.textSize = 30f
        textPaint.color = Color.argb(220, 210, 210, 210)
    }

    private fun manualRotationMatrix(): FloatArray {
        val azimuthRad = Math.toRadians(manualAzimuth.toDouble())
        val altitudeRad = Math.toRadians(manualAltitude.toDouble())

        val sinAz = sin(azimuthRad)
        val cosAz = cos(azimuthRad)
        val sinAlt = sin(altitudeRad)
        val cosAlt = cos(altitudeRad)

        val rightX = -cosAz
        val rightY = sinAz
        val rightZ = 0.0

        val backwardX = sinAz * cosAlt
        val backwardY = cosAz * cosAlt
        val backwardZ = -sinAlt

        val upX = backwardY * rightZ - backwardZ * rightY
        val upY = backwardZ * rightX - backwardX * rightZ
        val upZ = backwardX * rightY - backwardY * rightX

        return floatArrayOf(
            rightX.toFloat(), upX.toFloat(), backwardX.toFloat(),
            rightY.toFloat(), upY.toFloat(), backwardY.toFloat(),
            rightZ.toFloat(), upZ.toFloat(), backwardZ.toFloat()
        )
    }

    private fun normalizeDegrees(value: Float): Float {
        val result = value % 360f
        return if (result < 0f) result + 360f else result
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

    private val Int.absoluteValue: Int
        get() = if (this < 0) -this else this
}
