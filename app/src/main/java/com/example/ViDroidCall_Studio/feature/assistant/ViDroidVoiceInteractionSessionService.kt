package com.example.ViDroidCall_Studio.feature.assistant

import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.example.ViDroidCall_Studio.feature.overlay.TroLyNoiForegroundController

import android.util.Log

/**
 * Quản lý phiên tương tác giọng nói khi người dùng gọi trợ lý qua phím cứng hoặc cử chỉ Android.
 */
class ViDroidVoiceInteractionSessionService : VoiceInteractionSessionService() {

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        Log.i(TAG, "[ASSISTANT_OPEN] Khởi tạo phiên VoiceInteractionSession mới")
        return ViDroidVoiceInteractionSession(this)
    }

    private class ViDroidVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
        override fun onShow(args: Bundle?, showFlags: Int) {
            super.onShow(args, showFlags)
            Log.i(TAG, "[ASSISTANT_OPEN] Kích hoạt Trợ lý nổi từ VoiceInteractionSession (Hardware/Gesture Trigger)")
            // Đóng cửa sổ session ngầm của hệ điều hành và kích hoạt giao diện Trợ lý nổi ViDroidCall
            hide()
            TroLyNoiForegroundController.showOverlay(context)
        }
    }

    companion object {
        private const val TAG = "ViDroidVoiceInteractionSession"
    }
}
