package com.example.ViDroidCall_Studio.feature.assistant

import android.service.voice.VoiceInteractionService

import android.util.Log

/**
 * Dịch vụ trợ lý kỹ thuật số mặc định cấp hệ thống cho ViDroidCall.
 * Cho phép thiết bị gọi trợ lý thông qua phím cứng (giữ phím Nguồn / Home / vuốt góc).
 */
class ViDroidVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        Log.i(TAG, "[SERVICE_RESTART] ViDroidVoiceInteractionService onReady - Sẵn sàng nhận lệnh Default Assistant")
    }

    override fun onShutdown() {
        Log.i(TAG, "[SERVICE_RESTART] ViDroidVoiceInteractionService onShutdown")
        super.onShutdown()
    }

    companion object {
        private const val TAG = "ViDroidVoiceInteractionService"
    }
}
