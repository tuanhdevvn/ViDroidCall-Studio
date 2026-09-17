// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.example.ViDroidCall_Studio.feature.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Quản lý tổng hợp giọng nói (Text-To-Speech - TTS) tiếng Việt native trên Android.
 */
class TextToSpeechManager(
    private val context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private data class PendingUtterance(
        val text: String,
        val queueMode: Int,
        val onDone: (() -> Unit)?,
        val onError: (() -> Unit)?
    )

    private data class UtteranceCallbacks(
        val onDone: (() -> Unit)?,
        val onError: (() -> Unit)?
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)

    var isInitialized: Boolean = false
        private set

    var isLanguageSupported: Boolean = false
        private set

    private val pendingUtterances = mutableListOf<PendingUtterance>()
    private val utteranceCallbacks = ConcurrentHashMap<String, UtteranceCallbacks>()

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            mainHandler.post { onSpeakingStateChanged(true) }
        }

        override fun onDone(utteranceId: String?) {
            mainHandler.post {
                onSpeakingStateChanged(false)
                consumeCallbacks(utteranceId)?.onDone?.invoke()
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            mainHandler.post {
                onSpeakingStateChanged(false)
                val cb = consumeCallbacks(utteranceId)
                cb?.onError?.invoke() ?: cb?.onDone?.invoke()
            }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            mainHandler.post {
                onSpeakingStateChanged(false)
                val cb = consumeCallbacks(utteranceId)
                cb?.onError?.invoke() ?: cb?.onDone?.invoke()
            }
        }
    }

    private fun consumeCallbacks(utteranceId: String?): UtteranceCallbacks? {
        if (utteranceId.isNullOrBlank()) return null
        return utteranceCallbacks.remove(utteranceId)
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

    fun speak(
        text: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        onDone: (() -> Unit)? = null,
        onError: (() -> Unit)? = null
    ) {
        if (text.isBlank()) {
            mainHandler.post { onDone?.invoke() }
            return
        }

        mainHandler.post {
            if (!isInitialized) {
                if (queueMode == TextToSpeech.QUEUE_FLUSH) {
                    clearPendingWithError()
                }
                pendingUtterances.add(PendingUtterance(text, queueMode, onDone, onError))
                Log.d(TAG, "TTS chưa sẵn sàng, xếp hàng: \"$text\"")
                return@post
            }
            speakInternal(text, queueMode, onDone, onError)
        }
    }

    private fun clearPendingWithError() {
        val queued = pendingUtterances.toList()
        pendingUtterances.clear()
        queued.forEach { pending ->
            pending.onError?.invoke() ?: pending.onDone?.invoke()
        }
    }

    private fun flushPendingUtterances() {
        if (pendingUtterances.isEmpty()) return
        val queued = pendingUtterances.toList()
        pendingUtterances.clear()
        queued.forEach { pending ->
            speakInternal(pending.text, pending.queueMode, pending.onDone, pending.onError)
        }
    }

    private fun speakInternal(
        text: String,
        queueMode: Int,
        onDone: (() -> Unit)?,
        onError: (() -> Unit)?
    ) {
        try {
            if (tts == null) {
                tts = TextToSpeech(context.applicationContext, this)
            }

            if (!isLanguageSupported) {
                Log.w(TAG, "Tiếng Việt chưa cài trên thiết bị, thử phát với giọng mặc định: \"$text\"")
            }

            if (queueMode == TextToSpeech.QUEUE_FLUSH) {
                clearActiveCallbacksAsStopped()
            }

            val utteranceId = "TTS_${System.currentTimeMillis()}"
            if (onDone != null || onError != null) {
                utteranceCallbacks[utteranceId] = UtteranceCallbacks(onDone, onError)
            }

            val result = tts?.speak(text, queueMode, null, utteranceId)
            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "Lỗi phát thoại TTS cho văn bản: $text")
                onSpeakingStateChanged(false)
                val cb = utteranceCallbacks.remove(utteranceId)
                cb?.onError?.invoke() ?: cb?.onDone?.invoke()
            } else {
                Log.d(TAG, "🔊 [TTS Speaking]: \"$text\"")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ngoại lệ khi gọi TTS speak: ${e.message}", e)
            onSpeakingStateChanged(false)
            onError?.invoke() ?: onDone?.invoke()
        }
    }

    private fun clearActiveCallbacksAsStopped() {
        val active = utteranceCallbacks.keys.toList()
        active.forEach { id ->
            val cb = utteranceCallbacks.remove(id)
            cb?.onError?.invoke() ?: cb?.onDone?.invoke()
        }
    }

    fun stop() {
        mainHandler.post {
            try {
                clearPendingWithError()
                clearActiveCallbacksAsStopped()
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
                clearPendingWithError()
                clearActiveCallbacksAsStopped()
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
