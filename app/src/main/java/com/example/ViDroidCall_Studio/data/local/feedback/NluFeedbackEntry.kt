package com.example.ViDroidCall_Studio.data.local.feedback

import org.json.JSONObject

/**
 * Một dòng mẫu NLU sai đã lưu trong file JSONL.
 */
data class NluFeedbackEntry(
    val index: Int,
    val sttText: String,
    val modelOutputJson: String,
    val savedAt: Long
) {
    companion object {
        fun fromJsonLine(index: Int, line: String): NluFeedbackEntry? {
            return try {
                val json = JSONObject(line)
                NluFeedbackEntry(
                    index = index,
                    sttText = json.optString("stt_text", ""),
                    modelOutputJson = json.optJSONObject("model_output")?.toString(2) ?: "{}",
                    savedAt = json.optLong("saved_at", 0L)
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
