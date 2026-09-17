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
