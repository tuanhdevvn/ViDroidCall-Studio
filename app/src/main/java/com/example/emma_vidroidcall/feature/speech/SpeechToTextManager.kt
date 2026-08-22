package com.example.emma_vidroidcall.feature.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Quản lý Android SpeechRecognizer — tách biệt khỏi UI Compose.
 */
class SpeechToTextManager(
    context: Context,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onListeningChanged(isListening: Boolean)
        fun onTextChanged(text: String)
        fun onFinalResult(text: String) {}
    }

    private val speechRecognizer: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            callbacks.onListeningChanged(true)
            callbacks.onTextChanged(LISTENING_PLACEHOLDER)
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            callbacks.onListeningChanged(false)
        }

        override fun onError(error: Int) {
            callbacks.onListeningChanged(false)
            callbacks.onTextChanged(ERROR_MESSAGE)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val recognized = matches[0]
                callbacks.onTextChanged(recognized)
                callbacks.onFinalResult(recognized)
            }
            callbacks.onListeningChanged(false)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                callbacks.onTextChanged(matches[0])
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    init {
        speechRecognizer?.setRecognitionListener(recognitionListener)
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE_VI_VN)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        speechRecognizer?.destroy()
    }

    companion object {
        const val LANGUAGE_VI_VN = "vi-VN"
        const val LISTENING_PLACEHOLDER = "Đang lắng nghe..."
        const val ERROR_MESSAGE = "Không nghe rõ. Vui lòng thử lại."
        const val PERMISSION_DENIED_MESSAGE = "Vui lòng cấp quyền ghi âm để sử dụng."
    }
}
