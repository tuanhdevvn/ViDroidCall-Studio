// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.feature.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.ViDroidCall_Studio.MainActivity
import com.example.ViDroidCall_Studio.R
import com.example.ViDroidCall_Studio.data.local.TroLyNoiPreferences
import com.example.ViDroidCall_Studio.feature.assistant.TroLyNoiAssistantHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Quản lý tiến trình chạy nền, điều phối cửa sổ Trợ lý nổi SYSTEM_ALERT_WINDOW
 * và lắng nghe từ khóa 'Trợ lý ơi' (Wake Word KWS).
 */
class TroLyNoiForegroundService : Service() {

    private var overlayManager: TroLyNoiOverlayManager? = null
    private var wakeWordManager: TroLyNoiWakeWordManager? = null
    private var screenReceiver: BroadcastReceiver? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isWakeWordActive = false
    @Volatile private var isBeingDestroyed = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "[SERVICE_RESTART] Khởi tạo TroLyNoiForegroundService")
        createChannel()
        // Gọi promoteToForeground ngay đầu onCreate để tránh ForegroundServiceDidNotStartInTimeException
        promoteToForeground()

        overlayManager = TroLyNoiOverlayManager(this).apply {
            onDismissListener = {
                Log.i(TAG, "[ASSISTANT_CLOSE] Overlay đã đóng, kiểm tra tiếp tục Wake Word")
                if (isWakeWordActive) {
                    wakeWordManager?.resume(retryCount = 3)
                }
            }
            // Không warm-up trước: STT/TTS/NLU chỉ nạp khi Overlay thực sự hiển thị (Lazy Init)
        }

        wakeWordManager = TroLyNoiWakeWordManager(
            context = this,
            canListen = { overlayManager?.isShowing != true },
            onWakeWordDetected = { extractedCommand ->
                overlayManager?.showAssistant(extractedCommand)
            }
        )

        registerScreenReceiver()

        // Bật master = bật Wake Word; Service lắng nghe trực tiếp enabledFlow (1 công tắc duy nhất)
        val prefs = TroLyNoiPreferences(this)
        serviceScope.launch {
            prefs.enabledFlow.collect { enabled ->
                isWakeWordActive = enabled
                Log.d(TAG, "Master switch → Wake Word: $enabled")
                if (enabled) {
                    wakeWordManager?.start()
                    // Warm-up so le (Staggered Warm-up): Đợi 2s để WakeWord nạp xong và giải tỏa CPU,
                    // sau đó nạp trước mô hình STT ngầm để khi người dùng nói "Trợ lý ơi" là mic bật tức thì (<100ms)
                    serviceScope.launch(Dispatchers.Default) {
                        delay(2000)
                        if (!isBeingDestroyed && isWakeWordActive) {
                            Log.d(TAG, "Bắt đầu warm-up trước mô hình STT trong nền...")
                            overlayManager?.ensureComponentsInitialized()
                        }
                    }
                } else {
                    wakeWordManager?.stop()
                }
                updateNotification()
            }
        }

        // Watchdog định kỳ tự động kiểm tra và phục hồi Wake Word listener nếu bị ngắt bất thường
        serviceScope.launch {
            while (isActive) {
                delay(8_000)
                if (!isBeingDestroyed &&
                    isWakeWordActive &&
                    overlayManager?.isShowing != true &&
                    TroLyNoiAssistantHelper.isScreenInteractiveAndUnlocked(this@TroLyNoiForegroundService) &&
                    wakeWordManager?.isLoopActive != true
                ) {
                    Log.i(TAG, "[WATCHDOG_HEAL] Phát hiện Wake Word listener ngưng hoạt động bất thường, đang tự khôi phục...")
                    wakeWordManager?.resume(retryCount = 2)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForeground()

        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> {
                Log.i(TAG, "[ASSISTANT_OPEN] Nhận yêu cầu mở trợ lý từ intent action")
                wakeWordManager?.pause()

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
                    val initialQuery = intent.getStringExtra(EXTRA_QUERY)
                    overlayManager?.showAssistant(initialQuery)
                }
            }

            ACTION_DISMISS_OVERLAY -> {
                Log.i(TAG, "[ASSISTANT_CLOSE] Nhận yêu cầu đóng trợ lý từ intent action")
                overlayManager?.dismiss()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        isBeingDestroyed = true
        Log.i(TAG, "[SERVICE_RESTART] TroLyNoiForegroundService onDestroy")
        serviceScope.cancel()
        unregisterScreenReceiverSafely()

        wakeWordManager?.release()
        wakeWordManager = null

        overlayManager?.destroy()
        overlayManager = null

        super.onDestroy()
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        Log.i(TAG, "[SCREEN_OFF] Màn hình tắt, tạm dừng Wake Word")
                        wakeWordManager?.pause()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        val isUnlocked = TroLyNoiAssistantHelper.isScreenInteractiveAndUnlocked(this@TroLyNoiForegroundService)
                        if (isUnlocked) {
                            Log.i(TAG, "[SCREEN_ON] Màn hình sáng và đã mở khóa")
                            if (isWakeWordActive) {
                                wakeWordManager?.resume(retryCount = 2)
                            }
                        } else {
                            Log.i(TAG, "[DEVICE_LOCKED] Màn hình sáng nhưng thiết bị đang khóa")
                        }
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        Log.i(TAG, "[DEVICE_UNLOCKED] Người dùng đã mở khóa thiết bị")
                        if (isWakeWordActive) {
                            wakeWordManager?.resume(retryCount = 3)
                        }
                    }
                }
            }
        }
        registerReceiver(screenReceiver, filter)
    }

    private fun unregisterScreenReceiverSafely() {
        screenReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi khi hủy đăng ký screenReceiver: ${e.message}")
            }
            screenReceiver = null
        }
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

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildWaitingNotification())
    }

    private fun buildWaitingNotification(): Notification {
        val showOverlayIntent = Intent(this, TroLyNoiForegroundService::class.java).apply {
            action = ACTION_SHOW_OVERLAY
        }
        val showOverlayPending = PendingIntent.getService(
            this,
            1,
            showOverlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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

        val contentText = if (isWakeWordActive) {
            "Đang lắng nghe 'Trợ lý ơi' • Chạm để nói câu lệnh"
        } else {
            "Chạm để nói câu lệnh. Vuốt xuống để chọn Cài đặt."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ViDroidCall: Trợ lý nổi sẵn sàng")
            .setContentText(contentText)
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
