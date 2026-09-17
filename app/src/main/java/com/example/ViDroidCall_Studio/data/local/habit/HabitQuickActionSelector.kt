// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.data.local.habit

/**
 * Top 5 theo số lần dùng trong 3 ngày. Danh sách lối tắt đóng băng đến hết ngày lịch.
 */
object HabitQuickActionSelector {

    data class ActionEvent(
        val slotKey: String,
        val intent: String,
        val label: String,
        val actionJson: String,
        val timestampMs: Long
    )

    data class DailySnapshot(
        val dayId: String,
        val slotKeys: List<String>
    )

    fun topQuickActions(
        events: List<ActionEvent>,
        limit: Int = HabitRules.MAX_QUICK_ACTIONS
    ): List<HabitQuickAction> {
        if (events.isEmpty() || limit <= 0) return emptyList()
        return events
            .filter { it.slotKey.isNotBlank() && it.actionJson.isNotBlank() }
            .groupBy { it.slotKey }
            .map { (_, group) ->
                val latest = group.maxBy { it.timestampMs }
                HabitQuickAction(
                    slotKey = latest.slotKey,
                    intent = latest.intent,
                    label = latest.label,
                    actionJson = latest.actionJson,
                    hits = group.size
                ) to group.maxOf { it.timestampMs }
            }
            .sortedWith(
                compareByDescending<Pair<HabitQuickAction, Long>> { it.first.hits }
                    .thenByDescending { it.second }
            )
            .take(limit)
            .map { it.first }
    }

    fun shouldRefreshSnapshot(snapshot: DailySnapshot?, todayId: String): Boolean {
        return snapshot == null || snapshot.dayId != todayId
    }

    fun actionsForKeys(
        keys: List<String>,
        events: List<ActionEvent>
    ): List<HabitQuickAction> {
        if (keys.isEmpty()) return emptyList()
        val ranked = topQuickActions(events, limit = Int.MAX_VALUE).associateBy { it.slotKey }
        return keys.mapNotNull { ranked[it] }
    }
}
