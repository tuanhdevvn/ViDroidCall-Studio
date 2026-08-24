package com.example.ViDroidCall_Studio.data.local.history

import android.content.Context
import com.example.ViDroidCall_Studio.data.model.NluResult
import com.example.ViDroidCall_Studio.feature.history.model.CommandHistoryItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository quản lý dữ liệu lịch sử câu lệnh của ứng dụng
 */
class CommandHistoryRepository(context: Context) {
    private val dbHelper = CommandHistoryDatabaseHelper(context.applicationContext)

    val historyFlow: Flow<List<CommandHistoryItem>> = dbHelper.getAllHistoryFlow()

    suspend fun addCommand(
        commandText: String,
        category: String = "Hệ thống",
        status: String = "Thành công"
    ): Long {
        if (commandText.isBlank()) return -1L
        return dbHelper.insertCommand(commandText.trim(), category, status)
    }

    suspend fun addFromNluResult(query: String, nluResult: NluResult?) {
        if (query.isBlank()) return

        val category = when (nluResult?.intent) {
            "call_contact" -> "Cuộc gọi"
            "send_sms" -> "Tin nhắn"
            "set_alarm" -> "Báo thức"
            "set_timer" -> "Hẹn giờ"
            "open_map" -> "Bản đồ"
            "open_app" -> "Ứng dụng"
            "clarify" -> "Hỏi lại"
            else -> "Hệ thống"
        }

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
}
