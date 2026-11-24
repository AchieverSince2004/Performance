package com.inkcraft.app.ink

import com.inkcraft.app.model.PenStyle
import com.inkcraft.app.model.Stroke
import com.inkcraft.app.model.StrokePoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Kotlin ink geometry, pressure+velocity width mapping, hit-testing,
 * lasso selection/transform, and hold-to-snap shape recognition.
 */
object InkMath {

    const val PAGE_WIDTH = 1200f
    const val PAGE_HEIGHT = 1680f

    fun computeWidth(
        point: StrokePoint,
        baseWidth: Float,
        style: PenStyle,
        prevPoint: StrokePoint? = null
    ): Float {
        val clampedPressure = point.pressure.coerceIn(0.05f, 1.0f)
        // Subtle velocity thinning for organic fountain & brush feel
        var velocityFactor = 1f
        if (prevPoint != null && point.timestamp > prevPoint.timestamp) {
            val dt = (point.timestamp - prevPoint.timestamp).coerceAtLeast(1L).toFloat()
            val dist = hypot(point.x - prevPoint.x, point.y - prevPoint.y)
            val speed = dist / dt // px per ms
            velocityFactor = (1.08f - speed * 0.14f).coerceIn(0.72f, 1.08f)
        }

        val shaped = when (style) {
            PenStyle.FOUNTAIN -> {
                val p = clampedPressure * clampedPressure * (3f - 2f * clampedPressure)
                val tiltBoost = 1f + (point.tilt.coerceIn(0f, 1.2f) * 0.22f)
                lerp(style.minPressureScale, style.maxPressureScale, p) * tiltBoost * velocityFactor
            }
            PenStyle.BRUSH -> {
                val p = sqrt(clampedPressure)
                lerp(style.minPressureScale, style.maxPressureScale, p) * velocityFactor
            }
            PenStyle.CALLIGRAPHY -> {
                val p = clampedPressure
                val dirScale = if (prevPoint != null) {
                    val dx = point.x - prevPoint.x
                    val dy = point.y - prevPoint.y
                    val len = hypot(dx, dy).coerceAtLeast(0.01f)
                    // 45-degree chisel nib projection
                    val nib = abs((dx / len) * 0.7071f - (dy / len) * 0.7071f)
                    0.45f + 0.85f * nib
                } else 1f
                lerp(style.minPressureScale, style.maxPressureScale, p) * dirScale
            }
            PenStyle.PENCIL -> lerp(style.minPressureScale, style.maxPressureScale, clampedPressure)
            PenStyle.BALLPOINT -> lerp(style.minPressureScale, style.maxPressureScale, clampedPressure)
            PenStyle.FINELINER -> lerp(style.minPressureScale, style.maxPressureScale, clampedPressure)
            PenStyle.HIGHLIGHTER -> lerp(style.minPressureScale, style.maxPressureScale, clampedPressure)
            PenStyle.LASER -> 1.0f
        }
        return (baseWidth * shaped).coerceAtLeast(1.0f)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun pointToSegmentDistance(
        px: Float, py: Float,
        ax: Float, ay: Float,
        bx: Float, by: Float
    ): Float {
        val dx = bx - ax
        val dy = by - ay
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-5f) return hypot(px - ax, py - ay)
        val t = (((px - ax) * dx + (py - ay) * dy) / lenSq).coerceIn(0f, 1f)
        val projX = ax + t * dx
        val projY = ay + t * dy
        return hypot(px - projX, py - projY)
    }

    fun hitTestStroke(stroke: Stroke, x: Float, y: Float, radius: Float): Boolean {
        val pts = stroke.points
        if (pts.isEmpty()) return false
        val effectiveRadius = radius + stroke.baseWidth * 0.5f
        if (pts.size == 1) {
            return hypot(x - pts[0].x, y - pts[0].y) <= effectiveRadius
        }
        for (i in 0 until pts.size - 1) {
            val d = pointToSegmentDistance(x, y, pts[i].x, pts[i].y, pts[i + 1].x, pts[i + 1].y)
            if (d <= effectiveRadius) return true
        }
        return false
    }

    fun splitStrokeByEraser(stroke: Stroke, eraseX: Float, eraseY: Float, radius: Float): List<Stroke> {
        val pts = stroke.points
        if (pts.isEmpty()) return emptyList()
        val effectiveRadius = radius + stroke.baseWidth * 0.35f
        val keptSegments = mutableListOf<MutableList<StrokePoint>>()
        var current = mutableListOf<StrokePoint>()

        for (pt in pts) {
            val dist = hypot(pt.x - eraseX, pt.y - eraseY)
            if (dist > effectiveRadius) {
                current.add(pt)
            } else {
                if (current.size >= 2) keptSegments.add(current)
                current = mutableListOf()
            }
        }
        if (current.size >= 2) keptSegments.add(current)

        if (keptSegments.size == 1 && keptSegments[0].size == pts.size) return listOf(stroke)

        return keptSegments.mapIndexed { idx, seg ->
            stroke.copy(id = if (idx == 0) stroke.id else "${stroke.id}_$idx", points = seg)
        }
    }

