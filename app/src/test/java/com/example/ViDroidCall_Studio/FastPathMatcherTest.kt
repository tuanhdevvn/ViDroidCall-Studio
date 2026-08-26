package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.data.model.NluIntent
import com.example.ViDroidCall_Studio.data.nlu.FastPathMatcher
import com.example.ViDroidCall_Studio.data.nlu.TimeProvider
import com.example.ViDroidCall_Studio.data.nlu.VietnameseNumberParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Arrays

class FastPathMatcherTest {

    private val matcher = FastPathMatcher(context = null)

    @Test
    fun testAll62RequiredSpecTestCases() {
        // === 30. UNIT TEST - NUMBER (1..12) ===
        assertEquals(1, VietnameseNumberParser.parse("một"))
        assertEquals(2, VietnameseNumberParser.parse("hai"))
        assertEquals(4, VietnameseNumberParser.parse("bốn"))
        assertEquals(4, VietnameseNumberParser.parse("tư"))
        assertEquals(5, VietnameseNumberParser.parse("năm"))
        assertEquals(5, VietnameseNumberParser.parse("lăm"))
        assertEquals(10, VietnameseNumberParser.parse("mười"))
        assertEquals(15, VietnameseNumberParser.parse("mười lăm"))
        assertEquals(20, VietnameseNumberParser.parse("hai mươi"))
        assertEquals(21, VietnameseNumberParser.parse("hai mươi mốt"))
        assertEquals(24, VietnameseNumberParser.parse("hai mươi tư"))
        assertEquals(25, VietnameseNumberParser.parse("hai mươi lăm"))

        // === 31. UNIT TEST - TIME (13..21) ===
        val r13 = matcher.match("báo thức sáu giờ")
        assertNotNull(r13); assertEquals(6, JSONObject(r13!!.argumentsJson).optInt("hour"))

        val r14 = matcher.match("báo thức sau gio")
        assertNotNull(r14); assertEquals(6, JSONObject(r14!!.argumentsJson).optInt("hour"))

        val r15 = matcher.match("báo thức bảy giờ rưỡi")
        assertNotNull(r15); assertEquals(7, JSONObject(r15!!.argumentsJson).optInt("hour")); assertEquals(30, JSONObject(r15.argumentsJson).optInt("minute"))

        val r16 = matcher.match("báo thức bay gio ruoi")
        assertNotNull(r16); assertEquals(7, JSONObject(r16!!.argumentsJson).optInt("hour")); assertEquals(30, JSONObject(r16.argumentsJson).optInt("minute"))

        val r17 = matcher.match("hẹn giờ mười lăm phút")
        assertNotNull(r17); assertEquals(15, JSONObject(r17!!.argumentsJson).optInt("duration"))

        val r18 = matcher.match("hẹn giờ muoi lam phut")
        assertNotNull(r18); assertEquals(15, JSONObject(r18!!.argumentsJson).optInt("duration"))

        val r19 = matcher.match("hẹn giờ hai mươi giây")
        assertNotNull(r19); assertEquals(20, JSONObject(r19!!.argumentsJson).optInt("duration")); assertEquals("seconds", JSONObject(r19.argumentsJson).optString("unit"))

        val r20 = matcher.match("hẹn giờ nua tieng")
        assertNotNull(r20); assertEquals(30, JSONObject(r20!!.argumentsJson).optInt("duration"))

        val r21 = matcher.match("hẹn giờ nua gio")
        assertNotNull(r21); assertEquals(30, JSONObject(r21!!.argumentsJson).optInt("duration"))

        // === 32. UNIT TEST - PERIOD (22..28) ===
        val r22 = matcher.match("báo thức 7 giờ tối")
        assertNotNull(r22); assertEquals(19, JSONObject(r22!!.argumentsJson).optInt("hour"))

        val r23 = matcher.match("báo thức 2 giờ chiều")
        assertNotNull(r23); assertEquals(14, JSONObject(r23!!.argumentsJson).optInt("hour"))

        val r24 = matcher.match("báo thức 8 giờ sáng")
        assertNotNull(r24); assertEquals(8, JSONObject(r24!!.argumentsJson).optInt("hour"))

        val r25 = matcher.match("báo thức 11 giờ đêm")
        assertNotNull(r25); assertEquals(23, JSONObject(r25!!.argumentsJson).optInt("hour"))

        val r26 = matcher.match("báo thức 12 giờ sáng")
        assertNotNull(r26); assertEquals(0, JSONObject(r26!!.argumentsJson).optInt("hour"))

        val r27 = matcher.match("báo thức 12 giờ trưa")
        assertNotNull(r27); assertEquals(12, JSONObject(r27!!.argumentsJson).optInt("hour"))

        val r28 = matcher.match("báo thức 12 giờ đêm")
        assertNotNull(r28); assertEquals(0, JSONObject(r28!!.argumentsJson).optInt("hour"))

        // === 33. UNIT TEST - GIỜ KÉM (29..41) ===
        val kiemMap = listOf(
            "báo thức 2 giờ kém 10" to Pair(1, 50),         // 29
            "báo thức 2 giờ kém 10 phút" to Pair(1, 50),    // 30
            "báo thức hai giờ kém mười" to Pair(1, 50),     // 31
            "báo thức hai giờ kém mười phút" to Pair(1, 50),// 32
            "báo thức 7 giờ kém 15" to Pair(6, 45),         // 33
            "báo thức bảy giờ kém mười lăm" to Pair(6, 45), // 34
            "báo thức 8 giờ kém 5" to Pair(7, 55),          // 35
            "báo thức tám giờ kém năm" to Pair(7, 55),      // 36
            "báo thức 2 giờ kém 10 tối" to Pair(19, 50),    // 37
            "báo thức 7 giờ kém 15 tối" to Pair(18, 45),    // 38
            "báo thức 8 giờ kém 5 tối" to Pair(19, 55),     // 39
            "báo thức 11 giờ kém 10 đêm" to Pair(22, 50),   // 40
            "báo thức 2 giờ thiếu 10" to Pair(1, 50)        // 41
        )
        for ((q, expected) in kiemMap) {
            val res = matcher.match(q)
            assertNotNull("Query '$q' must match", res)
            assertEquals("set_alarm", res?.intent)
            val json = JSONObject(res!!.argumentsJson)
            assertEquals("Hour mismatch for '$q'", expected.first, json.optInt("hour"))
            assertEquals("Minute mismatch for '$q'", expected.second, json.optInt("minute"))
        }

        // === 34. UNIT TEST - RELATIVE TIME (42..49) ===
        val fixedTP = TimeProvider.createFixed(hour = 10, minute = 20, second = 0)
        val relMatcher = FastPathMatcher(context = null, timeProvider = fixedTP)

        val r42 = relMatcher.match("sau 5 phút")
        assertNotNull(r42); assertEquals("set_timer", r42?.intent); assertEquals(5, JSONObject(r42!!.argumentsJson).optInt("duration"))

        val r43 = relMatcher.match("năm phút nữa")
        assertNotNull(r43); assertEquals("set_timer", r43?.intent); assertEquals(5, JSONObject(r43!!.argumentsJson).optInt("duration"))

        val r44 = relMatcher.match("sau 2 tiếng")
        assertNotNull(r44); assertEquals("set_timer", r44?.intent); assertEquals(2, JSONObject(r44!!.argumentsJson).optInt("duration")); assertEquals("hours", JSONObject(r44.argumentsJson).optString("unit"))

        val r45 = relMatcher.match("hai tiếng nữa")
        assertNotNull(r45); assertEquals("set_timer", r45?.intent); assertEquals(2, JSONObject(r45!!.argumentsJson).optInt("duration"))

        val r46 = relMatcher.match("sau 20 giây")
        assertNotNull(r46); assertEquals("set_timer", r46?.intent); assertEquals(20, JSONObject(r46!!.argumentsJson).optInt("duration")); assertEquals("seconds", JSONObject(r46.argumentsJson).optString("unit"))

        val r47 = relMatcher.match("hai mươi giây nữa")
        assertNotNull(r47); assertEquals("set_timer", r47?.intent); assertEquals(20, JSONObject(r47!!.argumentsJson).optInt("duration"))

        val r48 = relMatcher.match("nửa tiếng nữa")
        assertNotNull(r48); assertEquals("set_timer", r48?.intent); assertEquals(30, JSONObject(r48!!.argumentsJson).optInt("duration"))

        val r49 = relMatcher.match("sau nửa giờ")
        assertNotNull(r49); assertEquals("set_timer", r49?.intent); assertEquals(30, JSONObject(r49!!.argumentsJson).optInt("duration"))

        // === 35. UNIT TEST - APP ALIAS (50..62) ===
        val appAliasMap = listOf(
            "du tup" to "youtube",         // 50
            "yutube" to "youtube",         // 51
            "phay buc" to "facebook",      // 52
            "top top" to "tiktok",         // 53
            "tik tok" to "tiktok",         // 54
            "guc go map" to "google_maps", // 55
            "may tinh" to "calculator",    // 56
            "bo suu tap" to "gallery",     // 57
            "danh ba" to "contacts",       // 58
            "ch play" to "playstore",      // 59
            "shopee" to "shopee",          // 60
            "lazada" to "lazada",          // 61
            "grab" to "grab"               // 62
        )
        for ((query, expectedApp) in appAliasMap) {
            val res = matcher.match(query)
            assertNotNull("App query '$query' must match", res)
            assertEquals("open_app", res?.intent)
            assertEquals("App name mismatch for '$query'", expectedApp, JSONObject(res!!.argumentsJson).optString("app_name"))
        }
    }

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
    fun testCrossDayRolloverForRelativeAlarm() {
        val midnightTimeProvider = TimeProvider.createFixed(hour = 23, minute = 50, second = 0)
        val midnightMatcher = FastPathMatcher(context = null, timeProvider = midnightTimeProvider)

        val rCrossDay = midnightMatcher.match("báo thức sau 20 phút")
        assertNotNull(rCrossDay)
        assertEquals("set_alarm", rCrossDay?.intent)
        assertEquals(0, JSONObject(rCrossDay!!.argumentsJson).optInt("hour"))
        assertEquals(10, JSONObject(rCrossDay.argumentsJson).optInt("minute"))
    }

