// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.feature.overlay

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Foreground service giữ process khi Trợ lý nổi bật.
 * Issue này chưa ghi âm / KWS; chỉ khai báo khung service.
 */
class TroLyNoiForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
