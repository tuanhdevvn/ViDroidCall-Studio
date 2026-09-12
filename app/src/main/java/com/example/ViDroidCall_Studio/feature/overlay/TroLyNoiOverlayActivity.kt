// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.feature.overlay

import android.Manifest
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ViDroidCall_Studio.data.nlu.FastPathMatcher
import com.example.ViDroidCall_Studio.data.nlu.NluActionDispatcher
import com.example.ViDroidCall_Studio.data.nlu.NluEngineManager
import com.example.ViDroidCall_Studio.domain.model.NativeAction
import com.example.ViDroidCall_Studio.feature.speech.SpeechToTextManager
import com.example.ViDroidCall_Studio.feature.speech.TextToSpeechManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Overlay Host: Hiển thị Trợ lý nổi trên nền trong suốt trên toàn hệ thống.
 * Cửa sổ thực sự trong suốt, hiển thị toàn bộ app thật phía sau.
 * Chỉ hiển thị khi thiết bị đã được mở khóa (unlocked).
 */
class TroLyNoiOverlayActivity : ComponentActivity() {

    private var onNewCommandCallback: ((String) -> Unit)? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                // Đóng overlay ngay khi màn hình tắt/khóa
                finish()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val cmd = intent.getStringExtra(EXTRA_COMMAND)
        if (!cmd.isNullOrBlank()) {
            onNewCommandCallback?.invoke(cmd)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Kiểm tra trạng thái khóa màn hình: nếu đang khóa thì lập tức đóng
        if (isDeviceLocked()) {
            Log.w(TAG, "Thiết bị đang khóa màn hình. Không hiển thị Trợ lý nổi.")
            finish()
            return
        }

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenReceiver, filter)

        enableEdgeToEdge()

