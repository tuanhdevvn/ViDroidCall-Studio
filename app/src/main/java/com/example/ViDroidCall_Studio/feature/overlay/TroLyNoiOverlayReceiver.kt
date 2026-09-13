// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.feature.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver tiếp nhận lệnh mở/đóng Trợ lý nổi từ hệ thống hoặc adb.
 */
class TroLyNoiOverlayReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            TroLyNoiForegroundService.ACTION_SHOW_OVERLAY -> {
                val stateName = intent.getStringExtra(TroLyNoiForegroundService.EXTRA_STATE)
                val state = stateName?.let {
                    runCatching { AssistantOverlayState.valueOf(it) }.getOrNull()
                }
                val query = intent.getStringExtra(TroLyNoiForegroundService.EXTRA_QUERY)
                val intentName = intent.getStringExtra(TroLyNoiForegroundService.EXTRA_INTENT)
                val target = intent.getStringExtra(TroLyNoiForegroundService.EXTRA_TARGET)
                val source = intent.getStringExtra(TroLyNoiForegroundService.EXTRA_SOURCE)

                TroLyNoiForegroundController.showOverlay(
                    context = context,
                    state = state,
                    query = query,
                    intentName = intentName,
                    target = target,
                    source = source
                )
            }

            TroLyNoiForegroundService.ACTION_DISMISS_OVERLAY -> {
                TroLyNoiForegroundController.dismissOverlay(context)
            }
        }
    }
}
