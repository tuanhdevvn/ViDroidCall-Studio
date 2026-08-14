package com.example.emma_vidroidcall.feature.home

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
import com.example.emma_vidroidcall.feature.assistant.AssistantScreen
import com.example.emma_vidroidcall.feature.history.HistoryScreen
import com.example.emma_vidroidcall.feature.history.model.CommandHistoryItem
import com.example.emma_vidroidcall.feature.settings.SettingsScreen
import com.example.emma_vidroidcall.feature.speech.rememberSpeechToText
import com.example.emma_vidroidcall.ui.component.CustomBottomMenuBar
import com.example.emma_vidroidcall.ui.component.NavTab
import com.example.emma_vidroidcall.ui.theme.EmmaViDroidCallTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(NavTab.ASSISTANT) }
    val speechToText = rememberSpeechToText()

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
                    onToggleListening = speechToText.toggleListening
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
