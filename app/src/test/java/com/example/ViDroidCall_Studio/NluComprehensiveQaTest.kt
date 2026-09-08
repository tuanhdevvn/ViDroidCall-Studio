// SPDX-License-Identifier: Apache-2.0

package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.data.model.NluIntent
import com.example.ViDroidCall_Studio.data.model.NluJsonParser
import com.example.ViDroidCall_Studio.data.model.NluResult
import com.example.ViDroidCall_Studio.data.nlu.FastPathMatcher
import com.example.ViDroidCall_Studio.data.nlu.NluActionDispatcher
import com.example.ViDroidCall_Studio.data.nlu.NluConstants
import com.example.ViDroidCall_Studio.data.nlu.TimeProvider
import com.example.ViDroidCall_Studio.domain.model.NativeAction
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive QA Test suite verifying all 13 NLU intents,
 * Fast-Path, Safety Net, Clarification, and Native Action Dispatcher.
 */
class NluComprehensiveQaTest {

    private val matcher = FastPathMatcher(context = null)
    private val fixedTimeMatcher = FastPathMatcher(
        context = null,
        timeProvider = TimeProvider.createFixed(hour = 8, minute = 0, second = 0)
    )

    @Test
    fun testAll13IntentsExistenceAndCoverage() {
        val allIntents = listOf(
            NluIntent.SET_ALARM,
            NluIntent.SET_TIMER,
            NluIntent.OPEN_APP,
            NluIntent.OPEN_MAP,
            NluIntent.CALL_CONTACT,
            NluIntent.SEND_SMS,
            NluIntent.SEARCH_VIDEO,
            NluIntent.PLAY_MUSIC,
            NluIntent.SEARCH_WEB,
            NluIntent.CLARIFY,
            NluIntent.GREETING,
            NluIntent.GOODBYE,
            NluIntent.UNSUPPORTED
        )
        assertEquals(13, allIntents.size)

        for (intent in allIntents) {
            val fromVal = NluIntent.fromValue(intent.value)
            assertEquals(intent, fromVal)
        }
    }

    @Test
    fun testIntent1_SetAlarm() {
        val res = fixedTimeMatcher.match("đặt báo thức 6 giờ 30 phút sáng")
        assertNotNull(res)
        assertEquals("set_alarm", res?.intent)
        val json = JSONObject(res!!.argumentsJson)
        assertEquals(6, json.optInt("hour"))
        assertEquals(30, json.optInt("minute"))

        val action = NativeAction.fromNluResult(res)
        assertTrue(action is NativeAction.SetAlarm)
        val alarm = action as NativeAction.SetAlarm
        assertEquals(6, alarm.hour)
        assertEquals(30, alarm.minute)
        assertEquals("Đang đặt báo thức lúc 6 giờ 30 phút", alarm.getSpeechFeedbackText())
    }

    @Test
    fun testIntent2_SetTimer() {
        val res = matcher.match("hẹn giờ 15 phút")
        assertNotNull(res)
        assertEquals("set_timer", res?.intent)
        val json = JSONObject(res!!.argumentsJson)
        assertEquals(15, json.optInt("duration"))
        assertEquals("minutes", json.optString("unit"))

        val action = NativeAction.fromNluResult(res)
        assertTrue(action is NativeAction.SetTimer)
        val timer = action as NativeAction.SetTimer
        assertEquals(900, timer.durationSeconds)
        assertEquals("Đang hẹn giờ 15 phút", timer.getSpeechFeedbackText())
    }

    @Test
    fun testIntent3_OpenApp() {
        val res = matcher.match("mở ứng dụng youtube")
        assertNotNull(res)
        assertEquals("open_app", res?.intent)
        val json = JSONObject(res!!.argumentsJson)
        assertEquals("youtube", json.optString("app_name"))

        val action = NativeAction.fromNluResult(res)
        assertTrue(action is NativeAction.OpenApp)
        val openApp = action as NativeAction.OpenApp
        assertEquals("youtube", openApp.appName)
        assertEquals("Đang mở ứng dụng YouTube", openApp.getSpeechFeedbackText())
    }

    @Test
    fun testIntent4_OpenMap() {
        val queries = listOf("chỉ đường đến hồ gươm", "quán phở gần tôi", "cây xăng gần nhất")
        for (q in queries) {
            val res = matcher.match(q)
            assertNotNull("Query '$q' must match open_map", res)
            assertEquals("open_map", res?.intent)
            val action = NativeAction.fromNluResult(res!!)
            assertTrue(action is NativeAction.OpenMap)
        }
    }

    @Test
    fun testIntent5_CallContact() {
        val res = matcher.match("gọi cho anh Tuấn")
        assertNotNull(res)
        assertEquals("call_contact", res?.intent)
        val json = JSONObject(res!!.argumentsJson)
        assertEquals("anh Tuấn", json.optString("contact"))

        val action = NativeAction.fromNluResult(res)
        assertTrue(action is NativeAction.CallContact)
        val call = action as NativeAction.CallContact
        assertTrue(call.requiresConfirmation)
        assertEquals("Đang thực hiện cuộc gọi tới anh Tuấn", call.getSpeechFeedbackText())
    }

    @Test
    fun testIntent6_SendSms() {
        val res = matcher.match("nhắn tin cho mẹ là con đang về rồi")
        assertNotNull(res)
        assertEquals("send_sms", res?.intent)
        val json = JSONObject(res!!.argumentsJson)
        assertEquals("mẹ", json.optString("contact"))
        assertEquals("con đang về rồi", json.optString("message"))

        val action = NativeAction.fromNluResult(res)
        assertTrue(action is NativeAction.SendSms)
        val sms = action as NativeAction.SendSms
        assertTrue(sms.requiresConfirmation)
    }