    @Test
    fun testNegativeCasesToAvoidFalsePositives() {
        assertNull("Câu 'tôi nói chuyện về youtube' không được tự biến thành open_app", matcher.match("tôi nói chuyện về youtube"))
        assertNull("Câu 'Tôi thích xem video ngắn' không được tự biến thành open_app", matcher.match("Tôi thích xem video ngắn"))
        assertNull("Câu 'Tôi cần số điện thoại của anh' không được tự biến thành open_app", matcher.match("Tôi cần số điện thoại của anh"))
        assertNull("Câu 'tôi đang tính toán' không được tự biến thành open_app", matcher.match("tôi đang tính toán"))

        // Distinction test between 2 giờ 10 and 2 giờ kém 10
        val rThuong = matcher.match("báo thức 2 giờ 10")
        assertNotNull(rThuong)
        assertEquals(2, JSONObject(rThuong!!.argumentsJson).optInt("hour"))
        assertEquals(10, JSONObject(rThuong.argumentsJson).optInt("minute"))

        val rKem = matcher.match("báo thức 2 giờ kém 10")
        assertNotNull(rKem)
        assertEquals(1, JSONObject(rKem!!.argumentsJson).optInt("hour"))
        assertEquals(50, JSONObject(rKem.argumentsJson).optInt("minute"))
    }

