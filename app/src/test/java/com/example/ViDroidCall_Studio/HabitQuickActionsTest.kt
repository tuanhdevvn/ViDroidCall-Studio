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

package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.data.local.habit.HabitQuickActionSelector
import com.example.ViDroidCall_Studio.data.local.habit.HabitRules
import com.example.ViDroidCall_Studio.data.local.habit.HabitTimeBucket
import com.example.ViDroidCall_Studio.data.local.habit.NativeActionCodec
import com.example.ViDroidCall_Studio.data.local.history.CommandEventDatabaseHelper
import com.example.ViDroidCall_Studio.domain.model.NativeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitQuickActionsTest {

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
    fun topQuickActions_ranksByHitCountThenRecency() {
        val now = 1_000_000L
        val events = listOf(
            event("call_contact|mai", "Gọi Mai", now),
            event("call_contact|mai", "Gọi Mai", now - 1_000),
            event("call_contact|mai", "Gọi Mai", now - 2_000),
            event("open_app|zalo", "Mở Zalo", now - 100),
            event("open_app|zalo", "Mở Zalo", now - 200),
            event("search_web|thoi tiet", "Tìm thời tiết", now)
        )
        val top = HabitQuickActionSelector.topQuickActions(events, limit = 5)
        assertEquals(listOf("call_contact|mai", "open_app|zalo", "search_web|thoi tiet"), top.map { it.slotKey })
        assertEquals(3, top[0].hits)
        assertEquals(2, top[1].hits)
        assertEquals(1, top[2].hits)
    }

    @Test
    fun topQuickActions_capsAtFiveAndKeepsLatestJson() {
        val now = 1_000_000L
        val events = (1..7).flatMap { index ->
            listOf(
                event("open_app|app$index", "Mở $index", now - index, json = """{"n":$index}"""),
                event("open_app|app$index", "Mở $index mới", now + index, json = """{"n":${index + 10}}""")
            )
        }
        val top = HabitQuickActionSelector.topQuickActions(events, limit = 5)
        assertEquals(5, top.size)
        assertTrue(top.all { it.hits == 2 })
        val app7 = top.first { it.slotKey == "open_app|app7" }
        assertEquals("Mở 7 mới", app7.label)
        assertEquals("""{"n":17}""", app7.actionJson)
    }

    @Test
    fun topQuickActions_ignoresBlankSlotOrJson() {
        val events = listOf(
            event("", "Trống", 1L),
            event("open_app|zalo", "Mở Zalo", 2L, json = "")
        )
        assertTrue(HabitQuickActionSelector.topQuickActions(events).isEmpty())
    }

    @Test
    fun snapshot_freezesSameCalendarDay() {
        assertFalse(
            HabitQuickActionSelector.shouldRefreshSnapshot(
                HabitQuickActionSelector.DailySnapshot("2026-09-17", listOf("a")),
                todayId = "2026-09-17"
            )
        )
        assertTrue(
            HabitQuickActionSelector.shouldRefreshSnapshot(
                HabitQuickActionSelector.DailySnapshot("2026-09-16", listOf("a")),
                todayId = "2026-09-17"
            )
        )
        assertTrue(HabitQuickActionSelector.shouldRefreshSnapshot(null, todayId = "2026-09-17"))
    }

    @Test
    fun actionsForKeys_keepsFrozenOrder() {
        val events = listOf(
            event("open_app|zalo", "Mở Zalo", 3L),
            event("open_app|zalo", "Mở Zalo", 2L),
            event("call_contact|mai", "Gọi Mai", 1L)
        )
        val frozen = HabitQuickActionSelector.actionsForKeys(
            listOf("call_contact|mai", "open_app|zalo"),
            events
        )
        assertEquals(listOf("call_contact|mai", "open_app|zalo"), frozen.map { it.slotKey })
        assertEquals(2, frozen[1].hits)
    }

    @Test
    fun window_isThreeDays() {
        assertEquals(3, HabitRules.WINDOW_DAYS)
        assertEquals(3L * 24 * 60 * 60 * 1000, HabitRules.WINDOW_MS)
    }

    @Test
    fun dayId_usesYearMonthDayFormat() {
        assertTrue(
            CommandEventDatabaseHelper.dayId(1_746_460_800_000L)
                .matches(Regex("""\d{4}-\d{2}-\d{2}"""))
        )
    }

    private fun event(
        slotKey: String,
        label: String,
        timestampMs: Long,
        json: String = "{}"
    ): HabitQuickActionSelector.ActionEvent {
        return HabitQuickActionSelector.ActionEvent(
            slotKey = slotKey,
            intent = slotKey.substringBefore("|"),
            label = label,
            actionJson = json,
            timestampMs = timestampMs
        )
    }
}
