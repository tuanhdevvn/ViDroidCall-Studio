package com.example.ViDroidCall_Studio.feature.speech

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

data class SpeechToTextState(
    val isListening: Boolean,
    val speechText: String,
    val toggleListening: () -> Unit,
    val stopListening: () -> Unit,
    val cancelListening: () -> Unit
)

/**
 * Hook Compose: Nhận diện giọng nói 100% In-App (SpeechToTextManager) trực tiếp trong App, không dùng popup Google.
 */
@Composable
fun rememberSpeechToText(
    onSpeechResult: (String) -> Unit = {},
    onPermissionDenied: () -> Unit = {}
): SpeechToTextState {
    var isListeningState by remember { mutableStateOf(false) }
    var speechTextState by remember { mutableStateOf("") }
    val context = LocalContext.current

    val manager = remember {
        SpeechToTextManager(
            context = context,
            callbacks = object : SpeechToTextManager.Callbacks {
                override fun onListeningChanged(isListening: Boolean) {
                    isListeningState = isListening
                }

                override fun onTextChanged(text: String) {
                    speechTextState = text
                }

                override fun onFinalResult(text: String) {
                    // CHỈ GỌI MODEL AI KHI ĐÃ NHẬN DIỆN THÀNH CÔNG VĂN BẢN
                    if (text.isNotBlank() && 
                        text != SpeechToTextManager.LISTENING_PLACEHOLDER && 
                        text != SpeechToTextManager.WAITING_PLACEHOLDER &&
                        text != SpeechToTextManager.ERROR_MESSAGE &&
                        text != SpeechToTextManager.PERMISSION_DENIED_MESSAGE) {
                        onSpeechResult(text)
                    }
                }
            }
        )
    }

    DisposableEffect(manager) {
        onDispose { manager.destroy() }
    }

    var lastActionTime by remember { mutableStateOf(0L) }
    val debounceThreshold = 350L

    val startListening = {
        speechTextState = ""
        manager.startListening()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            speechTextState = SpeechToTextManager.PERMISSION_DENIED_MESSAGE
            onPermissionDenied()
        }
    }

    val toggleListening: () -> Unit = {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastActionTime >= debounceThreshold) {
            lastActionTime = now

            if (isListeningState) {
                manager.stopListening()
                isListeningState = false
            } else {
                speechTextState = ""
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    startListening()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    val stopListening: () -> Unit = {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastActionTime >= debounceThreshold) {
            lastActionTime = now
            manager.stopListening()
            isListeningState = false
        }
    }

    val cancelListening: () -> Unit = {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastActionTime >= debounceThreshold) {
            lastActionTime = now
            manager.cancelListening()
            speechTextState = ""
            isListeningState = false
        }
    }

    return SpeechToTextState(
        isListening = isListeningState,
        speechText = speechTextState,
        toggleListening = toggleListening,
        stopListening = stopListening,
        cancelListening = cancelListening
    )
}
