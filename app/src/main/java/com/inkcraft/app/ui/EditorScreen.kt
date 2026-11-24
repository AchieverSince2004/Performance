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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.inkcraft.app.ai.InkAiEngine
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
import com.inkcraft.app.model.TextBox
import com.inkcraft.app.model.ToolMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val QUICK_COLORS = listOf(
    0xFF1D1D1F, // Apple Charcoal
    0xFF007AFF, // macOS Blue
    0xFFFF3B30, // macOS Red
    0xFF34C759, // macOS Green
    0xFF5856D6, // macOS Indigo
    0xFFFF9500, // macOS Amber
    0xFFAF52DE, // macOS Purple
    0xFFFFFFFF, // Pure White
    0xFFFFCC00  // Highlighter Yellow
)

enum class ToolbarDock { TOP, LEFT, RIGHT }

@Composable
fun EditorScreen(
    notebook: Notebook,
    presets: List<PenPreset>,
    pdfPageBitmaps: Map<String, Bitmap>,
    imageBitmaps: Map<String, Bitmap>,
    isRecordingAudio: Boolean,
    isPlayingAudio: Boolean,
    aiEngine: InkAiEngine,
    onBack: () -> Unit,
    onUpdateNotebook: (Notebook) -> Unit,
    onExportPdf: () -> Unit,
    onImportImage: (Int) -> Unit,
    onImportPdfIntoNotebook: () -> Unit,
    onToggleRecordAudio: () -> Unit,
    onPlayAudio: (AudioMemo) -> Unit,
    onStopAudio: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var currentPageIdx by remember(notebook.id) { mutableIntStateOf(0) }
    val safePageIdx = currentPageIdx.coerceIn(0, (notebook.pages.size - 1).coerceAtLeast(0))
    val currentPage = notebook.pages.getOrNull(safePageIdx) ?: NotePage()

    // Tool states
    var toolMode by remember { mutableStateOf(ToolMode.PEN) }
    var penStyle by remember { mutableStateOf(PenStyle.FOUNTAIN) }
    var penColorArgb by remember { mutableLongStateOf(0xFF1D1D1F) }
    var penWidth by remember { mutableFloatStateOf(5.0f) }
    var stylusOnlyMode by remember { mutableStateOf(true) }
    var pageLocked by remember { mutableStateOf(false) }
    var shapeRecognition by remember { mutableStateOf(true) }
    var showRuler by remember { mutableStateOf(false) }
    var showZoomBox by remember { mutableStateOf(false) }
    var zoomBoxPageX by remember { mutableFloatStateOf(120f) }
    var zoomBoxPageY by remember { mutableFloatStateOf(240f) }
    var showPageSidebar by remember { mutableStateOf(false) }
    var showPenPopover by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var toolbarDock by remember { mutableStateOf(ToolbarDock.TOP) }

    // AI Studio Modal state
    var showAiStudio by remember { mutableStateOf(false) }
    var aiLoading by remember { mutableStateOf(false) }
    var aiRecognizedText by remember { mutableStateOf("") }
    var aiError by remember { mutableStateOf<String?>(null) }
    var aiSpeaking by remember { mutableStateOf(false) }
    var aiSpeechRate by remember { mutableFloatStateOf(1.0f) }

    // Undo / Redo stacks per page
    var undoStacks by remember(notebook.id) { mutableStateOf<Map<String, List<List<Stroke>>>>(emptyMap()) }
    var redoStacks by remember(notebook.id) { mutableStateOf<Map<String, List<List<Stroke>>>>(emptyMap()) }

    // Lasso selection & overlays
    var selectedStrokeIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var editingStickyNote by remember { mutableStateOf<StickyNote?>(null) }
    var editingTextBox by remember { mutableStateOf<TextBox?>(null) }
    var currentZoomScale by remember { mutableFloatStateOf(1f) }

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

    var canvasViewRef by remember { mutableStateOf<InkCanvasView?>(null) }
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

    fun runAiRecognition() {
        showAiStudio = true
        aiLoading = true
        aiError = null
        scope.launch {
            val res = aiEngine.recognizeHandwriting(currentPage.strokes)
            aiLoading = false
            res.onSuccess { text ->
                aiRecognizedText = text
            }.onFailure { err ->
                aiError = err.message ?: "Handwriting recognition failed"
            }
        }
    }

    Scaffold(
        topBar = {
            // macOS Unified Studio Titlebar
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(6.dp))
                    Text(notebook.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                    Spacer(Modifier.width(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "${safePageIdx + 1} / ${notebook.pages.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Compact Undo / Redo Pill
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row {
                            IconButton(
                                onClick = { undo() },
                                enabled = undoStacks[currentPage.id]?.isNotEmpty() == true,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(17.dp))
                            }
                            IconButton(
                                onClick = { redo() },
                                enabled = redoStacks[currentPage.id]?.isNotEmpty() == true,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", modifier = Modifier.size(17.dp))
                            }
                        }
                    }

                    // Stylus-Only Palm Rejection Pill
                    MacTitlebarPill(
                        selected = stylusOnlyMode,
                        icon = Icons.Default.TouchApp,
                        label = if (stylusOnlyMode) "Palm Safe" else "Touch+Pen",
                        onClick = { stylusOnlyMode = !stylusOnlyMode }
                    )
                    Spacer(Modifier.width(4.dp))

                    // Lock Page Position Pill (zero accidental pan!)
                    MacTitlebarPill(
                        selected = pageLocked,
                        icon = if (pageLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        label = if (pageLocked) "Locked" else "Pan Free",
                        onClick = { pageLocked = !pageLocked }
                    )
                    Spacer(Modifier.width(4.dp))

                    // Shape Snap Pill
                    MacTitlebarPill(
                        selected = shapeRecognition,
                        icon = Icons.Default.AutoFixHigh,
                        label = "Shapes",
                        onClick = { shapeRecognition = !shapeRecognition }
                    )
                    Spacer(Modifier.width(6.dp))

                    // AI Studio Button (ML Kit Handwriting -> Text + Voice)
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { runAiRecognition() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Studio", tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("AI Studio", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    IconButton(onClick = { showRuler = !showRuler }, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Default.Straighten,
                            contentDescription = "Ruler",
                            tint = if (showRuler) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = { showZoomBox = !showZoomBox }, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Default.ZoomIn,
                            contentDescription = "Zoom Box",
                            tint = if (showZoomBox) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onToggleRecordAudio, modifier = Modifier.size(34.dp)) {
                        Icon(
                            if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Record Audio",
                            tint = if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Pomodoro Timer Pill
                    Surface(
                        color = if (focusRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { focusRunning = !focusRunning }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("%02d:%02d".format(focusSecondsLeft / 60, focusSecondsLeft % 60), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    IconButton(onClick = { showPageSidebar = !showPageSidebar }, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = "Pages",
                            tint = if (showPageSidebar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box {
                        IconButton(onClick = { moreMenuExpanded = true }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "More", modifier = Modifier.size(18.dp))
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
                                    Text(
                                        when (toolbarDock) {
                                            ToolbarDock.TOP -> "Dock Toolbar Left"
                                            ToolbarDock.LEFT -> "Dock Toolbar Right"
                                            ToolbarDock.RIGHT -> "Dock Toolbar Top"
                                        }
                                    )
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
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
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

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val pdfKey = if (currentPage.pdfFileName != null && currentPage.pdfPageIndex != null) {
                        "${currentPage.pdfFileName}:${currentPage.pdfPageIndex}"
                    } else null

                    AndroidView(
                        factory = { ctx -> InkCanvasView(ctx).also { canvasViewRef = it } },
                        update = { view ->
                            view.page = currentPage
                            view.pdfBackgroundBitmap = pdfKey?.let { pdfPageBitmaps[it] }
                            view.imageBitmaps = imageBitmaps
                            view.toolMode = toolMode
                            view.penStyle = penStyle
                            view.penColorArgb = penColorArgb
                            view.penWidth = penWidth
                            view.stylusOnlyMode = stylusOnlyMode
                            view.pageLocked = pageLocked
                            view.shapeRecognitionEnabled = shapeRecognition
                            view.showRuler = showRuler
                            view.onStrokeCommitted = { stroke ->
                                pushUndoAndReplaceStrokes(currentPage.strokes + stroke)
                            }
                            view.onStrokesReplaced = { newStrokes ->
                                pushUndoAndReplaceStrokes(newStrokes)
                            }
                            view.onStickyNoteTapped = { note -> editingStickyNote = note }
                            view.onStickyNoteAdded = { x, y ->
                                val newNote = StickyNote(x = x - 120f, y = y - 100f)
                                mutateCurrentPage { it.copy(stickyNotes = it.stickyNotes + newNote) }
                                editingStickyNote = newNote
                            }
                            view.onStickyNoteMoved = { id, dx, dy ->
                                mutateCurrentPage { p ->
                                    p.copy(stickyNotes = p.stickyNotes.map { if (it.id == id) it.copy(x = it.x + dx, y = it.y + dy) else it })
                                }
                            }
                            view.onTextBoxTapped = { tb -> editingTextBox = tb }
                            view.onTextBoxAdded = { x, y ->
                                val newTb = TextBox(x = x, y = y, text = "")
                                mutateCurrentPage { it.copy(textBoxes = it.textBoxes + newTb) }
                                editingTextBox = newTb
                            }
                            view.onViewportChanged = { scale, _, _ -> currentZoomScale = scale }
                            view.onLassoSelectionChanged = { ids -> selectedStrokeIds = ids }
                            view.onPageSwipe = { delta ->
                                val next = safePageIdx + delta
                                if (next in 0..notebook.pages.lastIndex) {
                                    currentPageIdx = next
                                } else if (delta > 0) {
                                    val newPage = NotePage(
                                        pageIndex = notebook.pages.size,
                                        template = currentPage.template,
                                        paperColor = currentPage.paperColor
                                    )
                                    val updated = notebook.pages + newPage
                                    onUpdateNotebook(notebook.copy(pages = updated))
                                    currentPageIdx = updated.lastIndex
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    val toolbarModifier = when (toolbarDock) {
                        ToolbarDock.TOP -> Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                        ToolbarDock.LEFT -> Modifier.align(Alignment.CenterStart).padding(start = 12.dp)
                        ToolbarDock.RIGHT -> Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
                    }

                    MacFloatingToolDock(
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

                    if (showPenPopover) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 74.dp)
                                .shadow(14.dp, RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
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
                                    Text("${penStyle.displayName} • Ink Studio", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    IconButton(onClick = { showPenPopover = false }, modifier = Modifier.size(26.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                                    }
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(QUICK_COLORS) { col ->
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(Color(col))
                                                .border(
                                                    width = if (col == penColorArgb) 2.5.dp else 1.dp,
                                                    color = if (col == penColorArgb) MaterialTheme.colorScheme.primary else Color.Gray,
                                                    shape = CircleShape
                                                )
                                                .clickable { penColorArgb = col }
                                        )
                                    }
                                }
                                Text("Stroke Width: ${penWidth.toInt()} px", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(value = penWidth, onValueChange = { penWidth = it }, valueRange = 1.5f..36f)
                            }
                        }
                    }

                    if (selectedStrokeIds.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                                .shadow(12.dp, RoundedCornerShape(20.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("${selectedStrokeIds.size} selected", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                IconButton(onClick = { canvasViewRef?.scaleSelectedStrokes(0.88f) }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.ZoomOut, contentDescription = "Shrink", modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { canvasViewRef?.scaleSelectedStrokes(1.14f) }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = "Enlarge", modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { canvasViewRef?.rotateSelectedStrokes(-15f) }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.RotateLeft, contentDescription = "Rotate Left", modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { canvasViewRef?.rotateSelectedStrokes(15f) }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate Right", modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { canvasViewRef?.duplicateSelectedStrokes() }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(16.dp))
                                }
                                QUICK_COLORS.take(3).forEach { col ->
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(col))
                                            .border(1.dp, Color.Gray, CircleShape)
                                            .clickable { canvasViewRef?.recolorSelectedStrokes(col) }
                                    )
                                }
                                IconButton(onClick = { canvasViewRef?.deleteSelectedStrokes() }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { canvasViewRef?.clearSelection() }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Bottom-Right macOS Zoom & Page Navigation HUD
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = if (showZoomBox) 264.dp else 16.dp)
                            .shadow(8.dp, RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { if (safePageIdx > 0) currentPageIdx = safePageIdx - 1 },
                                enabled = safePageIdx > 0,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Page", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                "P.${safePageIdx + 1}/${notebook.pages.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = { if (safePageIdx < notebook.pages.lastIndex) currentPageIdx = safePageIdx + 1 },
                                enabled = safePageIdx < notebook.pages.lastIndex,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    val newPage = NotePage(
                                        pageIndex = notebook.pages.size,
                                        template = currentPage.template,
                                        paperColor = currentPage.paperColor
                                    )
                                    val updated = notebook.pages + newPage
                                    onUpdateNotebook(notebook.copy(pages = updated))
                                    currentPageIdx = updated.lastIndex
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "New Page", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }

                            Box(Modifier.width(1.dp).height(18.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)))

                            IconButton(onClick = { canvasViewRef?.setFixedZoom(currentZoomScale - 0.15f) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                "${(currentZoomScale * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { canvasViewRef?.fitPageToWidth() }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                            IconButton(onClick = { canvasViewRef?.setFixedZoom(currentZoomScale + 0.15f) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (showZoomBox) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(250.dp)
                                .shadow(14.dp, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Zoom Box (2.2x Precision)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                                            Icon(Icons.Default.Close, contentDescription = "Close")
                                        }
                                    }
                                }
                                AndroidView(
                                    factory = { ctx -> InkCanvasView(ctx) },
                                    update = { zv ->
                                        zv.page = currentPage
                                        zv.pdfBackgroundBitmap = pdfKey?.let { pdfPageBitmaps[it] }
                                        zv.imageBitmaps = imageBitmaps
                                        zv.toolMode = toolMode
                                        zv.penStyle = penStyle
                                        zv.penColorArgb = penColorArgb
                                        zv.penWidth = penWidth
                                        zv.stylusOnlyMode = stylusOnlyMode
                                        zv.pageLocked = true
                                        zv.shapeRecognitionEnabled = shapeRecognition
                                        zv.setViewport(2.2f, -zoomBoxPageX * 2.2f, -zoomBoxPageY * 2.2f)
                                        zv.onStrokeCommitted = { stroke -> pushUndoAndReplaceStrokes(currentPage.strokes + stroke) }
                                        zv.onStrokesReplaced = { newStrokes -> pushUndoAndReplaceStrokes(newStrokes) }
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

    // On-Device AI Studio Dialog (ML Kit Handwriting -> Text + Voice Read-Aloud + Insights)
    if (showAiStudio) {
        val insights = remember(aiRecognizedText) { InkAiEngine.buildSmartInsights(aiRecognizedText) }
        AlertDialog(
            onDismissRequest = {
                aiEngine.stopSpeaking()
                aiSpeaking = false
                showAiStudio = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("InkCraft AI Studio", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (aiLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text("Running on-device ML Kit Digital Ink model...", fontSize = 13.sp)
                        }
                    } else if (aiError != null) {
                        Text(aiError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    } else {
                        Text("Recognized Handwriting (Editable)", style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = aiRecognizedText,
                            onValueChange = { aiRecognizedText = it },
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Voice Read-Aloud Controls
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Voice Read-Aloud", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(0.85f to "0.85x", 1.0f to "1x", 1.25f to "1.25x").forEach { (rate, label) ->
                                        FilterChip(
                                            selected = aiSpeechRate == rate,
                                            onClick = { aiSpeechRate = rate },
                                            label = { Text(label, fontSize = 11.sp) }
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            if (aiSpeaking) {
                                                aiEngine.stopSpeaking()
                                                aiSpeaking = false
                                            } else {
                                                aiSpeaking = true
                                                aiEngine.speak(aiRecognizedText, aiSpeechRate) {
                                                    aiSpeaking = false
                                                }
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(if (aiSpeaking) "Stop" else "Speak", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Smart Insights
                        if (insights.wordCount > 0) {
                            Text("Smart Insights (${insights.wordCount} words)", style = MaterialTheme.typography.labelMedium)
                            if (insights.keyPoints.isNotEmpty()) {
                                insights.keyPoints.forEach { pt ->
                                    Text("• $pt", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (insights.actionItems.isNotEmpty()) {
                                Text("Detected Action Items", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                insights.actionItems.forEach { act ->
                                    Text("☐ $act", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { clipboard.setText(AnnotatedString(aiRecognizedText)) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copy Text", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    val note = StickyNote(x = 180f, y = 260f, width = 360f, height = 240f, text = aiRecognizedText)
                                    mutateCurrentPage { it.copy(stickyNotes = it.stickyNotes + note) }
                                    showAiStudio = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.StickyNote2, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Insert Note", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    aiEngine.stopSpeaking()
                    aiSpeaking = false
                    showAiStudio = false
                }) { Text("Close") }
            }
        )
    }

    editingStickyNote?.let { note ->
        var text by remember(note.id) { mutableStateOf(note.text) }
        AlertDialog(
            onDismissRequest = { editingStickyNote = null },
            title = { Text("Sticky Note", fontWeight = FontWeight.SemiBold) },
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
                    mutateCurrentPage { p -> p.copy(stickyNotes = p.stickyNotes.filterNot { it.id == note.id }) }
                    editingStickyNote = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        )
    }

    editingTextBox?.let { tb ->
        var text by remember(tb.id) { mutableStateOf(tb.text) }
        AlertDialog(
            onDismissRequest = { editingTextBox = null },
            title = { Text("Text Box", fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Text") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    mutateCurrentPage { p ->
                        p.copy(textBoxes = p.textBoxes.map { if (it.id == tb.id) it.copy(text = text) else it })
                    }
                    editingTextBox = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mutateCurrentPage { p -> p.copy(textBoxes = p.textBoxes.filterNot { it.id == tb.id }) }
                    editingTextBox = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        )
    }

    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("Page Template & Paper", fontWeight = FontWeight.SemiBold) },
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
private fun MacTitlebarPill(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MacFloatingToolDock(
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
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(alpha = 0.14f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
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
            ToolBtn(Icons.Default.AutoFixHigh, "Calligraphy", toolMode == ToolMode.PEN && penStyle == PenStyle.CALLIGRAPHY) {
                onSelectPenStyle(PenStyle.CALLIGRAPHY)
            }
            ToolBtn(Icons.Default.Edit, "Graphite Pencil", toolMode == ToolMode.PEN && penStyle == PenStyle.PENCIL) {
                onSelectPenStyle(PenStyle.PENCIL)
            }
            ToolBtn(Icons.Default.Highlight, "Highlighter", toolMode == ToolMode.PEN && penStyle == PenStyle.HIGHLIGHTER) {
                onSelectPenStyle(PenStyle.HIGHLIGHTER)
            }
            ToolBtn(Icons.Default.CenterFocusStrong, "Laser", toolMode == ToolMode.PEN && penStyle == PenStyle.LASER) {
                onSelectPenStyle(PenStyle.LASER)
            }
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(penColorArgb))
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable(onClick = onTogglePenPopover)
            )
            presets.take(3).forEach { preset ->
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .size(20.dp)
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
            ToolBtn(Icons.Default.TextFields, "Text Box", toolMode == ToolMode.TEXT_BOX) {
                onSelectTool(ToolMode.TEXT_BOX)
            }
            ToolBtn(Icons.Default.StickyNote2, "Sticky Note", toolMode == ToolMode.STICKY_NOTE) {
                onSelectTool(ToolMode.STICKY_NOTE)
            }
            ToolBtn(Icons.Default.PanTool, "Pan", toolMode == ToolMode.PAN_ONLY) {
                onSelectTool(ToolMode.PAN_ONLY)
            }
        }

        if (vertical) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)) { content() }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) { content() }
        }
    }
}

@Composable
private fun ToolBtn(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
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
        color = Color(0xFFF2F2F7),
        modifier = Modifier
            .width(214.dp)
            .fillMaxHeight()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pages", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                IconButton(onClick = onAddPage, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add Page", modifier = Modifier.size(18.dp))
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
                            .height(106.dp)
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectPage(idx) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            Text(
                                "Page ${idx + 1}\n${page.template.displayName}",
                                fontSize = 11.sp,
                                color = if (page.paperColor.isDark) Color.White else Color.Black,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                            Row(modifier = Modifier.align(Alignment.BottomEnd)) {
                                IconButton(onClick = { onToggleBookmark(idx) }, modifier = Modifier.size(26.dp)) {
                                    Icon(
                                        if (page.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (page.isBookmarked) Color(0xFFFF9500) else Color.Gray,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                IconButton(onClick = { onDuplicatePage(idx) }, modifier = Modifier.size(26.dp)) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(14.dp))
                                }
                                if (pages.size > 1) {
                                    IconButton(onClick = { onDeletePage(idx) }, modifier = Modifier.size(26.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (audioMemos.isNotEmpty()) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("AUDIO MEMOS", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    audioMemos.forEachIndexed { i, memo ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .clickable { if (isPlayingAudio) onStopAudio() else onPlayAudio(memo) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                                Icon(if (isPlayingAudio) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
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
