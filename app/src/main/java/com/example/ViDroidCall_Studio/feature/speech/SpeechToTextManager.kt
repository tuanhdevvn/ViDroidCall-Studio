package com.example.ViDroidCall_Studio.feature.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Quản lý nhận diện giọng nói 100% In-App (SpeechRecognizer), chạy ngầm trực tiếp trong App, không dùng popup Google.
 */
class SpeechToTextManager(
    private val context: Context,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onListeningChanged(isListening: Boolean)
        fun onTextChanged(text: String)
        fun onFinalResult(text: String)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningActive = false

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isListeningActive = true
            mainHandler.post {
                callbacks.onListeningChanged(true)
                callbacks.onTextChanged("") // Reset văn bản khi bắt đầu lắng nghe mới
            }
            Log.d(TAG, "SpeechRecognizer: Sẵn sàng nhận giọng nói trong App")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "SpeechRecognizer: Người dùng bắt đầu nói...")
        }

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            isListeningActive = false
            mainHandler.post {
                callbacks.onListeningChanged(false)
            }
            Log.d(TAG, "SpeechRecognizer: Người dùng đã ngưng nói, đang phân tích...")
        }

        override fun onError(error: Int) {
            isListeningActive = false
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Lỗi thu âm. Vui lòng thử lại."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> PERMISSION_DENIED_MESSAGE
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Lỗi kết nối mạng."
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> ""
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> ""
                else -> ERROR_MESSAGE
            }

            mainHandler.post {
                callbacks.onListeningChanged(false)
                if (errorMessage.isNotEmpty()) {
                    callbacks.onTextChanged(errorMessage)
                }
            }
            Log.w(TAG, "SpeechRecognizer onError: $error ($errorMessage)")
        }

        override fun onResults(results: Bundle?) {
            isListeningActive = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList<String>()

            // Ưu tiên chọn ứng viên (candidate) chứa từ khẩu ngữ nguyên bản (kém, thiếu, rưỡi) nếu Google ITN tự ý đổi số
            val recognized = matches.firstOrNull { candidate ->
                candidate.isNotBlank() && (
                    candidate.contains("kém", ignoreCase = true) ||
                    candidate.contains("thiếu", ignoreCase = true) ||
                    candidate.contains("rưỡi", ignoreCase = true)
                )
            } ?: matches.firstOrNull { it.isNotBlank() }

            mainHandler.post {
                callbacks.onListeningChanged(false)
                if (!recognized.isNullOrBlank()) {
                    Log.i(TAG, "🎤 [Nhận diện thành công 100% In-App]: \"$recognized\"")
                    callbacks.onTextChanged(recognized)
                    callbacks.onFinalResult(recognized)
                } else {
                    Log.w(TAG, "Không nhận diện được từ nào, không gọi model AI.")
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull { it.isNotBlank() }
            if (!partial.isNullOrBlank()) {
                mainHandler.post {
                    callbacks.onTextChanged(partial)
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun ensureRecognizer(): Boolean {
        if (speechRecognizer != null) return true
        return try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                    setRecognitionListener(recognitionListener)
                }
                true
            } else {
                Log.w(TAG, "SpeechRecognizer không khả dụng trên thiết bị này")
                callbacks.onTextChanged("Thiết bị chưa hỗ trợ bộ nhận diện giọng nói.")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khởi tạo SpeechRecognizer: ${e.message}", e)
            false
        }
    }

    fun startListening() {
        mainHandler.post {
            try {
                if (isListeningActive) {
                    stopListening()
                    return@post
                }

                if (!ensureRecognizer()) return@post

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE_VI_VN)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, LANGUAGE_VI_VN)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, LANGUAGE_VI_VN)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    
                    // Cấu hình khoảng lặng để tránh ngắt mic sớm cho người lớn tuổi
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi gọi startListening: ${e.message}", e)
                isListeningActive = false
                callbacks.onListeningChanged(false)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                if (isListeningActive) {
                    speechRecognizer?.stopListening()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi khi gọi stopListening: ${e.message}")
            } finally {
                isListeningActive = false
                callbacks.onListeningChanged(false)
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi khi destroy: ${e.message}")
            } finally {
                speechRecognizer = null
                isListeningActive = false
            }
        }
    }

    companion object {
        private const val TAG = "SpeechToTextManager"
        const val LANGUAGE_VI_VN = "vi-VN"
        const val LISTENING_PLACEHOLDER = "Đang lắng nghe..."
        const val ERROR_MESSAGE = "Không nghe rõ. Vui lòng thử lại."
        const val PERMISSION_DENIED_MESSAGE = "Vui lòng cấp quyền ghi âm để sử dụng micro."
    }
}
