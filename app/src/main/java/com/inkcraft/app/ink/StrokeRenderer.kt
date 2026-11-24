package com.inkcraft.app.ink

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.inkcraft.app.model.PenStyle
import com.inkcraft.app.model.Stroke
import com.inkcraft.app.model.StrokePoint
import kotlin.math.hypot

/**
 * Production-grade Apple PencilKit-style Ribbon Ink Renderer.
 * Uses Catmull-Rom spline subdivision + perpendicular normal ribbon polygon
 * construction so strokes are silky smooth with organic pressure & velocity tapering.
 */
object StrokeRenderer {

    private data class RibbonNode(val x: Float, val y: Float, val halfWidth: Float)

    fun drawStroke(canvas: Canvas, stroke: Stroke, laserAlpha: Float = 1f) {
        val pts = stroke.points
        if (pts.isEmpty()) return

        val baseColor = stroke.colorArgb.toInt()
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = if (stroke.penStyle == PenStyle.HIGHLIGHTER) Paint.Cap.SQUARE else Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = baseColor
        }

        when (stroke.penStyle) {
            PenStyle.HIGHLIGHTER -> {
                strokePaint.alpha = (0x52 * laserAlpha).toInt().coerceIn(0, 255)
                strokePaint.strokeWidth = stroke.baseWidth
                drawSmoothQuadPath(canvas, stroke, strokePaint)
                return
            }
            PenStyle.LASER -> {
                val glowPaint = Paint(strokePaint).apply {
                    color = Color.argb((110 * laserAlpha).toInt().coerceIn(0, 255), Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
                    strokeWidth = stroke.baseWidth * 2.4f
                }
                drawSmoothQuadPath(canvas, stroke, glowPaint)
                strokePaint.color = Color.argb((240 * laserAlpha).toInt().coerceIn(0, 255), 255, 255, 255)
                strokePaint.strokeWidth = stroke.baseWidth * 0.7f
                drawSmoothQuadPath(canvas, stroke, strokePaint)
                return
            }
            else -> Unit
        }

        if (stroke.isShape || stroke.penStyle == PenStyle.FINELINER) {
            strokePaint.strokeWidth = stroke.baseWidth
            drawSmoothQuadPath(canvas, stroke, strokePaint)
            return
        }

        // Single-point dot
        if (pts.size == 1) {
            val w = InkMath.computeWidth(pts[0], stroke.baseWidth, stroke.penStyle)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = baseColor
            }
            canvas.drawCircle(pts[0].x, pts[0].y, w * 0.5f, fillPaint)
            return
        }

