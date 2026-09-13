package com.example.ViDroidCall_Studio.feature.assistant

import android.content.Intent
import android.speech.RecognitionService
import android.util.Log

/**
 * RecognitionService tiêu chuẩn của Android cấp hệ thống cho ViDroidCall.
 * Bắt buộc phải có để hệ điều hành Android chấp nhận ViDroidCall làm Ứng dụng hỗ trợ mặc định (Default Assistant App).
 */
class ViDroidRecognitionService : RecognitionService() {

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        Log.d(TAG, "RecognitionService onStartListening")
    }

    override fun onCancel(listener: Callback?) {
        Log.d(TAG, "RecognitionService onCancel")
    }

    override fun onStopListening(listener: Callback?) {
        Log.d(TAG, "RecognitionService onStopListening")
    }

    companion object {
        private const val TAG = "ViDroidRecognitionSrv"
    }
}
