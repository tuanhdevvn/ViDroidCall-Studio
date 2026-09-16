// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.data.local.habit

/**
 * Một việc đã xác nhận (intent + slot), không phải câu STT.
 */
data class HabitActionRecord(
    val slotKey: String,
    val intent: String,
    val label: String,
    val actionJson: String,
    val lastUsedMs: Long
)

data class HabitEvent(
    val slotKey: String,
    val timestampMs: Long
)

data class HabitSnapshot(
    val dayId: String,
    val slotKeys: List<String>
)

/**
 * Lối tắt hiển thị trên tab Lịch sử — bấm chạy NativeAction đã lưu.
 */
data class HabitQuickAction(
    val slotKey: String,
    val intent: String,
    val label: String,
    val actionJson: String
)

enum class HabitTimeBucket {
    MORNING,
    AFTERNOON,
    EVENING
}

object HabitRules {
    const val WINDOW_DAYS = 14
    const val STALE_DAYS = 30
    const val PURGE_DAYS = 60
    const val MIN_HITS = 2
    const val MAX_QUICK_ACTIONS = 5
    const val REPLACE_RATIO = 1.5

    const val WINDOW_MS = WINDOW_DAYS * 24L * 60L * 60L * 1000L
    const val STALE_MS = STALE_DAYS * 24L * 60L * 60L * 1000L
    const val PURGE_MS = PURGE_DAYS * 24L * 60L * 60L * 1000L

    fun bucketForHour(hour: Int): HabitTimeBucket {
        return when (hour) {
            in 5..10 -> HabitTimeBucket.MORNING
            in 11..17 -> HabitTimeBucket.AFTERNOON
            else -> HabitTimeBucket.EVENING
        }
    }

    fun bucketTitle(bucket: HabitTimeBucket): String {
        return when (bucket) {
            HabitTimeBucket.MORNING -> "Hay dùng buổi sáng"
            HabitTimeBucket.AFTERNOON -> "Hay dùng buổi chiều"
            HabitTimeBucket.EVENING -> "Hay dùng buổi tối"
        }
    }
}
