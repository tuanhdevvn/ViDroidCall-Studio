package com.example.ViDroidCall_Studio.feature.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Quản lý tổng hợp giọng nói (Text-To-Speech - TTS) tiếng Việt native trên Android.
 */
class TextToSpeechManager(
    private val context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)

    var isInitialized: Boolean = false
        private set

    var isLanguageSupported: Boolean = false
        private set

    private val pendingUtterances = mutableListOf<Pair<String, Int>>()

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            mainHandler.post { onSpeakingStateChanged(true) }
        }

        override fun onDone(utteranceId: String?) {
            mainHandler.post { onSpeakingStateChanged(false) }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            mainHandler.post { onSpeakingStateChanged(false) }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            mainHandler.post { onSpeakingStateChanged(false) }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val localeVi = Locale.forLanguageTag("vi-VN")
            val result = tts?.setLanguage(localeVi)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val fallbackVi = Locale.forLanguageTag("vi")
                val fallbackResult = tts?.setLanguage(fallbackVi)
                if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Gói giọng nói Tiếng Việt chưa được cài đặt trên thiết bị này.")
                    isLanguageSupported = false
                } else {
                    isLanguageSupported = true
                    Log.i(TAG, "Đã cài đặt ngôn ngữ Tiếng Việt (Fallback: vi) cho TTS")
                }
            } else {
                isLanguageSupported = true
                Log.i(TAG, "Đã cài đặt ngôn ngữ Tiếng Việt (vi-VN) cho TTS thành công")
            }

            tts?.setOnUtteranceProgressListener(utteranceListener)
            isInitialized = true
            flushPendingUtterances()
        } else {
            Log.e(TAG, "Khởi tạo TextToSpeech thất bại với mã lỗi: $status")
            isInitialized = false
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (text.isBlank()) return

        mainHandler.post {
            if (!isInitialized) {
                if (queueMode == TextToSpeech.QUEUE_FLUSH) {
                    pendingUtterances.clear()
                }
                pendingUtterances.add(text to queueMode)
                Log.d(TAG, "TTS chưa sẵn sàng, xếp hàng: \"$text\"")
                return@post
            }
            speakInternal(text, queueMode)
        }
    }

    private fun flushPendingUtterances() {
        if (pendingUtterances.isEmpty()) return
        val queued = pendingUtterances.toList()
        pendingUtterances.clear()
        queued.forEach { (text, mode) -> speakInternal(text, mode) }
    }

    private fun speakInternal(text: String, queueMode: Int) {
        try {
            if (tts == null) {
                tts = TextToSpeech(context.applicationContext, this)
            }

            if (!isLanguageSupported) {
                Log.w(TAG, "Tiếng Việt chưa cài trên thiết bị, thử phát với giọng mặc định: \"$text\"")
            }

            val utteranceId = "TTS_${System.currentTimeMillis()}"
            val result = tts?.speak(text, queueMode, null, utteranceId)
            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "Lỗi phát thoại TTS cho văn bản: $text")
                onSpeakingStateChanged(false)
            } else {
                Log.d(TAG, "🔊 [TTS Speaking]: \"$text\"")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ngoại lệ khi gọi TTS speak: ${e.message}", e)
            onSpeakingStateChanged(false)
        }
    }

    fun stop() {
        mainHandler.post {
            try {
                if (tts?.isSpeaking == true) {
                    tts?.stop()
                }
                onSpeakingStateChanged(false)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi dừng TTS: ${e.message}")
            }
        }
    }

    fun shutdown() {
        mainHandler.post {
            try {
                tts?.stop()
                tts?.shutdown()
                tts = null
                isInitialized = false
                pendingUtterances.clear()
                onSpeakingStateChanged(false)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi giải phóng TTS: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
    }
}
