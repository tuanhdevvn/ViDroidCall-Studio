package com.example.ViDroidCall_Studio.data.nlu

/**
 * Các hằng số và ChatML Prompt template theo đặc tả ANDROID_INTEGRATION_SPEC.md
 */
object NluConstants {

    const val GGUF_EXTENSION = ".gguf"

    /**
     * System prompt bắt buộc của mô hình Qwen2.5-1.5B-NLU
     */
    const val MANDATORY_SYSTEM_PROMPT =
        "Bạn là bộ phân tích NLU trích xuất ý định (intent) và tham số (arguments). Các intent hỗ trợ: [set_alarm, set_timer, open_app, open_map, call_contact, send_sms, search_video, play_music, clarify, unsupported]. Chỉ trả về JSON duy nhất: {\"intent\": string, \"arguments\": object, \"risk_level\": \"low\"|\"medium\"|\"high\", \"status\": \"success\"|\"needs_clarification\"|\"invalid\"|\"unsupported\", \"requires_confirmation\": boolean}."

    /**
     * Format prompt theo định dạng ChatML: <|im_start|>system...<|im_end|><|im_start|>user...<|im_end|><|im_start|>assistant
     */
    fun buildChatMlPrompt(userInput: String): String {
        return buildString {
            append("<|im_start|>system\n")
            append(MANDATORY_SYSTEM_PROMPT)
            append("<|im_end|>\n")
            append("<|im_start|>user\n")
            append(userInput.trim())
            append("<|im_end|>\n")
            append("<|im_start|>assistant\n")
        }
    }
}
