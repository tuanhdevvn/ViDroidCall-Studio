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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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
            // Khi mở trợ lý, luôn hiển thị giao diện LISTENING ("Hãy nói gì đó..." + sóng âm)
            // trong 600ms để người dùng thấy rõ Trợ lý lắng nghe trước khi chuyển sang STT
            show(AssistantOverlayData(state = AssistantOverlayState.LISTENING))
            scope.launch {
                delay(600)
                overlayDataFlow.value = AssistantOverlayData(
                    state = AssistantOverlayState.STT,
                    recognizedText = initialCommand
                )
                handleFinalSpeechResult(initialCommand)
            }
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
                        val trimmed = text.trim()
                        if (trimmed.isBlank() || 
                            trimmed == SpeechToTextManager.WAITING_PLACEHOLDER || 
                            trimmed == SpeechToTextManager.LISTENING_PLACEHOLDER ||
                            trimmed == "Đang lắng nghe câu lệnh..." ||
                            trimmed == "Hãy nói gì đó..."
                        ) {
                            if (overlayDataFlow.value.state != AssistantOverlayState.LISTENING) {
                                overlayDataFlow.value = overlayDataFlow.value.copy(
                                    state = AssistantOverlayState.LISTENING
                                )
                            }
                            return
                        }

                        // Chỉ khi nhận diện được câu chữ thực tế của người dùng mới chuyển sang STT
                        overlayDataFlow.value = overlayDataFlow.value.copy(
                            state = AssistantOverlayState.STT,
                            recognizedText = trimmed
                        )
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
        // Lưu ý: Không khởi tạo nluEngineManager tại đây để không nạp trước mô hình GGUF
        // GGUF chỉ được nạp khi Fast-Path không khớp (Miss)
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
        scope.cancel()
        speechToTextManager?.destroy()
        speechToTextManager = null
        textToSpeechManager?.shutdown()
        textToSpeechManager = null
        nluEngineManager = null
        fastPathMatcher = null
        actionDispatcher = null
        historyRepository = null
    }

    /**
     * Xử lý câu nói sau khi bộ nhận diện giọng nói STT cho kết quả cuối cùng.
     * Quy tắc:
     * - Fast-Path chạy trước (chưa nạp GGUF). Khớp -> hiển thị ngay câu nói + xác nhận hành động cho MỌI intent.
     * - Fast-Path không khớp -> Hiển thị "AI đang phân tích...", lúc này mới nạp GGUF và suy luận, xong hiện xác nhận.
     */
    private fun handleFinalSpeechResult(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            dismiss()
            return
        }

        // 1. Hiển thị ngay trạng thái STT: đoạn text có animation chạy dọc câu vừa nói
        overlayDataFlow.value = overlayDataFlow.value.copy(
            state = AssistantOverlayState.STT,
            recognizedText = trimmed
        )

        scope.launch {
            // Dành khoảng 900ms để người dùng thấy rõ câu nói vừa nhận diện xong trượt dọc mượt mà
            delay(900)

            // Kiểm tra Fast-Path trước (<5ms, hoàn toàn chưa nạp GGUF)
            val fastResult = fastPathMatcher?.match(trimmed)
            if (fastResult != null) {
                val nativeAction = NativeAction.fromNluResult(fastResult)
                historyRepository?.addFromNluResult(trimmed, fastResult)
                // Chuyển tiếp sang màn hình xác nhận hành động
                showConfirmationForAction(trimmed, nativeAction)
                return@launch
            }

            // 2. Không khớp Fast-Path -> Chuyển sang "AI đang phân tích..." (logo thở hào quang, không lộ thông số kỹ thuật)
            overlayDataFlow.value = AssistantOverlayData(
                state = AssistantOverlayState.ANALYZING,
                recognizedText = trimmed
            )

            try {
                if (nluEngineManager == null) {
                    nluEngineManager = NluEngineManager(appContext)
                }
                val nlu = nluEngineManager ?: run {
                    dismiss()
                    return@launch
                }

                // Lúc này mới bắt đầu nạp model GGUF (nếu chưa nạp) và chạy suy luận
                if (!nlu.isModelReady()) {
                    val state = nlu.modelState.first { it !is NluModelState.Loading && it !is NluModelState.Uninitialized }
                    if (state !is NluModelState.Ready) {
                        textToSpeechManager?.speak("Không tìm thấy mô hình AI để xử lý câu lệnh này.")
                        delay(2000)
                        dismiss()
                        return@launch
                    }
                }

                // Lắng nghe kết quả kế tiếp từ nluEvents
                val listenerJob = launch {
                    nlu.nluEvents.collect { result ->
                        val nativeAction = NativeAction.fromNluResult(result)
                        historyRepository?.addFromNluResult(trimmed, result)
                        showConfirmationForAction(trimmed, nativeAction)
                        cancel()
                    }
                }

                nlu.processQuery(trimmed)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi chạy GGUF NLU: ${e.message}", e)
                dismiss()
            }
        }
    }

    /**
     * Hiển thị màn hình xác nhận hành động trực quan cho MỌI intent.
     * Người dùng bấm [Xác nhận] mới thực thi hành động; bấm [Hủy] thì đóng box.
     */
    private fun showConfirmationForAction(recognizedText: String, action: NativeAction) {
        when (action) {
            is NativeAction.Informational -> {
                val speech = action.getSpeechFeedbackText()
                if (speech.isNotBlank()) {
                    textToSpeechManager?.speak(speech)
                }
                overlayDataFlow.value = AssistantOverlayData(
                    state = AssistantOverlayState.CONFIRM_ACTION,
                    recognizedText = recognizedText,
                    actionTitle = "Trợ lý phản hồi",
                    actionDescription = action.message,
                    actionIconType = OverlayActionIconType.GENERIC,
                    onConfirm = { dismiss() },
                    onCancel = { dismiss() }
                )
                scope.launch {
                    delay(2500)
                    dismiss()
                }
            }

            is NativeAction.Unsupported -> {
                val speech = action.getSpeechFeedbackText()
                if (speech.isNotBlank()) {
                    textToSpeechManager?.speak(speech)
                }
                overlayDataFlow.value = AssistantOverlayData(
                    state = AssistantOverlayState.CONFIRM_ACTION,
                    recognizedText = recognizedText,
                    actionTitle = "Chưa hỗ trợ",
                    actionDescription = action.message,
                    actionIconType = OverlayActionIconType.GENERIC,
                    onConfirm = { dismiss() },
                    onCancel = { dismiss() }
                )
                scope.launch {
                    delay(2500)
                    dismiss()
                }
            }

            else -> {
                val iconType = when (action.getActionIconType()) {
                    "CALL" -> OverlayActionIconType.CALL
                    "SMS" -> OverlayActionIconType.SMS
                    "OPEN_APP" -> OverlayActionIconType.OPEN_APP
                    "ALARM" -> OverlayActionIconType.ALARM
                    "TIMER" -> OverlayActionIconType.TIMER
                    "MAP" -> OverlayActionIconType.MAP
                    "SEARCH" -> OverlayActionIconType.SEARCH
                    "YOUTUBE" -> OverlayActionIconType.YOUTUBE
                    "MUSIC" -> OverlayActionIconType.MUSIC
                    else -> OverlayActionIconType.GENERIC
                }

                overlayDataFlow.value = AssistantOverlayData(
                    state = AssistantOverlayState.CONFIRM_ACTION,
                    recognizedText = recognizedText,
                    intentName = action.intentName,
                    actionTitle = action.getActionTitle(),
                    actionDescription = action.getActionSummary(),
                    actionIconType = iconType,
                    onConfirm = {
                        val speech = action.getSpeechFeedbackText()
                        if (speech.isNotBlank()) {
                            textToSpeechManager?.speak(speech)
                        }
                        actionDispatcher?.executeNativeAction(action)
                        dismiss()
                    },
                    onCancel = {
                        dismiss()
                    }
                )
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
