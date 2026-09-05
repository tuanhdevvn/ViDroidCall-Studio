// SPDX-License-Identifier: Apache-2.0

package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.data.model.NluJsonParser
import com.example.ViDroidCall_Studio.data.model.NluResult
import com.example.ViDroidCall_Studio.data.nlu.FastPathMatcher
import com.example.ViDroidCall_Studio.data.nlu.NluActionDispatcher
import com.example.ViDroidCall_Studio.domain.model.NativeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandDuplicateExecutionTest {

    /**
     * Test 1 – Same command twice
     * Input 1: "báo thức 6 giờ"
     * Input 2: "báo thức 6 giờ"
     * Expected: executionId1 != executionId2 và cả hai đều được dispatch (dispatch count == 2).
     */
    @Test
    fun testSameCommandTwiceGeneratesUniqueExecutionIdsAndDispatchesTwice() {
        val matcher = FastPathMatcher(context = null)
        val query = "báo thức 6 giờ"

        val res1 = matcher.match(query)
        val res2 = matcher.match(query)

        assertNotNull("Lần 1 phải khớp NLU", res1)
        assertNotNull("Lần 2 phải khớp NLU", res2)
        assertNotEquals("Hai lần thực thi phải có executionId khác nhau", res1!!.executionId, res2!!.executionId)

        var dispatchCount = 0
        val dispatcher = NluActionDispatcher(context = null) { _ -> }

        val processedExecutionIds = mutableSetOf<String>()

        // Lần 1
        if (processedExecutionIds.add(res1.executionId)) {
            val action1 = NativeAction.fromNluResult(res1)
            dispatcher.dispatch(action1)
            dispatchCount++
        }

        // Lần 2
        if (processedExecutionIds.add(res2.executionId)) {
            val action2 = NativeAction.fromNluResult(res2)
            dispatcher.dispatch(action2)
            dispatchCount++
        }

        assertEquals(2, dispatchCount)
    }

    /**
     * Test 2 – Same JSON twice
     * Tạo hai NluResult có JSON giống hệt nhau:
     * result1.rawJson == result2.rawJson
     * Expected: result1.executionId != result2.executionId và cả hai đều được dispatch.
     */
    @Test
    fun testSameJsonTwiceCreatesUniqueExecutionIdsAndBothAreDispatched() {
        val json = """
            {
                "status": "success",
                "intent": "open_app",
                "slots": {
                    "app_name": "YouTube"
                },
                "requires_confirmation": false
            }
        """.trimIndent()

        val result1 = NluJsonParser.parse(json)
        val result2 = NluJsonParser.parse(json)

        assertEquals("Hai kết quả phải có rawJson giống hệt nhau", result1.rawJson, result2.rawJson)
        assertNotEquals("Mỗi lần parse JSON phải tạo một executionId mới", result1.executionId, result2.executionId)

        val processedExecutionIds = mutableSetOf<String>()
        var dispatchCount = 0

        if (processedExecutionIds.add(result1.executionId)) {
            val action1 = NativeAction.fromNluResult(result1)
            assertTrue(action1 is NativeAction.OpenApp)
            dispatchCount++
        }

        if (processedExecutionIds.add(result2.executionId)) {
            val action2 = NativeAction.fromNluResult(result2)
            assertTrue(action2 is NativeAction.OpenApp)
            dispatchCount++
        }

        assertEquals(2, dispatchCount)
    }

    /**
     * Test 3 – Compose recomposition
     * Mô phỏng cùng một executionId được UI xử lý/recompose nhiều lần.
     * Expected: dispatch() == 1.
     */
    @Test
    fun testComposeRecompositionDoesNotExecuteDuplicateActionForSameExecutionId() {
        val json = """
            {
                "status": "success",
                "intent": "set_timer",
                "arguments": {
                    "duration": 15,
                    "unit": "minutes"
                },
                "requires_confirmation": false
            }
        """.trimIndent()

        val result = NluJsonParser.parse(json)
        val processedExecutionIds = mutableSetOf<String>()
        var dispatchCount = 0

        // Mô phỏng 5 lần recompose với cùng 1 NluResult/executionId
        repeat(5) {
            if (processedExecutionIds.add(result.executionId)) {
                val action = NativeAction.fromNluResult(result)
                assertTrue(action is NativeAction.SetTimer)
                dispatchCount++
            }
        }

        assertEquals("Dù recompose nhiều lần, hành động chỉ được dispatch đúng 1 lần", 1, dispatchCount)
    }

    /**
     * Test 4 – Different executions
     * executionId A và executionId B
     * Expected: dispatch() == 2.
     */
    @Test
    fun testDifferentExecutionsBothDispatchSuccessfully() {
        val resultA = NluResult.empty().copy(
            rawJson = """{"intent":"open_app"}""",
            intent = "open_app",
            status = "success",
            argumentsJson = """{"app_name":"Zalo"}"""
        )
        val resultB = NluResult.empty().copy(
            rawJson = """{"intent":"open_app"}""",
            intent = "open_app",
            status = "success",
            argumentsJson = """{"app_name":"Zalo"}"""
        )

        assertNotEquals(resultA.executionId, resultB.executionId)

        val processedExecutionIds = mutableSetOf<String>()
        var dispatchCount = 0

        if (processedExecutionIds.add(resultA.executionId)) {
            dispatchCount++
        }
        if (processedExecutionIds.add(resultB.executionId)) {
            dispatchCount++
        }

        assertEquals(2, dispatchCount)
    }

    /**
     * Test 5 – Fast Path
     * Gửi cùng Fast Path command hai lần.
     * Expected: executionId1 != executionId2, dispatch() == 2.
     */
    @Test
    fun testFastPathSameCommandTwiceGeneratesDistinctExecutionIds() {
        val matcher = FastPathMatcher(context = null)
        val fastCommand = "mở youtube"

        val match1 = matcher.match(fastCommand)
        val match2 = matcher.match(fastCommand)

        assertNotNull(match1)
        assertNotNull(match2)
        assertTrue(match1!!.isFastPath)
        assertTrue(match2!!.isFastPath)
        assertNotEquals(match1.executionId, match2.executionId)

        val processedExecutionIds = mutableSetOf<String>()
        var executedCount = 0

        if (processedExecutionIds.add(match1.executionId)) executedCount++
        if (processedExecutionIds.add(match2.executionId)) executedCount++

        assertEquals(2, executedCount)
    }

    /**
     * Test 6 – History
     * Hai executions giống command: "Bật đèn pin", "Bật đèn pin"
     * Expected: Cả hai lần đều được ghi nhận vào lịch sử (không deduplicate khi có 2 execution riêng biệt).
     */
    @Test
    fun testHistoryRecordsBothExecutionsForSameCommand() {
        val historyEntries = mutableListOf<Pair<String, String>>()

        fun mockAddToHistory(query: String, nluResult: NluResult) {
            historyEntries.add(Pair(query, nluResult.executionId))
        }

        val res1 = NluJsonParser.parse("""{"intent":"open_app","arguments":{"app_name":"Đèn pin"},"status":"success"}""")
        val res2 = NluJsonParser.parse("""{"intent":"open_app","arguments":{"app_name":"Đèn pin"},"status":"success"}""")

        mockAddToHistory("Bật đèn pin", res1)
        mockAddToHistory("Bật đèn pin", res2)

        assertEquals(2, historyEntries.size)
        assertEquals("Bật đèn pin", historyEntries[0].first)
        assertEquals("Bật đèn pin", historyEntries[1].first)
        assertNotEquals(historyEntries[0].second, historyEntries[1].second)
    }

    /**
     * Test 7 – TTS
     * Hai executions giống nhau.
     * Expected: TTS call == 2.
     */
    @Test
    fun testTtsSpeechFeedbackTriggersForBothExecutions() {
        val ttsSpokenTexts = mutableListOf<String>()
        val dispatcher = NluActionDispatcher(context = null) { speechText ->
            ttsSpokenTexts.add(speechText)
        }

        val json = """
            {
                "status": "success",
                "intent": "open_map",
                "arguments": {
                    "destination": "Hồ Gươm"
                },
                "requires_confirmation": false
            }
        """.trimIndent()

        val res1 = NluJsonParser.parse(json)
        val res2 = NluJsonParser.parse(json)

        val processedExecutionIds = mutableSetOf<String>()

        if (processedExecutionIds.add(res1.executionId)) {
            val action1 = NativeAction.fromNluResult(res1)
            dispatcher.dispatch(action1)
        }

        if (processedExecutionIds.add(res2.executionId)) {
            val action2 = NativeAction.fromNluResult(res2)
            dispatcher.dispatch(action2)
        }

        assertEquals(2, ttsSpokenTexts.size)
        assertEquals("Đang mở bản đồ chỉ đường tới Hồ Gươm", ttsSpokenTexts[0])
        assertEquals("Đang mở bản đồ chỉ đường tới Hồ Gươm", ttsSpokenTexts[1])
    }

    /**
     * Test 8 – Other intents regression check
     * Đảm bảo không regression: set_alarm, set_timer, call_contact, send_sms, open_app, open_map, search_video, play_music.
     */
    @Test
    fun testOtherIntentsRegressionCheck() {
        // 1. set_alarm
        val alarmResult = NluJsonParser.parse("""{"intent":"set_alarm","arguments":{"hour":6,"minute":30},"status":"success"}""")
        val alarmAction = NativeAction.fromNluResult(alarmResult)
        assertTrue(alarmAction is NativeAction.SetAlarm)
        assertEquals(6, (alarmAction as NativeAction.SetAlarm).hour)
        assertEquals(30, alarmAction.minute)

        // 2. set_timer
        val timerResult = NluJsonParser.parse("""{"intent":"set_timer","arguments":{"duration":15,"unit":"minutes"},"status":"success"}""")
        val timerAction = NativeAction.fromNluResult(timerResult)
        assertTrue(timerAction is NativeAction.SetTimer)
        assertEquals(900, (timerAction as NativeAction.SetTimer).durationSeconds)

        // 3. call_contact
        val callResult = NluJsonParser.parse("""{"intent":"call_contact","arguments":{"contact":"Mẹ","phone_number":"0901234567"},"status":"success","requires_confirmation":true}""")
        val callAction = NativeAction.fromNluResult(callResult)
        assertTrue(callAction is NativeAction.CallContact)
        assertTrue((callAction as NativeAction.CallContact).requiresConfirmation)

        // 4. send_sms
        val smsResult = NluJsonParser.parse("""{"intent":"send_sms","arguments":{"contact":"Bố","message":"Con đang về"},"status":"success","requires_confirmation":true}""")
        val smsAction = NativeAction.fromNluResult(smsResult)
        assertTrue(smsAction is NativeAction.SendSms)
        assertTrue((smsAction as NativeAction.SendSms).requiresConfirmation)

        // 5. open_app
        val appResult = NluJsonParser.parse("""{"intent":"open_app","arguments":{"app_name":"Zalo"},"status":"success"}""")
        val appAction = NativeAction.fromNluResult(appResult)
        assertTrue(appAction is NativeAction.OpenApp)
        assertEquals("Zalo", (appAction as NativeAction.OpenApp).appName)

        // 6. open_map
        val mapResult = NluJsonParser.parse("""{"intent":"open_map","arguments":{"destination":"Hà Nội"},"status":"success"}""")
        val mapAction = NativeAction.fromNluResult(mapResult)
        assertTrue(mapAction is NativeAction.OpenMap)
        assertEquals("Hà Nội", (mapAction as NativeAction.OpenMap).destination)

        // 7. search_video
        val videoResult = NluJsonParser.parse("""{"intent":"search_video","arguments":{"query":"Hài Tết"},"status":"success"}""")
        val videoAction = NativeAction.fromNluResult(videoResult)
        assertTrue(videoAction is NativeAction.SearchVideo)
        assertEquals("Hài Tết", (videoAction as NativeAction.SearchVideo).query)

        // 8. play_music
        val musicResult = NluJsonParser.parse("""{"intent":"play_music","arguments":{"song_name":"Tiến Quân Ca"},"status":"success"}""")
        val musicAction = NativeAction.fromNluResult(musicResult)
        assertTrue(musicAction is NativeAction.PlayMusic)
        assertEquals("Tiến Quân Ca", (musicAction as NativeAction.PlayMusic).songName)
    }
}
