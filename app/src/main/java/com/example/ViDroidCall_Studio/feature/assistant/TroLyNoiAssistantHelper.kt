package com.example.ViDroidCall_Studio.feature.assistant

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Tiện ích hỗ trợ kiểm tra và thiết lập ViDroidCall làm Trợ lý mặc định hệ thống.
 */
object TroLyNoiAssistantHelper {
    private const val TAG = "TroLyNoiAssistantHelper"

    /**
     * Kiểm tra xem ViDroidCall hiện có đang là Ứng dụng hỗ trợ mặc định của thiết bị hay không.
     */
    fun isDefaultAssistant(context: Context): Boolean {
        return try {
            val setting = Settings.Secure.getString(
                context.contentResolver,
                "voice_interaction_service"
            ) ?: return false
            val myComponent = ComponentName(context, ViDroidVoiceInteractionService::class.java).flattenToString()
            setting == myComponent
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi kiểm tra voice_interaction_service: ${e.message}")
            false
        }
    }

    /**
     * Mở màn hình cài đặt Ứng dụng trợ lý mặc định của Android để người dùng lựa chọn.
     */
    fun openAssistantSettings(context: Context) {
        val intentList = listOf(
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        for (intent in intentList) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Không thể mở intent ${intent.action}: ${e.message}")
            }
        }
    }

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
