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
    private val fixedTP = TimeProvider.createFixed(hour = 10, minute = 20, second = 0)
    private val relMatcher = FastPathMatcher(context = null, timeProvider = fixedTP)

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
        val r1 = matcher.match("báo thức bảy giờ")
        assertNotNull(r1)
        assertEquals("set_alarm", r1?.intent)
        assertEquals(7, JSONObject(r1!!.argumentsJson).optInt("hour"))
        assertEquals(0, JSONObject(r1.argumentsJson).optInt("minute"))

        val r2 = matcher.match("báo thức bảy giờ rưỡi")
        assertNotNull(r2)
        assertEquals("set_alarm", r2?.intent)
        assertEquals(7, JSONObject(r2!!.argumentsJson).optInt("hour"))
        assertEquals(30, JSONObject(r2.argumentsJson).optInt("minute"))

        val r3 = matcher.match("báo thức 7 giờ tối")
        assertNotNull(r3)
        assertEquals("set_alarm", r3?.intent)
        assertEquals(19, JSONObject(r3!!.argumentsJson).optInt("hour"))
        assertEquals(0, JSONObject(r3.argumentsJson).optInt("minute"))

        val r3Text = matcher.match("báo thức bảy giờ tối")
        assertNotNull(r3Text)
        assertEquals(19, JSONObject(r3Text!!.argumentsJson).optInt("hour"))

        val r4 = matcher.match("đặt báo thức bảy giờ rưỡi tối")
        assertNotNull(r4)
        assertEquals("set_alarm", r4?.intent)
        assertEquals(19, JSONObject(r4!!.argumentsJson).optInt("hour"))
        assertEquals(30, JSONObject(r4.argumentsJson).optInt("minute"))

        val r5 = matcher.match("báo thức hai giờ chiều")
        assertNotNull(r5)
        assertEquals("set_alarm", r5?.intent)
        assertEquals(14, JSONObject(r5!!.argumentsJson).optInt("hour"))
        assertEquals(0, JSONObject(r5.argumentsJson).optInt("minute"))

        val r6 = matcher.match("báo thức tám giờ sáng")
        assertNotNull(r6)
        assertEquals("set_alarm", r6?.intent)
        assertEquals(8, JSONObject(r6!!.argumentsJson).optInt("hour"))
        assertEquals(0, JSONObject(r6.argumentsJson).optInt("minute"))

        val r7 = matcher.match("báo thức mười một giờ đêm")
        assertNotNull(r7)
        assertEquals("set_alarm", r7?.intent)
        assertEquals(23, JSONObject(r7!!.argumentsJson).optInt("hour"))
        assertEquals(0, JSONObject(r7.argumentsJson).optInt("minute"))

        val r8 = matcher.match("báo thức mười hai giờ đêm")
        assertNotNull(r8)
        assertEquals("set_alarm", r8?.intent)
        assertEquals(0, JSONObject(r8!!.argumentsJson).optInt("hour"))
        assertEquals(0, JSONObject(r8.argumentsJson).optInt("minute"))

        val rTrua = matcher.match("báo thức 12 giờ trưa")
        assertNotNull(rTrua)
        assertEquals(12, JSONObject(rTrua!!.argumentsJson).optInt("hour"))

        val rKem1 = matcher.match("báo thức 8 giờ kém 15")
        assertNotNull(rKem1)
        assertEquals("set_alarm", rKem1?.intent)
        assertEquals(7, JSONObject(rKem1!!.argumentsJson).optInt("hour"))
        assertEquals(45, JSONObject(rKem1.argumentsJson).optInt("minute"))

        val rKem3 = matcher.match("báo thức 2 h kém 10")
        assertNotNull(rKem3)
        assertEquals("set_alarm", rKem3?.intent)
        assertEquals(1, JSONObject(rKem3!!.argumentsJson).optInt("hour"))
        assertEquals(50, JSONObject(rKem3.argumentsJson).optInt("minute"))

        val rKem4 = matcher.match("báo thức 3 h kém 10")
        assertNotNull(rKem4)
        assertEquals("set_alarm", rKem4?.intent)
        assertEquals(2, JSONObject(rKem4!!.argumentsJson).optInt("hour"))
        assertEquals(50, JSONObject(rKem4.argumentsJson).optInt("minute"))

        val rKem2 = matcher.match("báo thức tám giờ kém mười lăm tối")
        assertNotNull(rKem2)
        assertEquals("set_alarm", rKem2?.intent)
        assertEquals(19, JSONObject(rKem2!!.argumentsJson).optInt("hour"))
        assertEquals(45, JSONObject(rKem2.argumentsJson).optInt("minute"))

        val rThieu = matcher.match("báo thức 8 giờ thiếu 15")
        assertNotNull(rThieu)
        assertEquals("set_alarm", rThieu?.intent)
        assertEquals(7, JSONObject(rThieu!!.argumentsJson).optInt("hour"))
        assertEquals(45, JSONObject(rThieu.argumentsJson).optInt("minute"))

        val rKhuya = matcher.match("báo thức 1 giờ khuya")
        assertNotNull(rKhuya)
        assertEquals("set_alarm", rKhuya?.intent)
        assertEquals(1, JSONObject(rKhuya!!.argumentsJson).optInt("hour"))
        assertEquals(0, JSONObject(rKhuya.argumentsJson).optInt("minute"))

        val rTam = matcher.match("báo thức tầm 7 giờ sáng")
        assertNotNull(rTam)
        assertEquals("set_alarm", rTam?.intent)
        assertEquals(7, JSONObject(rTam!!.argumentsJson).optInt("hour"))
        assertEquals(0, JSONObject(rTam.argumentsJson).optInt("minute"))

        val rDung = matcher.match("báo thức 7 giờ đúng")
        assertNotNull(rDung)
        assertEquals("set_alarm", rDung?.intent)
        assertEquals(7, JSONObject(rDung!!.argumentsJson).optInt("hour"))
        assertEquals(0, JSONObject(rDung.argumentsJson).optInt("minute"))

        val rToMo = matcher.match("báo thức 4 giờ tờ mờ sáng")
        assertNotNull(rToMo)
        assertEquals(4, JSONObject(rToMo!!.argumentsJson).optInt("hour"))

        val rXeChieu = matcher.match("báo thức 4 giờ xế chiều")
        assertNotNull(rXeChieu)
        assertEquals(16, JSONObject(rXeChieu!!.argumentsJson).optInt("hour"))

        val rChangVang = matcher.match("báo thức 6 giờ chạng vạng")
        assertNotNull(rChangVang)
        assertEquals(18, JSONObject(rChangVang!!.argumentsJson).optInt("hour"))

        val rNhacToi = matcher.match("nhắc tôi 7 giờ sáng")
        assertNotNull(rNhacToi)
        assertEquals("set_alarm", rNhacToi?.intent)
        assertEquals(7, JSONObject(rNhacToi!!.argumentsJson).optInt("hour"))

        val rChuongBaoThuc = matcher.match("đặt chuông báo thức 8 giờ tối")
        assertNotNull(rChuongBaoThuc)
        assertEquals("set_alarm", rChuongBaoThuc?.intent)
        assertEquals(20, JSONObject(rChuongBaoThuc!!.argumentsJson).optInt("hour"))

        val rKhongGioRuoi = matcher.match("báo thức không giờ rưỡi")
        assertNotNull(rKhongGioRuoi)
        assertEquals("set_alarm", rKhongGioRuoi?.intent)
        assertEquals(0, JSONObject(rKhongGioRuoi!!.argumentsJson).optInt("hour"))
        assertEquals(30, JSONObject(rKhongGioRuoi!!.argumentsJson).optInt("minute"))
    }

    @Test
    fun testKiemAndThieu20RequiredTestCases() {
        val testMap = listOf(
            "báo thức 2 giờ kém 10" to Pair(1, 50),
            "báo thức 2 giờ kém 10 phút" to Pair(1, 50),
            "báo thức hai giờ kém mười" to Pair(1, 50),
            "báo thức hai giờ kém mười phút" to Pair(1, 50),
            "báo thức 7 giờ kém 15" to Pair(6, 45),
            "báo thức bảy giờ kém mười lăm" to Pair(6, 45),
            "báo thức 8 giờ kém 5" to Pair(7, 55),
            "báo thức tám giờ kém năm" to Pair(7, 55),
            "báo thức 8 giờ kém 5 sáng" to Pair(7, 55),
            "báo thức 2 giờ kém 10 chiều" to Pair(13, 50),
            "báo thức 2 giờ kém 10 tối" to Pair(19, 50),
            "báo thức 7 giờ kém 15 tối" to Pair(18, 45),
            "báo thức 8 giờ kém 5 tối" to Pair(19, 55),
            "báo thức 11 giờ kém 10 đêm" to Pair(22, 50),
            "báo thức 12 giờ kém 10" to Pair(11, 50),
            "báo thức 1 giờ kém 10" to Pair(0, 50),
            "báo thức 2 giờ 10" to Pair(2, 10),
            "báo thức 2 giờ 10 phút" to Pair(2, 10),
            "báo thức 2 giờ rưỡi" to Pair(2, 30),
            "báo thức 2 giờ thiếu 10" to Pair(1, 50)
        )

        for ((query, expected) in testMap) {
            val result = matcher.match(query)
            assertNotNull("Query '$query' must match alarm", result)
            assertEquals("set_alarm", result?.intent)
            val json = JSONObject(result!!.argumentsJson)
            assertEquals("Hour mismatch for query: '$query'", expected.first, json.optInt("hour"))
            assertEquals("Minute mismatch for query: '$query'", expected.second, json.optInt("minute"))
        }
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
        val fixedTimeProvider = TimeProvider.createFixed(hour = 10, minute = 20, second = 0)
        val relativeMatcher = FastPathMatcher(context = null, timeProvider = fixedTimeProvider)

        val r1 = relativeMatcher.match("sau 5 phút")
        assertNotNull(r1)
        assertEquals("set_timer", r1?.intent)
        assertEquals(5, JSONObject(r1!!.argumentsJson).optInt("duration"))
        assertEquals("minutes", JSONObject(r1.argumentsJson).optString("unit"))

        val r2 = relativeMatcher.match("sau năm phút")
        assertNotNull(r2)
        assertEquals("set_timer", r2?.intent)
        assertEquals(5, JSONObject(r2!!.argumentsJson).optInt("duration"))

        val r3 = relativeMatcher.match("5 phút nữa")
        assertNotNull(r3)
        assertEquals("set_timer", r3?.intent)
        assertEquals(5, JSONObject(r3!!.argumentsJson).optInt("duration"))

        val r4 = relativeMatcher.match("năm phút nữa")
        assertNotNull(r4)
        assertEquals("set_timer", r4?.intent)
        assertEquals(5, JSONObject(r4!!.argumentsJson).optInt("duration"))

        val r5 = relativeMatcher.match("sau 1 giờ")
        assertNotNull(r5)
        assertEquals("set_timer", r5?.intent)
        assertEquals(1, JSONObject(r5!!.argumentsJson).optInt("duration"))
        assertEquals("hours", JSONObject(r5.argumentsJson).optString("unit"))

        val r6 = relativeMatcher.match("sau một giờ")
        assertNotNull(r6)
        assertEquals("set_timer", r6?.intent)
        assertEquals(1, JSONObject(r6!!.argumentsJson).optInt("duration"))

        val r7 = relativeMatcher.match("2 tiếng nữa")
        assertNotNull(r7)
        assertEquals("set_timer", r7?.intent)
        assertEquals(2, JSONObject(r7!!.argumentsJson).optInt("duration"))
        assertEquals("hours", JSONObject(r7.argumentsJson).optString("unit"))

        val r8 = relativeMatcher.match("hai tiếng nữa")
        assertNotNull(r8)
        assertEquals("set_timer", r8?.intent)
        assertEquals(2, JSONObject(r8!!.argumentsJson).optInt("duration"))

        val r9 = relativeMatcher.match("nửa tiếng nữa")
        assertNotNull(r9)
        assertEquals("set_timer", r9?.intent)
        assertEquals(30, JSONObject(r9!!.argumentsJson).optInt("duration"))

        val r10 = relativeMatcher.match("sau nửa giờ")
        assertNotNull(r10)
        assertEquals("set_timer", r10?.intent)
        assertEquals(30, JSONObject(r10!!.argumentsJson).optInt("duration"))

        val r11 = relativeMatcher.match("sau 20 giây")
        assertNotNull(r11)
        assertEquals("set_timer", r11?.intent)
        assertEquals(20, JSONObject(r11!!.argumentsJson).optInt("duration"))
        assertEquals("seconds", JSONObject(r11.argumentsJson).optString("unit"))

        val r12 = relativeMatcher.match("hai mươi giây nữa")
        assertNotNull(r12)
        assertEquals("set_timer", r12?.intent)
        assertEquals(20, JSONObject(r12!!.argumentsJson).optInt("duration"))

        val r14 = relativeMatcher.match("báo thức bây giờ cộng 10 phút")
        assertNotNull(r14)
        assertEquals("set_alarm", r14?.intent)
        assertEquals(10, JSONObject(r14!!.argumentsJson).optInt("hour"))
        assertEquals(30, JSONObject(r14.argumentsJson).optInt("minute"))

        val rTimer = relativeMatcher.match("sau 10 phút")
        assertEquals("set_timer", rTimer?.intent)

        val rAlarm = relativeMatcher.match("báo thức sau 10 phút")
        assertEquals("set_alarm", rAlarm?.intent)
        assertEquals(10, JSONObject(rAlarm!!.argumentsJson).optInt("hour"))
        assertEquals(30, JSONObject(rAlarm.argumentsJson).optInt("minute"))
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
    fun testSetTimerVietnameseNumbersAndHalfHour() {
        val r1 = matcher.match("hẹn giờ mười lăm phút")
        assertNotNull(r1)
        assertEquals("set_timer", r1?.intent)
        assertEquals(15, JSONObject(r1!!.argumentsJson).optInt("duration"))
        assertEquals("minutes", JSONObject(r1.argumentsJson).optString("unit"))

        val r2 = matcher.match("hẹn giờ hai mươi giây")
        assertNotNull(r2)
        assertEquals("set_timer", r2?.intent)
        assertEquals(20, JSONObject(r2!!.argumentsJson).optInt("duration"))
        assertEquals("seconds", JSONObject(r2.argumentsJson).optString("unit"))

        val r3 = matcher.match("hẹn giờ nửa tiếng")
        assertNotNull(r3)
        assertEquals("set_timer", r3?.intent)
        assertEquals(30, JSONObject(r3!!.argumentsJson).optInt("duration"))
        assertEquals("minutes", JSONObject(r3.argumentsJson).optString("unit"))

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
        val r1 = matcher.match("du tup")
        assertNotNull(r1)
        assertEquals("open_app", r1?.intent)
        assertEquals("youtube", JSONObject(r1!!.argumentsJson).optString("app_name"))

        val r2 = matcher.match("phay buc")
        assertNotNull(r2)
        assertEquals("open_app", r2?.intent)
        assertEquals("facebook", JSONObject(r2!!.argumentsJson).optString("app_name"))

        val r3 = matcher.match("top top")
        assertNotNull(r3)
        assertEquals("open_app", r3?.intent)
        assertEquals("tiktok", JSONObject(r3!!.argumentsJson).optString("app_name"))

        val r4 = matcher.match("guc go map")
        assertNotNull(r4)
        assertEquals("open_app", r4?.intent)
        assertEquals("google_maps", JSONObject(r4!!.argumentsJson).optString("app_name"))

        val r5 = matcher.match("ch play")
        assertNotNull(r5)
        assertEquals("open_app", r5?.intent)
        assertEquals("playstore", JSONObject(r5!!.argumentsJson).optString("app_name"))

        val r6 = matcher.match("may tinh")
        assertNotNull(r6)
        assertEquals("open_app", r6?.intent)
        assertEquals("calculator", JSONObject(r6!!.argumentsJson).optString("app_name"))

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
            "báo thức 2 giờ thiếu 10" to Pair(1, 50),       // 41
            "báo thức 12 giờ kém 10 trưa" to Pair(11, 50),
            "báo thức 1 giờ kém 10 trưa" to Pair(12, 50),
            "báo thức 12 giờ kém 10 đêm" to Pair(23, 50),
            "báo thức 1 giờ kém 10 đêm" to Pair(0, 50)
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

    // =========================================================================
    // 100+ GRANULAR TEST METHODS (INDIVIDUAL PER-CASE TEST COVERAGE)
    // =========================================================================

    // 1. VIETNAMESE NUMBER PARSER TESTS
    @Test fun testGranularNumber_mot() = assertEquals(1, VietnameseNumberParser.parse("một"))
    @Test fun testGranularNumber_hai() = assertEquals(2, VietnameseNumberParser.parse("hai"))
    @Test fun testGranularNumber_ba() = assertEquals(3, VietnameseNumberParser.parse("ba"))
    @Test fun testGranularNumber_bon() = assertEquals(4, VietnameseNumberParser.parse("bốn"))
    @Test fun testGranularNumber_tu() = assertEquals(4, VietnameseNumberParser.parse("tư"))
    @Test fun testGranularNumber_nam() = assertEquals(5, VietnameseNumberParser.parse("năm"))
    @Test fun testGranularNumber_lam() = assertEquals(5, VietnameseNumberParser.parse("lăm"))
    @Test fun testGranularNumber_sau() = assertEquals(6, VietnameseNumberParser.parse("sáu"))
    @Test fun testGranularNumber_bay() = assertEquals(7, VietnameseNumberParser.parse("bảy"))
    @Test fun testGranularNumber_tam() = assertEquals(8, VietnameseNumberParser.parse("tám"))
    @Test fun testGranularNumber_chin() = assertEquals(9, VietnameseNumberParser.parse("chín"))
    @Test fun testGranularNumber_muoi() = assertEquals(10, VietnameseNumberParser.parse("mười"))
    @Test fun testGranularNumber_muoiMot() = assertEquals(11, VietnameseNumberParser.parse("mười một"))
    @Test fun testGranularNumber_muoiLam() = assertEquals(15, VietnameseNumberParser.parse("mười lăm"))
    @Test fun testGranularNumber_haiMuoi() = assertEquals(20, VietnameseNumberParser.parse("hai mươi"))
    @Test fun testGranularNumber_haiMuoiMot() = assertEquals(21, VietnameseNumberParser.parse("hai mươi mốt"))
    @Test fun testGranularNumber_haiMuoiTu() = assertEquals(24, VietnameseNumberParser.parse("hai mươi tư"))
    @Test fun testGranularNumber_haiMuoiLam() = assertEquals(25, VietnameseNumberParser.parse("hai mươi lăm"))

    // 2. ALARM HOUR WORDS TESTS
    @Test fun testGranularAlarm_sauGio() { val r = matcher.match("báo thức sáu giờ"); assertNotNull(r); assertEquals(6, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testGranularAlarm_sauGioNoAccents() { val r = matcher.match("báo thức sau gio"); assertNotNull(r); assertEquals(6, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testGranularAlarm_bayGioRuoi() { val r = matcher.match("báo thức bảy giờ rưỡi"); assertNotNull(r); assertEquals(7, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(30, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_bayGioRuoiNoAccents() { val r = matcher.match("báo thức bay gio ruoi"); assertNotNull(r); assertEquals(7, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(30, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_bayGioRuoiToi() { val r = matcher.match("báo thức 7 giờ rưỡi tối"); assertNotNull(r); assertEquals(19, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(30, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_haiGioRuoiChieu() { val r = matcher.match("báo thức 2 giờ rưỡi chiều"); assertNotNull(r); assertEquals(14, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(30, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_muoiGioSang() { val r = matcher.match("báo thức mười giờ sáng"); assertNotNull(r); assertEquals(10, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testGranularAlarm_muoiHaiGioTrua() { val r = matcher.match("báo thức mười hai giờ trưa"); assertNotNull(r); assertEquals(12, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testGranularAlarm_muoiHaiGioDem() { val r = matcher.match("báo thức mười hai giờ đêm"); assertNotNull(r); assertEquals(0, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testGranularAlarm_khongGioRuoi() { val r = matcher.match("báo thức không giờ rưỡi"); assertNotNull(r); assertEquals(0, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(30, JSONObject(r.argumentsJson).optInt("minute")) }

    // 3. ALARM PERIODS TESTS
    @Test fun testGranularAlarm_7GioToi() { val r = matcher.match("báo thức 7 giờ tối"); assertNotNull(r); assertEquals(19, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testGranularAlarm_2GioChieu() { val r = matcher.match("báo thức 2 giờ chiều"); assertNotNull(r); assertEquals(14, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testGranularAlarm_8GioSang() { val r = matcher.match("báo thức 8 giờ sáng"); assertNotNull(r); assertEquals(8, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testGranularAlarm_11GioDem() { val r = matcher.match("báo thức 11 giờ đêm"); assertNotNull(r); assertEquals(23, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testGranularAlarm_12GioSang() { val r = matcher.match("báo thức 12 giờ sáng"); assertNotNull(r); assertEquals(0, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testGranularAlarm_12GioTrua() { val r = matcher.match("báo thức 12 giờ trưa"); assertNotNull(r); assertEquals(12, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testGranularAlarm_12GioDem() { val r = matcher.match("báo thức 12 giờ đêm"); assertNotNull(r); assertEquals(0, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testGranularAlarm_3GioChieu() { val r = matcher.match("báo thức 3 giờ chiều"); assertNotNull(r); assertEquals(15, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testGranularAlarm_9GioToi() { val r = matcher.match("báo thức 9 giờ tối"); assertNotNull(r); assertEquals(21, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testGranularAlarm_5GioSang() { val r = matcher.match("báo thức 5 giờ sáng"); assertNotNull(r); assertEquals(5, JSONObject(r!!.argumentsJson).optInt("hour")) }

    // 4. GIỜ KÉM & THIẾU TESTS
    @Test fun testGranularAlarm_2GioKem10() { val r = matcher.match("báo thức 2 giờ kém 10"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_2GioKem10Phut() { val r = matcher.match("báo thức 2 giờ kém 10 phút"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_haiGioKemMuoi() { val r = matcher.match("báo thức hai giờ kém mười"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_haiGioKemMuoiPhut() { val r = matcher.match("báo thức hai giờ kém mười phút"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_7GioKem15() { val r = matcher.match("báo thức 7 giờ kém 15"); assertNotNull(r); assertEquals(6, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(45, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_bayGioKemMuoiLam() { val r = matcher.match("báo thức bảy giờ kém mười lăm"); assertNotNull(r); assertEquals(6, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(45, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_8GioKem5() { val r = matcher.match("báo thức 8 giờ kém 5"); assertNotNull(r); assertEquals(7, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(55, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_tamGioKemNam() { val r = matcher.match("báo thức tám giờ kém năm"); assertNotNull(r); assertEquals(7, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(55, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_2GioKem10Toi() { val r = matcher.match("báo thức 2 giờ kém 10 tối"); assertNotNull(r); assertEquals(19, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_7GioKem15Toi() { val r = matcher.match("báo thức 7 giờ kém 15 tối"); assertNotNull(r); assertEquals(18, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(45, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_8GioKem5Toi() { val r = matcher.match("báo thức 8 giờ kém 5 tối"); assertNotNull(r); assertEquals(19, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(55, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_11GioKem10Dem() { val r = matcher.match("báo thức 11 giờ kém 10 đêm"); assertNotNull(r); assertEquals(22, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_2GioThieu10() { val r = matcher.match("báo thức 2 giờ thiếu 10"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_12GioKem10Trua() { val r = matcher.match("báo thức 12 giờ kém 10 trưa"); assertNotNull(r); assertEquals(11, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_1GioKem10Trua() { val r = matcher.match("báo thức 1 giờ kém 10 trưa"); assertNotNull(r); assertEquals(12, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_12GioKem10Dem() { val r = matcher.match("báo thức 12 giờ kém 10 đêm"); assertNotNull(r); assertEquals(23, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_1GioKem10Dem() { val r = matcher.match("báo thức 1 giờ kém 10 đêm"); assertNotNull(r); assertEquals(0, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGranularAlarm_2_45_Kem15() { val r = matcher.match("Đặt báo thức 2:45 kém 15"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(45, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGoogleSttItnCorrection_2_45() { val r = matcher.match("Đặt báo thức 2:45"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(45, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGoogleSttItnCorrection_2_50() { val r = matcher.match("Đặt báo thức 2:50"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testGoogleSttSpoken_2Gio45() { val r = matcher.match("Đặt báo thức 2 giờ 45"); assertNotNull(r); assertEquals(2, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(45, JSONObject(r.argumentsJson).optInt("minute")) }

    // 5. TIMER DURATIONS TESTS
    @Test fun testGranularTimer_15Phut() { val r = matcher.match("hẹn giờ 15 phút"); assertNotNull(r); assertEquals(15, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testGranularTimer_muoiLamPhut() { val r = matcher.match("hẹn giờ mười lăm phút"); assertNotNull(r); assertEquals(15, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testGranularTimer_muoiLamPhutNoAccents() { val r = matcher.match("hẹn giờ muoi lam phut"); assertNotNull(r); assertEquals(15, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testGranularTimer_20Giay() { val r = matcher.match("hẹn giờ 20 giây"); assertNotNull(r); assertEquals(20, JSONObject(r!!.argumentsJson).optInt("duration")); assertEquals("seconds", JSONObject(r.argumentsJson).optString("unit")) }
    @Test fun testGranularTimer_haiMuoiGiay() { val r = matcher.match("hẹn giờ hai mươi giây"); assertNotNull(r); assertEquals(20, JSONObject(r!!.argumentsJson).optInt("duration")); assertEquals("seconds", JSONObject(r.argumentsJson).optString("unit")) }
    @Test fun testGranularTimer_nuaTieng() { val r = matcher.match("hẹn giờ nua tieng"); assertNotNull(r); assertEquals(30, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testGranularTimer_nuaGio() { val r = matcher.match("hẹn giờ nua gio"); assertNotNull(r); assertEquals(30, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testGranularTimer_1Gio() { val r = matcher.match("hẹn giờ 1 giờ"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("duration")); assertEquals("hours", JSONObject(r.argumentsJson).optString("unit")) }
    @Test fun testGranularTimer_motTieng() { val r = matcher.match("hẹn giờ một tiếng"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testGranularTimer_30Phut() { val r = matcher.match("hẹn giờ 30 phút"); assertNotNull(r); assertEquals(30, JSONObject(r!!.argumentsJson).optInt("duration")) }

    // 6. RELATIVE TIME TESTS
    @Test fun testGranularRelativeTimer_sau5Phut() { val r = relMatcher.match("sau 5 phút"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(5, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testGranularRelativeTimer_namPhutNua() { val r = relMatcher.match("năm phút nữa"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(5, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testGranularRelativeTimer_sau2Tieng() { val r = relMatcher.match("sau 2 tiếng"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(2, JSONObject(r!!.argumentsJson).optInt("duration")); assertEquals("hours", JSONObject(r.argumentsJson).optString("unit")) }
    @Test fun testGranularRelativeTimer_haiTiengNua() { val r = relMatcher.match("hai tiếng nữa"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(2, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testGranularRelativeTimer_sau20Giay() { val r = relMatcher.match("sau 20 giây"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(20, JSONObject(r!!.argumentsJson).optInt("duration")); assertEquals("seconds", JSONObject(r.argumentsJson).optString("unit")) }
    @Test fun testGranularRelativeTimer_haiMuoiGiayNua() { val r = relMatcher.match("hai mươi giây nữa"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(20, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testGranularRelativeTimer_nuaTiengNua() { val r = relMatcher.match("nửa tiếng nữa"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(30, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testGranularRelativeTimer_sauNuaGio() { val r = relMatcher.match("sau nửa giờ"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(30, JSONObject(r!!.argumentsJson).optInt("duration")) }

    // 7. APP ALIASES TESTS
    @Test fun testGranularApp_camera() { val r = matcher.match("mở camera"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("camera", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_mayAnh() { val r = matcher.match("mở máy ảnh"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("camera", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_gallery() { val r = matcher.match("mở gallery"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("gallery", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_boSuuTap() { val r = matcher.match("bo suu tap"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("gallery", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_contacts() { val r = matcher.match("danh ba"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("contacts", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_danhBa() { val r = matcher.match("mở danh bạ"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("contacts", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_calculator() { val r = matcher.match("mở máy tính"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("calculator", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_mayTinh() { val r = matcher.match("may tinh"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("calculator", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_playstore() { val r = matcher.match("ch play"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("playstore", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_youtube() { val r = matcher.match("mở youtube"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("youtube", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_duTup() { val r = matcher.match("du tup"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("youtube", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_yutube() { val r = matcher.match("yutube"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("youtube", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_facebook() { val r = matcher.match("mở facebook"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("facebook", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_phayBuc() { val r = matcher.match("phay buc"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("facebook", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_tiktok() { val r = matcher.match("mở tiktok"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("tiktok", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_topTop() { val r = matcher.match("top top"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("tiktok", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_googleMaps() { val r = matcher.match("mở google maps"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("google_maps", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_gucGoMap() { val r = matcher.match("guc go map"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("google_maps", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_shopee() { val r = matcher.match("shopee"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("shopee", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_lazada() { val r = matcher.match("lazada"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("lazada", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_grab() { val r = matcher.match("grab"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("grab", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testGranularApp_be() { val r = matcher.match("mở app be"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("be", JSONObject(r!!.argumentsJson).optString("app_name")) }

    // 8. NEGATIVE TESTS
    @Test fun testGranularNegative_toiNoiChuyenVeYoutube() { assertNull(matcher.match("tôi nói chuyện về youtube")) }
    @Test fun testGranularNegative_toiThichXemVideoNgan() { assertNull(matcher.match("Tôi thích xem video ngắn")) }
    @Test fun testGranularNegative_toiCanSoDienThoai() { assertNull(matcher.match("Tôi cần số điện thoại của anh")) }
    @Test fun testGranularNegative_toiDangTinhToan() { assertNull(matcher.match("tôi đang tính toán")) }
    @Test fun testGranularNegative_unknownApp() { assertNull(matcher.match("tôi muốn mở ứng dụng lạ không có thật abc xyz")) }
    @Test fun testGranularNegative_hourOutOfRange() { assertNull(matcher.match("báo thức lúc 25 giờ 90 phút")) }
    @Test fun testGranularNegative_minuteOutOfRange() { assertNull(matcher.match("báo thức 2 giờ 70 phút")) }

    @Test
    fun testVietnameseVoiceVariationsUpgrade() {
        // a. Chuyển đổi số dạng chữ (Vietnamese Number Word Parser)
        val rSauGio = matcher.match("báo thức sáu giờ")
        assertNotNull(rSauGio); assertEquals("set_alarm", rSauGio?.intent); assertEquals(6, JSONObject(rSauGio!!.argumentsJson).optInt("hour")); assertEquals(0, JSONObject(rSauGio.argumentsJson).optInt("minute"))

        val rSauGioUnaccented = matcher.match("báo thức sau gio")
        assertNotNull(rSauGioUnaccented); assertEquals("set_alarm", rSauGioUnaccented?.intent); assertEquals(6, JSONObject(rSauGioUnaccented!!.argumentsJson).optInt("hour")); assertEquals(0, JSONObject(rSauGioUnaccented.argumentsJson).optInt("minute"))

        val rBayGioRuoi = matcher.match("báo thức bảy giờ rưỡi")
        assertNotNull(rBayGioRuoi); assertEquals("set_alarm", rBayGioRuoi?.intent); assertEquals(7, JSONObject(rBayGioRuoi!!.argumentsJson).optInt("hour")); assertEquals(30, JSONObject(rBayGioRuoi.argumentsJson).optInt("minute"))

        val rBayGioRuoiUnaccented = matcher.match("báo thức bay gio ruoi")
        assertNotNull(rBayGioRuoiUnaccented); assertEquals("set_alarm", rBayGioRuoiUnaccented?.intent); assertEquals(7, JSONObject(rBayGioRuoiUnaccented!!.argumentsJson).optInt("hour")); assertEquals(30, JSONObject(rBayGioRuoiUnaccented.argumentsJson).optInt("minute"))

        val rMuoiLamPhut = matcher.match("hẹn giờ mười lăm phút")
        assertNotNull(rMuoiLamPhut); assertEquals("set_timer", rMuoiLamPhut?.intent); assertEquals(15, JSONObject(rMuoiLamPhut!!.argumentsJson).optInt("duration")); assertEquals("minutes", JSONObject(rMuoiLamPhut.argumentsJson).optString("unit"))

        val rMuoiLamPhutUnaccented = matcher.match("hẹn giờ muoi lam phut")
        assertNotNull(rMuoiLamPhutUnaccented); assertEquals("set_timer", rMuoiLamPhutUnaccented?.intent); assertEquals(15, JSONObject(rMuoiLamPhutUnaccented!!.argumentsJson).optInt("duration"))

        val rHaiMuoiGiay = matcher.match("hẹn giờ hai mươi giây")
        assertNotNull(rHaiMuoiGiay); assertEquals("set_timer", rHaiMuoiGiay?.intent); assertEquals(20, JSONObject(rHaiMuoiGiay!!.argumentsJson).optInt("duration")); assertEquals("seconds", JSONObject(rHaiMuoiGiay.argumentsJson).optString("unit"))

        val rHaiMuoiGiayUnaccented = matcher.match("hẹn giờ hai muoi giay")
        assertNotNull(rHaiMuoiGiayUnaccented); assertEquals("set_timer", rHaiMuoiGiayUnaccented?.intent); assertEquals(20, JSONObject(rHaiMuoiGiayUnaccented!!.argumentsJson).optInt("duration")); assertEquals("seconds", JSONObject(rHaiMuoiGiayUnaccented.argumentsJson).optString("unit"))

        val rNuaTieng = matcher.match("hẹn giờ nửa tiếng")
        assertNotNull(rNuaTieng); assertEquals("set_timer", rNuaTieng?.intent); assertEquals(30, JSONObject(rNuaTieng!!.argumentsJson).optInt("duration")); assertEquals("minutes", JSONObject(rNuaTieng.argumentsJson).optString("unit"))

        val rNuaGioUnaccented = matcher.match("hẹn giờ nua gio")
        assertNotNull(rNuaGioUnaccented); assertEquals("set_timer", rNuaGioUnaccented?.intent); assertEquals(30, JSONObject(rNuaGioUnaccented!!.argumentsJson).optInt("duration")); assertEquals("minutes", JSONObject(rNuaGioUnaccented.argumentsJson).optString("unit"))

        // Test độc lập không từ tiền tố
        val rStandalone1 = matcher.match("sau gio")
        assertNotNull(rStandalone1); assertEquals("set_alarm", rStandalone1?.intent); assertEquals(6, JSONObject(rStandalone1!!.argumentsJson).optInt("hour"))

        val rStandalone2 = matcher.match("bay gio ruoi")
        assertNotNull(rStandalone2); assertEquals("set_alarm", rStandalone2?.intent); assertEquals(7, JSONObject(rStandalone2!!.argumentsJson).optInt("hour")); assertEquals(30, JSONObject(rStandalone2.argumentsJson).optInt("minute"))

        val rStandalone3 = matcher.match("muoi lam phut")
        assertNotNull(rStandalone3); assertEquals("set_timer", rStandalone3?.intent); assertEquals(15, JSONObject(rStandalone3!!.argumentsJson).optInt("duration")); assertEquals("minutes", JSONObject(rStandalone3.argumentsJson).optString("unit"))

        val rStandalone4 = matcher.match("hai muoi giay")
        assertNotNull(rStandalone4); assertEquals("set_timer", rStandalone4?.intent); assertEquals(20, JSONObject(rStandalone4!!.argumentsJson).optInt("duration")); assertEquals("seconds", JSONObject(rStandalone4.argumentsJson).optString("unit"))

        val rStandalone5 = matcher.match("nua tieng")
        assertNotNull(rStandalone5); assertEquals("set_timer", rStandalone5?.intent); assertEquals(30, JSONObject(rStandalone5!!.argumentsJson).optInt("duration")); assertEquals("minutes", JSONObject(rStandalone5.argumentsJson).optString("unit"))

        // b. Quy đổi thời gian theo buổi (24-Hour Period Normalizer)
        val periodTestMap = mapOf(
            "báo thức 5 giờ chiều" to Pair(17, 0),
            "báo thức 6 giờ tối" to Pair(18, 0),
            "báo thức 7 giờ tối" to Pair(19, 0),
            "báo thức 8 giờ tối" to Pair(20, 0),
            "báo thức 9 giờ tối" to Pair(21, 0),
            "báo thức 10 giờ tối" to Pair(22, 0),
            "báo thức 11 giờ tối" to Pair(23, 0),
            "báo thức 12 giờ đêm" to Pair(0, 0),
            "báo thức 1 giờ đêm" to Pair(1, 0),
            "báo thức 2 giờ đêm" to Pair(2, 0),
            "báo thức 3 giờ đêm" to Pair(3, 0),
            "báo thức 4 giờ sáng" to Pair(4, 0),
            "báo thức 7 giờ sáng" to Pair(7, 0),
            "báo thức 12 giờ trưa" to Pair(12, 0),
            "báo thức 1 giờ trưa" to Pair(13, 0),
            "báo thức 2 giờ chiều" to Pair(14, 0),
            "báo thức 7 giờ kém một phần tư" to Pair(6, 45),
            "báo thức 8:00 sáng" to Pair(8, 0),
            "báo thức 8:00 tối" to Pair(20, 0),
            "báo thức lúc 8:00 sáng" to Pair(8, 0)
        )
        for ((query, expected) in periodTestMap) {
            val res = matcher.match(query)
            assertNotNull("Period query '$query' must not be null", res)
            assertEquals("set_alarm", res?.intent)
            val json = JSONObject(res!!.argumentsJson)
            assertEquals("Hour mismatch for '$query'", expected.first, json.optInt("hour"))
            assertEquals("Minute mismatch for '$query'", expected.second, json.optInt("minute"))
        }

        // c. Mở rộng từ điển App Name & Từ lóng tiếng Việt
        val appTestMap = mapOf(
            "du tup" to "youtube",
            "yutube" to "youtube",
            "youtube" to "youtube",
            "nhac youtube" to "youtube",
            "mở nhac youtube" to "youtube",
            "phay" to "facebook",
            "phay buc" to "facebook",
            "fb" to "facebook",
            "facebook" to "facebook",
            "face" to "facebook",
            "mở face" to "facebook",
            "vào face" to "facebook",
            "top top" to "tiktok",
            "tik tok" to "tiktok",
            "tiktok" to "tiktok",
            "guc go map" to "google_maps",
            "bản đồ" to "google_maps",
            "chỉ đường" to "google_maps",
            "máy tính" to "calculator",
            "bộ sưu tập" to "gallery",
            "anh" to "gallery",
            "mở ảnh" to "gallery",
            "danh bạ" to "contacts",
            "ch play" to "playstore",
            "shopee" to "shopee",
            "lazada" to "lazada",
            "grab" to "grab",
            "be" to "be"
        )

        for ((query, expectedApp) in appTestMap) {
            val res = matcher.match(query)
            assertNotNull("App query '$query' must not be null", res)
            assertEquals("open_app", res?.intent)
            val json = JSONObject(res!!.argumentsJson)
            assertEquals("App name mismatch for '$query'", expectedApp, json.optString("app_name"))
        }
    }

    @Test
    fun testVietnameseTimeKiemAndThieuBugFixes() {
        val testCases = listOf(
            "12 giờ kém 15" to Pair(11, 45),
            "12h kém 15" to Pair(11, 45),
            "12 kém 15" to Pair(11, 45),
            "12 giờ kém 10" to Pair(11, 50),
            "12h kém 10" to Pair(11, 50),
            "1 giờ kém 10" to Pair(0, 50),
            "1h kém 10" to Pair(0, 50),
            "2 giờ kém 10" to Pair(1, 50),
            "2h kém 10" to Pair(1, 50),
            "nói 12 giờ kém 15" to Pair(11, 45),
            "đặt 12h kém 15" to Pair(11, 45),
            "báo thức 12 giờ kém 15" to Pair(11, 45),
            "báo thức lúc 12 giờ kém 15" to Pair(11, 45),
            "đặt báo thức vào lúc 12h kém 15" to Pair(11, 45),
            "0 giờ kém 10" to Pair(23, 50),
            "1 giờ kém 5" to Pair(0, 55),
            "12 giờ kém 1" to Pair(11, 59),
            "12 giờ kém 30" to Pair(11, 30),
            "12 giờ kém 45" to Pair(11, 15),
            "12 giờ kém 59" to Pair(11, 1),
            "23 giờ kém 10" to Pair(22, 50),
            "12 giờ thiếu 15" to Pair(11, 45),
            "12h thiếu 15" to Pair(11, 45),
            "12 thiếu 15" to Pair(11, 45)
        )

        for ((query, expected) in testCases) {
            val result = matcher.match(query)
            assertNotNull("Query '$query' should match set_alarm", result)
            assertEquals("set_alarm", result?.intent)
            val json = JSONObject(result!!.argumentsJson)
            assertEquals("Hour mismatch for query '$query'", expected.first, json.optInt("hour"))
            assertEquals("Minute mismatch for query '$query'", expected.second, json.optInt("minute"))
        }
    }

    @Test
    fun testCompactTimePreservation() {
        val testCases = listOf(
            "12h45" to Pair(12, 45),
            "12h50" to Pair(12, 50),
            "1h50" to Pair(1, 50),
            "2h50" to Pair(2, 50)
        )

        for ((query, expected) in testCases) {
            val result = matcher.match(query)
            assertNotNull("Compact time query '$query' should match set_alarm", result)
            assertEquals("set_alarm", result?.intent)
            val json = JSONObject(result!!.argumentsJson)
            assertEquals("Hour mismatch for compact time '$query'", expected.first, json.optInt("hour"))
            assertEquals("Minute mismatch for compact time '$query'", expected.second, json.optInt("minute"))
        }
    }
}
}
