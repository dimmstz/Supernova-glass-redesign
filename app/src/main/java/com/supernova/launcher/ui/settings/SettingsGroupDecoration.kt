package com.supernova.launcher.ui.settings

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.supernova.launcher.R
import com.supernova.launcher.ui.dp
import kotlin.math.min

// The cards of a settings page: each run of rows between Headers sits on
// one rounded glass plate, 16dp in from the sides, with a 4dp inset above its
// first row and below its last. Geometry and row grouping are unchanged; the
// paint adds a low-opacity fill and a reusable specular rim without per-frame
// bitmap, blur or shader allocation.
class SettingsGroupDecoration(context: Context, private val adapter: SettingsAdapter) :
    RecyclerView.ItemDecoration() {

    private val cardAlpha = context.resources.getInteger(R.integer.glass_card_alpha)
    private val accentGlowAlpha = context.resources.getInteger(R.integer.glass_accent_glow_alpha)
    private val strokeWidth = context.resources.getDimension(R.dimen.glass_stroke_width)
    private var resolvedCardColor: Int = Color.TRANSPARENT
    private var resolvedAccentColor: Int = context.getColor(R.color.accent)

    var cardColor: Int
        get() = resolvedCardColor
        set(value) {
            resolvedCardColor = value
            val alpha = min(Color.alpha(value), cardAlpha)
            cardPaint.color = ColorUtils.setAlphaComponent(value, alpha)
        }

    var accentColor: Int
        get() = resolvedAccentColor
        set(value) {
            resolvedAccentColor = value
            accentPaint.color = ColorUtils.setAlphaComponent(value, accentGlowAlpha)
        }

    var dividerColor: Int
        get() = dividerPaint.color
        set(value) {
            dividerPaint.color = value
        }

    private val gutter = context.dp(GUTTER_DP)
    private val inset = context.dp(INSET_DP)
    private val groupGap = context.dp(GROUP_GAP_DP)
    private val entryGap = context.dp(ENTRY_GAP_DP)
    // On a tiled page the search bar's own 8dp bottom padding makes up the
    // rest of the 24dp between it and the first card; a section page's list
    // starts under its toolbar or its preview card.
    private val tiledFirstGap = context.dp(GROUP_GAP_DP - SEARCH_PADDING_DP)
    private val radius = context.resources.getDimension(R.dimen.glass_card_radius)
    private val tiledDividerStart = context.dp(TILED_DIVIDER_START_DP)
    private val dividerStart = context.dp(DIVIDER_START_DP)
    private val dividerHeight = context.dp(DIVIDER_DP).coerceAtLeast(1)
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dividerPaint = Paint()
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@SettingsGroupDecoration.strokeWidth
        color = context.getColor(R.color.glassSurfaceOutline)
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@SettingsGroupDecoration.strokeWidth
    }
    private val specularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@SettingsGroupDecoration.strokeWidth
        color = context.getColor(R.color.glassSpecularStrong)
    }
    private val slice = RectF()
    private val path = Path()
    private val arc = RectF()
    private val radii = FloatArray(RADII_SIZE)

    init {
        accentColor = resolvedAccentColor
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        outRect.left = gutter
        outRect.right = gutter
        val firstGap = if (adapter.tiled) tiledFirstGap else groupGap
        if (adapter.isGroupCard(position)) {
            if (adapter.startsCard(position)) outRect.top = inset + gapAbove(position, firstGap)
            if (adapter.endsCard(position)) outRect.bottom = inset
        } else {
            outRect.top = if (position == 0) firstGap else groupGap
        }
    }

    // A card under its label sits 8dp under it (the label's padding); one
    // that follows another card stands clear of it.
    private fun gapAbove(position: Int, firstGap: Int): Int = when {
        position == 0 -> firstGap
        adapter.isLabel(position - 1) -> 0
        !adapter.isGroupCard(position - 1) -> groupGap
        adapter.isEntryCard(position) || adapter.isEntryCard(position - 1) -> entryGap
        else -> groupGap
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val dividerStart = if (adapter.tiled) tiledDividerStart else dividerStart
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION || !adapter.isGroupCard(position)) continue
            val first = adapter.startsCard(position)
            val last = adapter.endsCard(position)
            val top = child.top + child.translationY
            val bottom = child.bottom + child.translationY
            slice.set(
                child.left.toFloat(), if (first) top - inset else top,
                child.right.toFloat(), if (last) bottom + inset else bottom
            )
            val upper = if (first) radius else 0f
            val lower = if (last) radius else 0f
            radii.fill(upper, 0, RADII_TOP_END)
            radii.fill(lower, RADII_TOP_END, RADII_SIZE)
            path.reset()
            path.addRoundRect(slice, radii, Path.Direction.CW)
            canvas.drawPath(path, cardPaint)
            drawGlassRim(canvas, first, last)

            if (!last) {
                canvas.drawRect(
                    child.left.toFloat() + dividerStart, bottom - dividerHeight,
                    child.right.toFloat(), bottom, dividerPaint
                )
            }
        }
    }

    private fun drawGlassRim(canvas: Canvas, first: Boolean, last: Boolean) {
        val left = slice.left + strokeWidth / 2f
        val right = slice.right - strokeWidth / 2f
        val top = slice.top + strokeWidth / 2f
        val bottom = slice.bottom - strokeWidth / 2f
        val corner = min(radius, min(slice.width(), slice.height()) / 2f)
        if (right <= left || bottom <= top) return

        canvas.drawLine(left, if (first) top + corner else top, left, if (last) bottom - corner else bottom, edgePaint)
        canvas.drawLine(right, if (first) top + corner else top, right, if (last) bottom - corner else bottom, edgePaint)

        if (first && corner > 0f) {
            arc.set(left, top, left + corner * 2f, top + corner * 2f)
            canvas.drawArc(arc, 180f, 90f, false, edgePaint)
            arc.set(right - corner * 2f, top, right, top + corner * 2f)
            canvas.drawArc(arc, 270f, 90f, false, edgePaint)
        }
        if (last && corner > 0f) {
            arc.set(left, bottom - corner * 2f, left + corner * 2f, bottom)
            canvas.drawArc(arc, 90f, 90f, false, edgePaint)
            arc.set(right - corner * 2f, bottom - corner * 2f, right, bottom)
            canvas.drawArc(arc, 0f, 90f, false, edgePaint)
        }

        if (first) {
            val lineStart = if (corner > 0f) left + corner * 0.58f else left
            val lineEnd = if (corner > 0f) right - corner * 0.58f else right
            if (lineEnd > lineStart) {
                canvas.drawLine(lineStart, top + strokeWidth, lineEnd, top + strokeWidth, accentPaint)
                canvas.drawLine(lineStart, top, lineEnd, top, specularPaint)
            }
        }
        if (last && corner > 0f) {
            canvas.drawLine(left + corner, bottom, right - corner, bottom, edgePaint)
        }
    }

    private companion object {
        const val GUTTER_DP = 16
        const val INSET_DP = 4
        const val GROUP_GAP_DP = 24
        const val ENTRY_GAP_DP = 12
        const val SEARCH_PADDING_DP = 8
        const val TILED_DIVIDER_START_DP = 72
        const val DIVIDER_START_DP = 16
        const val DIVIDER_DP = 1
        const val RADII_TOP_END = 4
        const val RADII_SIZE = 8
    }
}
