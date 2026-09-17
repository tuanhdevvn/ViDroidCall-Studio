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

package com.example.ViDroidCall_Studio.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Quyền chỉ xin khi user bật công tắc Trợ lý nổi: mic, thông báo (API 33+), hiện trên ứng dụng khác.
 */
object TroLyNoiPermissions {

    enum class Gate {
        RECORD_AUDIO,
        POST_NOTIFICATIONS,
        SYSTEM_ALERT_WINDOW,
        NONE,
    }

    fun hasRecordAudio(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun hasAllRequired(context: Context): Boolean {
        return nextMissingPermission(
            hasRecordAudio = hasRecordAudio(context),
            notificationsRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            hasNotifications = hasNotifications(context),
            canDrawOverlays = canDrawOverlays(context),
        ) == Gate.NONE
    }

    /**
     * Bước quyền còn thiếu theo thứ tự: mic → thông báo → overlay.
     */
    fun nextMissingPermission(
        hasRecordAudio: Boolean,
        notificationsRequired: Boolean,
        hasNotifications: Boolean,
        canDrawOverlays: Boolean,
    ): Gate {
        if (!hasRecordAudio) return Gate.RECORD_AUDIO
        if (notificationsRequired && !hasNotifications) return Gate.POST_NOTIFICATIONS
        if (!canDrawOverlays) return Gate.SYSTEM_ALERT_WINDOW
        return Gate.NONE
    }

    fun overlaySettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }
}
