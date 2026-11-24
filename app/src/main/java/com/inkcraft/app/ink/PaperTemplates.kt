package com.inkcraft.app.ink

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.inkcraft.app.model.PaperColor
import com.inkcraft.app.model.PaperTemplate

/**
 * Renders crisp paper backgrounds and templates onto any Android Canvas
 * (used by both the live InkCanvas and the PDF/PNG exporter).
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
            strokeWidth = 1.4f
            style = Paint.Style.STROKE
        }

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (paperColor.isDark) 0xFF7F3A44.toInt() else 0xFFE8A2A2.toInt()
            strokeWidth = 2.0f
            style = Paint.Style.STROKE
        }

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (paperColor.isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt()
            textSize = 20f
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

            PaperTemplate.SQUARE_GRID -> {
                val step = 36f
                var x = step
                while (x < width) {
                    canvas.drawLine(x, 0f, x, height, linePaint)
                    x += step
                }
                var y = step
                while (y < height) {
                    canvas.drawLine(0f, y, width, y, linePaint)
                    y += step
                }
            }

            PaperTemplate.ISOMETRIC -> {
                val step = 48f
                val slope = 0.57735f // tan(30 deg)
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

                labelPaint.textSize = 20f
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
                val cols = 2
                val rows = 3
                val padX = 52f
                val topY = 124f
                val cellW = (width - padX * 2) / cols
                val cellH = (height - topY - 60f) / rows

                days.forEachIndexed { idx, day ->
                    val c = idx % cols
                    val r = idx / cols
                    val left = padX + c * cellW
                    val top = topY + r * cellH
                    canvas.drawRect(left + 8f, top + 8f, left + cellW - 8f, top + cellH - 8f, linePaint)
                    canvas.drawText(day, left + 24f, top + 40f, labelPaint)
                    var ly = top + 84f
                    while (ly < top + cellH - 24f) {
                        canvas.drawLine(left + 20f, ly, left + cellW - 20f, ly, linePaint)
                        ly += 48f
                    }
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
}
