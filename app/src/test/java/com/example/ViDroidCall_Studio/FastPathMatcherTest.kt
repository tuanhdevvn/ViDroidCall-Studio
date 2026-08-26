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

    // ==========================================
    // 1. VIETNAMESE NUMBER PARSER TESTS (18)
    // ==========================================
    @Test fun testNumber_mot() = assertEquals(1, VietnameseNumberParser.parse("một"))
    @Test fun testNumber_hai() = assertEquals(2, VietnameseNumberParser.parse("hai"))
    @Test fun testNumber_ba() = assertEquals(3, VietnameseNumberParser.parse("ba"))
    @Test fun testNumber_bon() = assertEquals(4, VietnameseNumberParser.parse("bốn"))
    @Test fun testNumber_tu() = assertEquals(4, VietnameseNumberParser.parse("tư"))
    @Test fun testNumber_nam() = assertEquals(5, VietnameseNumberParser.parse("năm"))
    @Test fun testNumber_lam() = assertEquals(5, VietnameseNumberParser.parse("lăm"))
    @Test fun testNumber_sau() = assertEquals(6, VietnameseNumberParser.parse("sáu"))
    @Test fun testNumber_bay() = assertEquals(7, VietnameseNumberParser.parse("bảy"))
    @Test fun testNumber_tam() = assertEquals(8, VietnameseNumberParser.parse("tám"))
    @Test fun testNumber_chin() = assertEquals(9, VietnameseNumberParser.parse("chín"))
    @Test fun testNumber_muoi() = assertEquals(10, VietnameseNumberParser.parse("mười"))
    @Test fun testNumber_muoiMot() = assertEquals(11, VietnameseNumberParser.parse("mười một"))
    @Test fun testNumber_muoiLam() = assertEquals(15, VietnameseNumberParser.parse("mười lăm"))
    @Test fun testNumber_haiMuoi() = assertEquals(20, VietnameseNumberParser.parse("hai mươi"))
    @Test fun testNumber_haiMuoiMot() = assertEquals(21, VietnameseNumberParser.parse("hai mươi mốt"))
    @Test fun testNumber_haiMuoiTu() = assertEquals(24, VietnameseNumberParser.parse("hai mươi tư"))
    @Test fun testNumber_haiMuoiLam() = assertEquals(25, VietnameseNumberParser.parse("hai mươi lăm"))

    // ==========================================
    // 2. ALARM HOUR WORDS TESTS (10)
    // ==========================================
    @Test
    fun testAlarm_sauGio() {
        val r = matcher.match("báo thức sáu giờ")
        assertNotNull(r); assertEquals(6, JSONObject(r!!.argumentsJson).optInt("hour"))
    }
    @Test
    fun testAlarm_sauGioNoAccents() {
        val r = matcher.match("báo thức sau gio")
        assertNotNull(r); assertEquals(6, JSONObject(r!!.argumentsJson).optInt("hour"))
    }
    @Test
    fun testAlarm_bayGioRuoi() {
        val r = matcher.match("báo thức bảy giờ rưỡi")
        assertNotNull(r); assertEquals(7, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(30, JSONObject(r.argumentsJson).optInt("minute"))
    }
    @Test
    fun testAlarm_bayGioRuoiNoAccents() {
        val r = matcher.match("báo thức bay gio ruoi")
        assertNotNull(r); assertEquals(7, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(30, JSONObject(r.argumentsJson).optInt("minute"))
    }
    @Test
    fun testAlarm_bayGioRuoiToi() {
        val r = matcher.match("báo thức 7 giờ rưỡi tối")
        assertNotNull(r); assertEquals(19, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(30, JSONObject(r.argumentsJson).optInt("minute"))
    }
    @Test
    fun testAlarm_haiGioRuoiChieu() {
        val r = matcher.match("báo thức 2 giờ rưỡi chiều")
        assertNotNull(r); assertEquals(14, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(30, JSONObject(r.argumentsJson).optInt("minute"))
    }
    @Test
    fun testAlarm_muoiGioSang() {
        val r = matcher.match("báo thức mười giờ sáng")
        assertNotNull(r); assertEquals(10, JSONObject(r!!.argumentsJson).optInt("hour"))
    }
    @Test
    fun testAlarm_muoiHaiGioTrua() {
        val r = matcher.match("báo thức mười hai giờ trưa")
        assertNotNull(r); assertEquals(12, JSONObject(r!!.argumentsJson).optInt("hour"))
    }
    @Test
    fun testAlarm_muoiHaiGioDem() {
        val r = matcher.match("báo thức mười hai giờ đêm")
        assertNotNull(r); assertEquals(0, JSONObject(r!!.argumentsJson).optInt("hour"))
    }
    @Test
    fun testAlarm_khongGioRuoi() {
        val r = matcher.match("báo thức không giờ rưỡi")
        assertNotNull(r); assertEquals(0, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(30, JSONObject(r.argumentsJson).optInt("minute"))
    }

    // ==========================================
    // 3. ALARM PERIODS TESTS (10)
    // ==========================================
    @Test fun testAlarm_7GioToi() { val r = matcher.match("báo thức 7 giờ tối"); assertNotNull(r); assertEquals(19, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testAlarm_2GioChieu() { val r = matcher.match("báo thức 2 giờ chiều"); assertNotNull(r); assertEquals(14, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testAlarm_8GioSang() { val r = matcher.match("báo thức 8 giờ sáng"); assertNotNull(r); assertEquals(8, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testAlarm_11GioDem() { val r = matcher.match("báo thức 11 giờ đêm"); assertNotNull(r); assertEquals(23, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testAlarm_12GioSang() { val r = matcher.match("báo thức 12 giờ sáng"); assertNotNull(r); assertEquals(0, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testAlarm_12GioTrua() { val r = matcher.match("báo thức 12 giờ trưa"); assertNotNull(r); assertEquals(12, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testAlarm_12GioDem() { val r = matcher.match("báo thức 12 giờ đêm"); assertNotNull(r); assertEquals(0, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testAlarm_3GioChieu() { val r = matcher.match("báo thức 3 giờ chiều"); assertNotNull(r); assertEquals(15, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testAlarm_9GioToi() { val r = matcher.match("báo thức 9 giờ tối"); assertNotNull(r); assertEquals(21, JSONObject(r!!.argumentsJson).optInt("hour")) }
    @Test fun testAlarm_5GioSang() { val r = matcher.match("báo thức 5 giờ sáng"); assertNotNull(r); assertEquals(5, JSONObject(r!!.argumentsJson).optInt("hour")) }

    // ==========================================
    // 4. GIỜ KÉM & THIẾU TESTS (17)
    // ==========================================
    @Test fun testAlarm_2GioKem10() { val r = matcher.match("báo thức 2 giờ kém 10"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testAlarm_2GioKem10Phut() { val r = matcher.match("báo thức 2 giờ kém 10 phút"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testAlarm_haiGioKemMuoi() { val r = matcher.match("báo thức hai giờ kém mười"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testAlarm_haiGioKemMuoiPhut() { val r = matcher.match("báo thức hai giờ kém mười phút"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testAlarm_7GioKem15() { val r = matcher.match("báo thức 7 giờ kém 15"); assertNotNull(r); assertEquals(6, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(45, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testAlarm_bayGioKemMuoiLam() { val r = matcher.match("báo thức bảy giờ kém mười lăm"); assertNotNull(r); assertEquals(6, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(45, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testAlarm_8GioKem5() { val r = matcher.match("báo thức 8 giờ kém 5"); assertNotNull(r); assertEquals(7, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(55, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testAlarm_tamGioKemNam() { val r = matcher.match("báo thức tám giờ kém năm"); assertNotNull(r); assertEquals(7, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(55, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testAlarm_2GioKem10Toi() { val r = matcher.match("báo thức 2 giờ kém 10 tối"); assertNotNull(r); assertEquals(19, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testAlarm_7GioKem15Toi() { val r = matcher.match("báo thức 7 giờ kém 15 tối"); assertNotNull(r); assertEquals(18, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(45, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testAlarm_8GioKem5Toi() { val r = matcher.match("báo thức 8 giờ kém 5 tối"); assertNotNull(r); assertEquals(19, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(55, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testAlarm_11GioKem10Dem() { val r = matcher.match("báo thức 11 giờ kém 10 đêm"); assertNotNull(r); assertEquals(22, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testAlarm_2GioThieu10() { val r = matcher.match("báo thức 2 giờ thiếu 10"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testAlarm_12GioKem10Trua() { val r = matcher.match("báo thức 12 giờ kém 10 trưa"); assertNotNull(r); assertEquals(11, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testAlarm_1GioKem10Trua() { val r = matcher.match("báo thức 1 giờ kém 10 trưa"); assertNotNull(r); assertEquals(12, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testAlarm_12GioKem10Dem() { val r = matcher.match("báo thức 12 giờ kém 10 đêm"); assertNotNull(r); assertEquals(23, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }
    @Test fun testAlarm_1GioKem10Dem() { val r = matcher.match("báo thức 1 giờ kém 10 đêm"); assertNotNull(r); assertEquals(0, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(r.argumentsJson).optInt("minute")) }

    // ==========================================
    // 5. TIMER DURATIONS TESTS (10)
    // ==========================================
    @Test fun testTimer_15Phut() { val r = matcher.match("hẹn giờ 15 phút"); assertNotNull(r); assertEquals(15, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testTimer_muoiLamPhut() { val r = matcher.match("hẹn giờ mười lăm phút"); assertNotNull(r); assertEquals(15, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testTimer_muoiLamPhutNoAccents() { val r = matcher.match("hẹn giờ muoi lam phut"); assertNotNull(r); assertEquals(15, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testTimer_20Giay() { val r = matcher.match("hẹn giờ 20 giây"); assertNotNull(r); assertEquals(20, JSONObject(r!!.argumentsJson).optInt("duration")); assertEquals("seconds", JSONObject(r.argumentsJson).optString("unit")) }
    @Test fun testTimer_haiMuoiGiay() { val r = matcher.match("hẹn giờ hai mươi giây"); assertNotNull(r); assertEquals(20, JSONObject(r!!.argumentsJson).optInt("duration")); assertEquals("seconds", JSONObject(r.argumentsJson).optString("unit")) }
    @Test fun testTimer_nuaTieng() { val r = matcher.match("hẹn giờ nua tieng"); assertNotNull(r); assertEquals(30, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testTimer_nuaGio() { val r = matcher.match("hẹn giờ nua gio"); assertNotNull(r); assertEquals(30, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testTimer_1Gio() { val r = matcher.match("hẹn giờ 1 giờ"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("duration")); assertEquals("hours", JSONObject(r.argumentsJson).optString("unit")) }
    @Test fun testTimer_motTieng() { val r = matcher.match("hẹn giờ một tiếng"); assertNotNull(r); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testTimer_30Phut() { val r = matcher.match("hẹn giờ 30 phút"); assertNotNull(r); assertEquals(30, JSONObject(r!!.argumentsJson).optInt("duration")) }

    // ==========================================
    // 6. RELATIVE TIME TESTS (10)
    // ==========================================
    private val fixedTP = TimeProvider.createFixed(hour = 10, minute = 20, second = 0)
    private val relMatcher = FastPathMatcher(context = null, timeProvider = fixedTP)

    @Test fun testRelativeTimer_sau5Phut() { val r = relMatcher.match("sau 5 phút"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(5, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testRelativeTimer_namPhutNua() { val r = relMatcher.match("năm phút nữa"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(5, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testRelativeTimer_sau2Tieng() { val r = relMatcher.match("sau 2 tiếng"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(2, JSONObject(r!!.argumentsJson).optInt("duration")); assertEquals("hours", JSONObject(r.argumentsJson).optString("unit")) }
    @Test fun testRelativeTimer_haiTiengNua() { val r = relMatcher.match("hai tiếng nữa"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(2, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testRelativeTimer_sau20Giay() { val r = relMatcher.match("sau 20 giây"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(20, JSONObject(r!!.argumentsJson).optInt("duration")); assertEquals("seconds", JSONObject(r.argumentsJson).optString("unit")) }
    @Test fun testRelativeTimer_haiMuoiGiayNua() { val r = relMatcher.match("hai mươi giây nữa"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(20, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testRelativeTimer_nuaTiengNua() { val r = relMatcher.match("nửa tiếng nữa"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(30, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testRelativeTimer_sauNuaGio() { val r = relMatcher.match("sau nửa giờ"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(30, JSONObject(r!!.argumentsJson).optInt("duration")) }
    @Test fun testRelativeAlarm_crossDayRollover() {
        val midnightTP = TimeProvider.createFixed(hour = 23, minute = 50, second = 0)
        val midnightM = FastPathMatcher(context = null, timeProvider = midnightTP)
        val r = midnightM.match("báo thức sau 20 phút")
        assertNotNull(r); assertEquals("set_alarm", r?.intent); assertEquals(0, JSONObject(r!!.argumentsJson).optInt("hour")); assertEquals(10, JSONObject(r.argumentsJson).optInt("minute"))
    }
    @Test fun testRelativeTimer_1GioNua() { val r = relMatcher.match("1 giờ nữa"); assertNotNull(r); assertEquals("set_timer", r?.intent); assertEquals(1, JSONObject(r!!.argumentsJson).optInt("duration")) }

    // ==========================================
    // 7. APP ALIASES - SYSTEM APPS (10)
    // ==========================================
    @Test fun testApp_camera() { val r = matcher.match("mở camera"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("camera", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_mayAnh() { val r = matcher.match("mở máy ảnh"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("camera", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_gallery() { val r = matcher.match("mở gallery"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("gallery", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_boSuuTap() { val r = matcher.match("bo suu tap"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("gallery", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_contacts() { val r = matcher.match("danh ba"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("contacts", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_danhBa() { val r = matcher.match("mở danh bạ"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("contacts", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_calculator() { val r = matcher.match("mở máy tính"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("calculator", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_mayTinh() { val r = matcher.match("may tinh"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("calculator", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_playstore() { val r = matcher.match("ch play"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("playstore", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_chPlay() { val r = matcher.match("ch play"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("playstore", JSONObject(r!!.argumentsJson).optString("app_name")) }

    // ==========================================
    // 8. APP ALIASES - MEDIA & SOCIAL APPS (10)
    // ==========================================
    @Test fun testApp_youtube() { val r = matcher.match("mở youtube"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("youtube", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_duTup() { val r = matcher.match("du tup"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("youtube", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_yutube() { val r = matcher.match("yutube"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("youtube", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_facebook() { val r = matcher.match("mở facebook"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("facebook", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_phayBuc() { val r = matcher.match("phay buc"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("facebook", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_tiktok() { val r = matcher.match("mở tiktok"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("tiktok", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_topTop() { val r = matcher.match("top top"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("tiktok", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_tikTok() { val r = matcher.match("tik tok"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("tiktok", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_googleMaps() { val r = matcher.match("mở google maps"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("google_maps", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_gucGoMap() { val r = matcher.match("guc go map"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("google_maps", JSONObject(r!!.argumentsJson).optString("app_name")) }

    // ==========================================
    // 9. APP ALIASES - COMMERCIAL & TRANSPORT (6)
    // ==========================================
    @Test fun testApp_shopee() { val r = matcher.match("shopee"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("shopee", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_lazada() { val r = matcher.match("lazada"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("lazada", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_grab() { val r = matcher.match("grab"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("grab", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_be() { val r = matcher.match("mở app be"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("be", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_zalo() { val r = matcher.match("mở zalo"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("zalo", JSONObject(r!!.argumentsJson).optString("app_name")) }
    @Test fun testApp_chrome() { val r = matcher.match("mở chrome"); assertNotNull(r); assertEquals("open_app", r?.intent); assertEquals("chrome", JSONObject(r!!.argumentsJson).optString("app_name")) }

    // ==========================================
    // 10. NEGATIVE & FALSE POSITIVE TESTS (10)
    // ==========================================
    @Test fun testNegative_toiNoiChuyenVeYoutube() { assertNull(matcher.match("tôi nói chuyện về youtube")) }
    @Test fun testNegative_toiThichXemVideoNgan() { assertNull(matcher.match("Tôi thích xem video ngắn")) }
    @Test fun testNegative_toiCanSoDienThoai() { assertNull(matcher.match("Tôi cần số điện thoại của anh")) }
    @Test fun testNegative_toiDangTinhToan() { assertNull(matcher.match("tôi đang tính toán")) }
    @Test fun testNegative_diffGioThuongVaGioKem() {
        val rThuong = matcher.match("báo thức 2 giờ 10")
        assertNotNull(rThuong); assertEquals(2, JSONObject(rThuong!!.argumentsJson).optInt("hour")); assertEquals(10, JSONObject(rThuong.argumentsJson).optInt("minute"))
        val rKem = matcher.match("báo thức 2 giờ kém 10")
        assertNotNull(rKem); assertEquals(1, JSONObject(rKem!!.argumentsJson).optInt("hour")); assertEquals(50, JSONObject(rKem.argumentsJson).optInt("minute"))
    }
    @Test fun testNegative_emptyQuery() { assertNull(matcher.match("")) }
    @Test fun testNegative_whitespaceOnly() { assertNull(matcher.match("   ")) }
    @Test fun testNegative_unknownApp() { assertNull(matcher.match("tôi muốn mở ứng dụng lạ không có thật abc xyz")) }
    @Test fun testNegative_hourOutOfRange() { assertNull(matcher.match("báo thức 25 giờ")) }
    @Test fun testNegative_minuteOutOfRange() { assertNull(matcher.match("báo thức 2 giờ 70 phút")) }

    // ==========================================
    // 11. SYSTEM & BENCHMARK TESTS (5)
    // ==========================================
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
        assertNotNull(r113); assertEquals("call_contact", r113?.intent); assertEquals("113", JSONObject(r113!!.argumentsJson).optString("contact"))
        val r114 = matcher.match("gọi cứu hỏa")
        assertNotNull(r114); assertEquals("call_contact", r114?.intent); assertEquals("114", JSONObject(r114!!.argumentsJson).optString("contact"))
        val r115 = matcher.match("gọi cấp cứu")
        assertNotNull(r115); assertEquals("call_contact", r115?.intent); assertEquals("115", JSONObject(r115!!.argumentsJson).optString("contact"))
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
    fun testStressBenchmark10000Queries() {
        val queries = listOf(
            "Xin chào", "Tạm biệt", "gọi 113", "mở youtube", "du tup", "phay buc", "top top", "guc go map",
            "báo thức bảy giờ rưỡi tối", "báo thức 8 giờ kém 15", "hẹn giờ mười lăm phút", "hẹn giờ nửa tiếng",
            "mở shopee", "mở máy tính", "bộ sưu tập", "sau 5 phút", "5 phút nữa", "sau 1 giờ", "sau nửa tiếng"
        )

        for (i in 0..500) { matcher.match(queries[i % queries.size]) }

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
}
