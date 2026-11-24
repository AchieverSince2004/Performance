package com.inkcraft.app.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

/**
 * Handles lecture/meeting audio recording and playback synchronized with note sessions.
 */
class AudioMemoManager(private val context: Context, private val mediaDir: File) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentFileName: String? = null
    private var recordStartTimeMs: Long = 0L

    var isRecording: Boolean = false
        private set

    var isPlaying: Boolean = false
        private set

    fun startRecording(): String? {
        if (isRecording) return currentFileName
        val fileName = "audio_${UUID.randomUUID()}.m4a"
        val outFile = File(mediaDir, fileName)
        return try {
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            currentFileName = fileName
            recordStartTimeMs = System.currentTimeMillis()
            isRecording = true
            fileName
        } catch (_: Exception) {
            null
        }
    }

    fun stopRecording(): Pair<String, Long>? {
        if (!isRecording) return null
        val fileName = currentFileName ?: return null
        val duration = System.currentTimeMillis() - recordStartTimeMs
        try {
            recorder?.stop()
        } catch (_: Exception) {
        } finally {
            recorder?.release()
            recorder = null
            isRecording = false
        }
        return fileName to duration.coerceAtLeast(500L)
    }

    fun play(fileName: String, onCompleted: () -> Unit) {
        stopPlayback()
        val file = File(mediaDir, fileName)
        if (!file.exists()) return
        try {
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    isPlaying = false
                    onCompleted()
                }
                start()
            }
            isPlaying = true
        } catch (_: Exception) {
            isPlaying = false
        }
    }

    fun stopPlayback() {
        try {
            player?.stop()
        } catch (_: Exception) {
        } finally {
            player?.release()
            player = null
            isPlaying = false
        }
    }

    fun release() {
        stopRecording()
        stopPlayback()
    }
}
