package com.inkcraft.app.ink

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.inkcraft.app.model.CanvasImage
import com.inkcraft.app.model.NotePage
import com.inkcraft.app.model.PenStyle
import com.inkcraft.app.model.StickyNote
import com.inkcraft.app.model.Stroke
import com.inkcraft.app.model.StrokePoint
import com.inkcraft.app.model.TextBox
import com.inkcraft.app.model.ToolMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Hardware-accelerated tablet stylus canvas with:
 * - Zero-drift constrained viewport & persistent user zoom
 * - True palm rejection (stylus pointer tracking + 550ms hover/lift cooldown + touchMajor filter)
 * - Interactive movable & rotatable straightedge ruler with live angle readout
 * - Lasso selection with move, scale, rotate, recolor, duplicate, delete
 * - Draggable Sticky Notes & Text Boxes
 */
class InkCanvasView(context: Context) : View(context) {

    var page: NotePage = NotePage()
        set(value) {
            field = value
            invalidate()
        }

    var pdfBackgroundBitmap: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }

    var imageBitmaps: Map<String, Bitmap> = emptyMap()
        set(value) {
            field = value
            invalidate()
        }

    var toolMode: ToolMode = ToolMode.PEN
    var penStyle: PenStyle = PenStyle.FOUNTAIN
    var penColorArgb: Long = 0xFF1D1D1F
    var penWidth: Float = 5.2f
    var stylusOnlyMode: Boolean = true
    var pageLocked: Boolean = false
    var shapeRecognitionEnabled: Boolean = true
    var showRuler: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private var lastStylusActivityMs: Long = 0L

    var zoomScale: Float = 1f
        private set
    var panX: Float = 0f
        private set
    var panY: Float = 0f
        private set
    private var hasInitializedFit = false

    var onStrokeCommitted: ((Stroke) -> Unit)? = null
    var onStrokesReplaced: ((List<Stroke>) -> Unit)? = null
    var onStickyNoteTapped: ((StickyNote) -> Unit)? = null
    var onStickyNoteAdded: ((Float, Float) -> Unit)? = null
    var onStickyNoteMoved: ((String, Float, Float) -> Unit)? = null
    var onTextBoxTapped: ((TextBox) -> Unit)? = null
    var onTextBoxAdded: ((Float, Float) -> Unit)? = null
    var onViewportChanged: ((Float, Float, Float) -> Unit)? = null
    var onLassoSelectionChanged: ((Set<String>) -> Unit)? = null

    private val activePoints = mutableListOf<StrokePoint>()
    private var isDrawing = false
    private var isErasing = false

    private val lassoPoints = mutableListOf<StrokePoint>()
    private var selectedStrokeIds = mutableSetOf<String>()
    private var isDraggingSelection = false
    private var draggingStickyId: String? = null
    private var lastDragX = 0f
    private var lastDragY = 0f

    private data class LaserTrail(val stroke: Stroke, val createdAt: Long)
    private val laserTrails = mutableListOf<LaserTrail>()

    // Ruler state
    private var rulerCx = InkMath.PAGE_WIDTH * 0.5f
    private var rulerCy = InkMath.PAGE_HEIGHT * 0.35f
    private var rulerAngleRad = 0f
    private var rulerMode = 0 // 0=none, 1=drag, 2=rotate

    private val handler = Handler(Looper.getMainLooper())
    private var lastMoveTime = 0L
    private var shapeSnappedForCurrentStroke = false
    private val shapeCheckRunnable = Runnable {
        if (isDrawing && shapeRecognitionEnabled && !shapeSnappedForCurrentStroke &&
            toolMode == ToolMode.PEN && penStyle != PenStyle.LASER &&
            System.currentTimeMillis() - lastMoveTime >= 420L
        ) {
            val snapped = InkMath.recognizeShape(activePoints)
            if (snapped != null) {
                activePoints.clear()
                activePoints.addAll(snapped)
                shapeSnappedForCurrentStroke = true
                invalidate()
            }
        }
    }

    private val laserFadeRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            laserTrails.removeAll { now - it.createdAt > 1600L }
            if (laserTrails.isNotEmpty()) {
                invalidate()
                handler.postDelayed(this, 32L)
            }
        }
    }

    private var isPanning = false
    private var prevFocusX = 0f
    private var prevFocusY = 0f

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val oldScale = zoomScale
                val newScale = (zoomScale * detector.scaleFactor).coerceIn(0.45f, 3.5f)
                val factor = newScale / oldScale
                panX = detector.focusX - (detector.focusX - panX) * factor
                panY = detector.focusY - (detector.focusY - panY) * factor
                zoomScale = newScale
                clampPan()
                onViewportChanged?.invoke(zoomScale, panX, panY)
                invalidate()
                return true
            }
        }
    )

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x1E000000
        style = Paint.Style.FILL
    }
    private val lassoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF007AFF.toInt()
        strokeWidth = 3f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
    }
    private val selectionBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x26007AFF
        style = Paint.Style.FILL
    }
    private val stickyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stickyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1D1D1F.toInt()
        textSize = 23f
    }
    private val textBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1D1D1F.toInt()
        textSize = 28f
    }
    private val rulerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xDD1C1F26.toInt()
        style = Paint.Style.FILL
    }
    private val rulerTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF8FAFC.toInt()
        strokeWidth = 2f
    }
    private val rulerHandlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF007AFF.toInt()
        style = Paint.Style.FILL
    }
    private val rulerAnglePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF8FAFC.toInt()
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!hasInitializedFit && w > 0 && h > 0) {
            fitPageToWidth(w, h)
            hasInitializedFit = true
        } else if (w > 0 && h > 0) {
            clampPan(w, h)
        }
    }

    /**
     * Fits the page to fill the tablet width comfortably (no tiny page!).
     */
    fun fitPageToWidth(w: Int = width, h: Int = height) {
        if (w <= 0 || h <= 0) return
        zoomScale = ((w - 40f) / InkMath.PAGE_WIDTH).coerceIn(0.55f, 2.2f)
        panX = (w - InkMath.PAGE_WIDTH * zoomScale) * 0.5f
        panY = 24f
        clampPan(w, h)
        onViewportChanged?.invoke(zoomScale, panX, panY)
        invalidate()
    }

    /**
     * Sets a fixed zoom scale centered on the screen and keeps it locked.
     */
    fun setFixedZoom(newScale: Float) {
        if (width <= 0 || height <= 0) {
            zoomScale = newScale.coerceIn(0.45f, 3.5f)
            return
        }
        val clamped = newScale.coerceIn(0.45f, 3.5f)
        val cx = width * 0.5f
        val cy = height * 0.35f
        val factor = clamped / zoomScale
        panX = cx - (cx - panX) * factor
        panY = cy - (cy - panY) * factor
        zoomScale = clamped
        clampPan()
        onViewportChanged?.invoke(zoomScale, panX, panY)
        invalidate()
    }

    fun setViewport(scale: Float, px: Float, py: Float) {
        zoomScale = scale.coerceIn(0.45f, 4.5f)
        panX = px
        panY = py
        invalidate()
    }

    /**
     * Prevents the page from sliding off-screen horizontally or vertically.
     */
    private fun clampPan(w: Int = width, h: Int = height) {
        if (w <= 0 || h <= 0) return
        val contentW = InkMath.PAGE_WIDTH * zoomScale
        val contentH = InkMath.PAGE_HEIGHT * zoomScale
        panX = if (contentW <= w - 16f) {
            (w - contentW) * 0.5f
        } else {
            val minX = w - contentW - 32f
            val maxX = 32f
            panX.coerceIn(minX, maxX)
        }
        val minY = min(24f, h - contentH - 48f)
        val maxY = 24f
        panY = panY.coerceIn(minY, maxY)
    }

    fun clearSelection() {
        selectedStrokeIds.clear()
        onLassoSelectionChanged?.invoke(emptySet())
        invalidate()
    }

    fun deleteSelectedStrokes() {
        if (selectedStrokeIds.isEmpty()) return
        val remaining = page.strokes.filterNot { it.id in selectedStrokeIds }
        selectedStrokeIds.clear()
        onLassoSelectionChanged?.invoke(emptySet())
        onStrokesReplaced?.invoke(remaining)
    }

    fun recolorSelectedStrokes(newColorArgb: Long) {
        if (selectedStrokeIds.isEmpty()) return
        val updated = page.strokes.map { s ->
            if (s.id in selectedStrokeIds) s.copy(colorArgb = newColorArgb) else s
        }
        onStrokesReplaced?.invoke(updated)
    }

    fun duplicateSelectedStrokes() {
        if (selectedStrokeIds.isEmpty()) return
        val copies = page.strokes.filter { it.id in selectedStrokeIds }.map {
            InkMath.translateStroke(it, 36f, 36f).copy(id = java.util.UUID.randomUUID().toString())
        }
        selectedStrokeIds = copies.map { it.id }.toMutableSet()
        onLassoSelectionChanged?.invoke(selectedStrokeIds.toSet())
        onStrokesReplaced?.invoke(page.strokes + copies)
    }

    fun scaleSelectedStrokes(factor: Float) {
        val (cx, cy) = selectionCenter() ?: return
        val updated = page.strokes.map { s ->
            if (s.id in selectedStrokeIds) InkMath.scaleStrokeAround(s, cx, cy, factor) else s
        }
        onStrokesReplaced?.invoke(updated)
    }

    fun rotateSelectedStrokes(angleDeg: Float) {
        val (cx, cy) = selectionCenter() ?: return
        val rad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val updated = page.strokes.map { s ->
            if (s.id in selectedStrokeIds) InkMath.rotateStrokeAround(s, cx, cy, rad) else s
        }
        onStrokesReplaced?.invoke(updated)
    }

    private fun selectionCenter(): Pair<Float, Float>? {
        val selected = page.strokes.filter { it.id in selectedStrokeIds }
        if (selected.isEmpty()) return null
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (s in selected) {
            for (p in s.points) {
                minX = min(minX, p.x)
                minY = min(minY, p.y)
                maxX = max(maxX, p.x)
                maxY = max(maxY, p.y)
            }
        }
        return ((minX + maxX) * 0.5f) to ((minY + maxY) * 0.5f)
    }

    private fun screenToPage(sx: Float, sy: Float): Pair<Float, Float> {
        return ((sx - panX) / zoomScale) to ((sy - panY) / zoomScale)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(0xFFE8E8ED.toInt())

        canvas.save()
        canvas.translate(panX, panY)
        canvas.scale(zoomScale, zoomScale)

        canvas.drawRoundRect(
            8f, 12f,
            InkMath.PAGE_WIDTH + 8f, InkMath.PAGE_HEIGHT + 12f,
            12f, 12f, shadowPaint
        )

        canvas.save()
        canvas.clipRect(0f, 0f, InkMath.PAGE_WIDTH, InkMath.PAGE_HEIGHT)

        val pdfBmp = pdfBackgroundBitmap
        if (pdfBmp != null) {
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(pdfBmp, null, RectF(0f, 0f, InkMath.PAGE_WIDTH, InkMath.PAGE_HEIGHT), null)
        } else {
            PaperTemplates.drawPaper(canvas, InkMath.PAGE_WIDTH, InkMath.PAGE_HEIGHT, page.template, page.paperColor)
        }

        for (img in page.images) {
            val bmp = imageBitmaps[img.fileName] ?: continue
            canvas.drawBitmap(bmp, null, RectF(img.x, img.y, img.x + img.width, img.y + img.height), null)
        }

        for (stroke in page.strokes) {
            StrokeRenderer.drawStroke(canvas, stroke)
            if (stroke.id in selectedStrokeIds) {
                drawSelectionHighlight(canvas, stroke)
            }
        }

        for (tb in page.textBoxes) {
            textBoxPaint.color = tb.colorArgb.toInt()
            textBoxPaint.textSize = tb.fontSize
            var ty = tb.y + tb.fontSize
            for (line in tb.text.split("\n")) {
                canvas.drawText(line, tb.x, ty, textBoxPaint)
                ty += tb.fontSize * 1.32f
            }
        }

        for (note in page.stickyNotes) {
            drawStickyNote(canvas, note)
        }

        if (isDrawing && activePoints.isNotEmpty()) {
            val preview = Stroke(
                points = activePoints,
                colorArgb = penColorArgb,
                baseWidth = penWidth,
                penStyle = penStyle,
                isShape = shapeSnappedForCurrentStroke
            )
            StrokeRenderer.drawStroke(canvas, preview)
        }

        val now = System.currentTimeMillis()
        for (trail in laserTrails) {
            val age = (now - trail.createdAt) / 1600f
            val alpha = (1f - age).coerceIn(0f, 1f)
            StrokeRenderer.drawStroke(canvas, trail.stroke, alpha)
        }

        if (lassoPoints.size >= 2) {
            val path = Path()
            path.moveTo(lassoPoints[0].x, lassoPoints[0].y)
            for (i in 1 until lassoPoints.size) path.lineTo(lassoPoints[i].x, lassoPoints[i].y)
            canvas.drawPath(path, lassoPaint)
        }

        if (showRuler) {
            drawRuler(canvas)
        }

        canvas.restore()
        canvas.restore()
    }

    private fun drawStickyNote(canvas: Canvas, note: StickyNote) {
        stickyPaint.color = 0x26000000
        canvas.drawRoundRect(note.x + 4f, note.y + 6f, note.x + note.width + 4f, note.y + note.height + 6f, 12f, 12f, stickyPaint)
        stickyPaint.color = note.colorArgb.toInt()
        canvas.drawRoundRect(note.x, note.y, note.x + note.width, note.y + note.height, 12f, 12f, stickyPaint)
        val lines = note.text.ifBlank { "Tap to edit note..." }.split("\n")
        var ty = note.y + 40f
        for (line in lines.take(7)) {
            canvas.drawText(line.take(26), note.x + 18f, ty, stickyTextPaint)
            ty += 28f
        }
    }

    private fun drawSelectionHighlight(canvas: Canvas, stroke: Stroke) {
        if (stroke.points.isEmpty()) return
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (p in stroke.points) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }
        canvas.drawRoundRect(minX - 10f, minY - 10f, maxX + 10f, maxY + 10f, 8f, 8f, selectionBoxPaint)
    }

    private fun drawRuler(canvas: Canvas) {
        canvas.save()
        canvas.translate(rulerCx, rulerCy)
        val deg = Math.toDegrees(rulerAngleRad.toDouble()).toFloat()
        canvas.rotate(deg)
        val halfLen = 460f
        val halfThick = 34f
        canvas.drawRoundRect(-halfLen, 0f, halfLen, halfThick * 2f, 10f, 10f, rulerPaint)
        var x = -halfLen + 20f
        var tick = 0
        while (x <= halfLen - 20f) {
            val tickLen = if (tick % 5 == 0) 20f else 10f
            canvas.drawLine(x, 0f, x, tickLen, rulerTickPaint)
            x += 20f
            tick++
        }
        // Rotation handles at both ends + angle badge in center
        canvas.drawCircle(-halfLen + 30f, halfThick, 14f, rulerHandlePaint)
        canvas.drawCircle(halfLen - 30f, halfThick, 14f, rulerHandlePaint)
        val normDeg = ((deg % 360f) + 360f) % 360f
        canvas.drawText("${normDeg.toInt()}°", 0f, halfThick + 10f, rulerAnglePaint)
        canvas.restore()
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (findStylusPointerIndex(event) >= 0) {
            lastStylusActivityMs = System.currentTimeMillis()
        }
        return super.onGenericMotionEvent(event)
    }

    private fun findStylusPointerIndex(event: MotionEvent): Int {
        for (i in 0 until event.pointerCount) {
            val t = event.getToolType(i)
            if (t == MotionEvent.TOOL_TYPE_STYLUS || t == MotionEvent.TOOL_TYPE_ERASER) return i
        }
        return -1
    }

    private fun isPalmOrRejectedTouch(event: MotionEvent): Boolean {
        if (System.currentTimeMillis() - lastStylusActivityMs < 550L) return true
        for (i in 0 until event.pointerCount) {
            if (event.getTouchMajor(i) > 115f) return true
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val stylusIdx = findStylusPointerIndex(event)
        val hasStylus = stylusIdx >= 0

        if (hasStylus) {
            lastStylusActivityMs = System.currentTimeMillis()
            isPanning = false
        } else {
            if (pageLocked) {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    val (px, py) = screenToPage(event.x, event.y)
                    page.stickyNotes.lastOrNull { px in it.x..(it.x + it.width) && py in it.y..(it.y + it.height) }?.let {
                        onStickyNoteTapped?.invoke(it)
                    }
                }
                return true
            }

            if (event.pointerCount >= 2) {
                if (!isPalmOrRejectedTouch(event) && toolMode != ToolMode.LASSO) {
                    isDrawing = false
                    isErasing = false
                    scaleDetector.onTouchEvent(event)
                    val focusX = (0 until event.pointerCount).map { event.getX(it) }.average().toFloat()
                    val focusY = (0 until event.pointerCount).map { event.getY(it) }.average().toFloat()
                    when (event.actionMasked) {
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            isPanning = true
                            prevFocusX = focusX
                            prevFocusY = focusY
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (isPanning && !scaleDetector.isInProgress) {
                                panX += focusX - prevFocusX
                                panY += focusY - prevFocusY
                                prevFocusX = focusX
                                prevFocusY = focusY
                                clampPan()
                                onViewportChanged?.invoke(zoomScale, panX, panY)
                                invalidate()
                            }
                        }
                        MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            isPanning = false
                        }
                    }
                }
                return true
            }

            if (stylusOnlyMode) {
                if (event.actionMasked == MotionEvent.ACTION_DOWN && !isPalmOrRejectedTouch(event)) {
                    val (px, py) = screenToPage(event.x, event.y)
                    page.stickyNotes.lastOrNull { px in it.x..(it.x + it.width) && py in it.y..(it.y + it.height) }?.let {
                        onStickyNoteTapped?.invoke(it)
                    }
                }
                isPanning = false
                return true
            }

            if (toolMode == ToolMode.PAN_ONLY) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        isPanning = true
                        prevFocusX = event.x
                        prevFocusY = event.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isPanning) {
                            panX += event.x - prevFocusX
                            panY += event.y - prevFocusY
                            prevFocusX = event.x
                            prevFocusY = event.y
                            clampPan()
                            onViewportChanged?.invoke(zoomScale, panX, panY)
                            invalidate()
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isPanning = false
                }
                return true
            }
        }

        val activeIdx = if (stylusIdx >= 0) stylusIdx else 0
        val rawX = event.getX(activeIdx)
        val rawY = event.getY(activeIdx)
        val toolType = event.getToolType(activeIdx)
        val isStylusEraser = toolType == MotionEvent.TOOL_TYPE_ERASER ||
            (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0

        if (toolMode == ToolMode.PAN_ONLY && !isStylusEraser) return true

        val (pageX, pageY) = screenToPage(rawX, rawY)

        // Ruler interactive move (center) or rotate (end handles)
        if (showRuler && (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN)) {
            val dx = pageX - rulerCx
            val dy = pageY - rulerCy
            val c = cos(-rulerAngleRad)
            val s = sin(-rulerAngleRad)
            val localX = dx * c - dy * s
            val localY = dx * s + dy * c
            if (localY in -20f..90f && localX in -480f..480f) {
                rulerMode = if (kotlin.math.abs(localX) > 350f) 2 else 1
                lastDragX = pageX
                lastDragY = pageY
                return true
            }
        }
        if (rulerMode != 0) {
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    if (rulerMode == 1) {
                        rulerCx += pageX - lastDragX
                        rulerCy += pageY - lastDragY
                    } else if (rulerMode == 2) {
                        val a1 = atan2(lastDragY - rulerCy, lastDragX - rulerCx)
                        val a2 = atan2(pageY - rulerCy, pageX - rulerCx)
                        rulerAngleRad += (a2 - a1)
                    }
                    lastDragX = pageX
                    lastDragY = pageY
                    invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> rulerMode = 0
            }
            return true
        }

        if (toolMode == ToolMode.TEXT_BOX && (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN)) {
            val hitTb = page.textBoxes.lastOrNull { pageX in it.x..(it.x + it.width) && pageY in it.y..(it.y + 120f) }
            if (hitTb != null) onTextBoxTapped?.invoke(hitTb) else onTextBoxAdded?.invoke(pageX, pageY)
            return true
        }

        if (toolMode == ToolMode.STICKY_NOTE) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val hitNote = page.stickyNotes.lastOrNull { pageX in it.x..(it.x + it.width) && pageY in it.y..(it.y + it.height) }
                    if (hitNote != null) {
                        draggingStickyId = hitNote.id
                        lastDragX = pageX
                        lastDragY = pageY
                    } else {
                        onStickyNoteAdded?.invoke(pageX, pageY)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    val id = draggingStickyId ?: return true
                    val dx = pageX - lastDragX
                    val dy = pageY - lastDragY
                    lastDragX = pageX
                    lastDragY = pageY
                    onStickyNoteMoved?.invoke(id, dx, dy)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                    val id = draggingStickyId
                    if (id != null && hypot(pageX - lastDragX, pageY - lastDragY) < 4f) {
                        page.stickyNotes.find { it.id == id }?.let { onStickyNoteTapped?.invoke(it) }
                    }
                    draggingStickyId = null
                }
            }
            return true
        }

        if (isStylusEraser || toolMode == ToolMode.ERASER_STROKE || toolMode == ToolMode.ERASER_PRECISION) {
            handleEraser(event, pageX, pageY, precision = toolMode == ToolMode.ERASER_PRECISION)
            return true
        }

        if (toolMode == ToolMode.LASSO) {
            handleLasso(event, pageX, pageY)
            return true
        }

        handlePenDrawing(event, activeIdx)
        return true
    }

    private fun handlePenDrawing(event: MotionEvent, pointerIndex: Int) {
        val action = event.actionMasked
        val actionIdx = event.actionIndex
        when {
            action == MotionEvent.ACTION_DOWN || (action == MotionEvent.ACTION_POINTER_DOWN && actionIdx == pointerIndex) -> {
                activePoints.clear()
                shapeSnappedForCurrentStroke = false
                isDrawing = true
                appendMotionPoint(event, pointerIndex, -1)
                lastMoveTime = System.currentTimeMillis()
                handler.removeCallbacks(shapeCheckRunnable)
                handler.postDelayed(shapeCheckRunnable, 450L)
                invalidate()
            }
            action == MotionEvent.ACTION_MOVE -> {
                if (!isDrawing) return
                for (h in 0 until event.historySize) appendMotionPoint(event, pointerIndex, h)
                appendMotionPoint(event, pointerIndex, -1)
                lastMoveTime = System.currentTimeMillis()
                handler.removeCallbacks(shapeCheckRunnable)
                handler.postDelayed(shapeCheckRunnable, 450L)
                invalidate()
            }
            action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL ||
                (action == MotionEvent.ACTION_POINTER_UP && actionIdx == pointerIndex) -> {
                handler.removeCallbacks(shapeCheckRunnable)
                if (!isDrawing) return
                isDrawing = false
                if (activePoints.isNotEmpty()) {
                    val committed = Stroke(
                        points = activePoints.toList(),
                        colorArgb = penColorArgb,
                        baseWidth = penWidth,
                        penStyle = penStyle,
                        isShape = shapeSnappedForCurrentStroke
                    )
                    if (penStyle == PenStyle.LASER) {
                        laserTrails.add(LaserTrail(committed, System.currentTimeMillis()))
                        handler.removeCallbacks(laserFadeRunnable)
                        handler.post(laserFadeRunnable)
                    } else {
                        onStrokeCommitted?.invoke(committed)
                    }
                }
                activePoints.clear()
                invalidate()
            }
        }
    }

    private fun appendMotionPoint(event: MotionEvent, pointerIndex: Int, historyIdx: Int) {
        val rawX = if (historyIdx >= 0) event.getHistoricalX(pointerIndex, historyIdx) else event.getX(pointerIndex)
        val rawY = if (historyIdx >= 0) event.getHistoricalY(pointerIndex, historyIdx) else event.getY(pointerIndex)
        val rawPressure = if (historyIdx >= 0) event.getHistoricalPressure(pointerIndex, historyIdx) else event.pressure
        val tilt = if (historyIdx >= 0) {
            event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, pointerIndex, historyIdx)
        } else {
            event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex)
        }
        val ts = if (historyIdx >= 0) event.getHistoricalEventTime(historyIdx) else event.eventTime

        var (px, py) = screenToPage(rawX, rawY)
        if (showRuler) {
            val snapped = InkMath.snapToRuler(px, py, rulerCx, rulerCy, rulerAngleRad)
            px = snapped.first
            py = snapped.second
        }

        val last = activePoints.lastOrNull()
        if (last != null && hypot(px - last.x, py - last.y) < 0.6f) return

        val normalizedPressure = if (rawPressure <= 0f) 0.5f else rawPressure.coerceIn(0.05f, 1.0f)
        activePoints.add(StrokePoint(px, py, normalizedPressure, tilt, ts))
    }

    private fun handleEraser(event: MotionEvent, pageX: Float, pageY: Float, precision: Boolean) {
        val eraseRadius = 22f
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                isErasing = true
                val current = page.strokes
                val updated = if (!precision) {
                    current.filterNot { InkMath.hitTestStroke(it, pageX, pageY, eraseRadius) }
                } else {
                    current.flatMap { InkMath.splitStrokeByEraser(it, pageX, pageY, eraseRadius) }
                }
                if (updated.size != current.size || updated != current) {
                    onStrokesReplaced?.invoke(updated)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isErasing = false
        }
    }

    private fun handleLasso(event: MotionEvent, pageX: Float, pageY: Float) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val tappedSelected = selectedStrokeIds.isNotEmpty() &&
                    page.strokes.any { it.id in selectedStrokeIds && InkMath.hitTestStroke(it, pageX, pageY, 28f) }
                if (tappedSelected) {
                    isDraggingSelection = true
                    lastDragX = pageX
                    lastDragY = pageY
                } else {
                    isDraggingSelection = false
                    selectedStrokeIds.clear()
                    onLassoSelectionChanged?.invoke(emptySet())
                    lassoPoints.clear()
                    lassoPoints.add(StrokePoint(pageX, pageY))
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingSelection) {
                    val dx = pageX - lastDragX
                    val dy = pageY - lastDragY
                    lastDragX = pageX
                    lastDragY = pageY
                    val moved = page.strokes.map { s ->
                        if (s.id in selectedStrokeIds) InkMath.translateStroke(s, dx, dy) else s
                    }
                    onStrokesReplaced?.invoke(moved)
                } else {
                    lassoPoints.add(StrokePoint(pageX, pageY))
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!isDraggingSelection && lassoPoints.size >= 3) {
                    val hits = page.strokes.filter { InkMath.isStrokeInLasso(it, lassoPoints) }.map { it.id }
                    selectedStrokeIds.addAll(hits)
                    onLassoSelectionChanged?.invoke(selectedStrokeIds.toSet())
                }
                isDraggingSelection = false
                lassoPoints.clear()
                invalidate()
            }
        }
    }
}
