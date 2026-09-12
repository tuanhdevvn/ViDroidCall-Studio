// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.feature.overlay

import com.example.ViDroidCall_Studio.domain.model.NativeAction

/**
 * Trạng thái của Hộp thoại Trợ lý nổi (TroLyNoSheet)
 */
sealed interface TroLyNoState {

    /**
     * Đang lắng nghe âm thanh từ microphone
     */
    data object Listening : TroLyNoState

    /**
     * Đang nhận diện giọng nói (STT) và hiển thị transcript theo thời gian thực
     */
    data class Stt(
        val text: String
    ) : TroLyNoState

    /**
     * Khớp tức thì với Fast-Path (< 5ms), không cần chờ mô hình AI suy luận
     */
    data class FastPath(
        val text: String,
        val action: NativeAction
    ) : TroLyNoState

    /**
     * Đang phân tích câu lệnh bằng mô hình AI on-device (GGUF)
     */
    data object Analyzing : TroLyNoState

    /**
     * Hộp thoại xác nhận trước khi thực thi các hành động nhạy cảm (call_contact, send_sms, v.v.)
     */
    data class Confirmation(
        val text: String,
        val action: NativeAction
    ) : TroLyNoState

    /**
     * Cần người dùng làm rõ thêm thông tin còn thiếu
     */
    data class Clarify(
        val text: String,
        val missing: List<String>
    ) : TroLyNoState
}
