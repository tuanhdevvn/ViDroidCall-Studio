// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.data.local.habit

/**
 * Lối tắt hiển thị trên tab Lịch sử — bấm chạy NativeAction đã lưu.
 */
data class HabitQuickAction(
    val slotKey: String,
    val intent: String,
    val label: String,
    val actionJson: String,
    val hits: Int = 0
)

enum class HabitTimeBucket {
    MORNING,
    AFTERNOON,
    EVENING
}

object HabitRules {
    const val WINDOW_HOURS = 24
    const val MAX_HISTORY_ITEMS = 10
    const val MAX_QUICK_ACTIONS = 5

    const val WINDOW_MS = WINDOW_HOURS * 60L * 60L * 1000L

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
