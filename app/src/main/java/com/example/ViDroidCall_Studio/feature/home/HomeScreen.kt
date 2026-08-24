package com.example.ViDroidCall_Studio.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.example.ViDroidCall_Studio.data.nlu.NluEngineManager
import com.example.ViDroidCall_Studio.feature.assistant.AssistantScreen
import com.example.ViDroidCall_Studio.feature.history.HistoryScreen
import com.example.ViDroidCall_Studio.feature.history.model.CommandHistoryItem
import com.example.ViDroidCall_Studio.feature.settings.SettingsScreen
import com.example.ViDroidCall_Studio.feature.speech.rememberSpeechToText
import com.example.ViDroidCall_Studio.ui.component.CustomBottomMenuBar
import com.example.ViDroidCall_Studio.ui.component.NavTab
import com.example.ViDroidCall_Studio.ui.theme.EmmaViDroidCallTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(NavTab.ASSISTANT) }

    // Quản lý NLU Engine
    val nluEngineManager = remember { NluEngineManager(context.applicationContext) }
    val nluResult by nluEngineManager.lastResult.collectAsState()
    val isNluProcessing by nluEngineManager.isGenerating.collectAsState()
    val modelState by nluEngineManager.modelState.collectAsState()

    val speechToText = rememberSpeechToText(
        onSpeechResult = { recognizedText ->
            nluEngineManager.processQuery(recognizedText)
        }
    )

    // Dữ liệu mẫu Lịch sử câu lệnh
    val historyItems = remember {
        listOf(
            CommandHistoryItem("1", "Gọi điện cho Mẹ", "10:45 AM", "Thành công", "Cuộc gọi"),
            CommandHistoryItem("2", "Mở ứng dụng Zalo", "09:30 AM", "Thành công", "Ứng dụng"),
            CommandHistoryItem("3", "Nhắn tin cho Nam: 'Tôi đang tới'", "Hôm qua", "Thành công", "Tin nhắn"),
            CommandHistoryItem("4", "Đặt báo thức 06:30 sáng", "Hôm qua", "Thành công", "Hệ thống"),
            CommandHistoryItem("5", "Bật chế độ tiết kiệm pin", "12/08/2026", "Thành công", "Hệ thống")
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            CustomBottomMenuBar(
                selectedTab = selectedTab,
                onTabSelected = { tab -> selectedTab = tab },
                onMicClick = speechToText.toggleListening,
                isListening = speechToText.isListening,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavTab.ASSISTANT -> AssistantScreen(
                    isListening = speechToText.isListening,
                    speechText = speechToText.speechText,
                    onToggleListening = speechToText.toggleListening,
                    nluResult = nluResult,
                    isNluProcessing = isNluProcessing,
                    modelState = modelState,
                    onSuggestionClick = { prompt ->
                        nluEngineManager.processQuery(prompt)
                    }
                )

                NavTab.HISTORY -> HistoryScreen(
                    historyItems = historyItems
                )

                NavTab.SETTINGS -> SettingsScreen()
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    EmmaViDroidCallTheme(dynamicColor = false) {
        HomeScreen()
    }
}
