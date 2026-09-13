package com.example.ViDroidCall_Studio.feature.overlay

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
import com.example.ViDroidCall_Studio.feature.assistant.TroLyNoiAssistantHelper
import com.example.ViDroidCall_Studio.util.SystemSoundHelper
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Quản lý lắng nghe từ khóa kích hoạt "Trợ lý ơi" (Wake Word Spotting) chạy nền.
 * Tối ưu hóa tiêu thụ pin với Silero VAD, tự động tạm dừng khi màn hình tắt/khóa hoặc khi Overlay đang mở.
 */
class TroLyNoiWakeWordManager(
    private val context: Context,
    private val onWakeWordDetected: (extractedCommand: String?) -> Unit,
) {
    private val isRunning = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var audioRecord: AudioRecord? = null
    private var recognizer: OfflineRecognizer? = null
    private var vad: Vad? = null
    private var isModelLoaded = false

    fun start() {
        if (isRunning.get()) {
            resume()
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "Không thể chạy Wake Word vì chưa có quyền RECORD_AUDIO")
            return
        }

        isRunning.set(true)
        isPaused.set(false)

        executor.execute {
            initModelIfNeeded()
            startListeningLoop()
        }
    }

    fun stop() {
        isRunning.set(false)
        isPaused.set(false)
        stopAudioRecord()
    }

    fun pause() {
        isPaused.set(true)
        stopAudioRecord()
    }

    fun resume() {
        if (!isRunning.get()) return
        if (!TroLyNoiAssistantHelper.isScreenInteractiveAndUnlocked(context)) {
            Log.d(TAG, "Màn hình đang tắt hoặc khóa, không resume Wake Word.")
            return
        }

        if (isPaused.compareAndSet(true, false)) {
            executor.execute {
                startListeningLoop()
            }
        }
    }

    private fun initModelIfNeeded() {
        if (isModelLoaded && recognizer != null && vad != null) return
        try {
            Log.d(TAG, "Nạp mô hình Sherpa-ONNX & Silero VAD cho Wake Word listener...")
            val assetManager = context.assets

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
                    numThreads = 1, // Tiết kiệm CPU cho luồng ngầm
                    debug = false,
                    provider = "cpu",
                    modelType = "zipformer2"
                )
            )
            recognizer = OfflineRecognizer(assetManager = assetManager, config = recognizerConfig)

            val vadConfig = VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = "sherpa-onnx-vi/silero_vad.onnx",
                    threshold = 0.5f,
                    minSilenceDuration = 0.5f,
                    minSpeechDuration = 0.25f,
                    windowSize = 512,
                    maxSpeechDuration = 8.0f
                ),
                sampleRate = SAMPLE_RATE,
                numThreads = 1,
                provider = "cpu",
                debug = false
            )
            vad = Vad(assetManager = assetManager, config = vadConfig)
            isModelLoaded = true
            Log.i(TAG, "Mô hình Wake Word đã nạp thành công.")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi nạp mô hình Wake Word: ${e.message}", e)
        }
    }

    private fun startListeningLoop() {
        if (!isRunning.get() || isPaused.get()) return

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
                Log.e(TAG, "Khởi tạo AudioRecord cho Wake Word thất bại")
                return
            }

            audioRecord?.startRecording()
            vad?.reset()

            val audioBuffer = ShortArray(512)
            val floatBuffer = FloatArray(512)

            Log.i(TAG, "Bắt đầu lắng nghe từ khóa 'Trợ lý ơi' ngầm...")

            while (isRunning.get() && !isPaused.get()) {
                val readCount = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                if (readCount <= 0) continue

                for (i in 0 until readCount) {
                    floatBuffer[i] = audioBuffer[i] / 32768.0f
                }

                val curVad = vad ?: break
                curVad.acceptWaveform(if (readCount == floatBuffer.size) floatBuffer else floatBuffer.copyOf(readCount))

                while (!curVad.empty()) {
                    val segment = curVad.front()
                    curVad.pop()

                    val curRecognizer = recognizer ?: break
                    val stream = curRecognizer.createStream()
                    stream.acceptWaveform(segment.samples, SAMPLE_RATE)
                    curRecognizer.decode(stream)
                    val result = curRecognizer.getResult(stream)
                    val recognizedText = result.text.trim()
                    stream.release()

                    if (recognizedText.isNotBlank()) {
                        Log.d(TAG, "WakeWord audio decoded: \"$recognizedText\"")
                        val match = parseWakeWordCommand(recognizedText)
                        if (match != null) {
                            Log.i(TAG, "🎉 Phát hiện từ khóa 'Trợ lý ơi'! Lệnh kèm theo: '${match.remainingCommand}'")
                            // Tạm dừng mic ngầm ngay để nhường mic cho Overlay STT
                            pause()

                            mainHandler.post {
                                SystemSoundHelper.playMicStartSound(context)
                                onWakeWordDetected(
                                    if (match.remainingCommand.isNotBlank()) match.remainingCommand else null
                                )
                            }
                            return
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong luồng lắng nghe Wake Word: ${e.message}", e)
        } finally {
            stopAudioRecord()
        }
    }

    private fun stopAudioRecord() {
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi khi dừng AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
        }
    }

    fun release() {
        stop()
        executor.execute {
            try {
                recognizer?.release()
                vad?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi giải phóng tài nguyên WakeWord: ${e.message}")
            } finally {
                recognizer = null
                vad = null
                isModelLoaded = false
            }
        }
    }

    data class WakeWordMatchResult(
        val matchedKeyword: String,
        val remainingCommand: String,
    )

    companion object {
        private const val TAG = "TroLyNoiWakeWord"
        private const val SAMPLE_RATE = 16000

        private val WAKE_KEYWORDS = listOf(
            "trợ lý ơi",
            "trợ lí ơi",
            "alo trợ lý",
            "alo trợ lí",
            "ê trợ lý",
            "ê trợ lí",
            "hey trợ lý",
            "hey trợ lí",
            "vidroidcall ơi",
            "vidroidcall",
            "trợ lý",
            "trợ lí"
        )

        /**
         * Kiểm tra chuỗi văn bản xem có chứa từ khóa kích hoạt hay không.
         * Nếu có, bóc tách và trả về câu lệnh kèm theo (nếu người dùng nói liền một mạch).
         */
        fun parseWakeWordCommand(rawText: String): WakeWordMatchResult? {
            val normalized = rawText.lowercase()
                .replace(Regex("[.,?!;:~]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            for (kw in WAKE_KEYWORDS) {
                if (normalized == kw) {
                    return WakeWordMatchResult(matchedKeyword = kw, remainingCommand = "")
                }
                if (normalized.startsWith("$kw ")) {
                    val remaining = normalized.removePrefix("$kw ").trim()
                    return WakeWordMatchResult(matchedKeyword = kw, remainingCommand = remaining)
                }
                // Hỗ trợ trường hợp câu nói có từ đệm ở trước (VD: "này trợ lý ơi gọi cho mẹ")
                val kwIndex = normalized.indexOf(kw)
                if (kwIndex > 0) {
                    val remaining = normalized.substring(kwIndex + kw.length).trim()
                    return WakeWordMatchResult(matchedKeyword = kw, remainingCommand = remaining)
                }
            }

            return null
        }
    }
}
