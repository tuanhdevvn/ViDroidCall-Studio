package com.example.ViDroidCall_Studio.data.local.feedback

import android.content.Context
import com.example.ViDroidCall_Studio.data.model.NluResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Lưu mẫu NLU sai dưới dạng JSONL để export và train lại model.
 *
 * Mỗi dòng:
 * {"stt_text":"...", "model_output":{...}, "saved_at":1234567890}
 */
class NluFeedbackLogRepository(context: Context) {

    private val logFile: File = File(
        context.applicationContext.getExternalFilesDir(null),
        LOG_FILE_NAME
    )

    suspend fun append(sttText: String, nluResult: NluResult): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val trimmed = sttText.trim()
                require(trimmed.isNotBlank()) { "STT text trống" }
                require(nluResult.isParsedSuccessfully) { "Kết quả NLU không hợp lệ" }

                val line = buildFeedbackLine(trimmed, nluResult)
                logFile.parentFile?.mkdirs()
                logFile.appendText("$line\n")
            }
        }

    suspend fun readAll(): List<NluFeedbackEntry> = withContext(Dispatchers.IO) {
        if (!logFile.exists()) return@withContext emptyList()
        logFile.readLines()
            .filter { it.isNotBlank() }
            .mapIndexedNotNull { index, line ->
                NluFeedbackEntry.fromJsonLine(index, line)
            }
    }

    suspend fun count(): Int = withContext(Dispatchers.IO) {
        if (!logFile.exists()) return@withContext 0
        logFile.readLines().count { it.isNotBlank() }
    }

    suspend fun deleteByIndex(index: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!logFile.exists()) return@runCatching
            val lines = logFile.readLines().filter { it.isNotBlank() }.toMutableList()
            require(index in lines.indices) { "Index không hợp lệ" }
            lines.removeAt(index)
            if (lines.isEmpty()) {
                logFile.delete()
            } else {
                logFile.writeText(lines.joinToString("\n", postfix = "\n"))
            }
        }
    }

    suspend fun clearAll(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (logFile.exists()) {
                logFile.delete()
            }
        }
    }

    fun getLogFile(): File = logFile

    fun getLogFilePath(): String = logFile.absolutePath

    companion object {
        const val LOG_FILE_NAME = "nlu_feedback_log.jsonl"

        fun buildFeedbackLine(sttText: String, nluResult: NluResult): String {
            val root = JSONObject()
            root.put("stt_text", sttText)
            root.put("model_output", JSONObject(nluResult.rawJson))
            root.put("saved_at", System.currentTimeMillis())
            return root.toString()
        }
    }
}
