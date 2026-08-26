package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.data.model.NluJsonParser
import com.example.ViDroidCall_Studio.domain.model.NativeAction
import com.example.ViDroidCall_Studio.util.AppResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeActionDispatcherUnitTest {

    @Test
    fun testOpenAppActionParsing() {
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

        val nluResult = NluJsonParser.parse(json)
        val action = NativeAction.fromNluResult(nluResult)

        assertTrue(action is NativeAction.OpenApp)
        val openApp = action as NativeAction.OpenApp
        assertEquals("YouTube", openApp.appName)
        assertFalse(openApp.requiresConfirmation)
        assertEquals("Đang mở ứng dụng YouTube", openApp.getSpeechFeedbackText())
    }

    @Test
    fun testCallContactActionParsingRequiresConfirmation() {
        val json = """
            {
                "status": "success",
                "intent": "call_contact",
                "slots": {
                    "contact": "Mẹ",
                    "phone_number": "113"
                },
                "requires_confirmation": true
            }
        """.trimIndent()

        val nluResult = NluJsonParser.parse(json)
        val action = NativeAction.fromNluResult(nluResult)

        assertTrue(action is NativeAction.CallContact)
        val call = action as NativeAction.CallContact
        assertEquals("Mẹ", call.contact)
        assertEquals("113", call.phoneNumber)
        assertTrue(call.requiresConfirmation)
        assertEquals("Xác nhận thực hiện cuộc gọi?", call.getConfirmationTitle())
        assertTrue(call.getConfirmationDescription().contains("Mẹ"))
        assertTrue(call.getConfirmationDescription().contains("113"))
    }

    @Test
    fun testSendSmsActionParsingRequiresConfirmation() {
        val json = """
            {
                "status": "success",
                "intent": "send_sms",
                "slots": {
                    "contact": "Mẹ",
                    "phone_number": "0901234567",
                    "message": "Mẹ về chưa?"
                },
                "requires_confirmation": true
            }
        """.trimIndent()

        val nluResult = NluJsonParser.parse(json)
        val action = NativeAction.fromNluResult(nluResult)

        assertTrue(action is NativeAction.SendSms)
        val sms = action as NativeAction.SendSms
        assertEquals("Mẹ", sms.contact)
        assertEquals("0901234567", sms.phoneNumber)
        assertEquals("Mẹ về chưa?", sms.message)
        assertTrue(sms.requiresConfirmation)
        assertEquals("Xác nhận gửi tin nhắn?", sms.getConfirmationTitle())
        assertTrue(sms.getConfirmationDescription().contains("Mẹ"))
        assertTrue(sms.getConfirmationDescription().contains("Mẹ về chưa?"))
    }

    @Test
    fun testOpenMapActionParsing() {
        val json = """
            {
                "status": "success",
                "intent": "open_map",
                "slots": {
                    "destination": "Hồ Gươm"
                },
                "requires_confirmation": false
            }
        """.trimIndent()

        val nluResult = NluJsonParser.parse(json)
        val action = NativeAction.fromNluResult(nluResult)

        assertTrue(action is NativeAction.OpenMap)
        val mapAction = action as NativeAction.OpenMap
        assertEquals("Hồ Gươm", mapAction.destination)
        assertFalse(mapAction.requiresConfirmation)
        assertEquals("Đang mở bản đồ chỉ đường tới Hồ Gươm", mapAction.getSpeechFeedbackText())
    }

    @Test
    fun testSetAlarmActionParsing() {
        val json = """
            {
                "status": "success",
                "intent": "set_alarm",
                "slots": {
                    "hour": 7,
                    "minute": 0,
                    "label": "Dậy đi làm"
                },
                "requires_confirmation": false
            }
        """.trimIndent()

        val nluResult = NluJsonParser.parse(json)
        val action = NativeAction.fromNluResult(nluResult)

        assertTrue(action is NativeAction.SetAlarm)
        val alarm = action as NativeAction.SetAlarm
        assertEquals(7, alarm.hour)
        assertEquals(0, alarm.minute)
        assertEquals("Dậy đi làm", alarm.label)
        assertFalse(alarm.requiresConfirmation)
    }

    @Test
    fun testSetTimerActionParsing() {
        val json = """
            {
                "status": "success",
                "intent": "set_timer",
                "slots": {
                    "duration": 10,
                    "unit": "minutes"
                },
                "requires_confirmation": false
            }
        """.trimIndent()

        val nluResult = NluJsonParser.parse(json)
        val action = NativeAction.fromNluResult(nluResult)

        assertTrue(action is NativeAction.SetTimer)
        val timer = action as NativeAction.SetTimer
        assertEquals(600, timer.durationSeconds)
        assertEquals(10, timer.displayDuration)
        assertEquals("phút", timer.unitText)
        assertFalse(timer.requiresConfirmation)
    }

    @Test
    fun testStatusNotSuccessDoesNotExecute() {
        val jsonError = """
            {
                "status": "error",
                "intent": null,
                "slots": {},
                "requires_confirmation": false
            }
        """.trimIndent()

        val nluResult = NluJsonParser.parse(jsonError)
        val action = NativeAction.fromNluResult(nluResult)

        assertTrue(action is NativeAction.Unsupported)
    }

    @Test
    fun testNeedsClarificationStatus() {
        val jsonClarify = """
            {
                "status": "needs_clarification",
                "intent": "call_contact",
                "slots": {},
                "requires_confirmation": false
            }
        """.trimIndent()

        val nluResult = NluJsonParser.parse(jsonClarify)
        val action = NativeAction.fromNluResult(nluResult)

        assertTrue(action is NativeAction.Informational)
        assertEquals("Bạn vui lòng cung cấp thêm thông tin", action.getSpeechFeedbackText())
    }

    @Test
    fun testUnknownIntent() {
        val jsonUnknown = """
            {
                "status": "success",
                "intent": "unknown_future_intent",
                "slots": {},
                "requires_confirmation": false
            }
        """.trimIndent()

        val nluResult = NluJsonParser.parse(jsonUnknown)
        val action = NativeAction.fromNluResult(nluResult)

        assertTrue(action is NativeAction.Unsupported)
        assertEquals("Xin lỗi, tôi chưa hỗ trợ tính năng này.", action.getSpeechFeedbackText())
    }

    @Test
    fun testAppResolverCleanName() {
        assertEquals("youtube", AppResolver.cleanAppName("mở ứng dụng YouTube"))
        assertEquals("zalo", AppResolver.cleanAppName("mở app zalo"))
        assertEquals("facebook", AppResolver.cleanAppName("Facebook"))
    }
}
