package com.inkcraft.app.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.inkcraft.app.ink.InkCanvasView
import com.inkcraft.app.ink.InkMath
import com.inkcraft.app.model.AudioMemo
import com.inkcraft.app.model.NotePage
import com.inkcraft.app.model.Notebook
import com.inkcraft.app.model.PaperColor
import com.inkcraft.app.model.PaperTemplate
import com.inkcraft.app.model.PenPreset
import com.inkcraft.app.model.PenStyle
import com.inkcraft.app.model.StickyNote
import com.inkcraft.app.model.Stroke
import com.inkcraft.app.model.ToolMode
import kotlinx.coroutines.delay

private val QUICK_COLORS = listOf(
    0xFF111827, // Ink Black
    0xFF2563EB, // Royal Blue
    0xFFDC2626, // Crimson
    0xFF059669, // Emerald
    0xFF7C3AED, // Violet
    0xFFD97706, // Amber
    0xFFEC4899, // Rose
    0xFFFFFFFF, // Pure White (for dark paper)
    0xFFFACC15  // Highlighter Yellow
)

enum class ToolbarDock { TOP, LEFT, RIGHT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    notebook: Notebook,
    presets: List<PenPreset>,
    pdfPageBitmaps: Map<String, Bitmap>,
    imageBitmaps: Map<String, Bitmap>,
    isRecordingAudio: Boolean,
    isPlayingAudio: Boolean,
    onBack: () -> Unit,
    onUpdateNotebook: (Notebook) -> Unit,
    onExportPdf: () -> Unit,
    onImportImage: (Int) -> Unit,
    onImportPdfIntoNotebook: () -> Unit,
    onToggleRecordAudio: () -> Unit,
    onPlayAudio: (AudioMemo) -> Unit,
    onStopAudio: () -> Unit
) {
    var currentPageIdx by remember(notebook.id) { mutableIntStateOf(0) }
    val safePageIdx = currentPageIdx.coerceIn(0, (notebook.pages.size - 1).coerceAtLeast(0))
    val currentPage = notebook.pages.getOrNull(safePageIdx) ?: NotePage()

    // Tool states
    var toolMode by remember { mutableStateOf(ToolMode.PEN) }
    var penStyle by remember { mutableStateOf(PenStyle.FOUNTAIN) }
    var penColorArgb by remember { mutableLongStateOf(0xFF111827) }
    var penWidth by remember { mutableFloatStateOf(5.0f) }
    var stylusOnlyMode by remember { mutableStateOf(true) }
    var shapeRecognition by remember { mutableStateOf(true) }
    var showRuler by remember { mutableStateOf(false) }
    var showZoomBox by remember { mutableStateOf(false) }
    var showPageSidebar by remember { mutableStateOf(false) }
    var showPenPopover by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var toolbarDock by remember { mutableStateOf(ToolbarDock.TOP) }

    // Undo / Redo stacks per page
    var undoStacks by remember(notebook.id) { mutableStateOf<Map<String, List<List<Stroke>>>>(emptyMap()) }
    var redoStacks by remember(notebook.id) { mutableStateOf<Map<String, List<List<Stroke>>>>(emptyMap()) }

    // Lasso selection
    var selectedStrokeIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Sticky note editing
    var editingStickyNote by remember { mutableStateOf<StickyNote?>(null) }

    // Pomodoro Focus Timer
    var focusRunning by remember { mutableStateOf(false) }
    var focusSecondsLeft by remember { mutableIntStateOf(25 * 60) }
    LaunchedEffect(focusRunning) {
        while (focusRunning && focusSecondsLeft > 0) {
            delay(1000L)
            focusSecondsLeft--
        }
        if (focusSecondsLeft == 0) focusRunning = false
    }

    // Canvas reference for imperative actions (fit, zoom box, lasso delete/recolor)
    var canvasViewRef by remember { mutableStateOf<InkCanvasView?>(null) }
    var zoomBoxViewRef by remember { mutableStateOf<InkCanvasView?>(null) }
    var zoomBoxPageX by remember { mutableFloatStateOf(120f) }
    var zoomBoxPageY by remember { mutableFloatStateOf(240f) }

    var moreMenuExpanded by remember { mutableStateOf(false) }

    fun mutateCurrentPage(transform: (NotePage) -> NotePage) {
        val updatedPages = notebook.pages.mapIndexed { idx, p ->
            if (idx == safePageIdx) transform(p) else p
        }
        onUpdateNotebook(notebook.copy(pages = updatedPages, updatedAt = System.currentTimeMillis()))
    }

    fun pushUndoAndReplaceStrokes(newStrokes: List<Stroke>) {
        val pageId = currentPage.id
        val prev = currentPage.strokes
        val stack = undoStacks[pageId].orEmpty()
        undoStacks = undoStacks + (pageId to (stack + listOf(prev)).takeLast(40))
        redoStacks = redoStacks - pageId
        mutateCurrentPage { it.copy(strokes = newStrokes) }
    }

    fun undo() {
        val pageId = currentPage.id
        val stack = undoStacks[pageId].orEmpty()
        if (stack.isEmpty()) return
        val restored = stack.last()
        undoStacks = undoStacks + (pageId to stack.dropLast(1))
        val rStack = redoStacks[pageId].orEmpty()
        redoStacks = redoStacks + (pageId to (rStack + listOf(currentPage.strokes)))
        mutateCurrentPage { it.copy(strokes = restored) }
    }

    fun redo() {
        val pageId = currentPage.id
        val rStack = redoStacks[pageId].orEmpty()
        if (rStack.isEmpty()) return
        val restored = rStack.last()
        redoStacks = redoStacks + (pageId to rStack.dropLast(1))
        val uStack = undoStacks[pageId].orEmpty()
        undoStacks = undoStacks + (pageId to (uStack + listOf(currentPage.strokes)))
        mutateCurrentPage { it.copy(strokes = restored) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Shelf")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(notebook.title, fontWeight = FontWeight.Bold, maxLines = 1)
                        Spacer(Modifier.width(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Page ${safePageIdx + 1} / ${notebook.pages.size}",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Undo / Redo
                    IconButton(onClick = { undo() }, enabled = undoStacks[currentPage.id]?.isNotEmpty() == true) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { redo() }, enabled = redoStacks[currentPage.id]?.isNotEmpty() == true) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }

                    // Stylus-Only Palm Rejection Toggle
                    FilterChip(
                        selected = stylusOnlyMode,
                        onClick = { stylusOnlyMode = !stylusOnlyMode },
                        label = { Text(if (stylusOnlyMode) "Stylus Only" else "Touch + Pen", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Shape Snap Toggle
                    FilterChip(
                        selected = shapeRecognition,
                        onClick = { shapeRecognition = !shapeRecognition },
                        label = { Text("Shapes", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Ruler Toggle
                    IconButton(onClick = { showRuler = !showRuler }) {
                        Icon(
                            Icons.Default.Straighten,
                            contentDescription = "Ruler",
                            tint = if (showRuler) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Zoom Box Toggle
                    IconButton(onClick = { showZoomBox = !showZoomBox }) {
                        Icon(
                            Icons.Default.ZoomIn,
                            contentDescription = "Zoom Box",
                            tint = if (showZoomBox) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Audio Recording
                    IconButton(onClick = onToggleRecordAudio) {
                        Icon(
                            if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Record Audio",
                            tint = if (isRecordingAudio) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Pomodoro Focus Timer Pill
                    Surface(
                        color = if (focusRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clickable { focusRunning = !focusRunning }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            val mins = focusSecondsLeft / 60
                            val secs = focusSecondsLeft % 60
                            Text("%02d:%02d".format(mins, secs), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Pages Sidebar
                    IconButton(onClick = { showPageSidebar = !showPageSidebar }) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = "Pages",
                            tint = if (showPageSidebar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(onClick = { moreMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = moreMenuExpanded, onDismissRequest = { moreMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Paper Template & Color") },
                                leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                                onClick = { moreMenuExpanded = false; showTemplateDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Insert Image") },
                                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                                onClick = { moreMenuExpanded = false; onImportImage(safePageIdx) }
                            )
                            DropdownMenuItem(
                                text = { Text("Append PDF Pages") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                onClick = { moreMenuExpanded = false; onImportPdfIntoNotebook() }
                            )
                            DropdownMenuItem(
                                text = { Text("Fit Page to Screen") },
                                leadingIcon = { Icon(Icons.Default.FitScreen, contentDescription = null) },
                                onClick = { moreMenuExpanded = false; canvasViewRef?.fitPageToWidth() }
                            )
                            DropdownMenuItem(
                                text = {
                                    val nextDock = when (toolbarDock) {
                                        ToolbarDock.TOP -> "Dock Toolbar Left"
                                        ToolbarDock.LEFT -> "Dock Toolbar Right"
                                        ToolbarDock.RIGHT -> "Dock Toolbar Top"
                                    }
                                    Text(nextDock)
                                },
                                onClick = {
                                    moreMenuExpanded = false
                                    toolbarDock = when (toolbarDock) {
                                        ToolbarDock.TOP -> ToolbarDock.LEFT
                                        ToolbarDock.LEFT -> ToolbarDock.RIGHT
                                        ToolbarDock.RIGHT -> ToolbarDock.TOP
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Notebook PDF") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = { moreMenuExpanded = false; onExportPdf() }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Optional Left Page Sidebar
                AnimatedVisibility(visible = showPageSidebar) {
                    PageThumbnailsSidebar(
                        pages = notebook.pages,
                        currentIdx = safePageIdx,
                        audioMemos = notebook.audioMemos,
                        isPlayingAudio = isPlayingAudio,
                        onSelectPage = { currentPageIdx = it },
                        onAddPage = {
                            val newPage = NotePage(
                                pageIndex = notebook.pages.size,
                                template = currentPage.template,
                                paperColor = currentPage.paperColor
                            )
                            val updated = notebook.pages + newPage
                            onUpdateNotebook(notebook.copy(pages = updated))
                            currentPageIdx = updated.lastIndex
                        },
                        onDuplicatePage = { idx ->
                            val src = notebook.pages[idx]
                            val copy = src.copy(id = java.util.UUID.randomUUID().toString())
                            val updated = notebook.pages.toMutableList().apply { add(idx + 1, copy) }
                            onUpdateNotebook(notebook.copy(pages = updated))
                            currentPageIdx = idx + 1
                        },
                        onToggleBookmark = { idx ->
                            val updated = notebook.pages.mapIndexed { i, p ->
                                if (i == idx) p.copy(isBookmarked = !p.isBookmarked) else p
                            }
                            onUpdateNotebook(notebook.copy(pages = updated))
                        },
                        onDeletePage = { idx ->
                            if (notebook.pages.size > 1) {
                                val updated = notebook.pages.filterIndexed { i, _ -> i != idx }
                                onUpdateNotebook(notebook.copy(pages = updated))
                                if (currentPageIdx >= updated.size) currentPageIdx = updated.lastIndex
                            }
                        },
                        onPlayAudio = onPlayAudio,
                        onStopAudio = onStopAudio
                    )
                }

                // Main Canvas Area
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val pdfKey = if (currentPage.pdfFileName != null && currentPage.pdfPageIndex != null) {
                        "${currentPage.pdfFileName}:${currentPage.pdfPageIndex}"
                    } else null

                    AndroidView(
                        factory = { ctx ->
                            InkCanvasView(ctx).also { view ->
                                canvasViewRef = view
                            }
                        },
                        update = { view ->
                            view.page = currentPage
                            view.pdfBackgroundBitmap = pdfKey?.let { pdfPageBitmaps[it] }
                            view.imageBitmaps = imageBitmaps
                            view.toolMode = toolMode
                            view.penStyle = penStyle
                            view.penColorArgb = penColorArgb
                            view.penWidth = penWidth
                            view.stylusOnlyMode = stylusOnlyMode
                            view.shapeRecognitionEnabled = shapeRecognition
                            view.showRuler = showRuler
                            view.onStrokeCommitted = { stroke ->
                                pushUndoAndReplaceStrokes(currentPage.strokes + stroke)
                            }
                            view.onStrokesReplaced = { newStrokes ->
                                pushUndoAndReplaceStrokes(newStrokes)
                            }
                            view.onStickyNoteTapped = { note ->
                                editingStickyNote = note
                            }
                            view.onStickyNoteAdded = { x, y ->
                                val newNote = StickyNote(x = x - 120f, y = y - 100f)
                                mutateCurrentPage { it.copy(stickyNotes = it.stickyNotes + newNote) }
                                editingStickyNote = newNote
                            }
                            view.onLassoSelectionChanged = { ids ->
                                selectedStrokeIds = ids
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Docked Stylus Toolbar
                    val toolbarModifier = when (toolbarDock) {
                        ToolbarDock.TOP -> Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                        ToolbarDock.LEFT -> Modifier.align(Alignment.CenterStart).padding(start = 12.dp)
                        ToolbarDock.RIGHT -> Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
                    }

                    StylusToolbar(
                        vertical = toolbarDock != ToolbarDock.TOP,
                        toolMode = toolMode,
                        penStyle = penStyle,
                        penColorArgb = penColorArgb,
                        presets = presets,
                        onSelectTool = {
                            toolMode = it
                            if (it != ToolMode.LASSO) canvasViewRef?.clearSelection()
                        },
                        onSelectPenStyle = {
                            penStyle = it
                            penWidth = it.defaultWidth
                            toolMode = ToolMode.PEN
                            showPenPopover = true
                        },
                        onSelectPreset = { preset ->
                            penStyle = preset.penStyle
                            penColorArgb = preset.colorArgb
                            penWidth = preset.width
                            toolMode = ToolMode.PEN
                        },
                        onTogglePenPopover = { showPenPopover = !showPenPopover },
                        modifier = toolbarModifier
                    )

                    // Pen Color & Width Popover
                    if (showPenPopover) {
                        Surface(
                            shadowElevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 76.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp).width(340.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${penStyle.displayName} Settings", fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { showPenPopover = false }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Close")
                                    }
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(QUICK_COLORS) { col ->
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color(col))
                                                .border(
                                                    width = if (col == penColorArgb) 3.dp else 1.dp,
                                                    color = if (col == penColorArgb) MaterialTheme.colorScheme.primary else Color.Gray,
                                                    shape = CircleShape
                                                )
                                                .clickable { penColorArgb = col }
                                        )
                                    }
                                }
                                Text("Thickness: ${penWidth.toInt()} px", fontSize = 12.sp)
                                Slider(
                                    value = penWidth,
                                    onValueChange = { penWidth = it },
                                    valueRange = 1.5f..36f
                                )
                            }
                        }
                    }

                    // Lasso Floating Action Bar
                    if (selectedStrokeIds.isNotEmpty()) {
                        Surface(
                            shadowElevation = 8.dp,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("${selectedStrokeIds.size} selected • Drag to move", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                QUICK_COLORS.take(4).forEach { col ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(col))
                                            .border(1.dp, Color.Gray, CircleShape)
                                            .clickable { canvasViewRef?.recolorSelectedStrokes(col) }
                                    )
                                }
                                IconButton(onClick = { canvasViewRef?.deleteSelectedStrokes() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Selection", tint = MaterialTheme.colorScheme.error)
                                }
                                IconButton(onClick = { canvasViewRef?.clearSelection() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                                }
                            }
                        }
                    }

                    // Noteshelf-style Zoom Box (Close-Up Writing Window)
                    if (showZoomBox) {
                        Surface(
                            shadowElevation = 12.dp,
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(250.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Zoom Box (2.2x)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row {
                                        IconButton(onClick = { zoomBoxPageX = (zoomBoxPageX - 220f).coerceAtLeast(0f) }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                                        }
                                        IconButton(onClick = { zoomBoxPageX = (zoomBoxPageX + 220f).coerceAtMost(InkMath.PAGE_WIDTH - 360f) }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Advance")
                                        }
                                        IconButton(onClick = {
                                            zoomBoxPageX = 80f
                                            zoomBoxPageY = (zoomBoxPageY + 120f).coerceAtMost(InkMath.PAGE_HEIGHT - 220f)
                                        }) {
                                            Icon(Icons.Default.SubdirectoryArrowLeft, contentDescription = "Next Line")
                                        }
                                        IconButton(onClick = { showZoomBox = false }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close Zoom Box")
                                        }
                                    }
                                }
                                AndroidView(
                                    factory = { ctx ->
                                        InkCanvasView(ctx).also { zoomBoxViewRef = it }
                                    },
                                    update = { zv ->
                                        zv.page = currentPage
                                        zv.pdfBackgroundBitmap = pdfKey?.let { pdfPageBitmaps[it] }
                                        zv.imageBitmaps = imageBitmaps
                                        zv.toolMode = toolMode
                                        zv.penStyle = penStyle
                                        zv.penColorArgb = penColorArgb
                                        zv.penWidth = penWidth
                                        zv.stylusOnlyMode = stylusOnlyMode
                                        zv.shapeRecognitionEnabled = shapeRecognition
                                        zv.setViewport(2.2f, -zoomBoxPageX * 2.2f, -zoomBoxPageY * 2.2f)
                                        zv.onStrokeCommitted = { stroke ->
                                            pushUndoAndReplaceStrokes(currentPage.strokes + stroke)
                                        }
                                        zv.onStrokesReplaced = { newStrokes ->
                                            pushUndoAndReplaceStrokes(newStrokes)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Sticky Note Edit Dialog
    editingStickyNote?.let { note ->
        var text by remember(note.id) { mutableStateOf(note.text) }
        AlertDialog(
            onDismissRequest = { editingStickyNote = null },
            title = { Text("Sticky Note") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Note Content") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    mutateCurrentPage { p ->
                        p.copy(stickyNotes = p.stickyNotes.map { if (it.id == note.id) it.copy(text = text) else it })
                    }
                    editingStickyNote = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mutateCurrentPage { p ->
                        p.copy(stickyNotes = p.stickyNotes.filterNot { it.id == note.id })
                    }
                    editingStickyNote = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        )
    }

    // Paper Template & Color Dialog
    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("Page Template & Paper") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Template", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PaperTemplate.entries) { tpl ->
                            FilterChip(
                                selected = currentPage.template == tpl,
                                onClick = { mutateCurrentPage { it.copy(template = tpl) } },
                                label = { Text(tpl.displayName) }
                            )
                        }
                    }
                    Text("Paper Color", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PaperColor.entries.forEach { pc ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(pc.bgArgb))
                                    .border(
                                        width = if (currentPage.paperColor == pc) 3.dp else 1.dp,
                                        color = if (currentPage.paperColor == pc) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { mutateCurrentPage { it.copy(paperColor = pc) } }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTemplateDialog = false }) { Text("Done") }
            }
        )
    }
}

@Composable
private fun StylusToolbar(
    vertical: Boolean,
    toolMode: ToolMode,
    penStyle: PenStyle,
    penColorArgb: Long,
    presets: List<PenPreset>,
    onSelectTool: (ToolMode) -> Unit,
    onSelectPenStyle: (PenStyle) -> Unit,
    onSelectPreset: (PenPreset) -> Unit,
    onTogglePenPopover: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        modifier = modifier
    ) {
        val content: @Composable () -> Unit = {
            ToolBtn(Icons.Default.Draw, "Fountain", toolMode == ToolMode.PEN && penStyle == PenStyle.FOUNTAIN) {
                onSelectPenStyle(PenStyle.FOUNTAIN)
            }
            ToolBtn(Icons.Default.Edit, "Ballpoint", toolMode == ToolMode.PEN && penStyle == PenStyle.BALLPOINT) {
                onSelectPenStyle(PenStyle.BALLPOINT)
            }
            ToolBtn(Icons.Default.Brush, "Brush", toolMode == ToolMode.PEN && penStyle == PenStyle.BRUSH) {
                onSelectPenStyle(PenStyle.BRUSH)
            }
            ToolBtn(Icons.Default.Highlight, "Highlighter", toolMode == ToolMode.PEN && penStyle == PenStyle.HIGHLIGHTER) {
                onSelectPenStyle(PenStyle.HIGHLIGHTER)
            }
            ToolBtn(Icons.Default.CenterFocusStrong, "Laser", toolMode == ToolMode.PEN && penStyle == PenStyle.LASER) {
                onSelectPenStyle(PenStyle.LASER)
            }
            // Active Color Dot
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(penColorArgb))
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable(onClick = onTogglePenPopover)
            )
            // 1-Tap Pinned Presets
            presets.take(3).forEach { preset ->
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(preset.colorArgb))
                        .border(1.dp, Color.White, CircleShape)
                        .clickable { onSelectPreset(preset) }
                )
            }
            ToolBtn(Icons.Default.CleaningServices, "Stroke Eraser", toolMode == ToolMode.ERASER_STROKE) {
                onSelectTool(ToolMode.ERASER_STROKE)
            }
            ToolBtn(Icons.Default.Gesture, "Lasso", toolMode == ToolMode.LASSO) {
                onSelectTool(ToolMode.LASSO)
            }
            ToolBtn(Icons.Default.StickyNote2, "Sticky Note", toolMode == ToolMode.STICKY_NOTE) {
                onSelectTool(ToolMode.STICKY_NOTE)
            }
            ToolBtn(Icons.Default.PanTool, "Pan", toolMode == ToolMode.PAN_ONLY) {
                onSelectTool(ToolMode.PAN_ONLY)
            }
        }

        if (vertical) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
            ) { content() }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) { content() }
        }
    }
}

@Composable
private fun ToolBtn(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = CircleShape,
        modifier = Modifier.padding(2.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PageThumbnailsSidebar(
    pages: List<NotePage>,
    currentIdx: Int,
    audioMemos: List<AudioMemo>,
    isPlayingAudio: Boolean,
    onSelectPage: (Int) -> Unit,
    onAddPage: () -> Unit,
    onDuplicatePage: (Int) -> Unit,
    onToggleBookmark: (Int) -> Unit,
    onDeletePage: (Int) -> Unit,
    onPlayAudio: (AudioMemo) -> Unit,
    onStopAudio: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier
            .width(210.dp)
            .fillMaxHeight()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pages", fontWeight = FontWeight.Bold)
                IconButton(onClick = onAddPage, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add Page")
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(pages) { idx, page ->
                    val selected = idx == currentIdx
                    Surface(
                        color = Color(page.paperColor.bgArgb),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectPage(idx) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            Text(
                                "Page ${idx + 1}\n${page.template.displayName}",
                                fontSize = 12.sp,
                                color = if (page.paperColor.isDark) Color.White else Color.Black,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                            Row(modifier = Modifier.align(Alignment.BottomEnd)) {
                                IconButton(onClick = { onToggleBookmark(idx) }, modifier = Modifier.size(26.dp)) {
                                    Icon(
                                        if (page.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (page.isBookmarked) Color(0xFFF59E0B) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(onClick = { onDuplicatePage(idx) }, modifier = Modifier.size(26.dp)) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(15.dp))
                                }
                                if (pages.size > 1) {
                                    IconButton(onClick = { onDeletePage(idx) }, modifier = Modifier.size(26.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (audioMemos.isNotEmpty()) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Audio Memos", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    audioMemos.forEachIndexed { i, memo ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { if (isPlayingAudio) onStopAudio() else onPlayAudio(memo) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    if (isPlayingAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Memo ${i + 1} (${memo.durationMs / 1000}s)", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