        // Subdivide into smooth Catmull-Rom ribbon nodes
        val nodes = buildSmoothRibbonNodes(pts, stroke.baseWidth, stroke.penStyle)
        if (nodes.size < 2) return

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = baseColor
            if (stroke.penStyle == PenStyle.PENCIL) {
                alpha = 205
                maskFilter = BlurMaskFilter(0.9f, BlurMaskFilter.Blur.NORMAL)
            }
        }

        drawRibbonPolygon(canvas, nodes, fillPaint)
    }

    private fun buildSmoothRibbonNodes(
        pts: List<StrokePoint>,
        baseWidth: Float,
        style: PenStyle
    ): List<RibbonNode> {
        val rawWidths = FloatArray(pts.size) { idx ->
            val prev = if (idx > 0) pts[idx - 1] else null
            InkMath.computeWidth(pts[idx], baseWidth, style, prev) * 0.5f
        }
        // Smooth width transitions with a 3-tap moving average so pressure never steps abruptly
        val smoothWidths = FloatArray(pts.size) { i ->
            val wPrev = rawWidths[(i - 1).coerceAtLeast(0)]
            val wCurr = rawWidths[i]
            val wNext = rawWidths[(i + 1).coerceAtMost(pts.lastIndex)]
            wPrev * 0.25f + wCurr * 0.5f + wNext * 0.25f
        }

        val out = ArrayList<RibbonNode>(pts.size * 4)
        for (i in 0 until pts.size - 1) {
            val p0 = pts[(i - 1).coerceAtLeast(0)]
            val p1 = pts[i]
            val p2 = pts[i + 1]
            val p3 = pts[(i + 2).coerceAtMost(pts.lastIndex)]

            val w1 = smoothWidths[i]
            val w2 = smoothWidths[i + 1]

            val segLen = hypot(p2.x - p1.x, p2.y - p1.y)
            val steps = (segLen / 4f).toInt().coerceIn(2, 10)

            for (s in 0 until steps) {
                val t = s.toFloat() / steps
                val x = catmullRom(p0.x, p1.x, p2.x, p3.x, t)
                val y = catmullRom(p0.y, p1.y, p2.y, p3.y, t)
                val hw = w1 + (w2 - w1) * t
                out.add(RibbonNode(x, y, hw))
            }
        }
        val last = pts.last()
        out.add(RibbonNode(last.x, last.y, smoothWidths.last()))
        return out
    }

    private fun catmullRom(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
        val t2 = t * t
        val t3 = t2 * t
        return 0.5f * (
            (2f * p1) +
                (-p0 + p2) * t +
                (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
                (-p0 + 3f * p1 - 3f * p2 + p3) * t3
            )
    }

    private fun drawRibbonPolygon(canvas: Canvas, nodes: List<RibbonNode>, paint: Paint) {
        val n = nodes.size
        val leftX = FloatArray(n)
        val leftY = FloatArray(n)
        val rightX = FloatArray(n)
        val rightY = FloatArray(n)

        for (i in 0 until n) {
            val prev = nodes[(i - 1).coerceAtLeast(0)]
            val next = nodes[(i + 1).coerceAtMost(n - 1)]
            var dx = next.x - prev.x
            var dy = next.y - prev.y
            val len = hypot(dx, dy)
            if (len < 1e-4f) {
                dx = 1f
                dy = 0f
            } else {
                dx /= len
                dy /= len
            }
            val nx = -dy
            val ny = dx
            val hw = nodes[i].halfWidth
            leftX[i] = nodes[i].x + nx * hw
            leftY[i] = nodes[i].y + ny * hw
            rightX[i] = nodes[i].x - nx * hw
            rightY[i] = nodes[i].y - ny * hw
        }

        val path = Path()
        path.moveTo(leftX[0], leftY[0])
        for (i in 1 until n) {
            path.lineTo(leftX[i], leftY[i])
        }
        for (i in n - 1 downTo 0) {
            path.lineTo(rightX[i], rightY[i])
        }
        path.close()

        canvas.drawPath(path, paint)
        // Smooth round start & end caps
        canvas.drawCircle(nodes.first().x, nodes.first().y, nodes.first().halfWidth, paint)
        canvas.drawCircle(nodes.last().x, nodes.last().y, nodes.last().halfWidth, paint)
    }

    private fun drawSmoothQuadPath(canvas: Canvas, stroke: Stroke, paint: Paint) {
        val pts = stroke.points
        if (pts.size == 1) {
            canvas.drawPoint(pts[0].x, pts[0].y, paint)
            return
        }
        val path = Path()
        path.moveTo(pts[0].x, pts[0].y)
        if (stroke.isShape || pts.size == 2) {
            for (i in 1 until pts.size) path.lineTo(pts[i].x, pts[i].y)
        } else {
            for (i in 1 until pts.size - 1) {
                val midX = (pts[i].x + pts[i + 1].x) * 0.5f
                val midY = (pts[i].y + pts[i + 1].y) * 0.5f
                path.quadTo(pts[i].x, pts[i].y, midX, midY)
            }
            path.lineTo(pts.last().x, pts.last().y)
        }
        canvas.drawPath(path, paint)
    }
}
