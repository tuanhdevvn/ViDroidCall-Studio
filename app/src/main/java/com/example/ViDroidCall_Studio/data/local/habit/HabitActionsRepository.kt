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

import android.content.Context
import com.example.ViDroidCall_Studio.data.local.history.CommandEventDatabaseHelper
import com.example.ViDroidCall_Studio.data.local.history.CommandHistoryRepository
import com.example.ViDroidCall_Studio.domain.model.NativeAction
import kotlinx.coroutines.flow.Flow

/**
 * Câu lệnh nhanh: top 5 `slot_key` trong 3 ngày, làm mới 1 lần/ngày.
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
