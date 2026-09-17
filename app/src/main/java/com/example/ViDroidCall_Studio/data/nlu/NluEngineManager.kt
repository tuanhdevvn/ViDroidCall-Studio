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

package com.example.ViDroidCall_Studio.data.nlu

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.ViDroidCall_Studio.data.model.NluJsonParser
import com.example.ViDroidCall_Studio.data.model.NluResult
import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.coroutines.withTimeoutOrNull
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
    private val loadGate = Any()
    @Volatile private var loadFinished: CompletableDeferred<Boolean>? = null

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
                        var finalParsed = parsed
                        if (finalParsed.intent == "call_contact") {
                            val currentInput = _currentQuery.value
                            if (SAFETY_NET_SEARCH_REGEX.containsMatchIn(currentInput)) {
                                finalParsed = NluResult(
                                    rawJson = finalParsed.rawJson,
                                    intent = "search_web",
                                    status = "success",
                                    riskLevel = "low",
                                    requiresConfirmation = false,
                                    argumentsJson = org.json.JSONObject().put("query", currentInput.trim()).toString(2),
                                    isParsedSuccessfully = true,
                                    slots = mapOf("query" to currentInput.trim())
                                )
                            }
                        }
                        _lastResult.value = finalParsed
                        _nluEvents.tryEmit(finalParsed)
                        _isGenerating.value = false
                        Log.i(TAG, "✅ [100% GGUF Model Output]:\n${finalParsed.rawJson}")
                    }
                    is LlamaHelper.LLMEvent.Error -> {
                        if (_modelState.value is NluModelState.Loading) {
                            _modelState.value = NluModelState.Error(event.message)
                        }
                        val pending = loadFinished
                        if (pending != null && !pending.isCompleted) {
                            pending.complete(false)
                        }
                        val errResult = NluResult.fromError(event.message)
                        _lastResult.value = errResult
                        _nluEvents.tryEmit(errResult)
                        _isGenerating.value = false
                        Log.e(TAG, "[GGUF Model Error]: ${event.message}")
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

    fun isModelReady(): Boolean {
        return isNativeReady &&
            llamaHelper != null &&
            _modelState.value is NluModelState.Ready
    }

    fun isModelLoading(): Boolean = _modelState.value is NluModelState.Loading

    /**
     * Tự động quét file mô hình .gguf trong bộ nhớ thiết bị.
     * @param force true khi user chủ động quét lại; bỏ qua guard model đã sẵn sàng.
     */
    fun autoDetectAndLoadModel(force: Boolean = false) {
        if (!beginLoad(force)) return
        scope.launch(Dispatchers.IO) {
            try {
                val targetFile = findGgufFile()
                if (targetFile != null) {
                    Log.i(
                        TAG,
                        "Tìm thấy file mô hình GGUF tại: ${targetFile.absolutePath} (${targetFile.length() / 1024 / 1024} MB)"
                    )
                    loadNativeModel(targetFile)
                } else {
                    Log.w(TAG, "Không tìm thấy file .gguf trong các thư mục quét")
                    _modelState.value = NluModelState.ModelNotFound
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Lỗi khi nạp file GGUF qua LlamaHelper: ${t.message}", t)
                _modelState.value = NluModelState.Error(
                    "Lỗi nạp GGUF: ${t.localizedMessage ?: t.javaClass.simpleName}"
                )
            }
        }
    }

    fun loadModelFromPath(filePath: String) {
        if (!beginLoad(force = true)) return
        scope.launch(Dispatchers.IO) {
            try {
                loadNativeModel(File(filePath))
            } catch (t: Throwable) {
                Log.e(TAG, "Lỗi khi nạp model từ path: ${t.message}", t)
                _modelState.value = NluModelState.Error(
                    "Không thể nạp mô hình: ${t.localizedMessage ?: t.javaClass.simpleName}"
                )
            }
        }
    }

    private fun beginLoad(force: Boolean): Boolean {
        synchronized(loadGate) {
            if (!NluModelLoadGate.shouldStartLoad(_modelState.value, force, isModelReady())) {
                Log.d(TAG, "Bỏ qua nạp GGUF (state=${_modelState.value}, force=$force)")
                return false
            }
            _modelState.value = NluModelState.Loading
            return true
        }
    }

    private fun findGgufFile(): File? {
        val searchDirs = listOfNotNull(
            context.getExternalFilesDir(null),
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            context.filesDir,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            File("/sdcard/Download"),
            File(Environment.getExternalStorageDirectory(), "Download")
        ).distinctBy { it.absolutePath }

        Log.d(TAG, "Đang quét file .gguf tại ${searchDirs.size} thư mục...")

        for (dir in searchDirs) {
            if (dir.exists() && dir.isDirectory) {
                val file = File(dir, NluConstants.MODEL_FILE_NAME)
                if (file.exists() && file.canRead() && file.length() > 0) {
                    return file
                }
            }
        }
        for (dir in searchDirs) {
            if (dir.exists() && dir.isDirectory) {
                val ggufFile = dir.listFiles()?.firstOrNull {
                    it.isFile && it.name.endsWith(".gguf", ignoreCase = true) && it.length() > 0
                }
                if (ggufFile != null) return ggufFile
            }
        }
        return null
    }

    private suspend fun loadNativeModel(targetFile: File) = withContext(Dispatchers.IO) {
        releaseNativeHelperLocked()
        val done = CompletableDeferred<Boolean>()
        loadFinished = done

        val fileUri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                targetFile
            )
        } catch (e: Exception) {
            Log.w(TAG, "FileProvider error: ${e.message}, dùng Uri.fromFile")
            Uri.fromFile(targetFile)
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
            Log.i(TAG, "[100% GGUF Model Loaded]: ${targetFile.name}")
            if (!done.isCompleted) done.complete(true)
        }
        llamaHelper = helper

        val finished = withTimeoutOrNull(LOAD_TIMEOUT_MS) { done.await() }
        if (finished != true && _modelState.value is NluModelState.Loading) {
            _modelState.value = NluModelState.Error("Không nạp được mô hình GGUF")
            if (!done.isCompleted) done.complete(false)
        }
    }

    /**
     * Giải phóng native GGUF khỏi RAM (LlamaHelper.releaseContext).
     * Dùng khi đóng overlay / không còn cần suy luận — giữ Fast-Path nhẹ.
     */
    fun releaseModel() {
        scope.launch(Dispatchers.IO) {
            try {
                releaseNativeHelperLocked()
                _isGenerating.value = false
                _modelState.value = NluModelState.Uninitialized
                Log.i(TAG, "[GGUF_UNLOAD] Đã giải phóng mô hình GGUF khỏi RAM")
            } catch (t: Throwable) {
                Log.w(TAG, "[GGUF_UNLOAD] Lỗi khi giải phóng GGUF: ${t.message}", t)
                llamaHelper = null
                isNativeReady = false
                _modelState.value = NluModelState.Uninitialized
            }
        }
    }

    private fun releaseNativeHelperLocked() {
        try {
            llamaHelper?.abort()
            llamaHelper?.stopPrediction()
            llamaHelper?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi release LlamaHelper: ${e.message}")
        } finally {
            llamaHelper = null
            isNativeReady = false
        }
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
        private const val LOAD_TIMEOUT_MS = 180_000L
        private val SAFETY_NET_SEARCH_REGEX = Regex(
            "(?:\\b(?:là\\s+ai|la\\s+ai|là\\s+gì|la\\s+gi|là\\s+người\\s+nào|la\\s+nguoi\\s+nao|nghĩa\\s+là\\s+gì|nghia\\s+la\\s+gi)\\b)",
            RegexOption.IGNORE_CASE
        )
    }
}
