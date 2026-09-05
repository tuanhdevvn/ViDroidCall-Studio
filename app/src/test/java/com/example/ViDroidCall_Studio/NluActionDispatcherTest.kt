// SPDX-License-Identifier: Apache-2.0

package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.data.nlu.NluActionDispatcher
import org.junit.Assert.assertEquals
import org.junit.Test

class NluActionDispatcherTest {

    @Test
    fun testTtsSpeechFeedbackForSetAlarm() {
        var spokenText = ""
        val dispatcher = NluActionDispatcher(context = null) { text ->
            spokenText = text
        }

        val json = """
            {
                "intent": "set_alarm",
                "arguments": { "hour": 6, "minute": 30 },
                "status": "success"
            }
        """.trimIndent()

        dispatcher.executeNluResponse(json)
        assertEquals("Đang đặt báo thức lúc 6 giờ 30 phút", spokenText)
    }

    @Test
    fun testTtsSpeechFeedbackForSetTimer() {
        var spokenText = ""
        val dispatcher = NluActionDispatcher(context = null) { text ->
            spokenText = text
        }

        val json = """
            {
                "intent": "set_timer",
                "arguments": { "duration": 5, "unit": "minutes" },
                "status": "success"
            }
        """.trimIndent()

        dispatcher.executeNluResponse(json)
        assertEquals("Đang hẹn giờ 5 phút", spokenText)
    }

    @Test
    fun testTtsSpeechFeedbackForCallContact() {
        var spokenText = ""
        val dispatcher = NluActionDispatcher(context = null) { text ->
            spokenText = text
        }

        val json = """
            {
                "intent": "call_contact",
                "arguments": { "contact": "mẹ" },
                "status": "success"
            }
        """.trimIndent()

        dispatcher.executeNluResponse(json)
        assertEquals("Đang thực hiện cuộc gọi tới mẹ", spokenText)
    }

    @Test
    fun testTtsSpeechFeedbackForSendSms() {
        var spokenText = ""
        val dispatcher = NluActionDispatcher(context = null) { text ->
            spokenText = text
        }

        val json = """
            {
                "intent": "send_sms",
                "arguments": { "contact": "anh Tuấn", "message": "Alo anh" },
                "status": "success"
            }
        """.trimIndent()

        dispatcher.executeNluResponse(json)
        assertEquals("Đang mở ứng dụng gửi tin nhắn tới anh Tuấn", spokenText)
    }

    @Test
    fun testTtsSpeechFeedbackForOpenMap() {
        var spokenText = ""
        val dispatcher = NluActionDispatcher(context = null) { text ->
            spokenText = text
        }

        val json = """
            {
                "intent": "open_map",
                "arguments": { "destination": "Hồ Gươm" },
                "status": "success"
            }
        """.trimIndent()

        dispatcher.executeNluResponse(json)
        assertEquals("Đang mở bản đồ chỉ đường tới Hồ Gươm", spokenText)
    }

    @Test
    fun testTtsSpeechFeedbackForOpenApp() {
        var spokenText = ""
        val dispatcher = NluActionDispatcher(context = null) { text ->
            spokenText = text
        }

        val json = """
            {
                "intent": "open_app",
                "arguments": { "app_name": "Youtube" },
                "status": "success"
            }
        """.trimIndent()

        dispatcher.executeNluResponse(json)
        assertEquals("Đang mở ứng dụng YouTube", spokenText)
    }

    @Test
    fun testTtsSpeechFeedbackForGreetingAndGoodbye() {
        var spokenText = ""
        val dispatcher = NluActionDispatcher(context = null) { text ->
            spokenText = text
        }

        val jsonGreeting = """{"intent": "greeting", "status": "success"}"""
        dispatcher.executeNluResponse(jsonGreeting)
        assertEquals("Xin chào bạn, tôi có thể giúp gì cho bạn?", spokenText)

        val jsonGoodbye = """{"intent": "goodbye", "status": "success"}"""
        dispatcher.executeNluResponse(jsonGoodbye)
        assertEquals("Tạm biệt bạn, hẹn gặp lại nhé!", spokenText)
    }

    @Test
    fun testTtsSpeechFeedbackForSpecialStatuses() {
        var spokenText = ""
        val dispatcher = NluActionDispatcher(context = null) { text ->
            spokenText = text
        }

        dispatcher.executeNluResponse("""{"intent": "call_contact", "status": "needs_clarification"}""")
        assertEquals("Bạn vui lòng cung cấp thêm thông tin", spokenText)

        dispatcher.executeNluResponse("""{"intent": "set_alarm", "status": "invalid"}""")
        assertEquals("Thời gian bạn yêu cầu không hợp lệ, vui lòng kiểm tra lại.", spokenText)

        dispatcher.executeNluResponse("""{"intent": "play_music", "status": "unsupported"}""")
        assertEquals("Xin lỗi, tôi chưa hỗ trợ tính năng này.", spokenText)
    }

    @Test
    fun testConfirmationDescriptions() {
        val callAction = com.example.ViDroidCall_Studio.domain.model.NativeAction.CallContact(
            contact = "mẹ",
            phoneNumber = "0912345678"
        )
        assertEquals("Bạn có muốn gọi tới mẹ (0912345678) không?", callAction.getConfirmationDescription())

        val callActionNumberOnly = com.example.ViDroidCall_Studio.domain.model.NativeAction.CallContact(
            contact = "",
            phoneNumber = "113"
        )
        assertEquals("Bạn có muốn gọi tới 113 không?", callActionNumberOnly.getConfirmationDescription())

        val smsAction = com.example.ViDroidCall_Studio.domain.model.NativeAction.SendSms(
            contact = "mẹ",
            phoneNumber = "0912345678",
            message = "Con về muộn nhé"
        )
        assertEquals("Bạn có muốn gửi tin nhắn cho mẹ với nội dung 'Con về muộn nhé' không?", smsAction.getConfirmationDescription())

        val smsActionNoMsg = com.example.ViDroidCall_Studio.domain.model.NativeAction.SendSms(
            contact = "bố",
            phoneNumber = "",
            message = ""
        )
        assertEquals("Bạn có muốn soạn tin nhắn gửi cho bố không?", smsActionNoMsg.getConfirmationDescription())
    }
}
