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

package com.example.ViDroidCall_Studio.feature.overlay

/**
 * Trạng thái giao diện của Trợ lý nổi theo thiết kế chuẩn Stitch.
 */
enum class AssistantOverlayState {
    LISTENING,
    STT,
    FAST_PATH,
    ANALYZING,
    CONFIRM_CALL,
    MAP_CONFIRM,
    CONFIRM_ACTION,
    /** Phản hồi hội thoại (chào, unsupported…) — 1 nút Nói tiếp / Đóng */
    CONVERSATIONAL_REPLY,
    GGUF_LOADING
}

/**
 * Loại biểu tượng hành động hiển thị trên nút/thẻ xác nhận
 */
enum class OverlayActionIconType {
    CALL,
    SMS,
    OPEN_APP,
    ALARM,
    TIMER,
    MAP,
    SEARCH,
    YOUTUBE,
    MUSIC,
    GENERIC
}

/**
 * Model chứa dữ liệu động hiển thị trên TroLyNoiSheet.
 */
data class AssistantOverlayData(
    val state: AssistantOverlayState = AssistantOverlayState.LISTENING,
    val recognizedText: String = "",
    val intentName: String = "",
    val targetName: String = "",
    val actionTitle: String = "",
    val actionDescription: String = "",
    val actionIconType: OverlayActionIconType = OverlayActionIconType.GENERIC,
    val sourceLabel: String = "",
    val statusMessage: String = "",
    val isTtsSpeaking: Boolean = false,
    val onConfirm: (() -> Unit)? = null,
    val onCancel: (() -> Unit)? = null,
    /** Callback nút hội thoại (Nói tiếp → resumeListening, Đóng → dismiss). */
    val onContinueListening: (() -> Unit)? = null,
    val continueButtonLabel: String = "Nói tiếp"
)
