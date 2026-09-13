// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.feature.overlay

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.ViDroidCall_Studio.data.local.history.CommandHistoryRepository
import com.example.ViDroidCall_Studio.data.nlu.FastPathMatcher
import com.example.ViDroidCall_Studio.data.nlu.NluActionDispatcher
import com.example.ViDroidCall_Studio.data.nlu.NluEngineManager
import com.example.ViDroidCall_Studio.data.nlu.NluModelState
import com.example.ViDroidCall_Studio.domain.model.NativeAction
import com.example.ViDroidCall_Studio.feature.speech.SpeechToTextManager
import com.example.ViDroidCall_Studio.feature.speech.TextToSpeechManager
import com.example.ViDroidCall_Studio.ui.theme.ViDroidCallTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Quản lý vòng đời cửa sổ hệ thống SYSTEM_ALERT_WINDOW thật.
 * Đảm bảo 100% không vẽ HomeScreen giả, nền hoàn toàn trong suốt nhìn thấy ứng dụng thật phía dưới.
 */
class TroLyNoiOverlayManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "ViDroidAssistant"
    }

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val keyguardManager = appContext.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var overlayView: android.view.View? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var screenOffReceiver: BroadcastReceiver? = null
    var onDismissListener: (() -> Unit)? = null

    // Quản lý trạng thái hiển thị
    private val overlayDataFlow = MutableStateFlow(AssistantOverlayData())

    // Thành phần xử lý âm thanh, nhận dạng & NLU
    private var speechToTextManager: SpeechToTextManager? = null
    private var textToSpeechManager: TextToSpeechManager? = null
    private var nluEngineManager: NluEngineManager? = null
    private var fastPathMatcher: FastPathMatcher? = null
    private var actionDispatcher: NluActionDispatcher? = null
    private var historyRepository: CommandHistoryRepository? = null

    val isShowing: Boolean
        get() = overlayView != null

    /**
     * Mở Trợ lý nổi ở chế độ lắng nghe giọng nói thật (End-to-End).
     * Nếu có initialCommand (từ Wake Word "Trợ lý ơi [câu lệnh]"), xử lý trực tiếp không cần thu âm lại.
     */
    fun showAssistant(initialCommand: String? = null) {
        if (initialCommand.isNullOrBlank()) {
            show(AssistantOverlayData(state = AssistantOverlayState.LISTENING))
            startSpeechRecognition()
        } else {
            show(AssistantOverlayData(state = AssistantOverlayState.STT, recognizedText = initialCommand))
            handleFinalSpeechResult(initialCommand)
        }
    }

    /**
     * Mở Trợ lý nổi với dữ liệu cấu hình cụ thể (phục vụ kiểm thử từng trạng thái Stitch).
     */
    fun showWithData(data: AssistantOverlayData) {
        show(data)
    }

    /**
     * Khởi tạo cửa sổ trong suốt và gắn TroLyNoiSheet vào WindowManager.
     */
    @Synchronized
    private fun show(initialData: AssistantOverlayData) {
        // 1. Kiểm tra trạng thái khóa màn hình: Chỉ hiển thị khi thiết bị ĐÃ MỞ KHÓA
        if (keyguardManager?.isKeyguardLocked == true) {
            Log.w(TAG, "Thiết bị đang khóa màn hình. Không hiển thị Trợ lý nổi theo yêu cầu an toàn.")
            return
        }

        // 2. Kiểm tra quyền SYSTEM_ALERT_WINDOW
        if (!Settings.canDrawOverlays(appContext)) {
            Log.w(TAG, "Chưa được cấp quyền SYSTEM_ALERT_WINDOW. Không thể mở overlay.")
            return
        }

        // 3. Nếu đang hiển thị thì chỉ cập nhật dữ liệu, không tạo thêm cửa sổ chồng lấn
        overlayDataFlow.value = initialData
        if (overlayView != null) {
            return
        }

        try {
            ensureComponentsInitialized()

            val owner = OverlayLifecycleOwner().apply {
                onCreate()
                onStart()
                onResume()
            }
            lifecycleOwner = owner

            val themedContext = android.view.ContextThemeWrapper(
                appContext,
                com.example.ViDroidCall_Studio.R.style.Theme_ViDroidCall
            )
            val composeView = ComposeView(themedContext).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)

                setContent {
                    val currentData by overlayDataFlow.collectAsState()

                    ViDroidCallTheme {
                        // Nền Root 100% TRONG SUỐT — Không có Wallpaper, Clock, Icon giả
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    dismiss()
                                },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .padding(bottom = 12.dp)
                            ) {
                                TroLyNoiSheet(
                                    data = currentData,
                                    onSheetClick = {}
                                )
                            }
                        }
                    }
                }
            }

            val rootLayout = object : android.widget.FrameLayout(themedContext) {
                override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
                    if (event?.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                        dismiss()
                        return true
                    }
                    return super.dispatchKeyEvent(event)
                }
            }.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                addView(
                    composeView,
                    android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }

            val layoutParams = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                    blurBehindRadius = 24
                }
            }

            windowManager.addView(rootLayout, layoutParams)
            overlayView = rootLayout
            rootLayout.requestFocus()

            registerScreenOffReceiver()
            Log.i(TAG, "[ASSISTANT_OPEN] Đã gắn thành công cửa sổ Trợ lý nổi trong suốt vào WindowManager.")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi hiển thị Trợ lý nổi: ${e.message}", e)
            dismiss()
        }
    }

    /**
     * Đóng và dọn dẹp an toàn cửa sổ Overlay.
     */
    @Synchronized
    fun dismiss() {
        if (overlayView == null) return
        try {
            stopSpeechRecognition()

            unregisterScreenOffReceiver()

            val viewToRemove = overlayView
            overlayView = null

            viewToRemove?.let { view ->
                try {
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    Log.w(TAG, "Lỗi khi removeView: ${e.message}")
                }
            }

            lifecycleOwner?.let { owner ->
                owner.onPause()
                owner.onStop()
                owner.onDestroy()
            }
            lifecycleOwner = null

            Log.i(TAG, "[ASSISTANT_CLOSE] Đã đóng và dọn dẹp cửa sổ Trợ lý nổi thành công.")
            onDismissListener?.invoke()
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong quá trình dismiss: ${e.message}", e)
        }
    }

    fun ensureComponentsInitialized() {
        if (speechToTextManager == null) {
            speechToTextManager = SpeechToTextManager(
                context = appContext,
                callbacks = object : SpeechToTextManager.Callbacks {
                    override fun onListeningChanged(isListening: Boolean) {
                        if (!isListening && overlayDataFlow.value.state == AssistantOverlayState.LISTENING) {
                            // Người dùng dừng nói hoặc kết thúc phiên nghe
                        }
                    }

                    override fun onTextChanged(text: String) {
                        if (text.isNotBlank()) {
                            overlayDataFlow.value = overlayDataFlow.value.copy(
                                state = AssistantOverlayState.STT,
                                recognizedText = text
                            )
                        }
                    }

                    override fun onFinalResult(text: String) {
                        handleFinalSpeechResult(text)
                    }
                }
            )
        }
        if (textToSpeechManager == null) {
            textToSpeechManager = TextToSpeechManager(appContext)
        }
        if (fastPathMatcher == null) {
            fastPathMatcher = FastPathMatcher(appContext)
        }
        if (nluEngineManager == null) {
            nluEngineManager = NluEngineManager(appContext)
        }
        if (historyRepository == null) {
            historyRepository = CommandHistoryRepository(appContext)
        }
        if (actionDispatcher == null) {
            actionDispatcher = NluActionDispatcher(
                context = appContext,
                enableAppLaunch = true,
                onActionError = {},
                onSpeakFeedback = { speech ->
                    textToSpeechManager?.speak(speech)
                }
            )
        }
    }

    private fun startSpeechRecognition() {
        ensureComponentsInitialized()
        try {
            speechToTextManager?.cancelListening()
            speechToTextManager?.startListening()
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khởi chạy thu âm: ${e.message}", e)
        }
    }

    private fun stopSpeechRecognition() {
        speechToTextManager?.cancelListening()
        // Không gán null để giữ mô hình Sherpa-ONNX đã nạp sẵn trong bộ nhớ (Warm State)
        // Nhờ vậy lần mở popup kế tiếp micro sẽ bắt đầu ngay lập tức (<15ms) thay vì phải đợi nạp lại
        textToSpeechManager?.stop()
    }

    fun destroy() {
        dismiss()
        speechToTextManager?.destroy()
        speechToTextManager = null
        textToSpeechManager?.shutdown()
        textToSpeechManager = null
    }

    /**
     * Xử lý câu nói sau khi bộ nhận diện giọng nói STT cho kết quả cuối cùng.
     */
    private fun handleFinalSpeechResult(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            dismiss()
            return
        }

        // Cập nhật câu lệnh nhận diện
        overlayDataFlow.value = overlayDataFlow.value.copy(
            state = AssistantOverlayState.STT,
            recognizedText = trimmed
        )

        // 1. Kiểm tra Fast-Path trước (<5ms)
        val fastResult = fastPathMatcher?.match(trimmed)
        if (fastResult != null) {
            val nativeAction = NativeAction.fromNluResult(fastResult)
            scope.launch {
                historyRepository?.addFromNluResult(trimmed, fastResult)
            }

            when {
                // Xác nhận cuộc gọi (call_contact)
                nativeAction is NativeAction.CallContact -> {
                    val target = if (nativeAction.phoneNumber.isNotBlank()) nativeAction.phoneNumber else nativeAction.contact
                    overlayDataFlow.value = AssistantOverlayData(
                        state = AssistantOverlayState.CONFIRM_CALL,
                        recognizedText = trimmed,
                        intentName = "call_contact",
                        targetName = target,
                        sourceLabel = "⚡ Fast-Path",
                        onConfirm = {
                            actionDispatcher?.executeNativeAction(nativeAction)
                            dismiss()
                        },
                        onCancel = {
                            dismiss()
                        }
                    )
                }

                // Xác nhận mở bản đồ (open_map)
                fastResult.intent == "open_map" -> {
                    val dest = fastResult.slots["destination"]?.toString() ?: "địa điểm yêu cầu"
                    overlayDataFlow.value = AssistantOverlayData(
                        state = AssistantOverlayState.MAP_CONFIRM,
                        recognizedText = trimmed,
                        intentName = "open_map",
                        targetName = dest,
                        sourceLabel = "⚡ Fast-Path",
                        onConfirm = {
                            actionDispatcher?.executeNativeAction(nativeAction)
                            dismiss()
                        },
                        onCancel = {
                            dismiss()
                        }
                    )
                }

                // Khớp nhanh không cần xác nhận
                else -> {
                    overlayDataFlow.value = AssistantOverlayData(
                        state = AssistantOverlayState.FAST_PATH,
                        recognizedText = trimmed,
                        intentName = fastResult.intent,
                        sourceLabel = "⚡ Fast-Path"
                    )
                    scope.launch {
                        val speech = nativeAction.getSpeechFeedbackText()
                        if (speech.isNotBlank()) {
                            textToSpeechManager?.speak(speech)
                        }
                        delay(1500)
                        actionDispatcher?.executeNativeAction(nativeAction)
                        delay(800)
                        dismiss()
                    }
                }
            }
            return
        }

        // 2. Không khớp Fast-Path -> Chuyển sang mô hình AI GGUF
        val nlu = nluEngineManager ?: return
        val isReady = nlu.isModelReady()

        if (!isReady) {
            overlayDataFlow.value = AssistantOverlayData(
                state = AssistantOverlayState.GGUF_LOADING,
                recognizedText = trimmed
            )
        } else {
            overlayDataFlow.value = AssistantOverlayData(
                state = AssistantOverlayState.ANALYZING,
                recognizedText = trimmed
            )
        }

        scope.launch {
            nlu.processQuery(trimmed)
            nlu.nluEvents.collect { result ->
                val nativeAction = NativeAction.fromNluResult(result)
                historyRepository?.addFromNluResult(trimmed, result)

                when {
                    nativeAction is NativeAction.CallContact -> {
                        val target = if (nativeAction.phoneNumber.isNotBlank()) nativeAction.phoneNumber else nativeAction.contact
                        overlayDataFlow.value = AssistantOverlayData(
                            state = AssistantOverlayState.CONFIRM_CALL,
                            recognizedText = trimmed,
                            intentName = "call_contact",
                            targetName = target,
                            sourceLabel = "🧠 On-Device AI (GGUF)",
                            onConfirm = {
                                actionDispatcher?.executeNativeAction(nativeAction)
                                dismiss()
                            },
                            onCancel = {
                                dismiss()
                            }
                        )
                    }

                    result.intent == "open_map" -> {
                        val dest = result.slots["destination"]?.toString() ?: "địa điểm yêu cầu"
                        overlayDataFlow.value = AssistantOverlayData(
                            state = AssistantOverlayState.MAP_CONFIRM,
                            recognizedText = trimmed,
                            intentName = "open_map",
                            targetName = dest,
                            sourceLabel = "🧠 On-Device AI (GGUF)",
                            onConfirm = {
                                actionDispatcher?.executeNativeAction(nativeAction)
                                dismiss()
                            },
                            onCancel = {
                                dismiss()
                            }
                        )
                    }

                    else -> {
                        overlayDataFlow.value = AssistantOverlayData(
                            state = AssistantOverlayState.FAST_PATH,
                            recognizedText = trimmed,
                            intentName = result.intent,
                            sourceLabel = "🧠 On-Device AI (GGUF)"
                        )
                        val speech = nativeAction.getSpeechFeedbackText()
                        if (speech.isNotBlank()) {
                            textToSpeechManager?.speak(speech)
                        }
                        delay(1500)
                        actionDispatcher?.executeNativeAction(nativeAction)
                        delay(800)
                        dismiss()
                    }
                }
            }
        }
    }

    private fun registerScreenOffReceiver() {
        if (screenOffReceiver != null) return
        screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF || intent?.action == Intent.ACTION_USER_PRESENT) {
                    if (keyguardManager?.isKeyguardLocked == true) {
                        dismiss()
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        appContext.registerReceiver(screenOffReceiver, filter)
    }

    private fun unregisterScreenOffReceiver() {
        screenOffReceiver?.let {
            try {
                appContext.unregisterReceiver(it)
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi unregisterReceiver: ${e.message}")
            }
        }
        screenOffReceiver = null
    }
}
