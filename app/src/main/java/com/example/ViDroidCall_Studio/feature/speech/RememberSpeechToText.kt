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
    val stopListening: () -> Unit
)

@Composable
fun rememberSpeechToText(
    onSpeechResult: (String) -> Unit = {}
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
                    if (text.isNotBlank() && text != SpeechToTextManager.LISTENING_PLACEHOLDER && text != SpeechToTextManager.ERROR_MESSAGE) {
                        onSpeechResult(text)
                    }
                }
            }
        )
    }

    DisposableEffect(manager) {
        onDispose { manager.destroy() }
    }

    val startListening = {
        manager.startListening()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            speechTextState = SpeechToTextManager.PERMISSION_DENIED_MESSAGE
        }
    }

    val toggleListening: () -> Unit = {
        if (isListeningState) {
            manager.stopListening()
            isListeningState = false
        } else {
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

    val stopListening: () -> Unit = {
        manager.stopListening()
        isListeningState = false
    }

    return SpeechToTextState(
        isListening = isListeningState,
        speechText = speechTextState,
        toggleListening = toggleListening,
        stopListening = stopListening
    )
}
