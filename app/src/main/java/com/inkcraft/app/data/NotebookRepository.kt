package com.inkcraft.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.inkcraft.app.model.CoverTheme
import com.inkcraft.app.model.Folder
import com.inkcraft.app.model.InkWorkspace
import com.inkcraft.app.model.NotePage
import com.inkcraft.app.model.Notebook
import com.inkcraft.app.model.PaperColor
import com.inkcraft.app.model.PaperTemplate
import com.inkcraft.app.model.PenStyle
import com.inkcraft.app.model.StickyNote
import com.inkcraft.app.model.Stroke
import com.inkcraft.app.model.StrokePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

class NotebookRepository(private val context: Context) {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val rootDir: File by lazy {
        File(context.filesDir, "notebooks").apply { mkdirs() }
    }

    val mediaDir: File by lazy {
        File(rootDir, "media").apply { mkdirs() }
    }

    private val workspaceFile: File by lazy {
        File(rootDir, "workspace.json")
    }

    suspend fun loadWorkspace(): InkWorkspace = withContext(Dispatchers.IO) {
        if (!workspaceFile.exists()) {
            val initial = createStarterWorkspace()
            saveWorkspace(initial)
            return@withContext initial
        }
        return@withContext try {
            json.decodeFromString<InkWorkspace>(workspaceFile.readText())
        } catch (_: Exception) {
            createStarterWorkspace()
        }
    }

    suspend fun saveWorkspace(workspace: InkWorkspace) = withContext(Dispatchers.IO) {
        val tmp = File(rootDir, "workspace.json.tmp")
        tmp.writeText(json.encodeToString(workspace))
        tmp.renameTo(workspaceFile)
    }

    suspend fun importUriToMedia(uri: Uri, prefix: String, ext: String): String? = withContext(Dispatchers.IO) {
        val fileName = "${prefix}_${UUID.randomUUID()}.$ext"
        val dest = File(mediaDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
            return@withContext fileName
        }
        return@withContext null
    }

    fun getMediaFile(fileName: String): File = File(mediaDir, fileName)

    fun loadBitmap(fileName: String): Bitmap? {
        val file = getMediaFile(fileName)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    private fun createStarterWorkspace(): InkWorkspace {
        val studyFolder = Folder(id = "f_study", name = "Lecture Notes", colorArgb = 0xFF3B82F6)
        val personalFolder = Folder(id = "f_personal", name = "Planners & Sketches", colorArgb = 0xFF10B981)

        // Create a decorative wave & circle on page 1 so the user sees pressure ink immediately
        val wavePoints = (0..60).map { i ->
            val x = 160f + i * 14f
            val y = 320f + sin(i * 0.25f) * 32f
            val p = 0.25f + 0.65f * ((sin(i * 0.15f) + 1f) * 0.5f)
            StrokePoint(x, y, pressure = p)
        }
        val circlePoints = (0..40).map { i ->
            val angle = (2.0 * Math.PI * i / 40).toFloat()
            StrokePoint(
                x = 960f + 75f * cos(angle),
                y = 290f + 75f * sin(angle),
                pressure = 0.65f
            )
        }

        val welcomeNotebook = Notebook(
            id = "nb_welcome",
            title = "Welcome to InkCraft",
            folderId = studyFolder.id,
            coverTheme = CoverTheme.LEATHER_NAVY,
            defaultTemplate = PaperTemplate.CORNELL,
            defaultPaperColor = PaperColor.CREAM,
            isFavorite = true,
            tags = listOf("Guide", "Stylus"),
            pages = listOf(
                NotePage(
                    pageIndex = 0,
                    template = PaperTemplate.CORNELL,
                    paperColor = PaperColor.CREAM,
                    isBookmarked = true,
                    title = "Stylus & Tools",
                    strokes = listOf(
                        Stroke(
                            points = wavePoints,
                            colorArgb = 0xFF2563EB,
                            baseWidth = 7f,
                            penStyle = PenStyle.FOUNTAIN
                        ),
                        Stroke(
                            points = circlePoints,
                            colorArgb = 0xFF059669,
                            baseWidth = 5f,
                            penStyle = PenStyle.FINELINER,
                            isShape = true
                        )
                    ),
                    stickyNotes = listOf(
                        StickyNote(
                            x = 180f,
                            y = 440f,
                            width = 360f,
                            height = 240f,
                            text = "Tips for Tablet Pen:\n• Hold at stroke end to snap shapes\n• Toggle Stylus-Only for palm rejection\n• Use Zoom Box for neat writing!",
                            colorArgb = 0xFFFEF08A
                        )
                    )
                ),
                NotePage(
                    pageIndex = 1,
                    template = PaperTemplate.DOT_GRID,
                    paperColor = PaperColor.WHITE,
                    title = "Dot Grid Canvas"
                )
            )
        )

        val plannerNotebook = Notebook(
            id = "nb_planner",
            title = "Weekly Focus Planner",
            folderId = personalFolder.id,
            coverTheme = CoverTheme.FOREST_GREEN,
            defaultTemplate = PaperTemplate.DAILY_PLANNER,
            defaultPaperColor = PaperColor.SEPIA,
            tags = listOf("Planner"),
            pages = listOf(
                NotePage(pageIndex = 0, template = PaperTemplate.DAILY_PLANNER, paperColor = PaperColor.SEPIA),
                NotePage(pageIndex = 1, template = PaperTemplate.WEEKLY_SCHEDULE, paperColor = PaperColor.SEPIA)
            )
        )

        val darkSketchbook = Notebook(
            id = "nb_dark",
            title = "Midnight Architecture",
            folderId = personalFolder.id,
            coverTheme = CoverTheme.CARBON_DARK,
            defaultTemplate = PaperTemplate.ISOMETRIC,
            defaultPaperColor = PaperColor.DARK_SLATE,
            tags = listOf("Design"),
            pages = listOf(
                NotePage(pageIndex = 0, template = PaperTemplate.ISOMETRIC, paperColor = PaperColor.DARK_SLATE)
            )
        )

        return InkWorkspace(
            notebooks = listOf(welcomeNotebook, plannerNotebook, darkSketchbook),
            folders = listOf(studyFolder, personalFolder)
        )
    }
}