    @Test
    fun testStressBenchmark10000Queries() {
        val queries = listOf(
            "Xin chào",
            "Tạm biệt",
            "gọi 113",
            "mở youtube",
            "du tup",
            "phay buc",
            "top top",
            "guc go map",
            "báo thức bảy giờ rưỡi tối",
            "báo thức 8 giờ kém 15",
            "hẹn giờ mười lăm phút",
            "hẹn giờ nửa tiếng",
            "mở shopee",
            "mở máy tính",
            "bộ sưu tập",
            "sau 5 phút",
            "5 phút nữa",
            "sau 1 giờ",
            "sau nửa tiếng"
        )

        for (i in 0..500) {
            matcher.match(queries[i % queries.size])
        }

        val iterations = 10000
        val latencies = DoubleArray(iterations)

        val totalStart = System.nanoTime()
        for (i in 0 until iterations) {
            val start = System.nanoTime()
            matcher.match(queries[i % queries.size])
            val end = System.nanoTime()
            latencies[i] = (end - start) / 1_000_000.0
        }
        val totalElapsedMs = (System.nanoTime() - totalStart) / 1_000_000.0

        Arrays.sort(latencies)
        val avg = totalElapsedMs / iterations
        val p50 = latencies[(iterations * 0.50).toInt()]
        val p95 = latencies[(iterations * 0.95).toInt()]
        val p99 = latencies[(iterations * 0.99).toInt()]

        println("🚀 FAST-PATH STRESS BENCHMARK (10,000 QUERIES):")
        println("   - Total execution time: ${totalElapsedMs}ms")
        println("   - Average Latency: ${avg}ms/query")
        println("   - P50 Latency: ${p50}ms")
        println("   - P95 Latency: ${p95}ms")
        println("   - P99 Latency: ${p99}ms")

        assertTrue("P95 latency must be < 5.0ms (Actual: ${p95}ms)", p95 < 5.0)
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
