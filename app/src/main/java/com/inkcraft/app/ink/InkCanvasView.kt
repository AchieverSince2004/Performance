package com.inkcraft.app.ink

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
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
import com.inkcraft.app.model.ToolMode
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Custom hardware-accelerated tablet stylus canvas.
 * Handles:
 * - True stylus pressure, tilt, historical event batching (120/240Hz)
 * - Stylus-only palm rejection + 2-finger pinch zoom & pan
 * - Stylus side-button & flip-eraser auto-detection
 * - Hold-to-snap shape recognition
 * - Straightedge ruler overlay & snapping
 * - Lasso selection & drag-to-move
 * - Ephemeral laser pointer trails
 */
class InkCanvasView(context: Context) : View(context) {

    // Page & content state
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

    // Active tool configuration
    var toolMode: ToolMode = ToolMode.PEN
    var penStyle: PenStyle = PenStyle.FOUNTAIN
    var penColorArgb: Long = 0xFF111827
    var penWidth: Float = 5.0f
    var stylusOnlyMode: Boolean = true
    var pageLocked: Boolean = false
    var shapeRecognitionEnabled: Boolean = true
    private var lastStylusActivityMs: Long = 0L
    var showRuler: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    // Viewport transform (page coordinates -> screen coordinates)
    var zoomScale: Float = 1f
        private set
    var panX: Float = 0f
        private set
    var panY: Float = 0f
        private set
    private var hasInitializedFit = false

    // Callbacks to ViewModel / Compose state
    var onStrokeCommitted: ((Stroke) -> Unit)? = null
    var onStrokesReplaced: ((List<Stroke>) -> Unit)? = null
    var onStickyNoteTapped: ((StickyNote) -> Unit)? = null
    var onStickyNoteAdded: ((Float, Float) -> Unit)? = null
    var onViewportChanged: ((Float, Float, Float) -> Unit)? = null
    var onLassoSelectionChanged: ((Set<String>) -> Unit)? = null

    // Active drawing state
    private val activePoints = mutableListOf<StrokePoint>()
    private var isDrawing = false
    private var isErasing = false

    // Lasso state
    private val lassoPoints = mutableListOf<StrokePoint>()
    private var selectedStrokeIds = mutableSetOf<String>()
    private var isDraggingSelection = false
    private var lastDragX = 0f
    private var lastDragY = 0f

    // Laser pointer ephemeral strokes
    private data class LaserTrail(val stroke: Stroke, val createdAt: Long)
    private val laserTrails = mutableListOf<LaserTrail>()

    // Ruler state in page coordinates
    private var rulerCx = InkMath.PAGE_WIDTH * 0.5f
    private var rulerCy = InkMath.PAGE_HEIGHT * 0.35f
    private var rulerAngleRad = 0f
    private var isDraggingRuler = false

    // Hold-to-snap timer
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

