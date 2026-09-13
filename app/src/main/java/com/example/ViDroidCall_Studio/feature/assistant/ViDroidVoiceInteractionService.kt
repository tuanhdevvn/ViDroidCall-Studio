package com.example.ViDroidCall_Studio.feature.assistant

import android.service.voice.VoiceInteractionService

/**
 * Dịch vụ trợ lý kỹ thuật số mặc định cấp hệ thống cho ViDroidCall.
 * Cho phép thiết bị gọi trợ lý thông qua phím cứng (giữ phím Nguồn / Home / vuốt góc).
 */
class ViDroidVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
    }

    override fun onShutdown() {
        super.onShutdown()
    }
}
