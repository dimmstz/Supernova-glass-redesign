package com.supernova.launcher.ui.theme

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils

/**
 * Ambient page backdrop used behind the Settings glass plates. A supported
 * Android 12+ window supplies the single compositor blur; this drawable only
 * paints cached tonal and accent gradients and remains the graceful fallback.
 */
class GlassBackdropDrawable(
    private val baseColor: Int,
    private val accentColor: Int,
    opacity: Int,
    private val glowAlpha: Int
) : Drawable() {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var baseShader: Shader? = null
    private var glowShader: Shader? = null
    private var shadeShader: Shader? = null
    private var drawableAlpha = opacity.coerceIn(0, 255)
    private var colorFilter: ColorFilter? = null

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return
        basePaint.shader = baseShader
        basePaint.alpha = drawableAlpha
        basePaint.colorFilter = colorFilter
        canvas.drawRect(bounds, basePaint)

        glowPaint.shader = glowShader
        glowPaint.alpha = drawableAlpha
        glowPaint.colorFilter = colorFilter
        canvas.drawRect(bounds, glowPaint)

        shadePaint.shader = shadeShader
        shadePaint.alpha = drawableAlpha
        shadePaint.colorFilter = colorFilter
        canvas.drawRect(bounds, shadePaint)
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        if (bounds.isEmpty) return
        val opaqueBase = ColorUtils.setAlphaComponent(baseColor, 255)
        val upper = ColorUtils.blendARGB(opaqueBase, Color.WHITE, 0.035f)
        val lower = ColorUtils.blendARGB(opaqueBase, Color.BLACK, 0.08f)
        baseShader = LinearGradient(
            bounds.left.toFloat(), bounds.top.toFloat(),
            bounds.left.toFloat(), bounds.bottom.toFloat(),
            upper, lower, Shader.TileMode.CLAMP
        )
        glowShader = RadialGradient(
            bounds.left + bounds.width() * 0.82f,
            bounds.top + bounds.height() * 0.08f,
            bounds.width() * 0.88f,
            ColorUtils.setAlphaComponent(accentColor, glowAlpha),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        shadeShader = LinearGradient(
            bounds.left.toFloat(), bounds.top.toFloat(),
            bounds.left.toFloat(), bounds.bottom.toFloat(),
            Color.TRANSPARENT,
            ColorUtils.setAlphaComponent(Color.BLACK, 28),
            Shader.TileMode.CLAMP
        )
    }

    override fun setAlpha(alpha: Int) {
        val clamped = alpha.coerceIn(0, 255)
        if (drawableAlpha == clamped) return
        drawableAlpha = clamped
        invalidateSelf()
    }

    override fun getAlpha(): Int = drawableAlpha

    override fun setColorFilter(colorFilter: ColorFilter?) {
        this.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
