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
