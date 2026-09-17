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
