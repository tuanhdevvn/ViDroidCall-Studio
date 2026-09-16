// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.data.local.habit

import java.time.ZoneId
import java.util.Calendar

/**
 * Chọn tối đa 5 lối tắt: cửa sổ 24 giờ, snapshot 3 ngày, hysteresis 1.5×.
 */
object HabitQuickActionSelector {

    data class RankedCandidate(
        val record: HabitActionRecord,
        val hits14d: Int,
        val bucketHits: Int
    )

    fun periodId(nowMs: Long): String {
        return (nowMs / HabitRules.SNAPSHOT_MS).toString()
    }

    fun hourOf(nowMs: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = nowMs
        return calendar.get(Calendar.HOUR_OF_DAY)
    }

    fun rankCandidates(
        records: List<HabitActionRecord>,
        events: List<HabitEvent>,
        nowMs: Long
    ): List<RankedCandidate> {
        val windowStart = nowMs - HabitRules.WINDOW_MS
        val staleBefore = nowMs - HabitRules.STALE_MS
        val currentBucket = HabitRules.bucketForHour(hourOf(nowMs))
        val recordByKey = records.associateBy { it.slotKey }

        val grouped = events.filter { it.timestampMs >= windowStart }
            .groupBy { it.slotKey }

        return grouped.mapNotNull { (key, keyEvents) ->
            val record = recordByKey[key] ?: return@mapNotNull null
            if (record.lastUsedMs < staleBefore) return@mapNotNull null
            val hits = keyEvents.size
            if (hits < HabitRules.MIN_HITS) return@mapNotNull null
            val bucketHits = keyEvents.count {
                HabitRules.bucketForHour(hourOf(it.timestampMs)) == currentBucket
            }
            RankedCandidate(record = record, hits14d = hits, bucketHits = bucketHits)
        }.sortedWith(
            compareByDescending<RankedCandidate> { it.bucketHits }
                .thenByDescending { it.hits14d }
                .thenByDescending { it.record.lastUsedMs }
        )
    }

    /**
     * @return snapshot mới và cờ đã refresh (hết chu kỳ 3 ngày hoặc chưa có snapshot).
     */
    fun resolveSnapshot(
        candidates: List<RankedCandidate>,
        previous: HabitSnapshot?,
        nowMs: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): Pair<HabitSnapshot, Boolean> {
        val currentPeriod = periodId(nowMs)
        if (previous != null && previous.dayId == currentPeriod) {
            return previous to false
        }

        val rankedKeys = candidates.map { it.record.slotKey }
        val hitsByKey = candidates.associate { it.record.slotKey to it.hits14d }
        val eligible = rankedKeys.toSet()

        val kept = previous?.slotKeys.orEmpty().filter { it in eligible }.toMutableList()
        val remaining = rankedKeys.filter { it !in kept }.toMutableList()

        while (kept.size < HabitRules.MAX_QUICK_ACTIONS && remaining.isNotEmpty()) {
            kept.add(remaining.removeAt(0))
        }

        while (kept.size == HabitRules.MAX_QUICK_ACTIONS && remaining.isNotEmpty()) {
            val weakestKey = kept.minByOrNull { hitsByKey[it] ?: 0 } ?: break
            val challenger = remaining.first()
            val weakHits = hitsByKey[weakestKey] ?: 0
            val newHits = hitsByKey[challenger] ?: 0
            if (newHits >= weakHits * HabitRules.REPLACE_RATIO && newHits > weakHits) {
                kept.remove(weakestKey)
                kept.add(challenger)
                remaining.removeAt(0)
            } else {
                break
            }
        }

        return HabitSnapshot(
            dayId = currentPeriod,
            slotKeys = kept.take(HabitRules.MAX_QUICK_ACTIONS)
        ) to true
    }

    fun toQuickActions(
        snapshot: HabitSnapshot,
        records: List<HabitActionRecord>
    ): List<HabitQuickAction> {
        val byKey = records.associateBy { it.slotKey }
        return snapshot.slotKeys.mapNotNull { key ->
            val record = byKey[key] ?: return@mapNotNull null
            HabitQuickAction(
                slotKey = record.slotKey,
                intent = record.intent,
                label = record.label,
                actionJson = record.actionJson
            )
        }
    }
}
