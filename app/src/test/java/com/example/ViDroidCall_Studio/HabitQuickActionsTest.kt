// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.data.local.habit.HabitActionRecord
import com.example.ViDroidCall_Studio.data.local.habit.HabitDemoSeed
import com.example.ViDroidCall_Studio.data.local.habit.HabitEvent
import com.example.ViDroidCall_Studio.data.local.habit.HabitQuickActionSelector
import com.example.ViDroidCall_Studio.data.local.habit.HabitRules
import com.example.ViDroidCall_Studio.data.local.habit.HabitSnapshot
import com.example.ViDroidCall_Studio.data.local.habit.HabitTimeBucket
import com.example.ViDroidCall_Studio.data.local.habit.NativeActionCodec
import com.example.ViDroidCall_Studio.domain.model.NativeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class HabitQuickActionsTest {

    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    private val morningMs = localTimeMs(2026, 9, 16, 7, 0)

    @Test
    fun bucketForHour_mapsMorningAfternoonEvening() {
        assertEquals(HabitTimeBucket.MORNING, HabitRules.bucketForHour(5))
        assertEquals(HabitTimeBucket.MORNING, HabitRules.bucketForHour(10))
        assertEquals(HabitTimeBucket.AFTERNOON, HabitRules.bucketForHour(11))
        assertEquals(HabitTimeBucket.AFTERNOON, HabitRules.bucketForHour(17))
        assertEquals(HabitTimeBucket.EVENING, HabitRules.bucketForHour(18))
        assertEquals(HabitTimeBucket.EVENING, HabitRules.bucketForHour(2))
    }

    @Test
    fun codec_excludesGreetingAndUnsupported() {
        val greeting = NativeAction.Informational(
            intentName = "greeting",
            message = "Xin chào",
            speechText = "Xin chào"
        )
        assertFalse(NativeActionCodec.isEligible(greeting))
        assertNull(NativeActionCodec.slotKey(greeting))

        val unsupported = NativeAction.Unsupported(intentName = "unsupported")
        assertFalse(NativeActionCodec.isEligible(unsupported))
    }

    @Test
    fun codec_callContact_usesIntentLabelNotUtterance() {
        val action = NativeAction.CallContact(contact = "Mai", phoneNumber = "0912345678")
        assertTrue(NativeActionCodec.isEligible(action))
        assertEquals("call_contact|mai", NativeActionCodec.slotKey(action))
        assertEquals("Gọi Mai", NativeActionCodec.quickLabel(action))

        val restored = NativeActionCodec.fromJson(NativeActionCodec.toJson(action))
        assertNotNull(restored)
        val call = restored as NativeAction.CallContact
        assertEquals("Mai", call.contact)
        assertEquals("0912345678", call.phoneNumber)
        assertTrue(call.requiresConfirmation)
    }

    @Test
    fun codec_sendSms_samePersonSharesSlotRegardlessOfMessage() {
        val first = NativeAction.SendSms(contact = "Mai", phoneNumber = "09", message = "A")
        val second = NativeAction.SendSms(contact = "Mai", phoneNumber = "09", message = "B")
        assertEquals(NativeActionCodec.slotKey(first), NativeActionCodec.slotKey(second))
        assertEquals("Nhắn Mai", NativeActionCodec.quickLabel(first))
    }

    @Test
    fun codec_openApp_roundTrip() {
        val action = NativeAction.OpenApp(appName = "youtube")
        assertEquals("open_app|youtube", NativeActionCodec.slotKey(action))
        assertEquals("Mở YouTube", NativeActionCodec.quickLabel(action))
        val restored = NativeActionCodec.fromJson(NativeActionCodec.toJson(action)) as NativeAction.OpenApp
        assertEquals("youtube", restored.appName)
    }

    @Test
    fun rank_requiresTwoHitsInsideWindow() {
        val mai = record("call_contact|mai", "Gọi Mai", morningMs)
        val events = listOf(
            HabitEvent(mai.slotKey, morningMs),
            HabitEvent(mai.slotKey, morningMs - TimeUnit.HOURS.toMillis(2))
        )
        val ranked = HabitQuickActionSelector.rankCandidates(listOf(mai), events, morningMs)
        assertEquals(1, ranked.size)
        assertEquals(2, ranked[0].hits14d)
    }

    @Test
    fun rank_ignoresSingleHitAndOldEvents() {
        val mai = record("call_contact|mai", "Gọi Mai", morningMs)
        val zalo = record("open_app|zalo", "Mở Zalo", morningMs)
        val events = listOf(
            HabitEvent(mai.slotKey, morningMs),
            HabitEvent(zalo.slotKey, morningMs - TimeUnit.HOURS.toMillis(25)),
            HabitEvent(zalo.slotKey, morningMs - TimeUnit.HOURS.toMillis(26))
        )
        val ranked = HabitQuickActionSelector.rankCandidates(listOf(mai, zalo), events, morningMs)
        assertTrue(ranked.none { it.record.slotKey == mai.slotKey })
        assertTrue(ranked.none { it.record.slotKey == zalo.slotKey })
    }

    @Test
    fun snapshot_freezesInsideSamePeriod() {
        val candidates = (1..6).map { index ->
            ranked("call_contact|p$index", "Gọi $index", hits = 3 + index)
        }
        val first = HabitQuickActionSelector.resolveSnapshot(candidates, null, morningMs, zone)
        assertTrue(first.second)
        assertEquals(5, first.first.slotKeys.size)

        val laterSamePeriod = morningMs + TimeUnit.DAYS.toMillis(1)
        val second = HabitQuickActionSelector.resolveSnapshot(candidates, first.first, laterSamePeriod, zone)
        assertFalse(second.second)
        assertEquals(first.first.slotKeys, second.first.slotKeys)
    }

    @Test
    fun snapshot_refreshesAfterPeriod() {
        val candidates = (1..5).map { index ->
            ranked("call_contact|p$index", "Gọi $index", hits = 3)
        }
        val first = HabitQuickActionSelector.resolveSnapshot(candidates, null, morningMs, zone)
        val nextPeriod = morningMs + HabitRules.SNAPSHOT_MS
        val second = HabitQuickActionSelector.resolveSnapshot(candidates, first.first, nextPeriod, zone)
        assertTrue(second.second)
        assertEquals(HabitQuickActionSelector.periodId(nextPeriod), second.first.dayId)
    }

    @Test
    fun demoSeed_ranksFiveActionsInCurrentWindow() {
        val records = HabitDemoSeed.records(morningMs)
        val events = records.flatMap { record ->
            HabitDemoSeed.eventTimestamps(morningMs).map { ts -> HabitEvent(record.slotKey, ts) }
        }
        val ranked = HabitQuickActionSelector.rankCandidates(records, events, morningMs)
        assertEquals(5, ranked.size)
        assertTrue(ranked.all { it.hits14d >= HabitRules.MIN_HITS })
        val snapshot = HabitQuickActionSelector.resolveSnapshot(ranked, null, morningMs, zone)
        assertEquals(5, snapshot.first.slotKeys.size)
        val labels = HabitQuickActionSelector.toQuickActions(snapshot.first, records).map { it.label }
        assertTrue(labels.contains("Mở Zalo"))
        assertTrue(labels.contains("Mở YouTube"))
        assertTrue(labels.contains("Báo thức 5 giờ"))
        assertTrue(labels.contains("Gọi Mai"))
        assertTrue(labels.contains("Video nhạc bolero"))
    }

    @Test
    fun snapshot_replacesOnlyWhenChallengerHasClearLead() {
        val yesterday = HabitSnapshot(
            dayId = LocalDate.of(2026, 9, 15).toString(),
            slotKeys = listOf("a", "b", "c", "d", "e")
        )
        val candidates = listOf(
            ranked("a", "A", hits = 4),
            ranked("b", "B", hits = 4),
            ranked("c", "C", hits = 4),
            ranked("d", "D", hits = 4),
            ranked("e", "E", hits = 4),
            ranked("f", "F", hits = 10)
        )
        val next = HabitQuickActionSelector.resolveSnapshot(candidates, yesterday, morningMs, zone)
        assertTrue(next.second)
        assertTrue(next.first.slotKeys.contains("f"))
        assertEquals(5, next.first.slotKeys.size)
    }

    @Test
    fun snapshot_keepsStableWhenLeadIsSmall() {
        val yesterday = HabitSnapshot(
            dayId = LocalDate.of(2026, 9, 15).toString(),
            slotKeys = listOf("a", "b", "c", "d", "e")
        )
        val candidates = listOf(
            ranked("a", "A", hits = 4),
            ranked("b", "B", hits = 4),
            ranked("c", "C", hits = 4),
            ranked("d", "D", hits = 4),
            ranked("e", "E", hits = 4),
            ranked("f", "F", hits = 5)
        )
        val next = HabitQuickActionSelector.resolveSnapshot(candidates, yesterday, morningMs, zone)
        assertEquals(yesterday.slotKeys, next.first.slotKeys)
        assertFalse(next.first.slotKeys.contains("f"))
    }

    private fun record(key: String, label: String, lastUsed: Long): HabitActionRecord {
        return HabitActionRecord(
            slotKey = key,
            intent = key.substringBefore("|"),
            label = label,
            actionJson = "{}",
            lastUsedMs = lastUsed
        )
    }

    private fun ranked(key: String, label: String, hits: Int): HabitQuickActionSelector.RankedCandidate {
        return HabitQuickActionSelector.RankedCandidate(
            record = record(key, label, morningMs),
            hits14d = hits,
            bucketHits = hits
        )
    }

    private fun localTimeMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return LocalDate.of(year, month, day)
            .atTime(hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }
}