    @Test
    fun testIntent7_SearchVideo() {
        val res = matcher.match("tìm video Sơn Tùng trên YouTube")
        assertNotNull(res)
        assertEquals("search_video", res?.intent)

        val action = NativeAction.fromNluResult(res!!)
        assertTrue(action is NativeAction.SearchVideo)
        val vid = action as NativeAction.SearchVideo
        assertTrue(vid.getSpeechFeedbackText().contains("Sơn Tùng"))
    }

    @Test
    fun testIntent8_PlayMusic() {
        val res = matcher.match("phát nhạc Trịnh Công Sơn")
        assertNotNull(res)
        assertEquals("play_music", res?.intent)

        val action = NativeAction.fromNluResult(res!!)
        assertTrue(action is NativeAction.PlayMusic)
        val music = action as NativeAction.PlayMusic
        assertTrue(music.getSpeechFeedbackText().contains("Trịnh Công Sơn") || music.getSpeechFeedbackText().contains("nhạc"))
    }

    @Test
    fun testIntent9_SearchWeb() {
        val queries = listOf(
            "Hà Anh Tuấn là ai",
            "VNeID là gì",
            "Hôm nay Hà Nội có mưa không",
            "thời tiết ngày mai",
            "giá vàng hôm nay"
        )
        for (q in queries) {
            val res = matcher.match(q)
            assertNotNull("Query '$q' must match search_web", res)
            assertEquals("search_web", res?.intent)
            assertEquals("low", res!!.riskLevel)
            assertFalse(res.requiresConfirmation)

            val action = NativeAction.fromNluResult(res)
            assertTrue(action is NativeAction.SearchWeb)
            val search = action as NativeAction.SearchWeb
            assertFalse(search.requiresConfirmation)
            assertEquals("Đang tìm: ${search.query}", search.getSpeechFeedbackText())
        }
    }

    @Test
    fun testIntent10_Clarify() {
        val clarifyQueries = listOf("tra cứu đi", "tìm trên mạng", "google giúp tôi", "tìm kiếm đi")
        for (q in clarifyQueries) {
            val res = matcher.match(q)
            assertNotNull("Query '$q' must match clarify", res)
            assertEquals("clarify", res?.intent)
            assertEquals("needs_clarification", res!!.status)

            val action = NativeAction.fromNluResult(res)
            assertTrue(action is NativeAction.Informational)
            val info = action as NativeAction.Informational
            assertEquals("Bạn muốn tìm kiếm thông tin gì?", info.speechText)
        }
    }

    @Test
    fun testIntent11_Greeting() {
        val res = matcher.match("xin chào")
        assertNotNull(res)
        assertEquals("greeting", res?.intent)
        val action = NativeAction.fromNluResult(res!!)
        assertTrue(action is NativeAction.Informational)
        assertEquals("Xin chào bạn, tôi có thể giúp gì cho bạn?", (action as NativeAction.Informational).speechText)
    }

    @Test
    fun testIntent12_Goodbye() {
        val res = matcher.match("tạm biệt")
        assertNotNull(res)
        assertEquals("goodbye", res?.intent)
        val action = NativeAction.fromNluResult(res!!)
        assertTrue(action is NativeAction.Informational)
        assertEquals("Tạm biệt bạn, hẹn gặp lại nhé!", (action as NativeAction.Informational).speechText)
    }

    @Test
    fun testIntent13_Unsupported() {
        val rawJson = """{"intent": "unsupported", "status": "unsupported"}"""
        val res = NluJsonParser.parse(rawJson)
        assertEquals("unsupported", res.intent)
        val action = NativeAction.fromNluResult(res)
        assertTrue(action is NativeAction.Unsupported)
        assertEquals("Xin lỗi, tôi chưa hỗ trợ tính năng này.", (action as NativeAction.Unsupported).getSpeechFeedbackText())
    }

    @Test
    fun testSafetyNetCallContactVsSearchWeb() {
        // Questions about identity/definitions should NEVER trigger call_contact:
        val questions = listOf(
            "Anh Tuấn là ai",
            "Chị Lan là ai",
            "Bác Hồ là ai",
            "VNeID là gì",
            "ChatGPT là gì",
            "Đức tính nghĩa là gì",
            "Ai là tổng thống Mỹ"
        )
        for (q in questions) {
            val res = matcher.match(q)
            assertNotNull("Question '$q' must match search_web", res)
            assertEquals("Must be search_web for '$q'", "search_web", res?.intent)
        }

        // Actual phone calls should ALWAYS remain call_contact:
        val calls = listOf(
            "gọi anh Tuấn",
            "gọi cho chị Lan",
            "gọi điện cho bác Hồ",
            "alo cho bạn Hùng"
        )
        for (c in calls) {
            val res = matcher.match(c)
            assertNotNull("Call '$c' must match call_contact", res)
            assertEquals("Must be call_contact for '$c'", "call_contact", res?.intent)
        }
    }

    @Test
    fun testNluConstantsWhitelist() {
        val prompt = NluConstants.MANDATORY_SYSTEM_PROMPT
        assertTrue(prompt.contains("search_web"))
        assertTrue(prompt.contains("X là ai"))
        assertTrue(prompt.contains("X là gì"))
        assertTrue(prompt.contains("thời tiết"))
    }
}
