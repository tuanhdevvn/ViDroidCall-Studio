// SPDX-License-Identifier: Apache-2.0

package com.example.ViDroidCall_Studio.feature.speech

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

data class TextToSpeechState(
    val isSpeaking: Boolean,
    val speak: (String) -> Unit,
    val stop: () -> Unit
)

/**
 * Hook Compose: Quản lý vòng đời và trạng thái phản hồi giọng nói TextToSpeech tiếng Việt.
 */
@Composable
fun rememberTextToSpeech(): TextToSpeechState {
    val context = LocalContext.current
    var isSpeakingState by remember { mutableStateOf(false) }

    val manager = remember {
        TextToSpeechManager(
            context = context,
            onSpeakingStateChanged = { isSpeaking ->
                isSpeakingState = isSpeaking
            }
        )
    }

    DisposableEffect(manager) {
        onDispose {
            manager.shutdown()
        }
    }

    return remember(isSpeakingState) {
        TextToSpeechState(
            isSpeaking = isSpeakingState,
            speak = { text -> manager.speak(text) },
            stop = { manager.stop() }
        )
    }
}
