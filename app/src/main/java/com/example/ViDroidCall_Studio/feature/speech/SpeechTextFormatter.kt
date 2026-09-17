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

package com.example.ViDroidCall_Studio.feature.speech

import java.util.Locale

/**
 * Chuẩn hóa văn bản hiển thị từ STT (Sherpa-ONNX trả về chữ IN HOA).
 * Chuyển về chữ thường và viết hoa chữ cái đầu mỗi câu.
 */
object SpeechTextFormatter {

    private val VI_LOCALE: Locale = Locale.forLanguageTag("vi-VN")

    fun formatDisplay(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return trimmed

        val lower = trimmed.lowercase(VI_LOCALE)
        return capitalizeSentences(lower)
    }

    private fun capitalizeSentences(text: String): String {
        val result = StringBuilder(text.length)
        var capitalizeNext = true

        for (char in text) {
            when {
                capitalizeNext && char.isLetter() -> {
                    result.append(char.titlecase(VI_LOCALE))
                    capitalizeNext = false
                }
                char in SENTENCE_ENDERS -> {
                    result.append(char)
                    capitalizeNext = true
                }
                else -> {
                    result.append(char)
                    if (!char.isWhitespace()) {
                        capitalizeNext = false
                    }
                }
            }
        }

        return result.toString()
    }

    private val SENTENCE_ENDERS = setOf('.', '!', '?', '…')
}
