package com.inkcraft.app.ink

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.inkcraft.app.model.PenStyle
import com.inkcraft.app.model.Stroke
import kotlin.math.hypot

/**
 * High-quality Android Canvas stroke renderer supporting variable-width pressure ink,
 * highlighters, laser trails, and crisp geometric shapes.
 */
object StrokeRenderer {

    fun drawStroke(canvas: Canvas, stroke: Stroke, laserAlpha: Float = 1f) {
        val pts = stroke.points
        if (pts.isEmpty()) return

        val baseColor = stroke.colorArgb.toInt()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = if (stroke.penStyle == PenStyle.HIGHLIGHTER) Paint.Cap.SQUARE else Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = baseColor
        }

        when (stroke.penStyle) {
            PenStyle.HIGHLIGHTER -> {
                paint.alpha = (0x55 * laserAlpha).toInt().coerceIn(0, 255)
                paint.strokeWidth = stroke.baseWidth
                drawSmoothPath(canvas, stroke, paint)
                return
            }
            PenStyle.LASER -> {
                val glowPaint = Paint(paint).apply {
                    color = Color.argb((110 * laserAlpha).toInt().coerceIn(0, 255), Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
                    strokeWidth = stroke.baseWidth * 2.4f
                }
                drawSmoothPath(canvas, stroke, glowPaint)
                paint.color = Color.argb((240 * laserAlpha).toInt().coerceIn(0, 255), 255, 255, 255)
                paint.strokeWidth = stroke.baseWidth * 0.7f
                drawSmoothPath(canvas, stroke, paint)
                return
            }
            else -> Unit
        }

        if (stroke.isShape || stroke.penStyle == PenStyle.FINELINER) {
            paint.strokeWidth = stroke.baseWidth
            drawSmoothPath(canvas, stroke, paint)
            return
        }

        // Variable-width pressure-sensitive segments for Fountain, Ballpoint, Brush
        if (pts.size == 1) {
            val w = InkMath.computeWidth(pts[0], stroke.baseWidth, stroke.penStyle)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(pts[0].x, pts[0].y, w * 0.5f, paint)
            return
        }

        for (i in 0 until pts.size - 1) {
            val p0 = pts[i]
            val p1 = pts[i + 1]
            val w0 = InkMath.computeWidth(p0, stroke.baseWidth, stroke.penStyle)
            val w1 = InkMath.computeWidth(p1, stroke.baseWidth, stroke.penStyle)
            paint.strokeWidth = (w0 + w1) * 0.5f
            canvas.drawLine(p0.x, p0.y, p1.x, p1.y, paint)
        }
    }

    private fun drawSmoothPath(canvas: Canvas, stroke: Stroke, paint: Paint) {
        val pts = stroke.points
        if (pts.size == 1) {
            canvas.drawPoint(pts[0].x, pts[0].y, paint)
            return
        }
        val path = Path()
        path.moveTo(pts[0].x, pts[0].y)
        if (stroke.isShape || pts.size == 2) {
            for (i in 1 until pts.size) {
                path.lineTo(pts[i].x, pts[i].y)
            }
        } else {
            for (i in 1 until pts.size - 1) {
                val midX = (pts[i].x + pts[i + 1].x) * 0.5f
                val midY = (pts[i].y + pts[i + 1].y) * 0.5f
                path.quadTo(pts[i].x, pts[i].y, midX, midY)
            }
            val last = pts.last()
            path.lineTo(last.x, last.y)
        }
        canvas.drawPath(path, paint)
    }
}
