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

/**
 * Các hằng số và ChatML Prompt template theo đặc tả ANDROID_INTEGRATION_SPEC.md
 */
object NluConstants {

    const val MODEL_FILE_NAME = "qwen3-nlu-Q4_K_M.gguf"

    /**
     * System prompt bắt buộc của mô hình Qwen3-0.6B-NLU
     */
    const val MANDATORY_SYSTEM_PROMPT =
        "Bạn là bộ phân tích NLU trích xuất ý định (intent) và tham số (arguments). Các intent hỗ trợ: [set_alarm, set_timer, open_app, open_map, call_contact, send_sms, search_video, play_music, search_web, clarify, greeting, goodbye, unsupported]. search_web dùng để tìm kiếm thông tin trên web (vd: X là ai, X là gì, nghĩa là gì, xuất xứ của X, thời tiết, giá vàng, tin tức, thông tin về X, hôm nay ... mưa không). Chỉ trả về JSON duy nhất: {\"intent\": string, \"arguments\": object, \"risk_level\": \"low\"|\"medium\"|\"high\", \"status\": \"success\"|\"needs_clarification\"|\"invalid\"|\"unsupported\", \"requires_confirmation\": boolean}."

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
