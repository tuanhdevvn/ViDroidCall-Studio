package com.example.ViDroidCall_Studio.feature.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.ViDroidCall_Studio.data.local.history.CommandHistoryRepository
import com.example.ViDroidCall_Studio.data.nlu.NluActionDispatcher
import com.example.ViDroidCall_Studio.data.nlu.NluEngineManager
import com.example.ViDroidCall_Studio.feature.assistant.AssistantScreen
import com.example.ViDroidCall_Studio.feature.history.HistoryScreen
import com.example.ViDroidCall_Studio.feature.settings.SettingsScreen
import com.example.ViDroidCall_Studio.feature.speech.rememberSpeechToText
import com.example.ViDroidCall_Studio.ui.component.CustomBottomMenuBar
import com.example.ViDroidCall_Studio.ui.component.NavTab
import com.example.ViDroidCall_Studio.ui.theme.ViDroidCallTheme
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(NavTab.ASSISTANT) }

    // Quản lý Lịch sử câu lệnh ngoại tuyến (SQLite Repository)
    val historyRepository = remember { CommandHistoryRepository(context.applicationContext) }
    val historyItems by historyRepository.historyFlow.collectAsState(initial = emptyList())

    // Quản lý NLU Engine
    val nluEngineManager = remember { NluEngineManager(context.applicationContext) }
    val nluResult by nluEngineManager.lastResult.collectAsState()
    val isNluProcessing by nluEngineManager.isGenerating.collectAsState()
    val modelState by nluEngineManager.modelState.collectAsState()

    // Quản lý Điều phối hành động Native (Gọi điện, Báo thức, Hẹn giờ, Mở app)
    val actionDispatcher = remember { NluActionDispatcher(context.applicationContext) }

    // Tự động lưu câu lệnh vào Lịch sử và hiển thị JSON trực tiếp trên màn hình (Chưa mở app ngoài)
    LaunchedEffect(nluResult) {
        val result = nluResult
        val query = nluEngineManager.currentQuery.value
        if (result != null && query.isNotBlank()) {
            historyRepository.addFromNluResult(query = query, nluResult = result)
        }
    }

    val speechToText = rememberSpeechToText(
        onSpeechResult = { recognizedText ->
            nluEngineManager.processQuery(recognizedText)
        }
    )

    // Xử lý bật tắt thu âm an toàn: Chặn kích hoạt khi AI đang bận suy luận
    val handleToggleListeningSafe: () -> Unit = {
        if (isNluProcessing) {
            Toast.makeText(context, "AI đang phân tích câu lệnh, vui lòng đợi...", Toast.LENGTH_SHORT).show()
        } else {
            speechToText.toggleListening()
        }
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
                onMicClick = handleToggleListeningSafe,
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
                    onToggleListening = handleToggleListeningSafe,
                    nluResult = nluResult,
                    isNluProcessing = isNluProcessing,
                    modelState = modelState,
                    onSuggestionClick = { prompt ->
                        if (!isNluProcessing) {
                            nluEngineManager.processQuery(prompt)
                        }
                    }
                )

                NavTab.HISTORY -> HistoryScreen(
                    historyItems = historyItems,
                    onRerunCommand = { query ->
                        nluEngineManager.processQuery(query)
                        selectedTab = NavTab.ASSISTANT
                    },
                    onDeleteItem = { id ->
                        scope.launch { historyRepository.deleteItem(id) }
                    },
                    onClearAll = {
                        scope.launch { historyRepository.clearHistory() }
                    }
                )

                NavTab.SETTINGS -> SettingsScreen(modelState = modelState)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    ViDroidCallTheme(dynamicColor = false) {
        HomeScreen()
    }
}
