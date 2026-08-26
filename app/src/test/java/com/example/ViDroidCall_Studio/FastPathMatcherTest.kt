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
        val testCases = listOf(
            "Xin chào", "chào em", "hello", "hi", "hey emma", "alo", "chào bạn",
            "chào em gái", "có ai ở đó không", "chào", "hello emma", "HEY EMMA", "CHÀO BẠN"
        )
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
        val testCases = listOf(
            "Tạm biệt", "bye", "bye bye", "hẹn gặp lại", "chào tạm biệt",
            "tắt đi", "kết thúc", "dừng lại", "đóng lại", "thoát ra", "TAM BIET", "hen gap lai"
        )
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
    fun testSystemAppsComprehensiveCoverage() {
        // 1. Camera
        val cameraQueries = listOf(
            "chup anh", "chup hinh", "may anh", "mo may anh", "mo camera", "camera", "ca me ra",
            "chụp ảnh", "chụp hình", "máy ảnh", "mở máy ảnh", "vui lòng mở máy ảnh", "bật máy ảnh",
            "mở giúp tôi camera", "cho tôi mở ca me ra"
        )
        cameraQueries.forEach { query ->
            val res = matcher.match(query)
            assertNotNull("Camera query '$query' must not be null", res)
            assertEquals("open_app", res?.intent)
            assertEquals("camera", JSONObject(res!!.argumentsJson).optString("app_name"))
        }

        // 2. Gallery
        val galleryQueries = listOf(
            "xem anh", "xem hinh", "bo suu tap", "album anh", "mo anh", "thu vien anh", "thu vien",
            "gallery", "xem ảnh", "xem hình", "bộ sưu tập", "album ảnh", "thư viện ảnh",
            "mở bộ sưu tập", "cho tôi xem bộ sưu tập", "vào thư viện ảnh", "vui lòng mở album ảnh"
        )
        galleryQueries.forEach { query ->
            val res = matcher.match(query)
            assertNotNull("Gallery query '$query' must not be null", res)
            assertEquals("open_app", res?.intent)
            assertEquals("gallery", JSONObject(res!!.argumentsJson).optString("app_name"))
        }

        // 3. Calculator
        val calculatorQueries = listOf(
            "may tinh", "tinh tien", "tinh toan", "ban tinh", "mo may tinh", "calculator",
            "máy tính", "tính tiền", "tính toán", "bàn tính", "mở máy tính", "mở bàn tính",
            "cho tôi mở máy tính", "vào máy tính", "bật tính tiền"
        )
        calculatorQueries.forEach { query ->
            val res = matcher.match(query)
            assertNotNull("Calculator query '$query' must not be null", res)
            assertEquals("open_app", res?.intent)
            assertEquals("calculator", JSONObject(res!!.argumentsJson).optString("app_name"))
        }

        // 4. Contacts
        val contactsQueries = listOf(
            "danh ba", "so dien thoai", "danh sach goi", "mo danh ba", "danh bạ",
            "số điện thoại", "danh sách gọi", "mở danh bạ", "cho tôi xem danh bạ",
            "mở số điện thoại", "vào danh sách gọi"
        )
        contactsQueries.forEach { query ->
            val res = matcher.match(query)
            assertNotNull("Contacts query '$query' must not be null", res)
            assertEquals("open_app", res?.intent)
            assertEquals("contacts", JSONObject(res!!.argumentsJson).optString("app_name"))
        }

        // 5. Clock
        val clockQueries = listOf(
            "xem gio", "dong ho", "dong ho bao thuc", "bao thuc", "mo dong ho", "xem giờ",
            "đồng hồ", "đồng hồ báo thức", "báo thức", "mở đồng hồ", "cho tôi xem giờ",
            "mở đồng hồ báo thức", "vào báo thức"
        )
        clockQueries.forEach { query ->
            val res = matcher.match(query)
            assertNotNull("Clock query '$query' must not be null", res)
            assertEquals("open_app", res?.intent)
            assertEquals("clock", JSONObject(res!!.argumentsJson).optString("app_name"))
        }

        // 6. Settings
        val settingsQueries = listOf(
            "cai dat", "thiet lap", "cai dat may", "mo cai dat", "cai dat dien thoai", "settings",
            "cài đặt", "thiết lập", "cài đặt máy", "cài đặt điện thoại", "mở cài đặt",
            "vào cài đặt máy", "cho tôi mở thiết lập"
        )
        settingsQueries.forEach { query ->
            val res = matcher.match(query)
            assertNotNull("Settings query '$query' must not be null", res)
            assertEquals("open_app", res?.intent)
            assertEquals("settings", JSONObject(res!!.argumentsJson).optString("app_name"))
        }

        // 7. Recorder
        val recorderQueries = listOf(
            "ghi am", "may ghi am", "thu am", "mo ghi am", "ghi âm", "máy ghi âm", "thu âm",
            "mở ghi âm", "mở máy ghi âm", "bật thu âm", "cho tôi mở ghi âm"
        )
        recorderQueries.forEach { query ->
            val res = matcher.match(query)
            assertNotNull("Recorder query '$query' must not be null", res)
            assertEquals("open_app", res?.intent)
            assertEquals("recorder", JSONObject(res!!.argumentsJson).optString("app_name"))
        }

        // 8. Files
        val filesQueries = listOf(
            "quan ly tep", "file cua ban", "tep tin", "mo file", "quan ly file", "file",
            "quản lý tệp", "file của bạn", "tệp tin", "mở file", "quản lý file", "vào quản lý tệp",
            "mở tệp tin", "cho tôi xem file của bạn"
        )
        filesQueries.forEach { query ->
            val res = matcher.match(query)
            assertNotNull("Files query '$query' must not be null", res)
            assertEquals("open_app", res?.intent)
            assertEquals("files", JSONObject(res!!.argumentsJson).optString("app_name"))
        }

        // 9. Play Store
        val playStoreQueries = listOf(
            "tai ung dung", "cai tro choi", "ch play", "cua hang", "cua hang ung dung", "google play",
            "play store", "tải ứng dụng", "cài trò chơi", "CH Play", "cửa hàng", "cửa hàng ứng dụng",
            "mở ch play", "vào cửa hàng ứng dụng", "cho tôi tải ứng dụng"
        )
        playStoreQueries.forEach { query ->
            val res = matcher.match(query)
            assertNotNull("PlayStore query '$query' must not be null", res)
            assertEquals("open_app", res?.intent)
            assertEquals("playstore", JSONObject(res!!.argumentsJson).optString("app_name"))
        }

        // 10. Chrome
        val chromeQueries = listOf(
            "doc bao", "xem tin tuc", "len mang", "guc go", "google", "mo trinh duyet", "trinh duyet",
            "chrome", "đọc báo", "xem tin tức", "lên mạng", "mở trình duyệt", "trình duyệt",
            "mở chrome", "cho tôi đọc báo", "cho tôi lên mạng", "vào google"
        )
        chromeQueries.forEach { query ->
            val res = matcher.match(query)
            assertNotNull("Chrome query '$query' must not be null", res)
            assertEquals("open_app", res?.intent)
            assertEquals("chrome", JSONObject(res!!.argumentsJson).optString("app_name"))
        }
    }

    @Test
    fun testPopularAppsComprehensiveCoverage() {
        // 1. YouTube
        val youtubeQueries = listOf(
            "diu tup", "du tup", "dut tup", "yutube", "youtube", "xem ca nhac",
            "mo youtube", "diu túp", "du túp", "đút túp", "xem ca nhạc", "mở diu túp",
            "bật youtube", "cho tôi xem ca nhạc", "vào du túp"
        )
        youtubeQueries.forEach { query ->
            val res = matcher.match(query)
            assertNotNull("YouTube query '$query' must not be null", res)
            assertEquals("open_app", res?.intent)
            assertEquals("youtube", JSONObject(res!!.argumentsJson).optString("app_name"))
        }

        // 2. Zalo
        val zaloQueries = listOf(
            "da lo", "za lo", "da ro", "zalo", "mo zalo",
            "da-lô", "za-lô", "mở da lô", "vào za lo", "bật zalo", "cho tôi mở da lô"
        )
        zaloQueries.forEach { query ->
            val res = matcher.match(query)
            assertNotNull("Zalo query '$query' must not be null", res)
            assertEquals("open_app", res?.intent)
            assertEquals("zalo", JSONObject(res!!.argumentsJson).optString("app_name"))
        }

        // 3. Facebook
        val facebookQueries = listOf(
            "phay", "phay buc", "phay bup", "fb", "xem phay", "facebook", "mo facebook",
            "phây", "phây búc", "phây búp", "mở phây búc", "vào facebook", "bật fb", "cho tôi xem phây"
        )
        facebookQueries.forEach { query ->
            val res = matcher.match(query)
            assertNotNull("Facebook query '$query' must not be null", res)
            assertEquals("open_app", res?.intent)
            assertEquals("facebook", JSONObject(res!!.argumentsJson).optString("app_name"))
        }

        // 4. TikTok
        val tiktokQueries = listOf(
            "top top", "toc toc", "tik tok", "tiktok", "xem video ngan", "mo tiktok",
            "tóp tóp", "tóc tóc", "xem video ngắn", "mở tóp tóp", "vào tóc tóc", "cho tôi xem video ngắn"
        )
        tiktokQueries.forEach { query ->
            val res = matcher.match(query)
            assertNotNull("TikTok query '$query' must not be null", res)
            assertEquals("open_app", res?.intent)
            assertEquals("tiktok", JSONObject(res!!.argumentsJson).optString("app_name"))
        }

        // 5. Google Maps
        val mapsQueries = listOf(
            "ban do", "chi duong", "guc go map", "tim duong", "google map", "google maps",
            "mo ban do", "bản đồ", "chỉ đường", "tìm đường", "mở bản đồ", "vào google map", "bật guc go map"
        )
        mapsQueries.forEach { query ->
            val res = matcher.match(query)
            assertNotNull("Google Maps query '$query' must not be null", res)
            assertEquals("open_app", res?.intent)
            assertEquals("google_maps", JSONObject(res!!.argumentsJson).optString("app_name"))
        }
    }

    @Test
    fun testExtendedCommandPrefixesAndVariations() {
        val prefixCases = listOf(
            "Vui lòng mở giúp tôi ứng dụng YouTube" to "youtube",
            "Cho tôi xem giúp tôi bộ sưu tập" to "gallery",
            "Mở giúp tôi ứng dụng Zalo" to "zalo",
            "Vào giúp tôi TikTok" to "tiktok",
            "Hãy mở cái máy tính" to "calculator",
            "Cho tôi cái danh bạ" to "contacts",
            "Bật giúp tôi ứng dụng camera" to "camera",
            "Vào app facebook" to "facebook",
            "Mở ứng dụng CH Play" to "playstore",
            "Vui lòng mở cài đặt máy" to "settings"
        )

        for ((query, expectedApp) in prefixCases) {
            val res = matcher.match(query)
            assertNotNull("Query '$query' should not be null", res)
            assertEquals("open_app", res?.intent)
            assertEquals(expectedApp, JSONObject(res!!.argumentsJson).optString("app_name"))
        }
    }

    @Test
    fun testExtensiveNegativeCasesToAvoidFalsePositives() {
        val negativeQueries = listOf(
            "tôi thích xem ảnh",
            "tôi đang tính toán",
            "nhà tôi có cái đồng hồ gỗ rất đẹp",
            "hôm nay tôi cần ghi âm bài giảng",
            "tôi muốn cài đặt lại lịch trình làm việc",
            "bạn có biết địa chỉ này trên bản đồ không",
            "ngày mai tôi phải đi làm sớm lúc 6h",
            "con tôi rất thích xem video trên mạng"
        )
        for (query in negativeQueries) {
            val res = matcher.match(query)
            assertNull("Negative query '$query' must return null to fall back to LLM", res)
        }
    }

    @Test
    fun testStressMatchingLatencyPerformance() {
        val testQueries = listOf(
            "mở máy ảnh", "cho toi xem youtube", "tạm biệt", "bộ sưu tập",
            "đặt báo thức 6 giờ", "tôi thích xem ảnh", "vui lòng mở giúp tôi zalo",
            "chỉ đường đến Hồ Gươm", "gọi cho mẹ", "nhắn tin cho bố"
        )

        // Warmup
        for (i in 0..500) {
            matcher.match(testQueries[i % testQueries.size])
        }

        // Benchmark 10,000 iterations
        val iterations = 10_000
        val start = System.nanoTime()
        for (i in 0 until iterations) {
            matcher.match(testQueries[i % testQueries.size])
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        val avgLatencyMs = elapsedMs / iterations

        println("⚡ FastPath Matcher Latency Benchmark over $iterations queries: total = ${elapsedMs}ms, avg = ${avgLatencyMs}ms/query")
        assertTrue("Average matching latency must be < 5ms (Actual: ${avgLatencyMs}ms)", avgLatencyMs < 5.0)
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
    fun testOpenMapDestination() {
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

        val resultVideo = matcher.match("tìm video")
        assertNotNull(resultVideo)
        assertEquals("clarify", resultVideo?.intent)
    }

    @Test
    fun testSearchVideoFastPath() {
        val result = matcher.match("tìm video hài hoài linh")
        assertNotNull(result)
        assertEquals("search_video", result?.intent)
        assertEquals(NluIntent.SEARCH_VIDEO, result?.intentEnum)
        assertEquals("hài hoài linh", JSONObject(result!!.argumentsJson).optString("query"))

        val result2 = matcher.match("mở youtube tìm nhạc sống thôn quê")
        assertNotNull(result2)
        assertEquals("search_video", result2?.intent)
        assertEquals("nhạc sống thôn quê", JSONObject(result2!!.argumentsJson).optString("query"))
    }

    @Test
    fun testPlayMusicFastPath() {
        val result = matcher.match("bật bài hát Diễm Xưa")
        assertNotNull(result)
        assertEquals("play_music", result?.intent)
        assertEquals(NluIntent.PLAY_MUSIC, result?.intentEnum)
        assertEquals("diễm xưa", JSONObject(result!!.argumentsJson).optString("song_name"))

        val resultGenre = matcher.match("mở nhạc bolero")
        assertNotNull(resultGenre)
        assertEquals("play_music", resultGenre?.intent)
        assertEquals(NluIntent.PLAY_MUSIC, resultGenre?.intentEnum)
        assertEquals("nhạc bolero", JSONObject(resultGenre!!.argumentsJson).optString("genre"))

        val resultGeneric = matcher.match("bật nhạc lên")
        assertNotNull(resultGeneric)
        assertEquals("play_music", resultGeneric?.intent)
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
