// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.data.local.habit

import com.example.ViDroidCall_Studio.domain.model.NativeAction

/**
 * Năm lệnh mẫu để bảng “Hay dùng” hiện ngay khi DB còn trống.
 */
object HabitDemoSeed {
    fun sampleActions(): List<NativeAction> {
        return listOf(
            NativeAction.OpenApp(appName = "zalo"),
            NativeAction.OpenApp(appName = "youtube"),
            NativeAction.SetAlarm(hour = 5, minute = 0),
            NativeAction.CallContact(contact = "Mai", phoneNumber = "0912345678"),
            NativeAction.SearchVideo(query = "nhạc bolero")
        )
    }

    fun records(nowMs: Long): List<HabitActionRecord> {
        return sampleActions().mapNotNull { action ->
            val slotKey = NativeActionCodec.slotKey(action) ?: return@mapNotNull null
            HabitActionRecord(
                slotKey = slotKey,
                intent = action.intentName,
                label = NativeActionCodec.quickLabel(action),
                actionJson = NativeActionCodec.toJson(action),
                lastUsedMs = nowMs
            )
        }
    }

    /** Ba lần dùng gần đây, cùng khung giờ hiện tại, đủ MIN_HITS. */
    fun eventTimestamps(nowMs: Long): List<Long> {
        return listOf(nowMs, nowMs - 60_000L, nowMs - 120_000L)
    }
}