    // Two-finger pan & pinch zoom
    private var isPanning = false
    private var prevFocusX = 0f
    private var prevFocusY = 0f

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val oldScale = zoomScale
                val newScale = (zoomScale * detector.scaleFactor).coerceIn(0.35f, 4.5f)
                val factor = newScale / oldScale
                panX = detector.focusX - (detector.focusX - panX) * factor
                panY = detector.focusY - (detector.focusY - panY) * factor
                zoomScale = newScale
                onViewportChanged?.invoke(zoomScale, panX, panY)
                invalidate()
                return true
            }
        }
    )

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x22000000
        style = Paint.Style.FILL
    }

    private val lassoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3B82F6.toInt()
        strokeWidth = 3f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
    }

    private val selectionBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x333B82F6
        style = Paint.Style.FILL
    }

    private val stickyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stickyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1F2937.toInt()
        textSize = 24f
    }

    private val rulerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCC1E293B.toInt()
        style = Paint.Style.FILL
    }
    private val rulerTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF8FAFC.toInt()
        strokeWidth = 2f
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!hasInitializedFit && w > 0 && h > 0) {
            fitPageToWidth(w, h)
            hasInitializedFit = true
        }
    }

    fun fitPageToWidth(w: Int = width, h: Int = height) {
        if (w <= 0 || h <= 0) return
        val padding = 48f
        val scaleX = (w - padding * 2) / InkMath.PAGE_WIDTH
        val scaleY = (h - padding * 2) / InkMath.PAGE_HEIGHT
        zoomScale = minOf(scaleX, scaleY * 1.15f).coerceIn(0.4f, 2.0f)
        panX = (w - InkMath.PAGE_WIDTH * zoomScale) * 0.5f
        panY = 32f
        onViewportChanged?.invoke(zoomScale, panX, panY)
        invalidate()
    }

    fun setViewport(scale: Float, px: Float, py: Float) {
        zoomScale = scale.coerceIn(0.35f, 4.5f)
        panX = px
        panY = py
        invalidate()
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

    private fun screenToPage(sx: Float, sy: Float): Pair<Float, Float> {
        return ((sx - panX) / zoomScale) to ((sy - panY) / zoomScale)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(0xFFE8E8ED.toInt())

        canvas.save()
        canvas.translate(panX, panY)
        canvas.scale(zoomScale, zoomScale)

        // Page drop shadow
        canvas.drawRoundRect(
            10f, 14f,
            InkMath.PAGE_WIDTH + 10f, InkMath.PAGE_HEIGHT + 14f,
            12f, 12f, shadowPaint
        )

        // Clip to page bounds
        canvas.save()
        canvas.clipRect(0f, 0f, InkMath.PAGE_WIDTH, InkMath.PAGE_HEIGHT)

        // 1. Paper template or PDF background
        val pdfBmp = pdfBackgroundBitmap
        if (pdfBmp != null) {
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(pdfBmp, null, RectF(0f, 0f, InkMath.PAGE_WIDTH, InkMath.PAGE_HEIGHT), null)
        } else {
            PaperTemplates.drawPaper(canvas, InkMath.PAGE_WIDTH, InkMath.PAGE_HEIGHT, page.template, page.paperColor)
        }

        // 2. Embedded images
        for (img in page.images) {
            drawCanvasImage(canvas, img)
        }

        // 3. Committed strokes
        for (stroke in page.strokes) {
            StrokeRenderer.drawStroke(canvas, stroke)
            if (stroke.id in selectedStrokeIds) {
                drawSelectionHighlight(canvas, stroke)
            }
        }

        // 4. Sticky notes
        for (note in page.stickyNotes) {
            drawStickyNote(canvas, note)
        }

        // 5. Active in-progress stroke
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

        // 6. Ephemeral laser trails
        val now = System.currentTimeMillis()
        for (trail in laserTrails) {
            val age = (now - trail.createdAt) / 1600f
            val alpha = (1f - age).coerceIn(0f, 1f)
            StrokeRenderer.drawStroke(canvas, trail.stroke, alpha)
        }

        // 7. Lasso path
        if (lassoPoints.size >= 2) {
            val path = Path()
            path.moveTo(lassoPoints[0].x, lassoPoints[0].y)
            for (i in 1 until lassoPoints.size) {
                path.lineTo(lassoPoints[i].x, lassoPoints[i].y)
            }
            canvas.drawPath(path, lassoPaint)
        }

        // 8. Straightedge ruler overlay
        if (showRuler) {
            drawRuler(canvas)
        }

        canvas.restore()
        canvas.restore()
    }

    private fun drawCanvasImage(canvas: Canvas, img: CanvasImage) {
        val bmp = imageBitmaps[img.fileName] ?: return
        canvas.drawBitmap(bmp, null, RectF(img.x, img.y, img.x + img.width, img.y + img.height), null)
    }

    private fun drawStickyNote(canvas: Canvas, note: StickyNote) {
        stickyPaint.color = 0x28000000
        canvas.drawRoundRect(note.x + 4f, note.y + 6f, note.x + note.width + 4f, note.y + note.height + 6f, 12f, 12f, stickyPaint)
        stickyPaint.color = note.colorArgb.toInt()
        canvas.drawRoundRect(note.x, note.y, note.x + note.width, note.y + note.height, 12f, 12f, stickyPaint)
        val lines = note.text.ifBlank { "Tap to edit note..." }.split("\n")
        var ty = note.y + 40f
        for (line in lines.take(6)) {
            canvas.drawText(line.take(24), note.x + 18f, ty, stickyTextPaint)
            ty += 30f
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
        canvas.rotate(Math.toDegrees(rulerAngleRad.toDouble()).toFloat())
        val halfLen = 460f
        val halfThick = 34f
        canvas.drawRoundRect(-halfLen, 0f, halfLen, halfThick * 2f, 8f, 8f, rulerPaint)
        var x = -halfLen + 20f
        var tick = 0
        while (x <= halfLen - 20f) {
            val tickLen = if (tick % 5 == 0) 20f else 10f
            canvas.drawLine(x, 0f, x, tickLen, rulerTickPaint)
            x += 20f
            tick++
        }
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
            if (t == MotionEvent.TOOL_TYPE_STYLUS || t == MotionEvent.TOOL_TYPE_ERASER) {
                return i
            }
        }
        return -1
    }

    private fun isPalmOrRejectedTouch(event: MotionEvent): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastStylusActivityMs < 550L) return true
        for (i in 0 until event.pointerCount) {
            val major = event.getTouchMajor(i)
            if (major > 115f) return true
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
            // No stylus in this event: handle finger/palm rejection & deliberate 2-finger zoom/pan
            if (pageLocked) {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    val (px, py) = screenToPage(event.x, event.y)
                    val hitNote = page.stickyNotes.lastOrNull {
                        px in it.x..(it.x + it.width) && py in it.y..(it.y + it.height)
                    }
                    if (hitNote != null) onStickyNoteTapped?.invoke(hitNote)
                }
                return true
            }

            if (event.pointerCount >= 2) {
                // Only allow 2-finger zoom/pan if no stylus was recently active and neither contact is a palm
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

            // Single-finger touch when Stylus-Only mode is enabled: NEVER pan the page!
            // This prevents resting or sliding your palm from moving the canvas.
            if (stylusOnlyMode) {
                if (event.actionMasked == MotionEvent.ACTION_DOWN && !isPalmOrRejectedTouch(event)) {
                    val (px, py) = screenToPage(event.x, event.y)
                    val hitNote = page.stickyNotes.lastOrNull {
                        px in it.x..(it.x + it.width) && py in it.y..(it.y + it.height)
                    }
                    if (hitNote != null) {
                        onStickyNoteTapped?.invoke(hitNote)
                    }
                }
                isPanning = false
                return true
            }

            // Explicit PAN_ONLY mode with single finger
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

        if (toolMode == ToolMode.PAN_ONLY && !isStylusEraser) {
            return true
        }

        val (pageX, pageY) = screenToPage(rawX, rawY)

        // Dragging ruler if touched near its center body
        if (showRuler && (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN)) {
            val distToRuler = hypot(pageX - rulerCx, pageY - rulerCy)
            if (distToRuler < 70f) {
                isDraggingRuler = true
                lastDragX = pageX
                lastDragY = pageY
                return true
            }
        }
        if (isDraggingRuler) {
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    rulerCx += pageX - lastDragX
                    rulerCy += pageY - lastDragY
                    lastDragX = pageX
                    lastDragY = pageY
                    invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> isDraggingRuler = false
            }
            return true
        }

        // Sticky note placement or tap
        if (toolMode == ToolMode.STICKY_NOTE && (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN)) {
            val hitNote = page.stickyNotes.lastOrNull {
                pageX in it.x..(it.x + it.width) && pageY in it.y..(it.y + it.height)
            }
            if (hitNote != null) {
                onStickyNoteTapped?.invoke(hitNote)
            } else {
                onStickyNoteAdded?.invoke(pageX, pageY)
            }
            return true
        }

        // Eraser mode (either selected tool OR stylus flip/side-button)
        if (isStylusEraser || toolMode == ToolMode.ERASER_STROKE || toolMode == ToolMode.ERASER_PRECISION) {
            handleEraser(event, pageX, pageY, precision = toolMode == ToolMode.ERASER_PRECISION)
            return true
        }

        // Lasso selection
        if (toolMode == ToolMode.LASSO) {
            handleLasso(event, pageX, pageY)
            return true
        }

        // Standard Pen / Highlighter / Laser drawing
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
                for (h in 0 until event.historySize) {
                    appendMotionPoint(event, pointerIndex, h)
                }
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
        val rawPressure = if (historyIdx >= 0) event.getHistoricalPressure(pointerIndex, historyIdx) else event.getPressure(pointerIndex)
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
        if (last != null && hypot(px - last.x, py - last.y) < 1.5f) return

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
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isErasing = false
            }
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
