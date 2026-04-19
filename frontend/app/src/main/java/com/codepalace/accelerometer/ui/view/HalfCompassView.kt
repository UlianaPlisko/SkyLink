package com.codepalace.accelerometer.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class HalfCompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 255, 215, 120)
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 255, 235, 170)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 255, 235, 170)
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 255, 235, 170)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    var headingDeg: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val radius = min(w * 0.42f, h * 0.95f)
        val cx = w / 2f
        val cy = h + radius * 0.15f

        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        canvas.drawArc(oval, 180f, 180f, false, arcPaint)

        val marks = listOf(
            0f to "E",
            45f to "SE",
            90f to "S",
            135f to "SW",
            180f to "W",
            225f to "NW",
            270f to "N",
            315f to "NE"
        )

        for ((worldDeg, label) in marks) {
            val relative = normalize360(headingDeg - worldDeg)
            if (relative > 180f) continue

            val angle = Math.toRadians((180f - relative).toDouble())

            val outerX = cx + radius * cos(angle).toFloat()
            val outerY = cy - radius * sin(angle).toFloat()

            val innerX = cx + (radius - 18f) * cos(angle).toFloat()
            val innerY = cy - (radius - 18f) * sin(angle).toFloat()

            canvas.drawLine(innerX, innerY, outerX, outerY, tickPaint)

            val textR = radius - 40f
            val tx = cx + textR * cos(angle).toFloat()
            val ty = cy - textR * sin(angle).toFloat() + 10f

            canvas.drawText(label, tx, ty, textPaint)
        }

        val pointerTopY = h * 0.08f
        canvas.drawLine(cx, pointerTopY, cx, pointerTopY + 28f, needlePaint)
    }

    private fun normalize360(value: Float): Float {
        var v = value % 360f
        if (v < 0f) v += 360f
        return v
    }
}