        setContent {
            val scope = rememberCoroutineScope()
            var state by remember { mutableStateOf<TroLyNoState>(TroLyNoState.Listening) }
            val fastPathMatcher = remember { FastPathMatcher(applicationContext) }
            val nluEngineManager = remember { NluEngineManager(applicationContext) }

            var textToSpeechRef by remember { mutableStateOf<TextToSpeechManager?>(null) }
            val textToSpeech = remember {
                TextToSpeechManager(applicationContext).also { textToSpeechRef = it }
            }

            var actionDispatcherRef by remember { mutableStateOf<NluActionDispatcher?>(null) }
            val actionDispatcher = remember(textToSpeech) {
                NluActionDispatcher(
                    context = applicationContext,
                    enableAppLaunch = true,
                    onActionError = {},
                    onSpeakFeedback = { speechText ->
                        textToSpeech.speak(speechText)
                    }
                ).also { actionDispatcherRef = it }
            }

            var speechManagerRef by remember { mutableStateOf<SpeechToTextManager?>(null) }

            // Xử lý câu lệnh sau khi STT nhận dạng thành công
            fun processCommand(query: String) {
                val cleanQuery = query.trim()
                if (cleanQuery.isEmpty()) return

                // 1. Kiểm tra Fast-Path trước (< 5ms, Zero-LLM)
                val fastResult = fastPathMatcher.match(cleanQuery)
                if (fastResult != null) {
                    val action = NativeAction.fromNluResult(fastResult)
                    if (action.requiresConfirmation) {
                        // CRITICAL SAFETY RULE: Chuyển sang Confirmation, KHÔNG thực thi trước
                        state = TroLyNoState.Confirmation(text = cleanQuery, action = action)
                        val promptSpeech = action.getConfirmationDescription()
                        if (promptSpeech.isNotBlank()) {
                            textToSpeech.speak(promptSpeech)
                        }
                    } else {
                        // Hành động an toàn (không cần xác nhận)
                        state = TroLyNoState.FastPath(text = cleanQuery, action = action)
                        val speech = action.getSpeechFeedbackText()
                        if (speech.isNotBlank()) {
                            textToSpeech.speak(speech)
                        }
                        actionDispatcher.executeNativeAction(action)
                        scope.launch {
                            delay(1500)
                            finish()
                        }
                    }
                    return
                }

                // 2. Không khớp Fast-Path -> Chuyển sang Analyzing và gửi vào NLU Model
                state = TroLyNoState.Analyzing
                nluEngineManager.processQuery(cleanQuery)
            }

            LaunchedEffect(Unit) {
                onNewCommandCallback = { cmd ->
                    processCommand(cmd)
                }
                val testCommand = intent.getStringExtra(EXTRA_COMMAND)
                if (!testCommand.isNullOrBlank()) {
                    processCommand(testCommand)
                }
            }

            // Lắng nghe kết quả từ NLU Engine
            LaunchedEffect(nluEngineManager) {
                nluEngineManager.nluEvents.collect { result ->
                    val query = nluEngineManager.currentQuery.value
                    val action = NativeAction.fromNluResult(result)

                    if (result.status == "success") {
                        if (action.requiresConfirmation) {
                            // CRITICAL SAFETY: Yêu cầu xác nhận trước khi thực thi
                            state = TroLyNoState.Confirmation(text = query, action = action)
                            val promptSpeech = action.getConfirmationDescription()
                            if (promptSpeech.isNotBlank()) {
                                textToSpeech.speak(promptSpeech)
                            }
                        } else {
                            val speech = action.getSpeechFeedbackText()
                            if (speech.isNotBlank()) {
                                textToSpeech.speak(speech)
                            }
                            actionDispatcher.executeNativeAction(action)
                            scope.launch {
                                delay(1500)
                                finish()
                            }
                        }
                    } else if (result.status == "needs_clarification") {
                        state = TroLyNoState.Clarify(
                            text = query,
                            missing = listOf("Vui lòng nêu rõ hơn yêu cầu")
                        )
                    } else {
                        val speech = action.getSpeechFeedbackText()
                        if (speech.isNotBlank()) {
                            textToSpeech.speak(speech)
                        }
                        scope.launch {
                            delay(2000)
                            finish()
                        }
                    }
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    speechManagerRef?.startListening()
                } else {
                    finish()
                }
            }

            // Khởi tạo STT Sherpa-ONNX
            DisposableEffect(Unit) {
                val stt = SpeechToTextManager(
                    context = applicationContext,
                    callbacks = object : SpeechToTextManager.Callbacks {
                        override fun onListeningChanged(isListening: Boolean) {
                            if (isListening) {
                                state = TroLyNoState.Listening
                            }
                        }

                        override fun onTextChanged(text: String) {
                            if (text.isNotBlank() &&
                                text != SpeechToTextManager.LISTENING_PLACEHOLDER &&
                                text != SpeechToTextManager.WAITING_PLACEHOLDER
                            ) {
                                state = TroLyNoState.Stt(text = text)
                            }
                        }

                        override fun onFinalResult(text: String) {
                            if (text.isNotBlank() &&
                                text != SpeechToTextManager.LISTENING_PLACEHOLDER &&
                                text != SpeechToTextManager.WAITING_PLACEHOLDER &&
                                text != SpeechToTextManager.ERROR_MESSAGE &&
                                text != SpeechToTextManager.PERMISSION_DENIED_MESSAGE
                            ) {
                                processCommand(text)
                            }
                        }
                    }
                )
                speechManagerRef = stt

                // Kiểm tra quyền RECORD_AUDIO trước khi lắng nghe
                val hasPermission = ContextCompat.checkSelfPermission(
                    this@TroLyNoiOverlayActivity,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    stt.startListening()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }

                onDispose {
                    onNewCommandCallback = null
                    stt.destroy()
                    textToSpeech.shutdown()
                }
            }

            // Xử lý nút Back của hệ thống
            BackHandler {
                speechManagerRef?.cancelListening()
                textToSpeech.stop()
                finish()
            }

            // Giao diện Overlay Host: Nền hoàn toàn trong suốt + TroLyNoSheet neo ở đáy
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.08f)) // Scrim nhẹ 8% tinh tế
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            // Bấm vào khoảng trống trong suốt bên ngoài sheet -> đóng trợ lý
                            speechManagerRef?.cancelListening()
                            textToSpeech.stop()
                            finish()
                        }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 28.dp, top = 8.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // Chặn click-through để không đóng sheet khi bấm vào trong hộp thoại
                        )
                ) {
                    TroLyNoSheet(
                        state = state,
                        onConfirm = {
                            val currentState = state
                            if (currentState is TroLyNoState.Confirmation) {
                                // CRITICAL SAFETY RULE: Chỉ execute sau khi user bấm Xác nhận
                                actionDispatcher.executeNativeAction(currentState.action)
                            }
                            speechManagerRef?.cancelListening()
                            textToSpeech.stop()
                            finish()
                        },
                        onCancel = {
                            // Hủy thao tác: không execute action và đóng overlay
                            speechManagerRef?.cancelListening()
                            textToSpeech.stop()
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isDeviceLocked()) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Đã unregister hoặc chưa đăng ký
        }
    }

    private fun isDeviceLocked(): Boolean {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguardManager?.isKeyguardLocked == true
    }

    companion object {
        private const val TAG = "TroLyNoiOverlay"
        const val EXTRA_COMMAND = "command"

        fun createIntent(context: Context): Intent {
            return Intent(context, TroLyNoiOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }
    }
}
