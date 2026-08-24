package com.example.ViDroidCall_Studio.data.model

import org.json.JSONObject

/**
 * Các Intent được hỗ trợ bởi mô hình Qwen2.5-1.5B-NLU
 */
enum class NluIntent(val value: String, val title: String) {
    SET_ALARM("set_alarm", "Cài báo thức"),
    SET_TIMER("set_timer", "Hẹn giờ đếm ngược"),
    OPEN_MAP("open_map", "Mở bản đồ / Chỉ đường"),
    OPEN_APP("open_app", "Mở ứng dụng"),
    CALL_CONTACT("call_contact", "Gọi điện thoại"),
    SEND_SMS("send_sms", "Gửi tin nhắn"),
    CLARIFY("clarify", "Yêu cầu bổ sung thông tin"),
    UNSUPPORTED("unsupported", "Không hỗ trợ");

    companion object {
        fun fromValue(value: String): NluIntent {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNSUPPORTED
        }
    }
}

/**
 * Trạng thái thực thi của NLU
 */
enum class NluStatus(val value: String, val label: String) {
    SUCCESS("success", "Thành công"),
    NEEDS_CLARIFICATION("needs_clarification", "Cần làm rõ"),
    INVALID("invalid", "Không hợp lệ"),
    UNSUPPORTED("unsupported", "Chưa hỗ trợ");

    companion object {
        fun fromValue(value: String): NluStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNSUPPORTED
        }
    }
}

/**
 * Mức độ rủi ro của tác vụ
 */
enum class NluRiskLevel(val value: String, val label: String) {
    LOW("low", "Thấp"),
    MEDIUM("medium", "Trung bình"),
    HIGH("high", "Cao");

    companion object {
        fun fromValue(value: String): NluRiskLevel {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: LOW
        }
    }
}

/**
 * Kết quả phân tích NLU
 */
data class NluResult(
    val rawJson: String,
    val intent: String,
    val status: String,
    val riskLevel: String,
    val requiresConfirmation: Boolean,
    val argumentsJson: String,
    val isParsedSuccessfully: Boolean = true,
    val errorMessage: String? = null
) {
    val intentEnum: NluIntent get() = NluIntent.fromValue(intent)
    val statusEnum: NluStatus get() = NluStatus.fromValue(status)
    val riskLevelEnum: NluRiskLevel get() = NluRiskLevel.fromValue(riskLevel)

    companion object {
        fun empty(): NluResult = NluResult(
            rawJson = "",
            intent = "",
            status = "",
            riskLevel = "low",
            requiresConfirmation = false,
            argumentsJson = "{}",
            isParsedSuccessfully = false
        )

        fun fromError(error: String): NluResult = NluResult(
            rawJson = "{\n  \"error\": \"$error\"\n}",
            intent = "unsupported",
            status = "unsupported",
            riskLevel = "low",
            requiresConfirmation = false,
            argumentsJson = "{}",
            isParsedSuccessfully = false,
            errorMessage = error
        )
    }
}

/**
 * Trình phân tích chuỗi JSON trả về từ mô hình ngôn ngữ
 */
object NluJsonParser {

    fun parse(rawResponse: String): NluResult {
        try {
            val jsonString = extractJsonString(rawResponse)
            val jsonObject = JSONObject(jsonString)

            val intent = jsonObject.optString("intent", "unsupported")
            val status = jsonObject.optString("status", "unsupported")
            val riskLevel = jsonObject.optString("risk_level", "low")
            val requiresConfirmation = jsonObject.optBoolean("requires_confirmation", false)
            val argsObj = jsonObject.optJSONObject("arguments") ?: JSONObject()

            val prettyJson = jsonObject.toString(2)

            return NluResult(
                rawJson = prettyJson,
                intent = intent,
                status = status,
                riskLevel = riskLevel,
                requiresConfirmation = requiresConfirmation,
                argumentsJson = argsObj.toString(2),
                isParsedSuccessfully = true
            )
        } catch (e: Exception) {
            return NluResult(
                rawJson = rawResponse.trim(),
                intent = "unsupported",
                status = "unsupported",
                riskLevel = "low",
                requiresConfirmation = false,
                argumentsJson = "{}",
                isParsedSuccessfully = false,
                errorMessage = "Không thể parse JSON: ${e.localizedMessage}"
            )
        }
    }

    private fun extractJsonString(text: String): String {
        var clean = text.trim()

        // Loại bỏ markdown code block nếu có ```json ... ```
        if (clean.contains("```")) {
            val startIdx = clean.indexOf("{")
            val endIdx = clean.lastIndexOf("}")
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                return clean.substring(startIdx, endIdx + 1)
            }
        }

        val startIdx = clean.indexOf("{")
        val endIdx = clean.lastIndexOf("}")
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            return clean.substring(startIdx, endIdx + 1)
        }

        return clean
    }
}
