// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.feature.assistant

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager

/**
 * Tiện ích kiểm tra trạng thái màn hình / khóa cho Trợ lý nổi (wake word).
 */
object TroLyNoiAssistantHelper {

    /**
     * Kiểm tra màn hình có đang sáng và thiết bị đã mở khóa hay không.
     * Dùng để tạm ngắt mic ngầm khi máy tắt màn hình hoặc bị khóa.
     */
    fun isScreenInteractiveAndUnlocked(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isInteractive = powerManager?.isInteractive ?: true
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = keyguardManager?.isKeyguardLocked ?: false
        return isInteractive && !isLocked
    }
}
