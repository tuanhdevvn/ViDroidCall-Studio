// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
    const val WINDOW_DAYS = 3
    const val MAX_HISTORY_ITEMS = 10
    const val MAX_QUICK_ACTIONS = 5

    const val WINDOW_MS = WINDOW_DAYS * 24L * 60L * 60L * 1000L

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
