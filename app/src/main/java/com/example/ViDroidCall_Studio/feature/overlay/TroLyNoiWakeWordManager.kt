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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Quản lý lắng nghe từ khóa kích hoạt "Trợ lý ơi" (Wake Word Spotting) chạy nền.
 * Tối ưu hóa tiêu thụ pin với Silero VAD, tự động tạm dừng khi màn hình tắt/khóa hoặc khi Overlay đang mở.
 */
class TroLyNoiWakeWordManager(
    private val context: Context,
    private val canListen: () -> Boolean = { true },
    private val onWakeWordDetected: (extractedCommand: String?) -> Unit,
) {
    private val isRunning = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private val isLoopRunning = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    val isLoopActive: Boolean
        get() = isLoopRunning.get()

    private var audioRecord: AudioRecord? = null
    private var recognizer: OfflineRecognizer? = null
    private var vad: Vad? = null
    private var isModelLoaded = false
    @Volatile private var loopFinishedLatch: CountDownLatch? = null
    private val isReleased = AtomicBoolean(false)

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
            Log.w(TAG, "[WAKE_WORD_STOP] Không thể chạy Wake Word vì chưa có quyền RECORD_AUDIO")
            return
        }

        isRunning.set(true)
        isPaused.set(false)

        if (!TroLyNoiAssistantHelper.isScreenInteractiveAndUnlocked(context) || !canListen()) {
            Log.i(TAG, "[DEVICE_LOCKED] Thiết bị đang tắt màn hình, bị khóa hoặc không đủ điều kiện nghe, tạm hoãn lắng nghe Wake Word.")
            isPaused.set(true)
            return
        }

        executor.execute {
            initModelIfNeeded()
            startListeningLoop()
        }
    }

    fun stop() {
        isRunning.set(false)
        isPaused.set(false)
        Log.i(TAG, "[WAKE_WORD_STOP] Đã dừng lắng nghe Wake Word.")
        // Không gọi stopAudioRecord() ở đây — để luồng loop thoát tự nhiên và tự dọn trong finally
    }

    fun pause() {
        isPaused.set(true)
        Log.i(TAG, "[WAKE_WORD_STOP] Tạm dừng lắng nghe Wake Word.")
        // Không gọi stopAudioRecord() ở đây — để luồng loop thoát tự nhiên
    }

    fun resume(retryCount: Int = 3) {
        if (!isRunning.get()) {
            start()
            return
        }

        if (!TroLyNoiAssistantHelper.isScreenInteractiveAndUnlocked(context) || !canListen()) {
            Log.i(TAG, "[DEVICE_LOCKED] Màn hình đang tắt hoặc máy đang khóa, hoãn resume Wake Word (retryCount=$retryCount).")
            if (retryCount > 0) {
                mainHandler.postDelayed({
                    if (isRunning.get() && !isLoopRunning.get()) {
                        resume(retryCount - 1)
                    }
                }, 300)
            }
            return
        }

        isPaused.set(false)
        if (!isLoopRunning.get()) {
            Log.i(TAG, "[WAKE_WORD_START] Tiếp tục lắng nghe Wake Word...")
            executor.execute {
                initModelIfNeeded()
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
        if (!isRunning.get() || isPaused.get() || !canListen()) return
        if (!isLoopRunning.compareAndSet(false, true)) {
            Log.d(TAG, "Luồng lắng nghe Wake Word đã đang chạy, bỏ qua lần gọi này.")
            return
        }

        val latch = CountDownLatch(1)
        loopFinishedLatch = latch

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Log.w(TAG, "[WAKE_WORD_STOP] Quyền RECORD_AUDIO không còn khả dụng, dừng lắng nghe.")
            isLoopRunning.set(false)
            stop()
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, 2048)

        try {
            var record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            // Thử lại nếu phần cứng micro vừa được ứng dụng khác hoặc Overlay giải phóng
            var retry = 0
            while (record.state != AudioRecord.STATE_INITIALIZED && retry < 3) {
                record.release()
                Thread.sleep(150)
                record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                retry++
            }

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "[WAKE_WORD_STOP] Khởi tạo AudioRecord cho Wake Word thất bại sau $retry lần thử (hardware mic busy)")
                record.release()
                return
            }

            audioRecord = record
            audioRecord?.startRecording()
            vad?.reset()

            val audioBuffer = ShortArray(512)
            val floatBuffer = FloatArray(512)
            var consecutiveErrors = 0

            Log.i(TAG, "[WAKE_WORD_START] Bắt đầu lắng nghe từ khóa 'Trợ lý ơi' ngầm...")

            while (isRunning.get() && !isPaused.get() && canListen()) {
                val readCount = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                if (readCount < 0) {
                    consecutiveErrors++
                    Log.w(TAG, "[WAKE_WORD_WARN] AudioRecord read error: $readCount (lỗi liên tiếp: $consecutiveErrors)")
                    Thread.sleep(150)
                    if (consecutiveErrors >= 5) {
                        Log.e(TAG, "[WAKE_WORD_ERROR] Quá 5 lỗi đọc mic liên tiếp, ngắt vòng lặp để khởi tạo lại sau.")
                        break
                    }
                    continue
                }
                consecutiveErrors = 0
                if (readCount == 0) {
                    Thread.sleep(20)
                    continue
                }

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
                        val match = parseWakeWordCommand(recognizedText)
                        if (match != null) {
                            Log.i(TAG, "[WAKE_WORD_DETECTED] keyword='${match.matchedKeyword}', hasTrailingCommand=${match.remainingCommand.isNotBlank()}")
                            // Tạm dừng mic ngầm ngay lập tức để nhường mic hoàn toàn cho Overlay STT
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
        } catch (e: SecurityException) {
            Log.e(TAG, "[WAKE_WORD_STOP] Quyền ghi âm bị thu hồi trong runtime: ${e.message}")
            stop()
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong luồng lắng nghe Wake Word: ${e.message}", e)
        } finally {
            isLoopRunning.set(false)
            stopAudioRecord()
            latch.countDown()
            // Tự phục hồi: Nếu trạng thái vẫn là đang chạy, không chủ động pause, và máy đang mở màn hình
            if (isRunning.get() && !isPaused.get() && canListen() && TroLyNoiAssistantHelper.isScreenInteractiveAndUnlocked(context)) {
                Log.i(TAG, "[WAKE_WORD_RECOVERY] Tự động kích hoạt lại Wake Word listener sau 1s...")
                mainHandler.postDelayed({
                    resume()
                }, 1000)
            }
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
        if (!isReleased.compareAndSet(false, true)) return
        isRunning.set(false)
        isPaused.set(false)

        // Chờ vòng lặp kết thúc tự nhiên (tối đa 2 giây) trước khi giải phóng con trỏ C++ native
        val latch = loopFinishedLatch
        if (latch != null && isLoopRunning.get()) {
            try {
                latch.await(2, TimeUnit.SECONDS)
            } catch (_: InterruptedException) { }
        }

        try {
            if (!executor.isShutdown) {
                executor.execute {
                    try {
                        // An toàn: vòng lặp đã kết thúc, không ai đang dùng recognizer/vad
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
                executor.shutdown()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi shutdown executor WakeWord: ${e.message}")
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