    fun isPointInPolygon(x: Float, y: Float, polygon: List<StrokePoint>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val xi = polygon[i].x
            val yi = polygon[i].y
            val xj = polygon[j].x
            val yj = polygon[j].y
            val intersect = ((yi > y) != (yj > y)) &&
                (x < (xj - xi) * (y - yi) / ((yj - yi).takeIf { abs(it) > 1e-5f } ?: 1e-5f) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    fun isStrokeInLasso(stroke: Stroke, lasso: List<StrokePoint>): Boolean {
        if (stroke.points.isEmpty() || lasso.size < 3) return false
        val insideCount = stroke.points.count { isPointInPolygon(it.x, it.y, lasso) }
        return insideCount * 2 >= stroke.points.size
    }

    fun translateStroke(stroke: Stroke, dx: Float, dy: Float): Stroke {
        return stroke.copy(points = stroke.points.map { it.copy(x = it.x + dx, y = it.y + dy) })
    }

    fun scaleStrokeAround(stroke: Stroke, cx: Float, cy: Float, factor: Float): Stroke {
        return stroke.copy(
            baseWidth = (stroke.baseWidth * factor).coerceIn(1.5f, 48f),
            points = stroke.points.map {
                it.copy(
                    x = cx + (it.x - cx) * factor,
                    y = cy + (it.y - cy) * factor
                )
            }
        )
    }

    fun rotateStrokeAround(stroke: Stroke, cx: Float, cy: Float, angleRad: Float): Stroke {
        val c = cos(angleRad)
        val s = sin(angleRad)
        return stroke.copy(
            points = stroke.points.map {
                val dx = it.x - cx
                val dy = it.y - cy
                it.copy(
                    x = cx + dx * c - dy * s,
                    y = cy + dx * s + dy * c
                )
            }
        )
    }

    fun recognizeShape(points: List<StrokePoint>): List<StrokePoint>? {
        if (points.size < 6) return null
        val first = points.first()
        val last = points.last()
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var pathLen = 0f

        for (i in points.indices) {
            val p = points[i]
            minX = min(minX, p.x)
            minY = min(minY, p.y)
            maxX = max(maxX, p.x)
            maxY = max(maxY, p.y)
            if (i > 0) pathLen += hypot(p.x - points[i - 1].x, p.y - points[i - 1].y)
        }

        val width = maxX - minX
        val height = maxY - minY
        val diag = hypot(width, height)
        if (diag < 25f || pathLen < 25f) return null

        val endToEnd = hypot(last.x - first.x, last.y - first.y)

        if (endToEnd > 25f && pathLen / endToEnd < 1.14f) {
            val avgPressure = points.map { it.pressure }.average().toFloat().coerceIn(0.35f, 0.9f)
            return listOf(first.copy(pressure = avgPressure), last.copy(pressure = avgPressure))
        }

        val isClosed = endToEnd < max(48f, diag * 0.35f)
        if (!isClosed) return null

        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        val rx = width / 2f
        val ry = height / 2f
        val avgPressure = points.map { it.pressure }.average().toFloat().coerceIn(0.35f, 0.9f)

        if (rx > 10f && ry > 10f) {
            val ellipseErrors = points.map { p ->
                val nx = (p.x - cx) / rx
                val ny = (p.y - cy) / ry
                abs(hypot(nx, ny) - 1f)
            }
            if (ellipseErrors.average() < 0.17) {
                val isCircle = abs(rx - ry) / max(rx, ry) < 0.22f
                val finalRx = if (isCircle) (rx + ry) / 2f else rx
                val finalRy = if (isCircle) (rx + ry) / 2f else ry
                val steps = 48
                return (0..steps).map { i ->
                    val angle = (2.0 * PI * i / steps).toFloat()
                    StrokePoint(cx + finalRx * cos(angle), cy + finalRy * sin(angle), avgPressure)
                }
            }
        }

        if (width > 20f && height > 20f) {
            val edgeErrors = points.map { p ->
                val dx = min(abs(p.x - minX), abs(p.x - maxX))
                val dy = min(abs(p.y - minY), abs(p.y - maxY))
                min(dx, dy) / diag
            }
            if (edgeErrors.average() < 0.095) {
                return listOf(
                    StrokePoint(minX, minY, avgPressure),
                    StrokePoint(maxX, minY, avgPressure),
                    StrokePoint(maxX, maxY, avgPressure),
                    StrokePoint(minX, maxY, avgPressure),
                    StrokePoint(minX, minY, avgPressure)
                )
            }
        }

        val simplified = ramerDouglasPeucker(points, diag * 0.08f)
        if (simplified.size in 4..5) {
            return listOf(
                simplified[0].copy(pressure = avgPressure),
                simplified[1].copy(pressure = avgPressure),
                simplified[2].copy(pressure = avgPressure),
                simplified[0].copy(pressure = avgPressure)
            )
        }

        return null
    }

    fun ramerDouglasPeucker(points: List<StrokePoint>, epsilon: Float): List<StrokePoint> {
        if (points.size < 3) return points
        var maxDist = 0f
        var index = 0
        val start = points.first()
        val end = points.last()
        for (i in 1 until points.size - 1) {
            val d = pointToSegmentDistance(points[i].x, points[i].y, start.x, start.y, end.x, end.y)
            if (d > maxDist) {
                maxDist = d
                index = i
            }
        }
        return if (maxDist > epsilon) {
            val left = ramerDouglasPeucker(points.subList(0, index + 1), epsilon)
            val right = ramerDouglasPeucker(points.subList(index, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(start, end)
        }
    }

    fun snapToRuler(
        x: Float, y: Float,
        rulerCx: Float, rulerCy: Float,
        rulerAngleRad: Float,
        snapDistance: Float = 28f
    ): Pair<Float, Float> {
        val dx = cos(rulerAngleRad)
        val dy = sin(rulerAngleRad)
        val vx = x - rulerCx
        val vy = y - rulerCy
        val perp = abs(vx * (-dy) + vy * dx)
        if (perp <= snapDistance) {
            val proj = vx * dx + vy * dy
            return (rulerCx + proj * dx) to (rulerCy + proj * dy)
        }
        return x to y
    }

    fun angleDegrees(dx: Float, dy: Float): Float = (atan2(dy, dx) * 180.0 / PI).toFloat()
}
