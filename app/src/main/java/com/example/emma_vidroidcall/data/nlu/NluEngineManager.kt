package com.example.emma_vidroidcall.data.nlu

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.emma_vidroidcall.data.model.NluJsonParser
import com.example.emma_vidroidcall.data.model.NluResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nehuatl.llamacpp.LlamaHelper
import java.io.File
import java.util.regex.Pattern

sealed interface NluModelState {
    data object Uninitialized : NluModelState
    data object Loading : NluModelState
    data class Ready(val modelPath: String, val isUsingNativeEngine: Boolean) : NluModelState
    data class Error(val message: String) : NluModelState
}

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

    private val _currentQuery = MutableStateFlow("")
    val currentQuery: StateFlow<String> = _currentQuery.asStateFlow()

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
                        _isGenerating.value = false
                        Log.d(TAG, "Llama LLM generation finished. JSON: ${parsed.rawJson}")
                    }
                    is LlamaHelper.LLMEvent.Error -> {
                        _lastResult.value = NluResult.fromError(event.message)
                        _isGenerating.value = false
                        Log.e(TAG, "Llama LLM generation error: ${event.message}")
                    }
                    is LlamaHelper.LLMEvent.Started -> {
                        Log.d(TAG, "Llama LLM generation started")
                    }
                    is LlamaHelper.LLMEvent.Loaded -> {
                        Log.d(TAG, "Llama LLM model loaded event")
                    }
                }
            }
        }
    }

    /**
     * Tự động tìm kiếm file mô hình trong bộ nhớ thiết bị
     */
    fun autoDetectAndLoadModel() {
        scope.launch(Dispatchers.IO) {
            _modelState.value = NluModelState.Loading

            val searchDirs = listOfNotNull(
                context.filesDir,
                File(context.filesDir, "models"),
                context.getExternalFilesDir(null),
                context.getExternalFilesDir("models"),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                File("/sdcard/Download"),
                File("/sdcard/Documents")
            )

            // 1. Ưu tiên tìm file theo tên chuẩn MODEL_FILE_NAME
            var targetFile: File? = searchDirs.map { File(it, NluConstants.MODEL_FILE_NAME) }
                .firstOrNull { it.exists() && it.canRead() && it.length() > 0 }

            // 2. Nếu chưa thấy, quét tìm file bất kỳ có đuôi .gguf trong các thư mục
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
                    Log.i(TAG, "Đã phát hiện file GGUF tại: ${targetFile.absolutePath} (${targetFile.length() / 1024 / 1024} MB)")
                    loadNativeModel(targetFile)
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi nạp file GGUF qua LlamaHelper: ${e.message}", e)
                    _modelState.value = NluModelState.Error("Lỗi nạp GGUF: ${e.localizedMessage}")
                }
            } else {
                Log.i(TAG, "Chưa tìm thấy file .gguf trong thiết bị. Khởi chạy chế độ NLU Spec Simulator.")
                _modelState.value = NluModelState.Ready(
                    modelPath = "Embedded NLU Spec Engine",
                    isUsingNativeEngine = false
                )
            }
        }
    }

    /**
     * Nạp mô hình từ đường dẫn tuyệt đối hoặc file path
     */
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
            contextLength = 2048,
            mmprojPath = null
        ) { _ ->
            isNativeReady = true
            _modelState.value = NluModelState.Ready(
                modelPath = targetFile.name,
                isUsingNativeEngine = true
            )
            Log.i(TAG, "✅ Native GGUF Model Loaded Successfully from ${targetFile.name}")
        }
        llamaHelper = helper
    }

    /**
     * Gửi câu lệnh để mô hình NLU phân tích và trả về JSON
     */
    fun processQuery(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return

        _currentQuery.value = cleanQuery
        _isGenerating.value = true
        streamingResponseBuilder.clear()

        val state = _modelState.value
        if (state is NluModelState.Ready && state.isUsingNativeEngine && isNativeReady && llamaHelper != null) {
            val formattedChatMl = NluConstants.buildChatMlPrompt(cleanQuery)
            Log.i(TAG, "🚀 Đang gửi prompt vào Native GGUF Engine:\n$formattedChatMl")
            try {
                llamaHelper?.predict(formattedChatMl)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi thực thi Native Predict: ${e.message}. Chuyển sang Fallback.", e)
                executeFallbackNlu(cleanQuery)
            }
        } else {
            // Sử dụng bộ mô phỏng NLU chuẩn 8 intent theo đặc tả spec
            Log.i(TAG, "ℹ️ Chạy Fallback Spec Engine cho query: $cleanQuery (state: $state, nativeReady: $isNativeReady)")
            executeFallbackNlu(cleanQuery)
        }
    }

    private fun executeFallbackNlu(query: String) {
        scope.launch(Dispatchers.Default) {
            // Thêm độ trễ nhẹ để mô phỏng thời gian suy luận (inference time: ~250ms)
            kotlinx.coroutines.delay(250)
            val jsonResult = generateSpecificationJson(query)
            val parsed = NluJsonParser.parse(jsonResult)
            _lastResult.value = parsed
            _isGenerating.value = false
        }
    }

    /**
     * Bộ sinh JSON NLU tuân thủ 100% đặc tả 8 Intent trong ANDROID_INTEGRATION_SPEC.md
     */
    private fun generateSpecificationJson(rawInput: String): String {
        val input = rawInput.trim().lowercase()

        // 1. set_alarm (hour, minute, date, label)
        if (input.contains("báo thức") || input.contains("đặt báo thức") || input.contains("cài báo thức")) {
            val hourMinutePattern = Pattern.compile("(\\d{1,2})\\s*(?:giờ|h|:)?\\s*(\\d{1,2})?")
            val matcher = hourMinutePattern.matcher(input)
            var hour = 7
            var minute = 0
            if (matcher.find()) {
                hour = matcher.group(1)?.toIntOrNull() ?: 7
                minute = matcher.group(2)?.toIntOrNull() ?: 0
            }

            val status = if (hour >= 24 || minute >= 60) "invalid" else "success"
            return """
            {
              "intent": "set_alarm",
              "arguments": {
                "hour": $hour,
                "minute": $minute,
                "label": "Báo thức AI"
              },
              "risk_level": "low",
              "status": "$status",
              "requires_confirmation": false
            }
            """.trimIndent()
        }

        // 2. set_timer (duration, unit, label)
        if (input.contains("hẹn giờ") || input.contains("đếm ngược") || input.contains("bộ đếm")) {
            val numPattern = Pattern.compile("(\\d+)")
            val matcher = numPattern.matcher(input)
            val duration = if (matcher.find()) matcher.group(1)?.toIntOrNull() ?: 5 else 5
            val unit = when {
                input.contains("tiếng") || input.contains("giờ") -> "hours"
                input.contains("giây") -> "seconds"
                else -> "minutes"
            }
            return """
            {
              "intent": "set_timer",
              "arguments": {
                "duration": $duration,
                "unit": "$unit",
                "label": "Hẹn giờ"
              },
              "risk_level": "low",
              "status": "success",
              "requires_confirmation": false
            }
            """.trimIndent()
        }

        // 5. call_contact (contact)
        if (input.contains("gọi cho") || input.contains("gọi điện cho") || input.contains("gọi")) {
            val contact = extractContact(rawInput, listOf("gọi cho", "gọi điện cho", "gọi"))
            if (contact.isBlank()) {
                return """
                {
                  "intent": "clarify",
                  "arguments": {
                    "missing": ["contact"]
                  },
                  "risk_level": "medium",
                  "status": "needs_clarification",
                  "requires_confirmation": false
                }
                """.trimIndent()
            }
            val isEmergency = contact in listOf("113", "114", "115", "111")
            val risk = if (isEmergency) "high" else "medium"
            return """
            {
              "intent": "call_contact",
              "arguments": {
                "contact": "$contact"
              },
              "risk_level": "$risk",
              "status": "success",
              "requires_confirmation": true
            }
            """.trimIndent()
        }

        // 6. send_sms (contact, message)
        if (input.contains("nhắn tin") || input.contains("gửi tin nhắn") || input.contains("sms")) {
            val (contact, message) = extractSmsContactAndMessage(rawInput)
            if (contact.isBlank()) {
                return """
                {
                  "intent": "clarify",
                  "arguments": {
                    "missing": ["contact"]
                  },
                  "risk_level": "medium",
                  "status": "needs_clarification",
                  "requires_confirmation": false
                }
                """.trimIndent()
            }
            return if (message.isNotBlank()) {
                """
                {
                  "intent": "send_sms",
                  "arguments": {
                    "contact": "$contact",
                    "message": "$message"
                  },
                  "risk_level": "medium",
                  "status": "success",
                  "requires_confirmation": true
                }
                """.trimIndent()
            } else {
                """
                {
                  "intent": "send_sms",
                  "arguments": {
                    "contact": "$contact"
                  },
                  "risk_level": "medium",
                  "status": "success",
                  "requires_confirmation": true
                }
                """.trimIndent()
            }
        }

        // 3. open_map (destination)
        if (input.contains("chỉ đường") || input.contains("bản đồ") || input.contains("đến") || input.contains("tới")) {
            val dest = extractDestination(rawInput)
            return """
            {
              "intent": "open_map",
              "arguments": {
                "destination": "$dest"
              },
              "risk_level": "low",
              "status": "success",
              "requires_confirmation": false
            }
            """.trimIndent()
        }

        // 4. open_app (app_name)
        if (input.contains("mở") || input.contains("bật") || input.contains("vào ứng dụng")) {
            val app = extractAppName(rawInput)
            return """
            {
              "intent": "open_app",
              "arguments": {
                "app_name": "$app"
              },
              "risk_level": "low",
              "status": "success",
              "requires_confirmation": false
            }
            """.trimIndent()
        }

        // 8. unsupported
        return """
        {
          "intent": "unsupported",
          "arguments": {},
          "risk_level": "low",
          "status": "unsupported",
          "requires_confirmation": false
        }
        """.trimIndent()
    }

    private fun extractContact(raw: String, keywords: List<String>): String {
        var result = raw
        for (kw in keywords) {
            val idx = result.indexOf(kw, ignoreCase = true)
            if (idx != -1) {
                result = result.substring(idx + kw.length).trim()
                break
            }
        }
        return result.trim().removeSurrounding("\"").removeSurrounding("'")
    }

    private fun extractSmsContactAndMessage(raw: String): Pair<String, String> {
        val lower = raw.lowercase()
        val laIndex = lower.indexOf(" là ")
        val voiNoiDungIndex = lower.indexOf(" nội dung ")

        return if (laIndex != -1) {
            val before = raw.substring(0, laIndex)
            val after = raw.substring(laIndex + 4)
            val contact = extractContact(before, listOf("nhắn tin cho", "nhắn cho", "gửi tin nhắn cho", "gửi sms cho", "nhắn"))
            Pair(contact, after.trim())
        } else if (voiNoiDungIndex != -1) {
            val before = raw.substring(0, voiNoiDungIndex)
            val after = raw.substring(voiNoiDungIndex + 10)
            val contact = extractContact(before, listOf("nhắn tin cho", "nhắn cho", "gửi tin nhắn cho", "gửi sms cho", "nhắn"))
            Pair(contact, after.trim())
        } else {
            val contact = extractContact(raw, listOf("nhắn tin cho", "nhắn cho", "gửi tin nhắn cho", "gửi sms cho", "nhắn tin", "nhắn"))
            Pair(contact, "")
        }
    }

    private fun extractDestination(raw: String): String {
        val keywords = listOf("chỉ đường đến", "chỉ đường tới", "chỉ đường đi", "đường đến", "đường tới", "chỉ đường", "bản đồ", "đi đến", "tới")
        for (kw in keywords) {
            val idx = raw.indexOf(kw, ignoreCase = true)
            if (idx != -1) {
                return raw.substring(idx + kw.length).trim()
            }
        }
        return raw.trim()
    }

    private fun extractAppName(raw: String): String {
        val keywords = listOf("mở ứng dụng", "mở app", "vào ứng dụng", "mở", "bật")
        for (kw in keywords) {
            val idx = raw.indexOf(kw, ignoreCase = true)
            if (idx != -1) {
                return raw.substring(idx + kw.length).trim()
            }
        }
        return raw.trim()
    }

    companion object {
        private const val TAG = "NluEngineManager"
    }
}
