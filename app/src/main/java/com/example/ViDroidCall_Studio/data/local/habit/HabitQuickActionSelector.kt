// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.data.local.habit

/**
 * Chọn tối đa 5 lối tắt theo số lần dùng (cùng `slot_key`) trong cửa sổ 24 giờ.
 */
object HabitQuickActionSelector {

    data class ActionEvent(
        val slotKey: String,
        val intent: String,
        val label: String,
        val actionJson: String,
        val timestampMs: Long
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
}
