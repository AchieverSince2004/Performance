package com.inkcraft.app.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import com.inkcraft.app.data.NotebookRepository
import com.inkcraft.app.ink.InkMath
import com.inkcraft.app.ink.PaperTemplates
import com.inkcraft.app.ink.StrokeRenderer
import com.inkcraft.app.model.NotePage
import com.inkcraft.app.model.Notebook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfTools {

    /**
     * Renders a single PDF page from app media storage into a crisp Bitmap.
     */
    suspend fun renderPdfPage(
        pdfFile: File,
        pageIndex: Int,
        targetWidth: Int = InkMath.PAGE_WIDTH.toInt(),
        targetHeight: Int = InkMath.PAGE_HEIGHT.toInt()
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!pdfFile.exists()) return@withContext null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
            renderer.openPage(pageIndex).use { page ->
                val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return@withContext bmp
            }
        } catch (_: Exception) {
            null
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }

    suspend fun getPdfPageCount(pdfFile: File): Int = withContext(Dispatchers.IO) {
        if (!pdfFile.exists()) return@withContext 0
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            renderer.pageCount
        } catch (_: Exception) {
            0
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }

    /**
     * Exports an entire [Notebook] to a multi-page PDF in cacheDir/exports and returns the File.
     */
    suspend fun exportNotebookToPdf(
        context: Context,
        notebook: Notebook,
        repository: NotebookRepository
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = notebook.title.replace(Regex("[^A-Za-z0-9_\\-]"), "_").ifBlank { "InkCraft_Note" }
        val outFile = File(exportDir, "$safeName.pdf")

        val document = PdfDocument()
        val w = InkMath.PAGE_WIDTH.toInt()
        val h = InkMath.PAGE_HEIGHT.toInt()

        notebook.pages.forEachIndexed { idx, page ->
            val pageInfo = PdfDocument.PageInfo.Builder(w, h, idx + 1).create()
            val pdfPage = document.startPage(pageInfo)
            drawPageForExport(pdfPage.canvas, page, repository)
            document.finishPage(pdfPage)
        }

        FileOutputStream(outFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()
        outFile
    }

    private suspend fun drawPageForExport(
        canvas: Canvas,
        page: NotePage,
        repository: NotebookRepository
    ) {
        val w = InkMath.PAGE_WIDTH
        val h = InkMath.PAGE_HEIGHT
        if (page.pdfFileName != null && page.pdfPageIndex != null) {
            val pdfBmp = renderPdfPage(repository.getMediaFile(page.pdfFileName), page.pdfPageIndex)
            if (pdfBmp != null) {
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(pdfBmp, null, RectF(0f, 0f, w, h), null)
                pdfBmp.recycle()
            } else {
                PaperTemplates.drawPaper(canvas, w, h, page.template, page.paperColor)
            }
        } else {
            PaperTemplates.drawPaper(canvas, w, h, page.template, page.paperColor)
        }

        for (img in page.images) {
            val bmp = repository.loadBitmap(img.fileName) ?: continue
            canvas.drawBitmap(bmp, null, RectF(img.x, img.y, img.x + img.width, img.y + img.height), null)
            bmp.recycle()
        }

        for (stroke in page.strokes) {
            StrokeRenderer.drawStroke(canvas, stroke)
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
    }
}
