// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.data.local.history

import android.content.Context
import com.example.ViDroidCall_Studio.data.model.NluResult
import com.example.ViDroidCall_Studio.feature.history.model.CommandHistoryItem
import kotlinx.coroutines.flow.Flow

/**
 * Lịch sử câu lệnh: 10 dòng mới nhất từ SQLite chung `vidroidcall_commands.db`.
 */
class CommandHistoryRepository(context: Context) {
    private val dbHelper = CommandEventDatabaseHelper.get(context)

    val historyFlow: Flow<List<CommandHistoryItem>> = dbHelper.historyFlow()

    suspend fun addCommand(
        commandText: String,
        category: String = "Hệ thống",
        status: String = "Thành công"
    ): Long {
        if (commandText.isBlank()) return -1L
        return dbHelper.insertHistory(commandText.trim(), category, status)
    }

    suspend fun addFromNluResult(query: String, nluResult: NluResult?) {
        if (query.isBlank()) return

        val category = categoryForIntent(nluResult?.intent)

        val status = if (nluResult != null && nluResult.isParsedSuccessfully && nluResult.errorMessage == null) {
            "Thành công"
        } else {
            "Chưa rõ"
        }

        addCommand(commandText = query, category = category, status = status)
    }

    suspend fun deleteItem(id: Long) {
        dbHelper.deleteById(id)
    }

    suspend fun clearHistory() {
        dbHelper.clearAll()
    }

    companion object {
        fun categoryForIntent(intent: String?): String {
            return when (intent) {
                "call_contact" -> "Cuộc gọi"
                "send_sms" -> "Tin nhắn"
                "set_alarm" -> "Báo thức"
                "set_timer" -> "Hẹn giờ"
                "open_map" -> "Bản đồ"
                "open_app" -> "Ứng dụng"
                "search_web" -> "Tìm web"
                "search_video" -> "Video"
                "play_music" -> "Nhạc"
                "greeting" -> "Chào hỏi"
                "goodbye" -> "Tạm biệt"
                "clarify" -> "Hỏi lại"
                "unsupported" -> "Không hỗ trợ"
                else -> "Hệ thống"
            }
        }
    }
}
