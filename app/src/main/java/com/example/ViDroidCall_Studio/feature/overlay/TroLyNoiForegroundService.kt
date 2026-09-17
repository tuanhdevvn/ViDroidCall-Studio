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
import android.os.PowerManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
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

    private var wakeLock: PowerManager.WakeLock? = null
    @Volatile private var isCallActive = false
    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: Any? = null
    private var phoneStateListener: PhoneStateListener? = null

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
                if (isWakeWordActive && !isCallActive && TroLyNoiAssistantHelper.isScreenInteractiveAndUnlocked(this@TroLyNoiForegroundService)) {
                    wakeWordManager?.resume(retryCount = 3)
                }
            }
            // Không warm-up trước: STT/TTS/NLU chỉ nạp khi Overlay thực sự hiển thị (Lazy Init)
        }

        wakeWordManager = TroLyNoiWakeWordManager(
            context = this,
            canListen = {
                overlayManager?.isShowing != true &&
                !isCallActive &&
                TroLyNoiAssistantHelper.isScreenInteractiveAndUnlocked(this)
            },
            onWakeWordDetected = { extractedCommand ->
                overlayManager?.showAssistant(extractedCommand)
            }
        )

        registerTelephonyListener()
        registerScreenReceiver()

        // Bật master = bật Wake Word; Service lắng nghe trực tiếp enabledFlow (1 công tắc duy nhất)
        val prefs = TroLyNoiPreferences(this)
        serviceScope.launch {
            prefs.enabledFlow.collect { enabled ->
                isWakeWordActive = enabled
                Log.d(TAG, "Master switch → Wake Word: $enabled")
                if (enabled) {
                    if (TroLyNoiAssistantHelper.isScreenInteractiveAndUnlocked(this@TroLyNoiForegroundService)) {
                        acquireWakeLock()
                    }
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
                    releaseWakeLock()
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
                    !isCallActive &&
                    overlayManager?.isShowing != true &&
                    TroLyNoiAssistantHelper.isScreenInteractiveAndUnlocked(this@TroLyNoiForegroundService) &&
                    wakeWordManager?.isLoopActive != true
                ) {
                    Log.i(TAG, "[WATCHDOG_HEAL] Phát hiện Wake Word listener ngưng hoạt động bất thường khi mở máy, đang tự khôi phục...")
                    acquireWakeLock()
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

                        val actionTitle = intent.getStringExtra("extra_action_title") ?: "Xác nhận thực hiện?"
                        val actionDesc = intent.getStringExtra("extra_action_desc") ?: target.ifBlank { "Thực hiện hành động" }
                        val actionIconStr = intent.getStringExtra("extra_action_icon") ?: "GENERIC"
                        val actionIcon = try {
                            OverlayActionIconType.valueOf(actionIconStr)
                        } catch (e: Exception) {
                            OverlayActionIconType.GENERIC
                        }
                        val statusMsg = intent.getStringExtra("extra_status") ?: when (state) {
                            AssistantOverlayState.LISTENING -> "Hãy nói gì đó..."
                            else -> ""
                        }

                        val data = AssistantOverlayData(
                            state = state,
                            recognizedText = query,
                            intentName = intentName,
                            targetName = target,
                            actionTitle = actionTitle,
                            actionDescription = actionDesc,
                            actionIconType = actionIcon,
                            sourceLabel = sourceLabel,
                            statusMessage = statusMsg,
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
        unregisterTelephonyListenerSafely()
        releaseWakeLock()

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
                        Log.i(TAG, "[SCREEN_OFF] Màn hình tắt -> Dừng hoàn toàn mic và giải phóng tài nguyên")
                        wakeWordManager?.pause()
                        releaseWakeLock()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        val isUnlocked = TroLyNoiAssistantHelper.isScreenInteractiveAndUnlocked(this@TroLyNoiForegroundService)
                        if (isUnlocked) {
                            Log.i(TAG, "[SCREEN_ON] Màn hình sáng và đã mở máy")
                            if (isWakeWordActive && !isCallActive && overlayManager?.isShowing != true) {
                                acquireWakeLock()
                                wakeWordManager?.resume(retryCount = 2)
                            }
                        } else {
                            Log.i(TAG, "[DEVICE_LOCKED] Màn hình sáng nhưng thiết bị đang khóa, chưa bật mic")
                        }
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        Log.i(TAG, "[DEVICE_UNLOCKED] Người dùng đã mở máy thành công -> Bật mic lắng nghe 'Trợ lý ơi'")
                        if (isWakeWordActive && !isCallActive && overlayManager?.isShowing != true) {
                            acquireWakeLock()
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

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            try {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "ViDroidCall:WakeWord247WakeLock"
                )?.apply {
                    setReferenceCounted(false)
                    acquire()
                    Log.i(TAG, "[WAKE_LOCK] Đã kích hoạt PARTIAL_WAKE_LOCK để mic lắng nghe 24/7 trong nền")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi acquire PARTIAL_WAKE_LOCK: ${e.message}", e)
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.i(TAG, "[WAKE_LOCK] Đã giải phóng PARTIAL_WAKE_LOCK")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi khi release PARTIAL_WAKE_LOCK: ${e.message}")
        }
        wakeLock = null
    }

    private fun registerTelephonyListener() {
        try {
            telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (telephonyManager == null) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleCallStateChanged(state)
                    }
                }
                telephonyManager?.registerTelephonyCallback(mainExecutor, callback)
                telephonyCallback = callback
                Log.d(TAG, "Đã đăng ký TelephonyCallback (Android 12+)")
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleCallStateChanged(state)
                    }
                }
                @Suppress("DEPRECATION")
                telephonyManager?.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                phoneStateListener = listener
                Log.d(TAG, "Đã đăng ký PhoneStateListener (Android <12)")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Thiếu quyền READ_PHONE_STATE để theo dõi cuộc gọi: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi khi đăng ký theo dõi trạng thái cuộc gọi: ${e.message}")
        }
    }

    private fun handleCallStateChanged(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING,
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                Log.i(TAG, "[TELEPHONY] Phát hiện cuộc gọi đang diễn ra (state=$state). Tạm dừng WakeWord & Overlay.")
                isCallActive = true
                overlayManager?.dismiss()
                wakeWordManager?.pause()
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                Log.i(TAG, "[TELEPHONY] Cuộc gọi kết thúc. Sẵn sàng khôi phục WakeWord.")
                val wasActive = isCallActive
                isCallActive = false
                if (wasActive && isWakeWordActive && overlayManager?.isShowing != true) {
                    wakeWordManager?.resume(retryCount = 3)
                }
            }
        }
    }

    private fun unregisterTelephonyListenerSafely() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (telephonyCallback as? TelephonyCallback)?.let {
                    telephonyManager?.unregisterTelephonyCallback(it)
                }
            } else {
                phoneStateListener?.let {
                    @Suppress("DEPRECATION")
                    telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi unregister telephony: ${e.message}")
        }
        telephonyCallback = null
        phoneStateListener = null
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
