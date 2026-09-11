// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.feature.overlay

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.ViDroidCall_Studio.util.TroLyNoiPermissions

/**
 * Bật/tắt foreground service theo công tắc DataStore. Không tự chạy khi preference false.
 */
object TroLyNoiForegroundController {

    fun sync(context: Context, enabled: Boolean) {
        if (enabled && TroLyNoiPermissions.hasAllRequired(context)) {
            start(context)
        } else {
            stop(context)
        }
    }

    fun start(context: Context) {
        val app = context.applicationContext
        val intent = Intent(app, TroLyNoiForegroundService::class.java)
        ContextCompat.startForegroundService(app, intent)
    }

    fun stop(context: Context) {
        val app = context.applicationContext
        app.stopService(Intent(app, TroLyNoiForegroundService::class.java))
    }
}
