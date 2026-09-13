package com.example.ViDroidCall_Studio.feature.assistant

import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.example.ViDroidCall_Studio.feature.overlay.TroLyNoiForegroundController

/**
 * Quản lý phiên tương tác giọng nói khi người dùng gọi trợ lý qua phím cứng hoặc cử chỉ Android.
 */
class ViDroidVoiceInteractionSessionService : VoiceInteractionSessionService() {

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return ViDroidVoiceInteractionSession(this)
    }

    private class ViDroidVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
        override fun onShow(args: Bundle?, showFlags: Int) {
            super.onShow(args, showFlags)
            // Đóng cửa sổ session ngầm của hệ điều hành và kích hoạt giao diện Trợ lý nổi ViDroidCall
            hide()
            TroLyNoiForegroundController.showOverlay(context)
        }
    }
}
