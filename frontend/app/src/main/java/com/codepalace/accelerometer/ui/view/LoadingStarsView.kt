package com.codepalace.accelerometer.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.content.res.AppCompatResources
import com.codepalace.accelerometer.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min

class LoadingStarsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress = 0f

    private val starDrawable: Drawable? =
        AppCompatResources.getDrawable(context, R.drawable.ic_load)
            ?.mutate()

    private val pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1800L
        repeatCount = ValueAnimator.INFINITE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener { animator ->
            progress = animator.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isShown) {
            startPulse()
        }
    }

    override fun onDetachedFromWindow() {
        stopPulse()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)

        if (isShown) {
            startPulse()
        } else {
            stopPulse()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val drawable = starDrawable ?: return
        val baseSize = min(width, height).toFloat()

        if (baseSize <= 0f) return

        drawStar(
            canvas = canvas,
            drawable = drawable,
            centerX = width * 0.52f,
            centerY = height * 0.49f,
            size = baseSize * 0.12f,
            alphaFactor = alphaPulse(0f)
        )

        drawStar(
            canvas = canvas,
            drawable = drawable,
            centerX = width * 0.46f,
            centerY = height * 0.52f,
            size = baseSize * 0.09f,
            alphaFactor = alphaPulse(0.34f)
        )

        drawStar(
            canvas = canvas,
            drawable = drawable,
            centerX = width * 0.52f,
            centerY = height * 0.55f,
            size = baseSize * 0.11f,
            alphaFactor = alphaPulse(0.68f)
        )
    }

    private fun drawStar(
        canvas: Canvas,
        drawable: Drawable,
        centerX: Float,
        centerY: Float,
        size: Float,
        alphaFactor: Float
    ) {
        val fixedSize = size.toInt()
        val halfSize = fixedSize / 2

        val left = centerX.toInt() - halfSize
        val top = centerY.toInt() - halfSize
        val right = left + fixedSize
        val bottom = top + fixedSize

        drawable.alpha = (MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * alphaFactor).toInt()
        drawable.setBounds(left, top, right, bottom)
        drawable.draw(canvas)
    }

    private fun alphaPulse(offset: Float): Float {
        val shifted = (progress + offset) % 1f
        return 0.5f - 0.5f * cos(shifted * 2f * PI).toFloat()
    }

    private fun startPulse() {
        if (!pulseAnimator.isStarted) {
            pulseAnimator.start()
        }
    }

    private fun stopPulse() {
        if (pulseAnimator.isStarted) {
            pulseAnimator.cancel()
        }
    }

    private companion object {
        const val MIN_ALPHA = 95
        const val MAX_ALPHA = 255
    }
}