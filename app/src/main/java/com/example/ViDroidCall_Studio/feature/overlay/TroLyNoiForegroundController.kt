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

    fun showOverlay(
        context: Context,
        state: AssistantOverlayState? = null,
        query: String? = null,
        intentName: String? = null,
        target: String? = null,
        source: String? = null
    ) {
        val app = context.applicationContext
        val intent = Intent(app, TroLyNoiForegroundService::class.java).apply {
            action = TroLyNoiForegroundService.ACTION_SHOW_OVERLAY
            if (state != null) {
                putExtra(TroLyNoiForegroundService.EXTRA_STATE, state.name)
            }
            if (query != null) {
                putExtra(TroLyNoiForegroundService.EXTRA_QUERY, query)
            }
            if (intentName != null) {
                putExtra(TroLyNoiForegroundService.EXTRA_INTENT, intentName)
            }
            if (target != null) {
                putExtra(TroLyNoiForegroundService.EXTRA_TARGET, target)
            }
            if (source != null) {
                putExtra(TroLyNoiForegroundService.EXTRA_SOURCE, source)
            }
        }
        ContextCompat.startForegroundService(app, intent)
    }

    fun dismissOverlay(context: Context) {
        val app = context.applicationContext
        val intent = Intent(app, TroLyNoiForegroundService::class.java).apply {
            action = TroLyNoiForegroundService.ACTION_DISMISS_OVERLAY
        }
        ContextCompat.startForegroundService(app, intent)
    }
}
