package com.inkcraft.app.ink

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import com.inkcraft.app.model.PaperColor
import com.inkcraft.app.model.PaperTemplate
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders 18 crisp procedural paper templates onto any Android Canvas
 * (used by live InkCanvasView and multi-page PDF exporter).
 */
object PaperTemplates {

    fun drawPaper(
        canvas: Canvas,
        width: Float,
        height: Float,
        template: PaperTemplate,
        paperColor: PaperColor
    ) {
        val bgPaint = Paint().apply {
            color = paperColor.bgArgb.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paperColor.lineArgb.toInt()
            strokeWidth = 1.35f
            style = Paint.Style.STROKE
        }

        val majorLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paperColor.lineArgb.toInt()
            strokeWidth = 2.4f
            style = Paint.Style.STROKE
        }

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (paperColor.isDark) 0xFF7F3A44.toInt() else 0xFFE8A2A2.toInt()
            strokeWidth = 2.0f
            style = Paint.Style.STROKE
        }

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (paperColor.isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt()
            textSize = 19f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        when (template) {
            PaperTemplate.BLANK -> Unit

            PaperTemplate.RULED_WIDE -> drawRuled(canvas, width, height, 52f, 130f, linePaint, accentPaint)

            PaperTemplate.RULED_NARROW -> drawRuled(canvas, width, height, 36f, 110f, linePaint, accentPaint)

            PaperTemplate.CORNELL -> {
                val topMargin = 140f
                val cueX = width * 0.28f
                val summaryY = height * 0.80f
                canvas.drawLine(0f, topMargin, width, topMargin, accentPaint)
                canvas.drawLine(cueX, topMargin, cueX, summaryY, accentPaint)
                canvas.drawLine(0f, summaryY, width, summaryY, accentPaint)
                var y = topMargin + 44f
                while (y < summaryY) {
                    canvas.drawLine(cueX + 8f, y, width - 32f, y, linePaint)
                    y += 44f
                }
                canvas.drawText("TOPIC / DATE", 40f, 68f, labelPaint)
                canvas.drawText("CUES", 40f, topMargin + 36f, labelPaint)
                canvas.drawText("NOTES", cueX + 24f, topMargin + 36f, labelPaint)
                canvas.drawText("SUMMARY", 40f, summaryY + 36f, labelPaint)
            }

            PaperTemplate.DOT_GRID -> {
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = paperColor.lineArgb.toInt()
                    style = Paint.Style.FILL
                }
                val step = 36f
                var x = step
                while (x < width) {
                    var y = step
                    while (y < height) {
                        canvas.drawCircle(x, y, 2.2f, dotPaint)
                        y += step
                    }
                    x += step
                }
            }

            PaperTemplate.SQUARE_GRID -> drawGrid(canvas, width, height, 36f, linePaint, null)

            PaperTemplate.ENGINEERING_GRID -> drawGrid(canvas, width, height, 24f, linePaint, majorLinePaint, majorEvery = 5)

            PaperTemplate.ISOMETRIC -> {
                val step = 48f
                val slope = 0.57735f
                var x = -height
                while (x < width + height) {
                    canvas.drawLine(x, 0f, x + height / slope, height, linePaint)
                    canvas.drawLine(x, 0f, x - height / slope, height, linePaint)
                    x += step
                }
                var vx = step
                while (vx < width) {
                    canvas.drawLine(vx, 0f, vx, height, linePaint)
                    vx += step
                }
            }

            PaperTemplate.HEXAGONAL -> {
                val r = 28f
                val dx = r * 1.5f
                val dy = r * 1.73205f
                var col = 0
                var cx = r
                while (cx < width + r) {
                    var cy = if (col % 2 == 0) r else r + dy * 0.5f
                    while (cy < height + r) {
                        val path = Path()
                        for (i in 0 until 6) {
                            val angle = Math.toRadians(60.0 * i).toFloat()
                            val px = cx + r * cos(angle)
                            val py = cy + r * sin(angle)
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        path.close()
                        canvas.drawPath(path, linePaint)
                        cy += dy
                    }
                    cx += dx
                    col++
                }
            }

            PaperTemplate.STORYBOARD -> {
                canvas.drawText("STORYBOARD", 56f, 72f, labelPaint)
                val cols = 2
                val rows = 3
                val padX = 56f
                val topY = 110f
                val gap = 28f
                val boxW = (width - padX * 2 - gap) / cols
                val boxH = (height - topY - 80f - gap * 2) / rows
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val left = padX + c * (boxW + gap)
                        val top = topY + r * (boxH + gap)
                        val frameH = boxH * 0.72f
                        canvas.drawRect(left, top, left + boxW, top + frameH, majorLinePaint)
                        canvas.drawText("SCENE ${r * cols + c + 1}", left + 12f, top + frameH + 28f, labelPaint.apply { textSize = 15f })
                        canvas.drawLine(left, top + frameH + 46f, left + boxW, top + frameH + 46f, linePaint)
                        canvas.drawLine(left, top + frameH + 82f, left + boxW, top + frameH + 82f, linePaint)
                    }
                }
            }

            PaperTemplate.WIREFRAME -> {
                canvas.drawText("TABLET UI WIREFRAME", 56f, 72f, labelPaint)
                val padX = 64f
                val topY = 110f
                val frameW = width - padX * 2
                val frameH = height - topY - 70f
                canvas.drawRoundRect(padX, topY, padX + frameW, topY + frameH, 24f, 24f, majorLinePaint)
                canvas.drawLine(padX, topY + 72f, padX + frameW, topY + 72f, linePaint)
                canvas.drawLine(padX + 240f, topY + 72f, padX + 240f, topY + frameH, linePaint)
                // Subtle dot grid inside wireframe
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = paperColor.lineArgb.toInt()
                    style = Paint.Style.FILL
                }
                var x = padX + 264f
                while (x < padX + frameW - 20f) {
                    var y = topY + 96f
                    while (y < topY + frameH - 20f) {
                        canvas.drawCircle(x, y, 1.8f, dotPaint)
                        y += 32f
                    }
                    x += 32f
                }
            }

            PaperTemplate.MUSIC_STAFF -> {
                var staffTop = 120f
                while (staffTop + 60f < height - 60f) {
                    for (i in 0 until 5) {
                        val y = staffTop + i * 14f
                        canvas.drawLine(64f, y, width - 64f, y, linePaint)
                    }
                    staffTop += 140f
                }
            }

            PaperTemplate.GUITAR_TAB -> {
                canvas.drawText("GUITAR TABLATURE", 64f, 76f, labelPaint)
                var tabTop = 126f
                while (tabTop + 90f < height - 60f) {
                    for (i in 0 until 6) {
                        val y = tabTop + i * 16f
                        canvas.drawLine(64f, y, width - 64f, y, linePaint)
                    }
                    canvas.drawLine(64f, tabTop, 64f, tabTop + 80f, majorLinePaint)
                    canvas.drawLine(width - 64f, tabTop, width - 64f, tabTop + 80f, majorLinePaint)
                    tabTop += 160f
                }
            }

            PaperTemplate.DAILY_PLANNER -> {
                canvas.drawText("DAILY PLANNER", 56f, 84f, labelPaint)
                canvas.drawLine(56f, 108f, width - 56f, 108f, accentPaint)
                val midX = width * 0.54f
                canvas.drawLine(midX, 130f, midX, height - 60f, linePaint)
                canvas.drawText("SCHEDULE", 56f, 152f, labelPaint)
                var y = 196f
                for (hour in 7..20) {
                    val label = if (hour <= 12) "${hour}am" else "${hour - 12}pm"
                    canvas.drawText(label, 56f, y - 8f, labelPaint.apply { textSize = 16f })
                    canvas.drawLine(120f, y, midX - 24f, y, linePaint)
                    y += 66f
                }
                labelPaint.textSize = 19f
                canvas.drawText("TOP PRIORITIES", midX + 28f, 152f, labelPaint)
                var ty = 200f
                repeat(5) {
                    canvas.drawRect(midX + 28f, ty - 22f, midX + 52f, ty + 2f, linePaint)
                    canvas.drawLine(midX + 68f, ty, width - 56f, ty, linePaint)
                    ty += 64f
                }
                canvas.drawText("NOTES & REFLECTION", midX + 28f, 560f, labelPaint)
                var ny = 610f
                while (ny < height - 80f) {
                    canvas.drawLine(midX + 28f, ny, width - 56f, ny, linePaint)
                    ny += 52f
                }
            }

            PaperTemplate.WEEKLY_SCHEDULE -> {
                canvas.drawText("WEEKLY OVERVIEW", 56f, 84f, labelPaint)
                val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT / SUN")
                val padX = 52f
                val topY = 124f
                val cellW = (width - padX * 2) / 2
                val cellH = (height - topY - 60f) / 3
                days.forEachIndexed { idx, day ->
                    val left = padX + (idx % 2) * cellW
                    val top = topY + (idx / 2) * cellH
                    canvas.drawRect(left + 8f, top + 8f, left + cellW - 8f, top + cellH - 8f, linePaint)
                    canvas.drawText(day, left + 24f, top + 40f, labelPaint)
                    var ly = top + 84f
                    while (ly < top + cellH - 24f) {
                        canvas.drawLine(left + 20f, ly, left + cellW - 20f, ly, linePaint)
                        ly += 48f
                    }
                }
            }

            PaperTemplate.MONTHLY_CALENDAR -> {
                canvas.drawText("MONTHLY CALENDAR", 56f, 78f, labelPaint)
                val padX = 48f
                val topY = 120f
                val cellW = (width - padX * 2) / 7f
                val cellH = (height - topY - 60f) / 5f
                val headers = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
                headers.forEachIndexed { c, h ->
                    canvas.drawText(h, padX + c * cellW + 14f, topY - 12f, labelPaint.apply { textSize = 15f })
                }
                for (r in 0..5) {
                    val y = topY + r * cellH
                    canvas.drawLine(padX, y, width - padX, y, linePaint)
                }
                for (c in 0..7) {
                    val x = padX + c * cellW
                    canvas.drawLine(x, topY, x, topY + 5 * cellH, linePaint)
                }
            }

            PaperTemplate.HABIT_TRACKER -> {
                canvas.drawText("HABIT & FOCUS TRACKER", 56f, 82f, labelPaint)
                val padX = 56f
                val nameColW = 300f
                val topY = 140f
                val rowH = 54f
                val days = 21
                val dayW = (width - padX * 2 - nameColW) / days
                canvas.drawText("HABIT", padX, topY - 16f, labelPaint.apply { textSize = 15f })
                for (d in 1..days) {
                    canvas.drawText("$d", padX + nameColW + (d - 1) * dayW + 10f, topY - 16f, labelPaint.apply { textSize = 13f })
                }
                for (r in 0 until 18) {
                    val y = topY + r * rowH
                    canvas.drawLine(padX, y + rowH, width - padX, y + rowH, linePaint)
                    for (d in 0 until days) {
                        val cx = padX + nameColW + d * dayW + dayW * 0.5f
                        canvas.drawCircle(cx, y + rowH * 0.5f, 11f, linePaint)
                    }
                }
            }

            PaperTemplate.MEETING_MINUTES -> {
                canvas.drawText("MEETING MINUTES & ACTION LOG", 56f, 80f, labelPaint)
                canvas.drawLine(56f, 102f, width - 56f, 102f, accentPaint)
                canvas.drawText("DATE / ATTENDEES", 56f, 144f, labelPaint.apply { textSize = 16f })
                canvas.drawLine(56f, 194f, width - 56f, 194f, linePaint)
                canvas.drawText("AGENDA & DISCUSSION", 56f, 240f, labelPaint)
                var y = 290f
                while (y < height * 0.65f) {
                    canvas.drawLine(56f, y, width - 56f, y, linePaint)
                    y += 48f
                }
                val actionTop = height * 0.68f
                canvas.drawLine(56f, actionTop - 36f, width - 56f, actionTop - 36f, accentPaint)
                canvas.drawText("ACTION ITEMS / OWNER / DUE", 56f, actionTop, labelPaint)
                var ay = actionTop + 48f
                while (ay < height - 60f) {
                    canvas.drawRect(56f, ay - 20f, 80f, ay + 4f, linePaint)
                    canvas.drawLine(96f, ay, width - 56f, ay, linePaint)
                    ay += 54f
                }
            }
        }
    }

    private fun drawRuled(
        canvas: Canvas,
        width: Float,
        height: Float,
        spacing: Float,
        topMargin: Float,
        linePaint: Paint,
        accentPaint: Paint
    ) {
        val marginX = 128f
        canvas.drawLine(marginX, 0f, marginX, height, accentPaint)
        var y = topMargin
        while (y < height - 40f) {
            canvas.drawLine(0f, y, width, y, linePaint)
            y += spacing
        }
    }

    private fun drawGrid(
        canvas: Canvas,
        width: Float,
        height: Float,
        step: Float,
        linePaint: Paint,
        majorPaint: Paint?,
        majorEvery: Int = 0
    ) {
        var idx = 1
        var x = step
        while (x < width) {
            val p = if (majorPaint != null && majorEvery > 0 && idx % majorEvery == 0) majorPaint else linePaint
            canvas.drawLine(x, 0f, x, height, p)
            x += step
            idx++
        }
        idx = 1
        var y = step
        while (y < height) {
            val p = if (majorPaint != null && majorEvery > 0 && idx % majorEvery == 0) majorPaint else linePaint
            canvas.drawLine(0f, y, width, y, p)
            y += step
            idx++
        }
    }
}
