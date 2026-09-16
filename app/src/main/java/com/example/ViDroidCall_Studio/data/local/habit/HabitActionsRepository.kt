// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.data.local.habit

import android.content.Context
import com.example.ViDroidCall_Studio.domain.model.NativeAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Ghi việc đã thực thi và trả về tối đa 5 lối tắt (snapshot 1 lần/ngày).
 */
class HabitActionsRepository(context: Context) {
    private val dbHelper = HabitActionsDatabaseHelper(context.applicationContext)

    val quickActionsFlow: Flow<List<HabitQuickAction>> = flow {
        dbHelper.quickActionsFlow().collect {
            emit(loadQuickActions())
        }
    }

    suspend fun record(action: NativeAction, nowMs: Long = System.currentTimeMillis()) {
        if (!NativeActionCodec.isEligible(action)) return
        val slotKey = NativeActionCodec.slotKey(action) ?: return
        dbHelper.upsertAction(
            HabitActionRecord(
                slotKey = slotKey,
                intent = action.intentName,
                label = NativeActionCodec.quickLabel(action),
                actionJson = NativeActionCodec.toJson(action),
                lastUsedMs = nowMs
            ),
            eventAtMs = nowMs
        )
    }

    suspend fun loadQuickActions(nowMs: Long = System.currentTimeMillis()): List<HabitQuickAction> {
        val records = dbHelper.loadRecords()
        val events = dbHelper.loadEvents()
        val candidates = HabitQuickActionSelector.rankCandidates(records, events, nowMs)
        val previous = dbHelper.loadSnapshot()
        val (snapshot, refreshed) = HabitQuickActionSelector.resolveSnapshot(
            candidates = candidates,
            previous = previous,
            nowMs = nowMs
        )
        if (refreshed) {
            dbHelper.saveSnapshot(snapshot)
        }
        return HabitQuickActionSelector.toQuickActions(snapshot, records)
    }
}
