package com.inkcraft.app.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.inkcraft.app.model.PenStyle
import com.inkcraft.app.model.Stroke
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * On-device AI Studio Engine:
 * 1. Google ML Kit Digital Ink Recognition (Handwriting -> Digital Text)
 * 2. Android Text-To-Speech (Read Handwritten Notes Aloud)
 * 3. Offline Extractive Summarizer & Action-Item Detector
 */
class InkAiEngine(context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var onSpeakDoneCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        onSpeakDoneCallback?.invoke()
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onSpeakDoneCallback?.invoke()
                    }
                })
            }
        }
    }

    /**
     * Runs Google ML Kit's on-device neural Digital Ink recognizer on the page's strokes.
     */
    suspend fun recognizeHandwriting(strokes: List<Stroke>): Result<String> {
        val writableStrokes = strokes.filter {
            it.penStyle != PenStyle.HIGHLIGHTER && it.penStyle != PenStyle.LASER && !it.isShape && it.points.isNotEmpty()
        }
        if (writableStrokes.isEmpty()) {
            return Result.failure(IllegalStateException("Write some notes on the page first!"))
        }

        val modelId = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
            ?: return Result.failure(IllegalStateException("English Digital Ink model unavailable"))
        val model = DigitalInkRecognitionModel.builder(modelId).build()
        val remoteModelManager = RemoteModelManager.getInstance()

        val downloaded = suspendCancellableCoroutine { cont ->
            remoteModelManager.isModelDownloaded(model)
                .addOnSuccessListener { isDownloaded ->
                    if (isDownloaded) {
                        cont.resume(true)
                    } else {
                        remoteModelManager.download(model, DownloadConditions.Builder().build())
                            .addOnSuccessListener { cont.resume(true) }
                            .addOnFailureListener { cont.resume(false) }
                    }
                }
                .addOnFailureListener { cont.resume(false) }
        }

        if (!downloaded) {
            return Result.failure(IllegalStateException("Could not download ML Kit handwriting model. Check internet connection once."))
        }

        val inkBuilder = Ink.builder()
        var syntheticTime = 0L
        for (stroke in writableStrokes) {
            val sb = Ink.Stroke.builder()
            for (pt in stroke.points) {
                val t = if (pt.timestamp > 0L) pt.timestamp else syntheticTime
                sb.addPoint(Ink.Point.create(pt.x, pt.y, t))
                syntheticTime += 12L
            }
            inkBuilder.addStroke(sb.build())
            syntheticTime += 120L
        }

        val recognizer = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).build()
        )

        return suspendCancellableCoroutine { cont ->
            recognizer.recognize(inkBuilder.build())
                .addOnSuccessListener { result ->
                    val top = result.candidates.firstOrNull()?.text?.trim().orEmpty()
                    recognizer.close()
                    if (top.isBlank()) {
                        cont.resume(Result.failure(IllegalStateException("No clear handwriting detected.")))
                    } else {
                        cont.resume(Result.success(top))
                    }
                }
                .addOnFailureListener { e ->
                    recognizer.close()
                    cont.resume(Result.failure(e))
                }
        }
    }

    fun speak(text: String, rate: Float = 1.0f, onDone: () -> Unit) {
        if (!ttsReady || text.isBlank()) {
            onDone()
            return
        }
        onSpeakDoneCallback = onDone
        tts?.setSpeechRate(rate.coerceIn(0.6f, 1.8f))
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "inkcraft_tts")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        /**
         * On-device extractive summary & action-item extraction from recognized text.
         */
        fun buildSmartInsights(text: String): SmartInsights {
            val cleaned = text.trim()
            if (cleaned.isBlank()) return SmartInsights(emptyList(), emptyList(), 0)
            val lines = cleaned.split(Regex("[\\n.!?;]+")).map { it.trim() }.filter { it.length > 2 }
            val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }

            val actionKeywords = listOf("todo", "fix", "buy", "call", "email", "submit", "review", "finish", "check", "remember", "deadline", "tomorrow", "today")
            val actions = lines.filter { line ->
                val lower = line.lowercase()
                actionKeywords.any { kw -> lower.contains(kw) }
            }.distinct().take(5)

            val bullets = lines
                .sortedByDescending { it.length }
                .take(4)
                .distinct()

            return SmartInsights(
                keyPoints = bullets,
                actionItems = actions,
                wordCount = words.size
            )
        }
    }
}

data class SmartInsights(
    val keyPoints: List<String>,
    val actionItems: List<String>,
    val wordCount: Int
)
