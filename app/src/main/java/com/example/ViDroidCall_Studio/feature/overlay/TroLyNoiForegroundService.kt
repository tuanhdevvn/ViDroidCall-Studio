// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.feature.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.ViDroidCall_Studio.MainActivity
import com.example.ViDroidCall_Studio.R

/**
 * Quản lý tiến trình chạy nền và điều phối cửa sổ Trợ lý nổi SYSTEM_ALERT_WINDOW.
 */
class TroLyNoiForegroundService : Service() {

    private var overlayManager: TroLyNoiOverlayManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        overlayManager = TroLyNoiOverlayManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForeground()

        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> {
                val stateName = intent.getStringExtra(EXTRA_STATE)
                if (stateName != null) {
                    try {
                        val state = AssistantOverlayState.valueOf(stateName)
                        val query = intent.getStringExtra(EXTRA_QUERY) ?: when (state) {
                            AssistantOverlayState.FAST_PATH -> "Gọi cho mẹ"
                            AssistantOverlayState.CONFIRM_CALL -> "Gọi cho mẹ"
                            AssistantOverlayState.ANALYZING,
                            AssistantOverlayState.GGUF_LOADING,
                            AssistantOverlayState.MAP_CONFIRM -> "Tìm giúp tôi đường đi bệnh viện Bạch Mai"
                            else -> ""
                        }
                        val intentName = intent.getStringExtra(EXTRA_INTENT) ?: when (state) {
                            AssistantOverlayState.MAP_CONFIRM -> "open_map"
                            else -> "call_contact"
                        }
                        val target = intent.getStringExtra(EXTRA_TARGET) ?: when (state) {
                            AssistantOverlayState.CONFIRM_CALL -> "mẹ"
                            AssistantOverlayState.MAP_CONFIRM -> "bệnh viện Bạch Mai"
                            else -> ""
                        }
                        val sourceLabel = intent.getStringExtra(EXTRA_SOURCE) ?: when (state) {
                            AssistantOverlayState.MAP_CONFIRM -> "🧠 On-Device AI (GGUF)"
                            else -> "⚡ Fast-Path"
                        }

                        val data = AssistantOverlayData(
                            state = state,
                            recognizedText = query,
                            intentName = intentName,
                            targetName = target,
                            sourceLabel = sourceLabel,
                            onConfirm = {
                                overlayManager?.dismiss()
                            },
                            onCancel = {
                                overlayManager?.dismiss()
                            }
                        )
                        overlayManager?.showWithData(data)
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi parse EXTRA_STATE: $stateName", e)
                        overlayManager?.showAssistant()
                    }
                } else {
                    overlayManager?.showAssistant()
                }
            }

            ACTION_DISMISS_OVERLAY -> {
                overlayManager?.dismiss()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        overlayManager?.dismiss()
        overlayManager = null
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Trợ lý nổi",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Thông báo khi Trợ lý nổi đang chạy nền"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun promoteToForeground() {
        val notification = buildWaitingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildWaitingNotification(): Notification {
        // Hành động mở Trợ lý nổi khi chạm vào thông báo
        val showOverlayIntent = Intent(this, TroLyNoiForegroundService::class.java).apply {
            action = ACTION_SHOW_OVERLAY
        }
        val showOverlayPending = PendingIntent.getService(
            this,
            1,
            showOverlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Hành động mở Cài đặt
        val settingsIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_SETTINGS, true)
        }
        val settingsPending = PendingIntent.getActivity(
            this,
            0,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ViDroidCall: Trợ lý nổi sẵn sàng")
            .setContentText("Chạm để nói câu lệnh. Vuốt xuống để chọn Cài đặt.")
            .setContentIntent(showOverlayPending)
            .addAction(0, "Nói câu lệnh", showOverlayPending)
            .addAction(0, "Cài đặt", settingsPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    companion object {
        private const val TAG = "TroLyNoiForegroundService"
        const val CHANNEL_ID = "tro_ly_noi_waiting"
        const val NOTIFICATION_ID = 51

        const val ACTION_SHOW_OVERLAY = "com.example.ViDroidCall_Studio.ACTION_SHOW_OVERLAY"
        const val ACTION_DISMISS_OVERLAY = "com.example.ViDroidCall_Studio.ACTION_DISMISS_OVERLAY"

        const val EXTRA_STATE = "extra_state"
        const val EXTRA_QUERY = "extra_query"
        const val EXTRA_INTENT = "extra_intent"
        const val EXTRA_TARGET = "extra_target"
        const val EXTRA_SOURCE = "extra_source"
    }
}
