package com.example.ViDroidCall_Studio.util

import android.content.Context
import android.media.AudioManager
import android.util.Log

/**
 * Phát âm thanh UI mặc định của hệ thống Android/OEM (từ /system/media/audio/ui/).
 */
object SystemSoundHelper {
    private const val TAG = "SystemSoundHelper"

    /** Bắt đầu lắng nghe — tiếng gõ phím chuẩn (KeypressStandard.ogg). */
    fun playMicStartSound(context: Context) {
        playSoundEffect(context, AudioManager.FX_KEYPRESS_STANDARD)
    }

    /** Kết thúc lắng nghe — tiếng xác nhận/Enter (KeypressReturn.ogg). */
    fun playMicStopSound(context: Context) {
        playSoundEffect(context, AudioManager.FX_KEYPRESS_RETURN)
    }

    private fun playSoundEffect(context: Context, effect: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return

            audioManager.loadSoundEffects()
            audioManager.playSoundEffect(effect, 1.0f)
        } catch (e: Exception) {
            Log.w(TAG, "Không phát được âm thanh hệ thống: ${e.message}")
        }
    }
}
