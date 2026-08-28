package com.example.ViDroidCall_Studio.data.nlu

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.ViDroidCall_Studio.data.model.NluJsonParser
import com.example.ViDroidCall_Studio.data.model.NluResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nehuatl.llamacpp.LlamaHelper
import java.io.File

sealed interface NluModelState {
    data object Uninitialized : NluModelState
    data object Loading : NluModelState
    data class Ready(val modelPath: String) : NluModelState
    data object ModelNotFound : NluModelState
    data class Error(val message: String) : NluModelState
}

/**
 * Quản lý NLU Engine: CHỈ DÙNG 100% MÔ HÌNH GGUF NATIVE (Llama.cpp), CHẠY NỀN KHÔNG BLOCK UI THREAD.
 */
class NluEngineManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
) {
    private val _modelState = MutableStateFlow<NluModelState>(NluModelState.Uninitialized)
    val modelState: StateFlow<NluModelState> = _modelState.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _lastResult = MutableStateFlow<NluResult?>(null)
    val lastResult: StateFlow<NluResult?> = _lastResult.asStateFlow()

    private val _nluEvents = MutableSharedFlow<NluResult>(extraBufferCapacity = 64)
    val nluEvents: SharedFlow<NluResult> = _nluEvents.asSharedFlow()

    private val _currentQuery = MutableStateFlow("")
    val currentQuery: StateFlow<String> = _currentQuery.asStateFlow()

    private val fastPathMatcher = FastPathMatcher(context.applicationContext)
    private var llamaHelper: LlamaHelper? = null
    private val llmEventFlow = MutableSharedFlow<LlamaHelper.LLMEvent>(extraBufferCapacity = 64)
    private var isNativeReady = false
    private var streamingResponseBuilder = StringBuilder()

    init {
        setupLlmEventListener()
        autoDetectAndLoadModel()
    }

    private fun setupLlmEventListener() {
        scope.launch {
            llmEventFlow.collect { event ->
                when (event) {
                    is LlamaHelper.LLMEvent.Ongoing -> {
                        streamingResponseBuilder.append(event.word)
                    }
                    is LlamaHelper.LLMEvent.Done -> {
                        val fullResponse = streamingResponseBuilder.toString()
                        val parsed = NluJsonParser.parse(fullResponse)
                        _lastResult.value = parsed
                        _nluEvents.tryEmit(parsed)
                        _isGenerating.value = false
                        Log.i(TAG, "✅ [100% GGUF Model Output]:\n${parsed.rawJson}")
                    }
                    is LlamaHelper.LLMEvent.Error -> {
                        val errResult = NluResult.fromError(event.message)
                        _lastResult.value = errResult
                        _nluEvents.tryEmit(errResult)
                        _isGenerating.value = false
                        Log.e(TAG, "❌ [GGUF Model Error]: ${event.message}")
                    }
                    is LlamaHelper.LLMEvent.Started -> {
                        Log.d(TAG, "🚀 [GGUF Engine] Bắt đầu suy luận...")
                    }
                    is LlamaHelper.LLMEvent.Loaded -> {
                        Log.d(TAG, "🧠 [GGUF Engine] Mô hình đã nạp thành công vào RAM")
                    }
                }
            }
        }
    }

    /**
     * Tự động quét file mô hình .gguf trong bộ nhớ thiết bị
     */
    fun autoDetectAndLoadModel() {
        scope.launch(Dispatchers.IO) {
            _modelState.value = NluModelState.Loading

            // Thư mục quét: Thư mục cá nhân của App (không cần quyền Storage) + Thư mục Download chung của máy
            val searchDirs = listOfNotNull(
                context.getExternalFilesDir(null),
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                context.filesDir,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                File("/sdcard/Download"),
                File(Environment.getExternalStorageDirectory(), "Download")
            ).distinctBy { it.absolutePath }

            Log.d(TAG, "Đang quét file .gguf tại ${searchDirs.size} thư mục...")

            var targetFile: File? = null

            // 1. Ưu tiên tìm file theo tên chuẩn MODEL_FILE_NAME
            for (dir in searchDirs) {
                if (dir.exists() && dir.isDirectory) {
                    val file = File(dir, NluConstants.MODEL_FILE_NAME)
                    if (file.exists() && file.canRead() && file.length() > 0) {
                        targetFile = file
                        break
                    }
                }
            }

            // 2. Nếu chưa thấy, tìm file bất kỳ có đuôi .gguf trong các thư mục
            if (targetFile == null) {
                for (dir in searchDirs) {
                    if (dir.exists() && dir.isDirectory) {
                        val ggufFile = dir.listFiles()?.firstOrNull { 
                            it.isFile && it.name.endsWith(".gguf", ignoreCase = true) && it.length() > 0 
                        }
                        if (ggufFile != null) {
                            targetFile = ggufFile
                            break
                        }
                    }
                }
            }

            if (targetFile != null) {
                try {
                    Log.i(TAG, "🔍 Tìm thấy file mô hình GGUF tại: ${targetFile.absolutePath} (${targetFile.length() / 1024 / 1024} MB)")
                    loadNativeModel(targetFile)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Lỗi khi nạp file GGUF qua LlamaHelper: ${e.message}", e)
                    _modelState.value = NluModelState.Error("Lỗi nạp GGUF: ${e.localizedMessage}")
                }
            } else {
                Log.w(TAG, "⚠️ Không tìm thấy bất kỳ file .gguf nào trong các thư mục quét!")
                _modelState.value = NluModelState.ModelNotFound
            }
        }
    }

    fun loadModelFromPath(filePath: String) {
        scope.launch(Dispatchers.IO) {
            _modelState.value = NluModelState.Loading
            try {
                loadNativeModel(File(filePath))
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi nạp model từ path: ${e.message}", e)
                _modelState.value = NluModelState.Error("Không thể nạp mô hình: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun loadNativeModel(targetFile: File) = withContext(Dispatchers.IO) {
        val fileUri = try {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                targetFile
            )
        } catch (e: Exception) {
            Log.w(TAG, "FileProvider error: ${e.message}, dùng Uri.fromFile")
            android.net.Uri.fromFile(targetFile)
        }
        val fileUriString = fileUri.toString()
        Log.i(TAG, "Khởi tạo LlamaHelper với URI: $fileUriString")

        val helper = LlamaHelper(
            contentResolver = context.contentResolver,
            scope = scope,
            sharedFlow = llmEventFlow
        )
        helper.load(
            path = fileUriString,
            contextLength = 512,
            mmprojPath = null
        ) { _ ->
            isNativeReady = true
            _modelState.value = NluModelState.Ready(
                modelPath = targetFile.name
            )
            Log.i(TAG, "✅ [100% GGUF Model Loaded]: ${targetFile.name}")
        }
        llamaHelper = helper
    }

    /**
     * Phân tích câu lệnh: Ưu tiên Fast-Path (Zero-LLM Latency) cho câu đơn giản/cố định,
     * tự động chuyển sang mô hình GGUF Native khi câu lệnh phức tạp.
     */
    fun processQuery(query: String) {
        try {
            val cleanQuery = query.trim()
            if (cleanQuery.isEmpty()) return

            _currentQuery.value = cleanQuery
            streamingResponseBuilder.clear()

            // 1. Khớp nhanh với bộ quy tắc Fast-Path (< 5ms, không tốn tài nguyên mô hình)
            val fastResult = fastPathMatcher.match(cleanQuery)
            if (fastResult != null) {
                Log.i(TAG, "⚡ [Fast-Path Match (Zero-LLM)]: Intent=${fastResult.intent}, RawJson=${fastResult.rawJson}")
                _isGenerating.value = false
                _lastResult.value = fastResult
                _nluEvents.tryEmit(fastResult)
                return
            }

            // 2. Không khớp quy tắc nhanh -> Gửi Prompt vào Native GGUF Model để suy luận
            _isGenerating.value = true
            val state = _modelState.value
            if (state !is NluModelState.Ready || !isNativeReady || llamaHelper == null) {
                val errResult = NluResult.fromError("Chưa có file mô hình AI (.gguf). Vui lòng đặt file vào thiết bị.")
                _isGenerating.value = false
                _lastResult.value = errResult
                _nluEvents.tryEmit(errResult)
                Log.w(TAG, "Không thể xử lý vì chưa nạp mô hình GGUF (State: $state)")
                return
            }

            val formattedChatMl = NluConstants.buildChatMlPrompt(cleanQuery)
            Log.i(TAG, "🚀 Đang gửi Prompt vào 100% GGUF Native Engine (Background Thread):\n$formattedChatMl")

            scope.launch(Dispatchers.Default) {
                try {
                    llamaHelper?.predict(formattedChatMl)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi thực thi Native Predict: ${e.message}", e)
                    val errResult = NluResult.fromError("Lỗi khi suy luận: ${e.localizedMessage}")
                    _isGenerating.value = false
                    _lastResult.value = errResult
                    _nluEvents.tryEmit(errResult)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong processQuery: ${e.message}", e)
            val errResult = NluResult.fromError("Lỗi xử lý câu lệnh: ${e.localizedMessage}")
            _isGenerating.value = false
            _lastResult.value = errResult
            _nluEvents.tryEmit(errResult)
        }
    }

    companion object {
        private const val TAG = "NluEngineManager"
    }
}
