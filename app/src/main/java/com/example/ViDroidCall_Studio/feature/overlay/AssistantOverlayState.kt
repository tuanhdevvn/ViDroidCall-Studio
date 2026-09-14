// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

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
    val onCancel: (() -> Unit)? = null
)
