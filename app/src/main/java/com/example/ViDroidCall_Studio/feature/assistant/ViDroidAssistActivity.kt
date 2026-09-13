package com.example.ViDroidCall_Studio.feature.assistant

import android.app.Activity
import android.os.Bundle
import com.example.ViDroidCall_Studio.feature.overlay.TroLyNoiForegroundController

/**
 * Activity trong suốt tiếp nhận Intent ACTION_ASSIST từ hệ điều hành khi người dùng
 * bấm phím trợ lý hoặc phím tắt truyền thống.
 */
class ViDroidAssistActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TroLyNoiForegroundController.showOverlay(this)
        finish()
    }
}
