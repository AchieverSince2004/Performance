package com.inkcraft.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.inkcraft.app.ai.InkAiEngine
import com.inkcraft.app.audio.AudioMemoManager
import com.inkcraft.app.data.NotebookRepository
import com.inkcraft.app.export.PdfTools
import com.inkcraft.app.model.AudioMemo
import com.inkcraft.app.model.CanvasImage
import com.inkcraft.app.model.CoverTheme
import com.inkcraft.app.model.Folder
import com.inkcraft.app.model.InkWorkspace
import com.inkcraft.app.model.NotePage
import com.inkcraft.app.model.Notebook
import com.inkcraft.app.model.PaperColor
import com.inkcraft.app.model.PaperTemplate
import com.inkcraft.app.ui.EditorScreen
import com.inkcraft.app.ui.InkCraftTheme
import com.inkcraft.app.ui.ShelfScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: NotebookRepository
    private lateinit var audioManager: AudioMemoManager
    private lateinit var aiEngine: InkAiEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = NotebookRepository(this)
        audioManager = AudioMemoManager(this, repository.mediaDir)
        aiEngine = InkAiEngine(this)

        setContent {
            InkCraftTheme {
                val scope = rememberCoroutineScope()
                var workspace by remember { mutableStateOf(InkWorkspace()) }
                var activeNotebookId by remember { mutableStateOf<String?>(null) }
                var pdfPageBitmaps by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
                var imageBitmaps by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
                var isRecordingAudio by remember { mutableStateOf(false) }
                var isPlayingAudio by remember { mutableStateOf(false) }
                var pendingImagePageIdx by remember { mutableStateOf(0) }

                LaunchedEffect(Unit) {
                    workspace = repository.loadWorkspace()
                }

                fun save(updated: InkWorkspace) {
                    workspace = updated
                    scope.launch { repository.saveWorkspace(updated) }
                }

                val activeNotebook = workspace.notebooks.find { it.id == activeNotebookId }

                // Preload PDF and Image bitmaps for the active notebook
                LaunchedEffect(activeNotebook?.id, activeNotebook?.pages?.size) {
                    val nb = activeNotebook ?: return@LaunchedEffect
                    val newPdfMap = pdfPageBitmaps.toMutableMap()
                    val newImgMap = imageBitmaps.toMutableMap()
                    for (page in nb.pages) {
                        if (page.pdfFileName != null && page.pdfPageIndex != null) {
                            val key = "${page.pdfFileName}:${page.pdfPageIndex}"
                            if (!newPdfMap.containsKey(key)) {
                                PdfTools.renderPdfPage(
                                    repository.getMediaFile(page.pdfFileName),
                                    page.pdfPageIndex
                                )?.let { newPdfMap[key] = it }
                            }
                        }
                        for (img in page.images) {
                            if (!newImgMap.containsKey(img.fileName)) {
                                repository.loadBitmap(img.fileName)?.let { newImgMap[img.fileName] = it }
                            }
                        }
                    }
                    pdfPageBitmaps = newPdfMap
                    imageBitmaps = newImgMap
                }

                val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    val nb = activeNotebook ?: return@rememberLauncherForActivityResult
                    if (uri == null) return@rememberLauncherForActivityResult
                    scope.launch {
                        val fileName = repository.importUriToMedia(uri, "img", "png") ?: return@launch
                        repository.loadBitmap(fileName)?.let { bmp ->
                            imageBitmaps = imageBitmaps + (fileName to bmp)
                        }
                        val newImg = CanvasImage(fileName = fileName, x = 180f, y = 260f, width = 420f, height = 320f)
                        val updatedPages = nb.pages.mapIndexed { idx, p ->
                            if (idx == pendingImagePageIdx) p.copy(images = p.images + newImg) else p
                        }
                        val updatedNb = nb.copy(pages = updatedPages, updatedAt = System.currentTimeMillis())
                        save(workspace.copy(notebooks = workspace.notebooks.map { if (it.id == nb.id) updatedNb else it }))
                    }
                }

                val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    scope.launch {
                        val fileName = repository.importUriToMedia(uri, "doc", "pdf") ?: return@launch
                        val count = PdfTools.getPdfPageCount(repository.getMediaFile(fileName))
                        if (count <= 0) {
                            Toast.makeText(this@MainActivity, "Could not read PDF", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val pdfPages = (0 until count).map { idx ->
                            NotePage(pageIndex = idx, pdfFileName = fileName, pdfPageIndex = idx)
                        }
                        val targetNb = activeNotebook
                        if (targetNb != null) {
                            val updatedNb = targetNb.copy(pages = targetNb.pages + pdfPages, updatedAt = System.currentTimeMillis())
                            save(workspace.copy(notebooks = workspace.notebooks.map { if (it.id == targetNb.id) updatedNb else it }))
                        } else {
                            val newNb = Notebook(
                                title = "Annotated PDF (${count}p)",
                                coverTheme = CoverTheme.BLUEPRINT,
                                pages = pdfPages
                            )
                            save(workspace.copy(notebooks = listOf(newNb) + workspace.notebooks))
                            activeNotebookId = newNb.id
                        }
                    }
                }

                val audioPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) {
                        if (audioManager.startRecording() != null) {
                            isRecordingAudio = true
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Microphone permission required for audio notes", Toast.LENGTH_SHORT).show()
                    }
                }

                if (activeNotebook == null) {
                    ShelfScreen(
                        workspace = workspace,
                        onOpenNotebook = { activeNotebookId = it.id },
                        onCreateNotebook = { title, folderId, cover, template, paperColor ->
                            val nb = Notebook(
                                title = title,
                                folderId = folderId,
                                coverTheme = cover,
                                defaultTemplate = template,
                                defaultPaperColor = paperColor,
                                pages = listOf(NotePage(template = template, paperColor = paperColor))
                            )
                            save(workspace.copy(notebooks = listOf(nb) + workspace.notebooks))
                            activeNotebookId = nb.id
                        },
                        onQuickNote = {
                            val nb = Notebook(
                                title = "Quick Note",
                                coverTheme = CoverTheme.MINIMAL_CREAM,
                                defaultTemplate = PaperTemplate.DOT_GRID,
                                defaultPaperColor = PaperColor.CREAM,
                                pages = listOf(NotePage(template = PaperTemplate.DOT_GRID, paperColor = PaperColor.CREAM))
                            )
                            save(workspace.copy(notebooks = listOf(nb) + workspace.notebooks))
                            activeNotebookId = nb.id
                        },
                        onDeleteNotebook = { id ->
                            save(workspace.copy(notebooks = workspace.notebooks.filterNot { it.id == id }))
                        },
                        onToggleFavorite = { id ->
                            save(workspace.copy(notebooks = workspace.notebooks.map {
                                if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
                            }))
                        },
                        onExportPdf = { nb ->
                            scope.launch {
                                val file = PdfTools.exportNotebookToPdf(this@MainActivity, nb, repository)
                                PdfTools.shareFile(this@MainActivity, file, "application/pdf")
                            }
                        },
                        onImportPdfAsNotebook = { pdfPicker.launch("application/pdf") },
                        onCreateFolder = { name ->
                            val folder = Folder(name = name)
                            save(workspace.copy(folders = workspace.folders + folder))
                        }
                    )
                } else {
                    EditorScreen(
                        notebook = activeNotebook,
                        presets = workspace.presets,
                        pdfPageBitmaps = pdfPageBitmaps,
                        imageBitmaps = imageBitmaps,
                        isRecordingAudio = isRecordingAudio,
                        isPlayingAudio = isPlayingAudio,
                        aiEngine = aiEngine,
                        onBack = {
                            audioManager.stopPlayback()
                            isPlayingAudio = false
                            activeNotebookId = null
                        },
                        onUpdateNotebook = { updatedNb ->
                            save(workspace.copy(notebooks = workspace.notebooks.map {
                                if (it.id == updatedNb.id) updatedNb else it
                            }))
                        },
                        onExportPdf = {
                            scope.launch {
                                val file = PdfTools.exportNotebookToPdf(this@MainActivity, activeNotebook, repository)
                                PdfTools.shareFile(this@MainActivity, file, "application/pdf")
                            }
                        },
                        onImportImage = { pageIdx ->
                            pendingImagePageIdx = pageIdx
                            imagePicker.launch("image/*")
                        },
                        onImportPdfIntoNotebook = { pdfPicker.launch("application/pdf") },
                        onToggleRecordAudio = {
                            if (isRecordingAudio) {
                                val result = audioManager.stopRecording()
                                isRecordingAudio = false
                                if (result != null) {
                                    val (fileName, duration) = result
                                    val memo = AudioMemo(fileName = fileName, durationMs = duration)
                                    val updatedNb = activeNotebook.copy(audioMemos = activeNotebook.audioMemos + memo)
                                    save(workspace.copy(notebooks = workspace.notebooks.map {
                                        if (it.id == updatedNb.id) updatedNb else it
                                    }))
                                }
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    if (audioManager.startRecording() != null) isRecordingAudio = true
                                } else {
                                    audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        onPlayAudio = { memo ->
                            isPlayingAudio = true
                            audioManager.play(memo.fileName) { isPlayingAudio = false }
                        },
                        onStopAudio = {
                            audioManager.stopPlayback()
                            isPlayingAudio = false
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
        aiEngine.release()
    }
}
