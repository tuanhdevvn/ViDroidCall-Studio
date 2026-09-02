package com.example.ViDroidCall_Studio.feature.speech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.example.ViDroidCall_Studio.util.SystemSoundHelper
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Quản lý nhận diện giọng nói tiếng Việt 100% Offline cục bộ bằng Sherpa-ONNX Zipformer & Silero VAD.
 * Sử dụng cơ chế VAD Endpoint chuẩn xác để giải mã trọn vẹn ngữ cảnh câu nói, tránh hiện tượng đoán sai từ dở dang.
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
    private val executor = Executors.newSingleThreadExecutor()

    private var recognizer: OfflineRecognizer? = null
    private var vad: Vad? = null
    private var audioRecord: AudioRecord? = null

    private val isListeningActive = AtomicBoolean(false)
    private val isCancelled = AtomicBoolean(false)
    private var isModelInitialized = false
    private var isInitializingModel = false
    private var lastActionTimestamp = 0L

    init {
        // Khởi tạo trước mô hình trên background thread để giảm thiểu độ trễ lần đầu bấm mic
        initModelAsync()
    }

    private fun initModelAsync(onComplete: (() -> Unit)? = null) {
        if (isModelInitialized) {
            onComplete?.invoke()
            return
        }
        if (isInitializingModel) return
        isInitializingModel = true

        executor.execute {
            try {
                Log.d(TAG, "Đang nạp mô hình Sherpa-ONNX Zipformer tiếng Việt & Silero VAD...")
                val assetManager = context.assets

                // 1. Cấu hình OfflineRecognizer (Zipformer 30M Int8 Tiếng Việt)
                val recognizerConfig = OfflineRecognizerConfig(
                    featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
                    modelConfig = OfflineModelConfig(
                        transducer = OfflineTransducerModelConfig(
                            encoder = "sherpa-onnx-vi/encoder.int8.onnx",
                            decoder = "sherpa-onnx-vi/decoder.onnx",
                            joiner = "sherpa-onnx-vi/joiner.int8.onnx"
                        ),
                        tokens = "sherpa-onnx-vi/tokens.txt",
                        bpeVocab = "sherpa-onnx-vi/bpe.model",
                        numThreads = 2,
                        debug = false,
                        provider = "cpu",
                        modelType = "zipformer2"
                    )
                )
                recognizer = OfflineRecognizer(assetManager = assetManager, config = recognizerConfig)

                // 2. Cấu hình VAD (Silero VAD)
                val vadConfig = VadModelConfig(
                    sileroVadModelConfig = SileroVadModelConfig(
                        model = "sherpa-onnx-vi/silero_vad.onnx",
                        threshold = 0.5f,
                        minSilenceDuration = 0.7f, // 700ms im lặng để ngắt câu lệnh dứt khoát
                        minSpeechDuration = 0.2f,
                        windowSize = 512,
                        maxSpeechDuration = 15.0f
                    ),
                    sampleRate = SAMPLE_RATE,
                    numThreads = 1,
                    provider = "cpu",
                    debug = false
                )
                vad = Vad(assetManager = assetManager, config = vadConfig)

                isModelInitialized = true
                Log.i(TAG, "Sherpa-ONNX & Silero VAD đã sẵn sàng 100% Offline!")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi khởi tạo Sherpa-ONNX: ${e.message}", e)
            } finally {
                isInitializingModel = false
                mainHandler.post {
                    onComplete?.invoke()
                }
            }
        }
    }

    fun startListening() {
        val now = System.currentTimeMillis()
        if (now - lastActionTimestamp < 300L) {
            Log.d(TAG, "Bỏ qua yêu cầu startListening do thao tác quá nhanh (< 300ms)")
            return
        }
        lastActionTimestamp = now

        if (isListeningActive.get()) {
            stopListening()
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            callbacks.onTextChanged(PERMISSION_DENIED_MESSAGE)
            callbacks.onListeningChanged(false)
            return
        }

        SystemSoundHelper.playMicStartSound(context)

        isCancelled.set(false)
        isListeningActive.set(true)
        callbacks.onListeningChanged(true)
        callbacks.onTextChanged(WAITING_PLACEHOLDER)

        initModelAsync {
            if (!isModelInitialized || recognizer == null || vad == null) {
                Log.e(TAG, "Không thể khởi động STT vì mô hình chưa nạp thành công.")
                isListeningActive.set(false)
                callbacks.onListeningChanged(false)
                callbacks.onTextChanged("Lỗi nạp mô hình nhận dạng giọng nói.")
                return@initModelAsync
            }

            executor.execute {
                startAudioRecordingLoop()
            }
        }
    }

    private fun startAudioRecordingLoop() {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, 2048)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord khởi tạo thất bại!")
                handleStopListeningInternal()
                return
            }

            audioRecord?.startRecording()
            vad?.reset()

            val audioBuffer = ShortArray(512)
            val floatBuffer = FloatArray(512)
            var speechDetected = false

            Log.d(TAG, "Bắt đầu thu âm PCM 16kHz & phát hiện giọng nói On-Device...")

            while (isListeningActive.get() && !isCancelled.get()) {
                val readCount = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                if (readCount <= 0) continue

                // Chuyển short PCM (-32768..32767) sang float (-1.0f..1.0f)
                for (i in 0 until readCount) {
                    floatBuffer[i] = audioBuffer[i] / 32768.0f
                }

                val curVad = vad ?: break
                curVad.acceptWaveform(if (readCount == floatBuffer.size) floatBuffer else floatBuffer.copyOf(readCount))

                // Kiểm tra trạng thái phát hiện tiếng nói
                if (curVad.isSpeechDetected()) {
                    if (!speechDetected) {
                        speechDetected = true
                        mainHandler.post {
                            if (isListeningActive.get() && !isCancelled.get()) {
                                callbacks.onTextChanged(LISTENING_PLACEHOLDER)
                            }
                        }
                    }
                }

                // Khi VAD phát hiện đã nói xong một đoạn trọn vẹn
                while (!curVad.empty()) {
                    val segment = curVad.front()
                    curVad.pop()

                    if (isCancelled.get()) break

                    Log.d(TAG, "VAD đã phát hiện câu hoàn chỉnh (${segment.samples.size} mẫu), đang giải mã...")
                    val curRecognizer = recognizer ?: break
                    val stream = curRecognizer.createStream()
                    stream.acceptWaveform(segment.samples, SAMPLE_RATE)
                    curRecognizer.decode(stream)
                    val result = curRecognizer.getResult(stream)
                    val recognizedText = result.text.trim()
                    stream.release()

                    if (recognizedText.isNotBlank()) {
                        val normalizedText = VietnameseNumberNormalizer.normalize(recognizedText)
                        Log.i(TAG, "🎤 [Sherpa-ONNX 100% Offline Raw]: \"$recognizedText\" -> Normalized: \"$normalizedText\"")
                        mainHandler.post {
                            callbacks.onTextChanged(normalizedText)
                            callbacks.onFinalResult(normalizedText)
                        }
                        SystemSoundHelper.playMicStopSound(context)
                        handleStopListeningInternal()
                        return
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong luồng thu âm: ${e.message}", e)
        } finally {
            handleStopListeningInternal()
        }
    }

    fun stopListening() {
        if (!isListeningActive.get()) return
        SystemSoundHelper.playMicStopSound(context)
        isListeningActive.set(false)
        executor.execute {
            try {
                vad?.flush()
                val curVad = vad
                val curRecognizer = recognizer
                if (curVad != null && curRecognizer != null && !isCancelled.get()) {
                    while (!curVad.empty()) {
                        val segment = curVad.front()
                        curVad.pop()
                        val stream = curRecognizer.createStream()
                        stream.acceptWaveform(segment.samples, SAMPLE_RATE)
                        curRecognizer.decode(stream)
                        val result = curRecognizer.getResult(stream)
                        val recognizedText = result.text.trim()
                        stream.release()
                        if (recognizedText.isNotBlank()) {
                            val normalizedText = VietnameseNumberNormalizer.normalize(recognizedText)
                            mainHandler.post {
                                callbacks.onTextChanged(normalizedText)
                                callbacks.onFinalResult(normalizedText)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi khi flush VAD: ${e.message}")
            } finally {
                handleStopListeningInternal()
            }
        }
    }

    fun cancelListening() {
        if (isListeningActive.get()) {
            SystemSoundHelper.playMicStopSound(context)
        }
        isCancelled.set(true)
        isListeningActive.set(false)
        executor.execute {
            handleStopListeningInternal()
            mainHandler.post {
                callbacks.onListeningChanged(false)
                callbacks.onTextChanged("")
            }
        }
    }

    private fun handleStopListeningInternal() {
        isListeningActive.set(false)
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi giải phóng AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
        }
        mainHandler.post {
            callbacks.onListeningChanged(false)
        }
    }

    fun destroy() {
        cancelListening()
        executor.execute {
            try {
                recognizer?.release()
                vad?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi giải phóng Sherpa-ONNX: ${e.message}")
            } finally {
                recognizer = null
                vad = null
                isModelInitialized = false
            }
        }
        executor.shutdown()
    }

    companion object {
        private const val TAG = "SpeechToTextManager"
        private const val SAMPLE_RATE = 16000
        const val LANGUAGE_VI_VN = "vi-VN"
        const val WAITING_PLACEHOLDER = "Hãy nói gì đó..."
        const val LISTENING_PLACEHOLDER = "Đang lắng nghe câu lệnh..."
        const val ERROR_MESSAGE = "Không nghe rõ. Vui lòng thử lại."
        const val PERMISSION_DENIED_MESSAGE = "Vui lòng cấp quyền ghi âm để sử dụng micro."
    }
}
