package com.example.ViDroidCall_Studio.feature.history.model

/**
 * Model dữ liệu câu lệnh lịch sử
 */
data class CommandHistoryItem(
    val id: Long = 0L,
    val commandText: String,
    val time: String,
    val status: String = "Thành công",
    val category: String = "Hệ thống",
    val timestamp: Long = System.currentTimeMillis()
)
