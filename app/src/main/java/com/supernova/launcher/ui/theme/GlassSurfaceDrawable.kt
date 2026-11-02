package com.supernova.launcher.ui.theme

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import com.supernova.launcher.R
import kotlin.math.max

/**
 * A lightweight native glass plate. Its gradients are rebuilt only when the
 * view bounds change; draw() reuses the same Paint, Path and Shader objects.
 * Backdrop blur is deliberately owned by the containing window, not every
 * card, so scrolling lists do not allocate or blur on each frame.
 */
class GlassSurfaceDrawable(
    context: Context,
    private val baseColor: Int,
    private val accentColor: Int,
    private val cornerRadiusPx: Float
) : Drawable() {

    private val strokeWidth = context.resources.getDimension(R.dimen.glass_stroke_width)
    private val outlineColor = context.getColor(R.color.glassSurfaceOutline)
    private val specularStrong = context.getColor(R.color.glassSpecularStrong)
    private val specularSoft = context.getColor(R.color.glassSpecularSoft)
    private val accentGlowAlpha = context.resources.getInteger(R.integer.glass_accent_glow_alpha)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@GlassSurfaceDrawable.strokeWidth
    }
    private val topRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@GlassSurfaceDrawable.strokeWidth
    }
    private val surface = RectF()
    private val outlinePath = Path()
    private var fillShader: Shader? = null
    private var glowShader: Shader? = null
    private var sheenShader: Shader? = null
    private var rimShader: Shader? = null
    private var drawableAlpha = 255
    private var colorFilter: ColorFilter? = null

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return
        fillPaint.shader = fillShader
        fillPaint.alpha = drawableAlpha
        fillPaint.colorFilter = colorFilter
        canvas.save()
        canvas.clipPath(outlinePath)
        canvas.drawRect(surface, fillPaint)

        glowPaint.shader = glowShader
        glowPaint.alpha = drawableAlpha
        glowPaint.colorFilter = colorFilter
        canvas.drawRect(surface, glowPaint)

        sheenPaint.shader = sheenShader
        sheenPaint.alpha = drawableAlpha
        sheenPaint.colorFilter = colorFilter
        canvas.drawRect(surface, sheenPaint)
        canvas.restore()

        val inset = strokeWidth / 2f
        val inner = RectF(surface).apply { inset(inset, inset) }
        val radius = (cornerRadiusPx - inset).coerceAtLeast(0f)
        rimPaint.shader = rimShader
        rimPaint.alpha = drawableAlpha
        rimPaint.colorFilter = colorFilter
        canvas.drawRoundRect(inner, radius, radius, rimPaint)

        // One clean top catchlight; the accent glint beneath it stays subtle.
        val insetRadius = radius.coerceAtMost(minOf(inner.width(), inner.height()) / 2f)
        val lineStart = inner.left + insetRadius * 0.58f
        val lineEnd = inner.right - insetRadius * 0.58f
        if (lineEnd > lineStart) {
            topRimPaint.color = accentColor
            topRimPaint.alpha = (drawableAlpha * accentGlowAlpha / 255f).toInt()
            topRimPaint.shader = null
            topRimPaint.colorFilter = colorFilter
            val y = inner.top + strokeWidth
            canvas.drawLine(lineStart, y, lineEnd, y, topRimPaint)
            topRimPaint.color = specularStrong
            topRimPaint.alpha = drawableAlpha
            canvas.drawLine(lineStart, inner.top, lineEnd, inner.top, topRimPaint)
        }
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        surface.set(bounds)
        outlinePath.reset()
        if (bounds.isEmpty) return

        val radius = cornerRadiusPx.coerceAtMost(minOf(surface.width(), surface.height()) / 2f)
        outlinePath.addRoundRect(surface, radius, radius, Path.Direction.CW)
        val top = ColorUtils.blendARGB(baseColor, Color.WHITE, 0.055f)
        val lower = ColorUtils.blendARGB(baseColor, accentColor, 0.035f)
        fillShader = LinearGradient(
            surface.left, surface.top, surface.right, surface.bottom,
            top, lower, Shader.TileMode.CLAMP
        )
        glowShader = RadialGradient(
            surface.left + surface.width() * 0.18f,
            surface.top + surface.height() * 0.05f,
            max(surface.width(), surface.height()) * 0.78f,
            ColorUtils.setAlphaComponent(accentColor, accentGlowAlpha),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        sheenShader = LinearGradient(
            surface.left, surface.top, surface.left, surface.top + surface.height() * 0.54f,
            specularSoft, Color.TRANSPARENT, Shader.TileMode.CLAMP
        )
        rimShader = LinearGradient(
            surface.left, surface.top, surface.right, surface.bottom,
            intArrayOf(specularStrong, outlineColor, specularSoft),
            floatArrayOf(0f, 0.52f, 1f), Shader.TileMode.CLAMP
        )
    }

    override fun getOutline(outline: Outline) {
        outline.setRoundRect(bounds, cornerRadiusPx.coerceAtMost(minOf(bounds.width(), bounds.height()) / 2f))
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
