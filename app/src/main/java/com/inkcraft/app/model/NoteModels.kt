package com.inkcraft.app.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class PenStyle(val displayName: String, val defaultWidth: Float, val minPressureScale: Float, val maxPressureScale: Float) {
    FOUNTAIN("Fountain Pen", 5.5f, 0.35f, 1.85f),
    BALLPOINT("Ballpoint", 4.0f, 0.75f, 1.20f),
    FINELINER("Fineliner", 2.8f, 0.95f, 1.05f),
    BRUSH("Brush Pen", 10.0f, 0.25f, 2.30f),
    HIGHLIGHTER("Highlighter", 26.0f, 0.90f, 1.15f),
    LASER("Laser Pointer", 7.0f, 1.0f, 1.0f)
}

enum class ToolMode {
    PEN,
    ERASER_STROKE,
    ERASER_PRECISION,
    LASSO,
    STICKY_NOTE,
    PAN_ONLY
}

@Serializable
enum class PaperTemplate(val displayName: String) {
    BLANK("Blank"),
    RULED_WIDE("Ruled Wide"),
    RULED_NARROW("Ruled Narrow"),
    CORNELL("Cornell Notes"),
    DOT_GRID("Dot Grid"),
    SQUARE_GRID("Square Grid"),
    ISOMETRIC("Isometric Grid"),
    MUSIC_STAFF("Music Staff"),
    DAILY_PLANNER("Daily Planner"),
    WEEKLY_SCHEDULE("Weekly Schedule")
}

@Serializable
enum class PaperColor(val displayName: String, val bgArgb: Long, val lineArgb: Long, val isDark: Boolean) {
    WHITE("Crisp White", 0xFFFFFFFF, 0xFFD0D7E2, false),
    CREAM("Warm Cream", 0xFFFBF8F1, 0xFFE2D9C8, false),
    SEPIA("Soft Sepia", 0xFFF4ECD8, 0xFFD5C7A9, false),
    DARK_SLATE("Dark Slate", 0xFF1E222A, 0xFF333A48, true),
    MIDNIGHT("Midnight Ink", 0xFF111318, 0xFF282D3A, true)
}

@Serializable
enum class CoverTheme(
    val displayName: String,
    val primaryArgb: Long,
    val accentArgb: Long,
    val spineArgb: Long,
    val textArgb: Long
) {
    LEATHER_NAVY("Classic Navy", 0xFF1E293B, 0xFF3B82F6, 0xFF0F172A, 0xFFF8FAFC),
    LEATHER_BURGUNDY("Oxblood Leather", 0xFF581C2C, 0xFFF59E0B, 0xFF3B0D19, 0xFFFEF3C7),
    FOREST_GREEN("Botanical Sage", 0xFF1B4332, 0xFF52B788, 0xFF081C15, 0xFFECFDF5),
    SUNSET_GRADIENT("Amber Horizon", 0xFF9A3412, 0xFFFB923C, 0xFF7C2D12, 0xFFFFF7ED),
    BLUEPRINT("Architect Blueprint", 0xFF1D4ED8, 0xFF93C5FD, 0xFF1E3A8A, 0xFFEFF6FF),
    MINIMAL_CREAM("Linen Cream", 0xFFE7E0D3, 0xFF78716C, 0xFFD6CEC0, 0xFF292524),
    CARBON_DARK("Carbon Studio", 0xFF18181B, 0xFFA855F7, 0xFF09090B, 0xFFFAFAFA),
    LAVENDER_MIST("Iris Mist", 0xFF4C1D95, 0xFFC4B5FD, 0xFF2E1065, 0xFFF5F3FF)
}

@Serializable
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,
    val tilt: Float = 0f,
    val timestamp: Long = 0L
)

@Serializable
data class Stroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<StrokePoint>,
    val colorArgb: Long,
    val baseWidth: Float,
    val penStyle: PenStyle,
    val isShape: Boolean = false,
    val audioOffsetMs: Long? = null
)

@Serializable
data class StickyNote(
    val id: String = UUID.randomUUID().toString(),
    val x: Float,
    val y: Float,
    val width: Float = 260f,
    val height: Float = 220f,
    val text: String = "",
    val colorArgb: Long = 0xFFFEF08A
)

@Serializable
data class CanvasImage(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

@Serializable
data class NotePage(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int = 0,
    val template: PaperTemplate = PaperTemplate.RULED_WIDE,
    val paperColor: PaperColor = PaperColor.CREAM,
    val strokes: List<Stroke> = emptyList(),
    val stickyNotes: List<StickyNote> = emptyList(),
    val images: List<CanvasImage> = emptyList(),
    val pdfFileName: String? = null,
    val pdfPageIndex: Int? = null,
    val isBookmarked: Boolean = false,
    val title: String = ""
)

@Serializable
data class AudioMemo(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val durationMs: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class Notebook(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val folderId: String? = null,
    val coverTheme: CoverTheme = CoverTheme.LEATHER_NAVY,
    val defaultTemplate: PaperTemplate = PaperTemplate.RULED_WIDE,
    val defaultPaperColor: PaperColor = PaperColor.CREAM,
    val pages: List<NotePage> = listOf(NotePage()),
    val audioMemos: List<AudioMemo> = emptyList(),
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class Folder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorArgb: Long = 0xFF3B82F6
)

@Serializable
data class PenPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val penStyle: PenStyle,
    val colorArgb: Long,
    val width: Float
)

@Serializable
data class InkWorkspace(
    val notebooks: List<Notebook> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val presets: List<PenPreset> = defaultPresets()
) {
    companion object {
        fun defaultPresets(): List<PenPreset> = listOf(
            PenPreset("p1", "Ink Black", PenStyle.FOUNTAIN, 0xFF111827, 5.0f),
            PenPreset("p2", "Royal Blue", PenStyle.FOUNTAIN, 0xFF2563EB, 5.0f),
            PenPreset("p3", "Crimson", PenStyle.BALLPOINT, 0xFFDC2626, 4.0f),
            PenPreset("p4", "Emerald", PenStyle.FINELINER, 0xFF059669, 3.0f),
            PenPreset("p5", "Sun Highlight", PenStyle.HIGHLIGHTER, 0xFFFACC15, 28.0f)
        )
    }
}
