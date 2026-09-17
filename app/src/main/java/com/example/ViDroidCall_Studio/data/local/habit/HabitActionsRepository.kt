// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.data.local.habit

import android.content.Context
import com.example.ViDroidCall_Studio.data.local.history.CommandEventDatabaseHelper
import com.example.ViDroidCall_Studio.data.local.history.CommandHistoryRepository
import com.example.ViDroidCall_Studio.domain.model.NativeAction
import kotlinx.coroutines.flow.Flow

/**
 * Câu lệnh nhanh: top 5 `slot_key` trong 24 giờ, cùng SQLite với lịch sử.
 */
class HabitActionsRepository(context: Context) {
    private val dbHelper = CommandEventDatabaseHelper.get(context)

    val quickActionsFlow: Flow<List<HabitQuickAction>> = dbHelper.quickActionsFlow()

    suspend fun record(action: NativeAction, nowMs: Long = System.currentTimeMillis()) {
        if (!NativeActionCodec.isEligible(action)) return
        val slotKey = NativeActionCodec.slotKey(action) ?: return
        dbHelper.attachOrInsertAction(
            slotKey = slotKey,
            intent = action.intentName,
            label = NativeActionCodec.quickLabel(action),
            actionJson = NativeActionCodec.toJson(action),
            category = CommandHistoryRepository.categoryForIntent(action.intentName),
            nowMs = nowMs
        )
    }

    suspend fun loadQuickActions(nowMs: Long = System.currentTimeMillis()): List<HabitQuickAction> {
        return dbHelper.getQuickActions(nowMs)
    }
}
