package com.example.emma_vidroidcall.feature.speech

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
fun rememberSpeechToText(): SpeechToTextState {
    var isListening by remember { mutableStateOf(false) }
    var speechText by remember { mutableStateOf("") }
    val context = LocalContext.current

    val manager = remember {
        SpeechToTextManager(
            context = context,
            callbacks = object : SpeechToTextManager.Callbacks {
                override fun onListeningChanged(listening: Boolean) {
                    isListening = listening
                }

                override fun onTextChanged(text: String) {
                    speechText = text
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
            speechText = SpeechToTextManager.PERMISSION_DENIED_MESSAGE
        }
    }

    val toggleListening: () -> Unit = {
        if (isListening) {
            manager.stopListening()
            isListening = false
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
        isListening = false
    }

    return SpeechToTextState(
        isListening = isListening,
        speechText = speechText,
        toggleListening = toggleListening,
        stopListening = stopListening
    )
}
