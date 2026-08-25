package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.data.model.NluIntent
import com.example.ViDroidCall_Studio.data.nlu.FastPathMatcher
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FastPathMatcherTest {

    private val matcher = FastPathMatcher(context = null) // Sử dụng default built-in rules

    @Test
    fun testGreetingMatching() {
        val testCases = listOf("Xin chào", "chào em", "hello", "hi", "hey emma", "alo", "chào bạn")
        for (query in testCases) {
            val result = matcher.match(query)
            assertNotNull("Query '$query' should match greeting", result)
            assertEquals("greeting", result?.intent)
            assertEquals(NluIntent.GREETING, result?.intentEnum)
            assertEquals("success", result?.status)
        }
    }

    @Test
    fun testGoodbyeMatching() {
        val testCases = listOf("Tạm biệt", "bye", "bye bye", "hẹn gặp lại", "chào tạm biệt")
        for (query in testCases) {
            val result = matcher.match(query)
            assertNotNull("Query '$query' should match goodbye", result)
            assertEquals("goodbye", result?.intent)
            assertEquals(NluIntent.GOODBYE, result?.intentEnum)
            assertEquals("success", result?.status)
        }
    }

    @Test
    fun testEmergencyCalls() {
        val r113 = matcher.match("gọi 113")
        assertNotNull(r113)
        assertEquals("call_contact", r113?.intent)
        assertEquals("113", JSONObject(r113!!.argumentsJson).optString("contact"))

        val r114 = matcher.match("gọi cứu hỏa")
        assertNotNull(r114)
        assertEquals("call_contact", r114?.intent)
        assertEquals("114", JSONObject(r114!!.argumentsJson).optString("contact"))

        val r115 = matcher.match("gọi cấp cứu")
        assertNotNull(r115)
        assertEquals("call_contact", r115?.intent)
        assertEquals("115", JSONObject(r115!!.argumentsJson).optString("contact"))
    }

    @Test
    fun testDynamicCallContact() {
        val result = matcher.match("gọi cho mẹ")
        assertNotNull(result)
        assertEquals("call_contact", result?.intent)
        assertEquals("mẹ", JSONObject(result!!.argumentsJson).optString("contact"))

        val result2 = matcher.match("gọi anh tuấn")
        assertNotNull(result2)
        assertEquals("call_contact", result2?.intent)
        assertEquals("anh tuấn", JSONObject(result2!!.argumentsJson).optString("contact"))
    }

    @Test
    fun testSetAlarm() {
        val result = matcher.match("đặt báo thức 6 giờ")
        assertNotNull(result)
        assertEquals("set_alarm", result?.intent)
        val args = JSONObject(result!!.argumentsJson)
        assertEquals(6, args.optInt("hour"))
        assertEquals(0, args.optInt("minute"))

        val result2 = matcher.match("báo thức 7 giờ 30")
        assertNotNull(result2)
        val args2 = JSONObject(result2!!.argumentsJson)
        assertEquals(7, args2.optInt("hour"))
        assertEquals(30, args2.optInt("minute"))
    }

    @Test
    fun testSetTimer() {
        val result = matcher.match("hẹn giờ 5 phút")
        assertNotNull(result)
        assertEquals("set_timer", result?.intent)
        val args = JSONObject(result!!.argumentsJson)
        assertEquals(5, args.optInt("duration"))
        assertEquals("minutes", args.optString("unit"))

        val result2 = matcher.match("đếm ngược 30 giây")
        assertNotNull(result2)
        val args2 = JSONObject(result2!!.argumentsJson)
        assertEquals(30, args2.optInt("duration"))
        assertEquals("seconds", args2.optString("unit"))
    }

    @Test
    fun testOpenApp() {
        val result = matcher.match("mở youtube")
        assertNotNull(result)
        assertEquals("open_app", result?.intent)
        assertEquals("youtube", JSONObject(result!!.argumentsJson).optString("app_name"))

        val result2 = matcher.match("vào zalo")
        assertNotNull(result2)
        assertEquals("open_app", result2?.intent)
        assertEquals("zalo", JSONObject(result2!!.argumentsJson).optString("app_name"))
    }

    @Test
    fun testOpenMap() {
        val result = matcher.match("mở bản đồ")
        assertNotNull(result)
        assertEquals("open_map", result?.intent)

        val result2 = matcher.match("chỉ đường đến Hồ Gươm")
        assertNotNull(result2)
        assertEquals("open_map", result2?.intent)
        assertEquals("hồ gươm", JSONObject(result2!!.argumentsJson).optString("destination"))
    }

    @Test
    fun testSendSms() {
        val result = matcher.match("nhắn tin cho mẹ nội dung con về rồi")
        assertNotNull(result)
        assertEquals("send_sms", result?.intent)
        val args = JSONObject(result!!.argumentsJson)
        assertEquals("mẹ", args.optString("contact"))
        assertEquals("con về rồi", args.optString("message"))
    }

    @Test
    fun testClarifyIncompleteCommands() {
        val resultCall = matcher.match("gọi")
        assertNotNull(resultCall)
        assertEquals("clarify", resultCall?.intent)

        val resultSms = matcher.match("nhắn tin")
        assertNotNull(resultSms)
        assertEquals("clarify", resultSms?.intent)

        val resultTimer = matcher.match("hẹn giờ")
        assertNotNull(resultTimer)
        assertEquals("clarify", resultTimer?.intent)
    }

    @Test
    fun testComplexQueryShouldReturnNullForLlmFallback() {
        val complexQueries = listOf(
            "Tôi muốn hỏi thời tiết ngày mai ở Hà Nội có mưa không",
            "Giải thích cho tôi thuyết tương đối của Einstein",
            "Nếu ngày mai trời nắng thì nhắc tôi đi mua mũ lúc 9h sáng nhé"
        )
        for (query in complexQueries) {
            val result = matcher.match(query)
            assertNull("Câu phức tạp '$query' phải trả về null để rơi về LLM", result)
        }
    }
}
