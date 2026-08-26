package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.data.model.NluIntent
import com.example.ViDroidCall_Studio.data.nlu.FastPathMatcher
import com.example.ViDroidCall_Studio.data.nlu.TimeProvider
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
    fun testSetAlarmVietnameseTimeAndPeriods() {
        // 9. bảy giờ -> 07:00
        val r1 = matcher.match("báo thức bảy giờ")
        assertNotNull(r1)
        assertEquals("set_alarm", r1?.intent)
        assertEquals(7, JSONObject(r1!!.argumentsJson).optInt("hour"))
        assertEquals(0, JSONObject(r1.argumentsJson).optInt("minute"))

        // 10. bảy giờ rưỡi -> 07:30
        val r2 = matcher.match("báo thức bảy giờ rưỡi")
        assertNotNull(r2)
        assertEquals("set_alarm", r2?.intent)
        assertEquals(7, JSONObject(r2!!.argumentsJson).optInt("hour"))
        assertEquals(30, JSONObject(r2.argumentsJson).optInt("minute"))

        // 11. bảy giờ tối -> 19:00
        val r3 = matcher.match("báo thức 7 giờ tối")
        assertNotNull(r3)
        assertEquals("set_alarm", r3?.intent)
        assertEquals(19, JSONObject(r3!!.argumentsJson).optInt("hour"))
        assertEquals(0, JSONObject(r3.argumentsJson).optInt("minute"))

        val r3Text = matcher.match("báo thức bảy giờ tối")
        assertNotNull(r3Text)
        assertEquals(19, JSONObject(r3Text!!.argumentsJson).optInt("hour"))

        // 12. bảy giờ rưỡi tối -> 19:30
        val r4 = matcher.match("đặt báo thức bảy giờ rưỡi tối")
        assertNotNull(r4)
        assertEquals("set_alarm", r4?.intent)
        assertEquals(19, JSONObject(r4!!.argumentsJson).optInt("hour"))
        assertEquals(30, JSONObject(r4.argumentsJson).optInt("minute"))

        // 13. hai giờ chiều -> 14:00
        val r5 = matcher.match("báo thức hai giờ chiều")
        assertNotNull(r5)
        assertEquals("set_alarm", r5?.intent)
        assertEquals(14, JSONObject(r5!!.argumentsJson).optInt("hour"))
        assertEquals(0, JSONObject(r5.argumentsJson).optInt("minute"))

        // 14. tám giờ sáng -> 08:00
        val r6 = matcher.match("báo thức tám giờ sáng")
        assertNotNull(r6)
        assertEquals("set_alarm", r6?.intent)
        assertEquals(8, JSONObject(r6!!.argumentsJson).optInt("hour"))
        assertEquals(0, JSONObject(r6.argumentsJson).optInt("minute"))

        // 15. mười một giờ đêm -> 23:00
        val r7 = matcher.match("báo thức mười một giờ đêm")
        assertNotNull(r7)
        assertEquals("set_alarm", r7?.intent)
        assertEquals(23, JSONObject(r7!!.argumentsJson).optInt("hour"))
        assertEquals(0, JSONObject(r7.argumentsJson).optInt("minute"))

        // 16. mười hai giờ đêm -> 00:00
        val r8 = matcher.match("báo thức mười hai giờ đêm")
        assertNotNull(r8)
        assertEquals("set_alarm", r8?.intent)
        assertEquals(0, JSONObject(r8!!.argumentsJson).optInt("hour"))
        assertEquals(0, JSONObject(r8.argumentsJson).optInt("minute"))

        // 12 giờ trưa -> 12:00
        val rTrua = matcher.match("báo thức 12 giờ trưa")
        assertNotNull(rTrua)
        assertEquals(12, JSONObject(rTrua!!.argumentsJson).optInt("hour"))

        // Giờ kém: 8 giờ kém 15 -> 07:45
        val rKem1 = matcher.match("báo thức 8 giờ kém 15")
        assertNotNull(rKem1)
        assertEquals("set_alarm", rKem1?.intent)
        assertEquals(7, JSONObject(rKem1!!.argumentsJson).optInt("hour"))
        assertEquals(45, JSONObject(rKem1.argumentsJson).optInt("minute"))

        // Giờ kém có khoảng trắng: 2 h kém 10 -> 01:50
        val rKem3 = matcher.match("báo thức 2 h kém 10")
        assertNotNull(rKem3)
        assertEquals("set_alarm", rKem3?.intent)
        assertEquals(1, JSONObject(rKem3!!.argumentsJson).optInt("hour"))
        assertEquals(50, JSONObject(rKem3.argumentsJson).optInt("minute"))

        // Giờ kém có khoảng trắng: 3 h kém 10 -> 02:50
        val rKem4 = matcher.match("báo thức 3 h kém 10")
        assertNotNull(rKem4)
        assertEquals("set_alarm", rKem4?.intent)
        assertEquals(2, JSONObject(rKem4!!.argumentsJson).optInt("hour"))
        assertEquals(50, JSONObject(rKem4.argumentsJson).optInt("minute"))

        // Giờ kém dạng chữ & buổi: tám giờ kém mười lăm tối -> 19:45
        val rKem2 = matcher.match("báo thức tám giờ kém mười lăm tối")
        assertNotNull(rKem2)
        assertEquals("set_alarm", rKem2?.intent)
        assertEquals(19, JSONObject(rKem2!!.argumentsJson).optInt("hour"))
        assertEquals(45, JSONObject(rKem2.argumentsJson).optInt("minute"))
    }

    @Test
    fun testComprehensiveAlarmVariations() {
        val testMap = mapOf(
            "báo thức 5 giờ sáng" to Pair(5, 0),
            "báo thức 10 giờ kém 15 sáng" to Pair(9, 45),
            "báo thức 5 giờ kém 20 chiều" to Pair(16, 40),
            "báo thức mười hai giờ kém mười lăm đêm" to Pair(23, 45),
            "báo thức một giờ kém mười lăm" to Pair(0, 45),
            "đặt báo thức ba giờ rưỡi chiều" to Pair(15, 30),
            "báo thức chín giờ tối" to Pair(21, 0)
        )
        for ((query, expected) in testMap) {
            val result = matcher.match(query)
            assertNotNull("Query '$query' should match alarm", result)
            val json = JSONObject(result!!.argumentsJson)
            assertEquals("Hour mismatch for query: $query", expected.first, json.optInt("hour"))
            assertEquals("Minute mismatch for query: $query", expected.second, json.optInt("minute"))
        }
    }

    @Test
    fun testRelativeTimeAndClockInjection() {
        // Mock current time = 10:20:00
        val fixedTimeProvider = TimeProvider.createFixed(hour = 10, minute = 20, second = 0)
        val relativeMatcher = FastPathMatcher(context = null, timeProvider = fixedTimeProvider)

        // 1. sau 5 phút -> timer 5 min
        val r1 = relativeMatcher.match("sau 5 phút")
        assertNotNull(r1)
        assertEquals("set_timer", r1?.intent)
        assertEquals(5, JSONObject(r1!!.argumentsJson).optInt("duration"))
        assertEquals("minutes", JSONObject(r1.argumentsJson).optString("unit"))

        // 2. sau năm phút -> timer 5 min
        val r2 = relativeMatcher.match("sau năm phút")
        assertNotNull(r2)
        assertEquals("set_timer", r2?.intent)
        assertEquals(5, JSONObject(r2!!.argumentsJson).optInt("duration"))

        // 3. 5 phút nữa -> timer 5 min
        val r3 = relativeMatcher.match("5 phút nữa")
        assertNotNull(r3)
        assertEquals("set_timer", r3?.intent)
        assertEquals(5, JSONObject(r3!!.argumentsJson).optInt("duration"))

        // 4. năm phút nữa -> timer 5 min
        val r4 = relativeMatcher.match("năm phút nữa")
        assertNotNull(r4)
        assertEquals("set_timer", r4?.intent)
        assertEquals(5, JSONObject(r4!!.argumentsJson).optInt("duration"))

        // 5. sau 1 giờ -> timer 1 hour
        val r5 = relativeMatcher.match("sau 1 giờ")
        assertNotNull(r5)
        assertEquals("set_timer", r5?.intent)
        assertEquals(1, JSONObject(r5!!.argumentsJson).optInt("duration"))
        assertEquals("hours", JSONObject(r5.argumentsJson).optString("unit"))

        // 6. sau một giờ -> timer 1 hour
        val r6 = relativeMatcher.match("sau một giờ")
        assertNotNull(r6)
        assertEquals("set_timer", r6?.intent)
        assertEquals(1, JSONObject(r6!!.argumentsJson).optInt("duration"))

        // 7. 2 tiếng nữa -> timer 2 hours
        val r7 = relativeMatcher.match("2 tiếng nữa")
        assertNotNull(r7)
        assertEquals("set_timer", r7?.intent)
        assertEquals(2, JSONObject(r7!!.argumentsJson).optInt("duration"))
        assertEquals("hours", JSONObject(r7.argumentsJson).optString("unit"))

        // 8. hai tiếng nữa -> timer 2 hours
        val r8 = relativeMatcher.match("hai tiếng nữa")
        assertNotNull(r8)
        assertEquals("set_timer", r8?.intent)
        assertEquals(2, JSONObject(r8!!.argumentsJson).optInt("duration"))

        // 9. nửa tiếng nữa -> timer 30 min
        val r9 = relativeMatcher.match("nửa tiếng nữa")
        assertNotNull(r9)
        assertEquals("set_timer", r9?.intent)
        assertEquals(30, JSONObject(r9!!.argumentsJson).optInt("duration"))

        // 10. sau nửa giờ -> timer 30 min
        val r10 = relativeMatcher.match("sau nửa giờ")
        assertNotNull(r10)
        assertEquals("set_timer", r10?.intent)
        assertEquals(30, JSONObject(r10!!.argumentsJson).optInt("duration"))

        // 11. sau 20 giây -> timer 20 sec
        val r11 = relativeMatcher.match("sau 20 giây")
        assertNotNull(r11)
        assertEquals("set_timer", r11?.intent)
        assertEquals(20, JSONObject(r11!!.argumentsJson).optInt("duration"))
        assertEquals("seconds", JSONObject(r11.argumentsJson).optString("unit"))

        // 12. hai mươi giây nữa -> timer 20 sec
        val r12 = relativeMatcher.match("hai mươi giây nữa")
        assertNotNull(r12)
        assertEquals("set_timer", r12?.intent)
        assertEquals(20, JSONObject(r12!!.argumentsJson).optInt("duration"))

        // 14. bây giờ + 10 phút (báo thức bây giờ cộng 10 phút) -> set_alarm: 10:20 + 10 = 10:30
        val r14 = relativeMatcher.match("báo thức bây giờ cộng 10 phút")
        assertNotNull(r14)
        assertEquals("set_alarm", r14?.intent)
        assertEquals(10, JSONObject(r14!!.argumentsJson).optInt("hour"))
        assertEquals(30, JSONObject(r14.argumentsJson).optInt("minute"))

        // 15. Phân biệt set_timer vs set_alarm:
        // "sau 10 phút" -> set_timer
        val rTimer = relativeMatcher.match("sau 10 phút")
        assertEquals("set_timer", rTimer?.intent)

        // "báo thức sau 10 phút" -> set_alarm
        val rAlarm = relativeMatcher.match("báo thức sau 10 phút")
        assertEquals("set_alarm", rAlarm?.intent)
        assertEquals(10, JSONObject(rAlarm!!.argumentsJson).optInt("hour"))
        assertEquals(30, JSONObject(rAlarm.argumentsJson).optInt("minute"))
    }

    @Test
    fun testCrossDayRolloverForRelativeAlarm() {
        // 13. Mock current time = 23:50:00 (vượt 24:00)
        val midnightTimeProvider = TimeProvider.createFixed(hour = 23, minute = 50, second = 0)
        val midnightMatcher = FastPathMatcher(context = null, timeProvider = midnightTimeProvider)

        // "báo thức sau 20 phút" -> 23:50 + 20 min = 00:10 ngày hôm sau
        val rCrossDay = midnightMatcher.match("báo thức sau 20 phút")
        assertNotNull(rCrossDay)
        assertEquals("set_alarm", rCrossDay?.intent)
        assertEquals(0, JSONObject(rCrossDay!!.argumentsJson).optInt("hour"))
        assertEquals(10, JSONObject(rCrossDay.argumentsJson).optInt("minute"))
    }

    @Test
    fun testSetTimerVietnameseNumbersAndHalfHour() {
        // 17. mười lăm phút -> 15 phút
        val r1 = matcher.match("hẹn giờ mười lăm phút")
        assertNotNull(r1)
        assertEquals("set_timer", r1?.intent)
        assertEquals(15, JSONObject(r1!!.argumentsJson).optInt("duration"))
        assertEquals("minutes", JSONObject(r1.argumentsJson).optString("unit"))

        // 18. hai mươi giây -> 20 giây
        val r2 = matcher.match("hẹn giờ hai mươi giây")
        assertNotNull(r2)
        assertEquals("set_timer", r2?.intent)
        assertEquals(20, JSONObject(r2!!.argumentsJson).optInt("duration"))
        assertEquals("seconds", JSONObject(r2.argumentsJson).optString("unit"))

        // 19. nửa tiếng -> 30 phút
        val r3 = matcher.match("hẹn giờ nửa tiếng")
        assertNotNull(r3)
        assertEquals("set_timer", r3?.intent)
        assertEquals(30, JSONObject(r3!!.argumentsJson).optInt("duration"))
        assertEquals("minutes", JSONObject(r3.argumentsJson).optString("unit"))

        // 20. nửa giờ -> 30 phút
        val r4 = matcher.match("hẹn giờ nửa giờ")
        assertNotNull(r4)
        assertEquals("set_timer", r4?.intent)
        assertEquals(30, JSONObject(r4!!.argumentsJson).optInt("duration"))
        assertEquals("minutes", JSONObject(r4.argumentsJson).optString("unit"))
    }

    @Test
    fun testComprehensiveTimerVariations() {
        val testMap = mapOf(
            "hẹn giờ ba mươi phút" to Pair(30, "minutes"),
            "hẹn giờ một tiếng" to Pair(1, "hours"),
            "hẹn giờ 2 tiếng" to Pair(2, "hours"),
            "đếm ngược bốn mươi lăm giây" to Pair(45, "seconds")
        )
        for ((query, expected) in testMap) {
            val result = matcher.match(query)
            assertNotNull("Query '$query' should match timer", result)
            val json = JSONObject(result!!.argumentsJson)
            assertEquals("Duration mismatch for query: $query", expected.first, json.optInt("duration"))
            assertEquals("Unit mismatch for query: $query", expected.second, json.optString("unit"))
        }
    }

    @Test
    fun testAppAliasesAndCommercialApps() {
        // 21. du tup -> youtube
        val r1 = matcher.match("du tup")
        assertNotNull(r1)
        assertEquals("open_app", r1?.intent)
        assertEquals("youtube", JSONObject(r1!!.argumentsJson).optString("app_name"))

        // 22. phay buc -> facebook
        val r2 = matcher.match("phay buc")
        assertNotNull(r2)
        assertEquals("open_app", r2?.intent)
        assertEquals("facebook", JSONObject(r2!!.argumentsJson).optString("app_name"))

        // 23. top top -> tiktok
        val r3 = matcher.match("top top")
        assertNotNull(r3)
        assertEquals("open_app", r3?.intent)
        assertEquals("tiktok", JSONObject(r3!!.argumentsJson).optString("app_name"))

        // 24. guc go map -> google_maps
        val r4 = matcher.match("guc go map")
        assertNotNull(r4)
        assertEquals("open_app", r4?.intent)
        assertEquals("google_maps", JSONObject(r4!!.argumentsJson).optString("app_name"))

        // 25. ch play -> playstore
        val r5 = matcher.match("ch play")
        assertNotNull(r5)
        assertEquals("open_app", r5?.intent)
        assertEquals("playstore", JSONObject(r5!!.argumentsJson).optString("app_name"))

        // 26. may tinh -> calculator
        val r6 = matcher.match("may tinh")
        assertNotNull(r6)
        assertEquals("open_app", r6?.intent)
        assertEquals("calculator", JSONObject(r6!!.argumentsJson).optString("app_name"))

        // Shopee / Lazada / Grab / Be
        val rShopee = matcher.match("mở shopee")
        assertNotNull(rShopee)
        assertEquals("open_app", rShopee?.intent)
        assertEquals("shopee", JSONObject(rShopee!!.argumentsJson).optString("app_name"))

        val rGrab = matcher.match("mở grab")
        assertNotNull(rGrab)
        assertEquals("open_app", rGrab?.intent)
        assertEquals("grab", JSONObject(rGrab!!.argumentsJson).optString("app_name"))
    }

    @Test
    fun testNegativeCasesToAvoidFalsePositives() {
        assertNull("Câu 'tôi nói chuyện về youtube' không được tự biến thành open_app", matcher.match("tôi nói chuyện về youtube"))
        assertNull("Câu 'Tôi thích xem video ngắn' không được tự biến thành open_app", matcher.match("Tôi thích xem video ngắn"))
        assertNull("Câu 'Tôi cần số điện thoại của anh' không được tự biến thành open_app", matcher.match("Tôi cần số điện thoại của anh"))
        assertNull("Câu 'tôi đang tính toán' không được tự biến thành open_app", matcher.match("tôi đang tính toán"))
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

        // Warmup 500 iterations
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
