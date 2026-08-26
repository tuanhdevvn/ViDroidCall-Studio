package com.example.ViDroidCall_Studio.data.nlu

import android.content.Context
import android.util.Log
import com.example.ViDroidCall_Studio.data.model.NluResult
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.regex.Pattern

/**
 * Bộ tiền xử lý và khớp quy tắc nhanh (Fast-Path Matcher)
 * Giúp nhận diện và phản hồi tức thì (< 5ms) cho các câu lệnh ngắn gọn,
 * hỗ trợ khẩu ngữ toàn diện 3 miền (Bắc - Trung - Nam), từ ngữ truyền thống (tờ mờ sáng, xế chiều, chạng vạng, khuya),
 * giờ trưa (11h-15h trưa), giờ chiều (1h-6h chiều), người cao tuổi, giờ kém/thiếu, từ đệm, biến thể STT thực tế & TimeProvider injection.
 */
class FastPathMatcher(
    private val context: Context? = null,
    private val timeProvider: TimeProvider = TimeProvider.createDefault()
) {

    private data class RuleEntry(
        val patterns: List<String>,
        val intent: String,
        val arguments: JSONObject,
        val riskLevel: String,
        val status: String,
        val requiresConfirmation: Boolean
    )

    private val exactRules = mutableListOf<RuleEntry>()
    private val lookupMap = HashMap<String, RuleEntry>()

    init {
        loadRulesFromAssets()
        rebuildLookupIndex()
    }

    private fun rebuildLookupIndex() {
        lookupMap.clear()
        for (rule in exactRules) {
            for (pattern in rule.patterns) {
                val normalizedPattern = normalizeText(pattern)
                if (normalizedPattern.isNotEmpty()) {
                    lookupMap[normalizedPattern] = rule
                }
            }
        }
    }

    private fun loadRulesFromAssets() {
        try {
            if (context != null) {
                val jsonString = context.assets.open("fast_path_rules.json").bufferedReader().use { it.readText() }
                parseRulesJson(jsonString)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Không thể đọc fast_path_rules.json từ Assets: ${e.message}, sử dụng bộ quy tắc mặc định.")
        }
        if (exactRules.isEmpty()) {
            loadDefaultRules()
        }
    }

    private fun parseRulesJson(jsonString: String) {
        val root = JSONObject(jsonString)
        val rulesArray = root.optJSONArray("exact_rules") ?: JSONArray()
        for (i in 0 until rulesArray.length()) {
            val ruleObj = rulesArray.getJSONObject(i)
            val patternsJson = ruleObj.getJSONArray("patterns")
            val patterns = mutableListOf<String>()
            for (j in 0 until patternsJson.length()) {
                patterns.add(patternsJson.getString(j))
            }
            exactRules.add(
                RuleEntry(
                    patterns = patterns,
                    intent = ruleObj.getString("intent"),
                    arguments = ruleObj.optJSONObject("arguments") ?: JSONObject(),
                    riskLevel = ruleObj.optString("risk_level", "low"),
                    status = ruleObj.optString("status", "success"),
                    requiresConfirmation = ruleObj.optBoolean("requires_confirmation", false)
                )
            )
        }
    }

    private fun loadDefaultRules() {
        addRule(patterns = listOf("xin chào", "chào em", "chào bạn", "chào emma", "hello", "hi", "alo", "hey emma", "chào em gái", "có ai ở đó không", "chào", "hello emma"), intent = "greeting", args = JSONObject(), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("tạm biệt", "chào tạm biệt", "bye", "bye bye", "hẹn gặp lại", "tắt đi", "kết thúc", "dừng lại", "đóng lại", "thoát ra"), intent = "goodbye", args = JSONObject(), risk = "low", status = "success", confirm = false)

        addRule(patterns = listOf("gọi 113", "gọi cảnh sát", "gọi công an", "báo công an"), intent = "call_contact", args = JSONObject().put("contact", "113"), risk = "high", status = "success", confirm = true)
        addRule(patterns = listOf("gọi 114", "gọi cứu hỏa", "báo cháy"), intent = "call_contact", args = JSONObject().put("contact", "114"), risk = "high", status = "success", confirm = true)
        addRule(patterns = listOf("gọi 115", "gọi cấp cứu"), intent = "call_contact", args = JSONObject().put("contact", "115"), risk = "high", status = "success", confirm = true)

        addRule(patterns = listOf("chup anh", "chup hinh", "may anh", "mo may anh", "mo camera", "camera", "ca me ra", "chụp ảnh", "chụp hình", "máy ảnh", "mở máy ảnh"), intent = "open_app", args = JSONObject().put("app_name", "camera"), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("xem anh", "xem hinh", "bo suu tap", "album anh", "mo anh", "thu vien anh", "thu vien", "gallery", "xem ảnh", "xem hình", "bộ sưu tập", "album ảnh", "thư viện ảnh"), intent = "open_app", args = JSONObject().put("app_name", "gallery"), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("may tinh", "tinh tien", "tinh toan", "ban tinh", "mo may tinh", "calculator", "máy tính", "tính tiền", "tính toán", "bàn tính"), intent = "open_app", args = JSONObject().put("app_name", "calculator"), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("danh ba", "so dien thoai", "danh sach goi", "mo danh ba", "danh bạ", "số điện thoại", "danh sách gọi"), intent = "open_app", args = JSONObject().put("app_name", "contacts"), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("xem gio", "dong ho", "dong ho bao thuc", "mo dong ho", "xem giờ", "đồng hồ", "đồng hồ báo thức"), intent = "open_app", args = JSONObject().put("app_name", "clock"), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("cai dat", "thiet lap", "cai dat may", "mo cai dat", "cai dat dien thoai", "settings", "cài đặt", "thiết lập", "cài đặt máy", "cài đặt điện thoại"), intent = "open_app", args = JSONObject().put("app_name", "settings"), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("ghi am", "may ghi am", "thu am", "mo ghi am", "ghi âm", "máy ghi âm", "thu âm"), intent = "open_app", args = JSONObject().put("app_name", "recorder"), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("quan ly tep", "file cua ban", "tep tin", "mo file", "quan ly file", "file", "quản lý tệp", "file của bạn", "tệp tin", "mở file", "quản lý file"), intent = "open_app", args = JSONObject().put("app_name", "files"), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("tai ung dung", "cai tro choi", "ch play", "cua hang", "cua hang ung dung", "google play", "play store", "tải ứng dụng", "cài trò chơi", "CH Play", "cửa hàng", "cửa hàng ứng dụng"), intent = "open_app", args = JSONObject().put("app_name", "playstore"), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("doc bao", "xem tin tuc", "len mang", "guc go", "google", "mo trinh duyet", "trinh duyet", "chrome", "đọc báo", "xem tin tức", "lên mạng", "mở trình duyệt", "trình duyệt"), intent = "open_app", args = JSONObject().put("app_name", "chrome"), risk = "low", status = "success", confirm = false)

        addRule(patterns = listOf("diu tup", "du tup", "dut tup", "yutube", "youtube", "xem ca nhac", "xem video", "mo youtube", "diu túp", "du túp", "đút túp", "xem ca nhạc"), intent = "open_app", args = JSONObject().put("app_name", "youtube"), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("da lo", "za lo", "da ro", "zalo", "nhan da lo", "goi da lo", "mo zalo", "da-lô", "za-lô", "nhắn da lô"), intent = "open_app", args = JSONObject().put("app_name", "zalo"), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("phay", "phay buc", "phay bup", "fb", "xem phay", "facebook", "mo facebook", "phây", "phây búc", "phây búp"), intent = "open_app", args = JSONObject().put("app_name", "facebook"), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("top top", "toc toc", "tik tok", "tiktok", "xem video ngan", "mo tiktok", "tóp tóp", "tóc tóc", "xem video ngắn"), intent = "open_app", args = JSONObject().put("app_name", "tiktok"), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("ban do", "chi duong", "guc go map", "tim duong", "google map", "google maps", "mo ban do", "bản đồ", "chỉ đường", "tìm đường"), intent = "open_app", args = JSONObject().put("app_name", "google_maps"), risk = "low", status = "success", confirm = false)

        addRule(patterns = listOf("shopee", "shop pi", "shoppe", "mo shopee"), intent = "open_app", args = JSONObject().put("app_name", "shopee"), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("lazada", "la da da", "mo lazada"), intent = "open_app", args = JSONObject().put("app_name", "lazada"), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("grab", "grab bike", "mo grab"), intent = "open_app", args = JSONObject().put("app_name", "grab"), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("be", "be bike", "be taxi", "mo be"), intent = "open_app", args = JSONObject().put("app_name", "be"), risk = "low", status = "success", confirm = false)

        addRule(patterns = listOf("gọi", "gọi điện", "gọi điện thoại"), intent = "clarify", args = JSONObject().put("missing", JSONArray().put("contact")), risk = "low", status = "needs_clarification", confirm = false)
        addRule(patterns = listOf("nhắn tin", "gửi tin nhắn", "soạn tin nhắn", "gửi tin"), intent = "clarify", args = JSONObject().put("missing", JSONArray().put("contact").put("message")), risk = "low", status = "needs_clarification", confirm = false)
        addRule(patterns = listOf("hẹn giờ", "đếm ngược"), intent = "clarify", args = JSONObject().put("missing", JSONArray().put("duration").put("unit")), risk = "low", status = "needs_clarification", confirm = false)
        addRule(patterns = listOf("đặt báo thức", "báo thức", "hẹn báo thức", "cài báo thức"), intent = "clarify", args = JSONObject().put("missing", JSONArray().put("hour").put("minute")), risk = "low", status = "needs_clarification", confirm = false)
        addRule(patterns = listOf("bật nhạc", "mở nhạc", "nghe nhạc", "phát nhạc", "bật nhạc lên", "mở nhạc lên"), intent = "play_music", args = JSONObject(), risk = "low", status = "success", confirm = false)
        addRule(patterns = listOf("tìm video", "mở video", "xem video", "bật video", "tìm clip", "xem clip"), intent = "clarify", args = JSONObject().put("missing", JSONArray().put("query")), risk = "low", status = "needs_clarification", confirm = false)
    }

    private fun addRule(
        patterns: List<String>,
        intent: String,
        args: JSONObject,
        risk: String,
        status: String,
        confirm: Boolean
    ) {
        exactRules.add(
            RuleEntry(
                patterns = patterns,
                intent = intent,
                arguments = args,
                riskLevel = risk,
                status = status,
                requiresConfirmation = confirm
            )
        )
    }

    fun match(query: String): NluResult? {
        return try {
            val rawClean = normalizePreservingAccents(query)
            val unaccented = normalizeText(query)
            if (unaccented.isEmpty()) return null

            // 1. Khớp O(1) Exact Match từ bảng index
            val directMatch = lookupMap[unaccented]
            if (directMatch != null) {
                return buildNluResult(
                    intent = directMatch.intent,
                    arguments = directMatch.arguments,
                    riskLevel = directMatch.riskLevel,
                    status = directMatch.status,
                    requiresConfirmation = directMatch.requiresConfirmation
                )
            }

            // 2. Kiểm tra các tiền tố câu lệnh mở app -> Tách tiền tố và tra cứu lookupMap
            for (prefix in COMMAND_PREFIXES) {
                if (unaccented.startsWith(prefix)) {
                    val remainder = unaccented.substring(prefix.length).trim()
                    if (remainder.isNotEmpty()) {
                        val directRemainderMatch = lookupMap[remainder]
                        if (directRemainderMatch != null) {
                            return buildNluResult(
                                intent = directRemainderMatch.intent,
                                arguments = directRemainderMatch.arguments,
                                riskLevel = directRemainderMatch.riskLevel,
                                status = directRemainderMatch.status,
                                requiresConfirmation = directRemainderMatch.requiresConfirmation
                            )
                        }

                        for (filler in FILLER_WORDS) {
                            if (remainder.startsWith(filler)) {
                                val strippedFiller = remainder.substring(filler.length).trim()
                                if (strippedFiller.isNotEmpty()) {
                                    val fillerMatch = lookupMap[strippedFiller]
                                    if (fillerMatch != null) {
                                        return buildNluResult(
                                            intent = fillerMatch.intent,
                                            arguments = fillerMatch.arguments,
                                            riskLevel = fillerMatch.riskLevel,
                                            status = fillerMatch.status,
                                            requiresConfirmation = fillerMatch.requiresConfirmation
                                        )
                                    }
                                }
                                break
                            }
                        }
                    }
                }
            }

            // 3. Khớp Regex Pattern động kết hợp VietnameseNumberParser, Time Period Normalizer & Relative Time
            matchDynamicPatterns(rawClean)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong quá trình Fast-Path match: ${e.message}", e)
            null
        }
    }

    private fun matchDynamicPatterns(text: String): NluResult? {
        val unaccentedText = normalizeText(text)

        // a. Lời chào
        if (GREETING_PATTERN.matcher(text).matches()) {
            return buildNluResult("greeting", JSONObject(), "low", "success", false)
        }

        // b. Lời tạm biệt
        if (GOODBYE_PATTERN.matcher(text).matches()) {
            return buildNluResult("goodbye", JSONObject(), "low", "success", false)
        }

        // c. Xử lý thời gian tương đối cho set_alarm
        if (unaccentedText.contains("bao thuc") || unaccentedText.startsWith("nhac toi")) {
            if (unaccentedText.contains("sau ") || unaccentedText.contains("nua") || unaccentedText.contains("bay gio") || unaccentedText.contains("hien tai")) {
                val relAlarmResult = parseRelativeAlarmCommand(text)
                if (relAlarmResult != null) return relAlarmResult
            }
        }

        // d. Xử lý thời gian tương đối cho set_timer
        val relTimerResult = parseRelativeTimerCommand(text)
        if (relTimerResult != null) return relTimerResult

        // e. Đặt báo thức (set_alarm) thời gian tuyệt đối
        if (isAlarmCommand(unaccentedText)) {
            val alarmResult = parseAlarmCommand(text)
            if (alarmResult != null) return alarmResult
        }

        // f. Hẹn giờ đếm ngược (set_timer) thời gian tuyệt đối / duration
        if (isTimerCommand(unaccentedText)) {
            val timerResult = parseTimerCommand(text)
            if (timerResult != null) return timerResult
        }

        // g. Mở ứng dụng (open_app) động
        val openAppMatcher = OPEN_APP_PATTERN.matcher(text)
        if (openAppMatcher.find()) {
            val targetApp = openAppMatcher.group(1)?.trim() ?: ""
            val unaccentedTarget = stripAccents(targetApp.lowercase())
            if (targetApp.isNotEmpty() && (COMMON_APPS.contains(unaccentedTarget) || unaccentedText.startsWith("mo ung dung") || unaccentedText.startsWith("mo app") || unaccentedText.startsWith("bat ung dung") || unaccentedText.startsWith("vao app"))) {
                val mappedAppName = APP_ALIAS_MAP[unaccentedTarget] ?: targetApp
                val args = JSONObject().apply {
                    put("app_name", mappedAppName)
                }
                return buildNluResult("open_app", args, "low", "success", false)
            }
        }

        // h. Mở bản đồ / Chỉ đường
        val mapMatcher = MAP_PATTERN.matcher(text)
        if (mapMatcher.find()) {
            val destination = mapMatcher.group(1)?.trim() ?: ""
            if (destination.isNotEmpty()) {
                val args = JSONObject().apply {
                    put("destination", destination)
                }
                return buildNluResult("open_map", args, "low", "success", false)
            }
        }

        // i. Gửi tin nhắn có nội dung
        val smsContentMatcher = SMS_CONTENT_PATTERN.matcher(text)
        if (smsContentMatcher.find()) {
            val contact = smsContentMatcher.group(1)?.trim() ?: ""
            val message = smsContentMatcher.group(2)?.trim() ?: ""
            if (contact.isNotEmpty()) {
                val args = JSONObject().apply {
                    put("contact", contact)
                    put("message", message)
                }
                return buildNluResult("send_sms", args, "medium", "success", false)
            }
        }

        // j. Soạn tin nhắn không nội dung
        val smsSimpleMatcher = SMS_SIMPLE_PATTERN.matcher(text)
        if (smsSimpleMatcher.find()) {
            val contact = smsSimpleMatcher.group(1)?.trim() ?: ""
            val unaccentedContact = stripAccents(contact.lowercase())
            if (contact.isNotEmpty() && !unaccentedContact.contains("noi dung") && !unaccentedContact.contains(" la ")) {
                val args = JSONObject().apply {
                    put("contact", contact)
                    put("message", "")
                }
                return buildNluResult("send_sms", args, "medium", "success", false)
            }
        }

        // k. Gọi điện (call_contact)
        val callMatcher = CALL_PATTERN.matcher(text)
        if (callMatcher.find()) {
            val contact = callMatcher.group(1)?.trim() ?: ""
            if (contact.isNotEmpty()) {
                val args = JSONObject().apply {
                    put("contact", contact)
                }
                return buildNluResult("call_contact", args, "high", "success", true)
            }
        }

        // l. Tìm kiếm video YouTube (search_video)
        val videoMatcher = VIDEO_PATTERN.matcher(text)
        if (videoMatcher.find()) {
            val query = videoMatcher.group(1)?.trim() ?: ""
            if (query.isNotEmpty()) {
                val args = JSONObject().apply {
                    put("query", query)
                }
                return buildNluResult("search_video", args, "low", "success", false)
            }
        }

        // m. Phát nhạc / Bài hát (play_music)
        val songMatcher = SONG_PATTERN.matcher(text)
        if (songMatcher.find()) {
            val songName = songMatcher.group(1)?.trim() ?: ""
            if (songName.isNotEmpty()) {
                val args = JSONObject().apply {
                    put("song_name", songName)
                }
                return buildNluResult("play_music", args, "low", "success", false)
            }
        }

        val genreMusicMatcher = GENRE_MUSIC_PATTERN.matcher(text)
        if (genreMusicMatcher.find()) {
            val genre = genreMusicMatcher.group(1)?.trim() ?: ""
            if (genre.isNotEmpty()) {
                val args = JSONObject().apply {
                    put("genre", "nhạc $genre")
                }
                return buildNluResult("play_music", args, "low", "success", false)
            }
        }

        return null
    }

    private fun isAlarmCommand(text: String): Boolean {
        return ALARM_PREFIXES.any { text.contains(it) }
    }

    private fun isTimerCommand(text: String): Boolean {
        return TIMER_PREFIXES.any { text.contains(it) }
    }

    private fun parseRelativeTimerCommand(text: String): NluResult? {
        val unaccented = stripAccents(text.lowercase())

        val isSau = unaccented.startsWith("sau ")
        val isNua = unaccented.endsWith("nua") || unaccented.endsWith("nua.")

        if (!isSau && !isNua) return null

        val payload = unaccented
            .replace("sau ", "")
            .replace("nua", "")
            .replace("bay gio", "")
            .replace("hien tai", "")
            .replace("luc nay", "")
            .replace("cong", "")
            .trim()

        if (payload.isEmpty()) return null

        if (payload == "nua tieng" || payload == "nua gio" || payload == "tieng" || payload == "gio") {
            val args = JSONObject().apply {
                put("duration", 30)
                put("unit", "minutes")
                put("label", "Hẹn giờ")
            }
            return buildNluResult("set_timer", args, "low", "success", false)
        }

        val unit = when {
            payload.contains("giay") || payload.endsWith("s") -> "seconds"
            payload.contains("tieng") || payload.contains("gio") || payload.endsWith("h") -> "hours"
            else -> "minutes"
        }

        val numStr = payload
            .replace(Regex("\\b(giay|phut|tieng|gio|s|p|h)\\b"), "")
            .trim()

        val duration = VietnameseNumberParser.parse(numStr)
        if (duration != null && duration > 0) {
            val args = JSONObject().apply {
                put("duration", duration)
                put("unit", unit)
                put("label", "Hẹn giờ")
            }
            return buildNluResult("set_timer", args, "low", "success", false)
        }

        return null
    }

    private fun parseRelativeAlarmCommand(text: String): NluResult? {
        val unaccented = stripAccents(text.lowercase())

        var payload = unaccented
        for (prefix in ALARM_PREFIXES) {
            payload = payload.replace(prefix, "")
        }

        payload = payload
            .replace("luc", "")
            .replace("bay gio", "")
            .replace("hien tai", "")
            .replace("luc nay", "")
            .replace("cong", "")
            .replace("sau ", "")
            .replace("nua", "")
            .trim()

        var addMins = 0

        if (payload == "nua tieng" || payload == "nua gio") {
            addMins = 30
        } else {
            val unit = when {
                payload.contains("tieng") || payload.contains("gio") || payload.endsWith("h") -> "hours"
                else -> "minutes"
            }
            val numStr = payload
                .replace(Regex("\\b(giay|phut|tieng|gio|s|p|h)\\b"), "")
                .trim()
            val parsedNum = VietnameseNumberParser.parse(numStr) ?: return null
            addMins = if (unit == "hours") parsedNum * 60 else parsedNum
        }

        if (addMins <= 0) return null

        val currentHour = timeProvider.getCurrentHour()
        val currentMinute = timeProvider.getCurrentMinute()

        val totalMins = currentHour * 60 + currentMinute + addMins

        val targetHour = (totalMins / 60) % 24
        val targetMinute = totalMins % 60

        val args = JSONObject().apply {
            put("hour", targetHour)
            put("minute", targetMinute)
            put("label", "Báo thức")
        }
        return buildNluResult("set_alarm", args, "low", "success", false)
    }

    private fun normalizeHourPeriod(
        hour: Int,
        isSang: Boolean,
        isTrua: Boolean,
        isChieu: Boolean,
        isToi: Boolean,
        isDem: Boolean
    ): Int {
        var h = hour
        if (isDem) {
            if (h == 12) {
                h = 0
            } else if (h in 9..11) {
                h += 12
            }
        } else if (isSang) {
            if (h == 12) {
                h = 0
            }
        } else if (isTrua) {
            if (h == 12 || h == 0) {
                h = 12
            } else if (h in 1..5) {
                h += 12
            }
        } else if (isChieu) {
            if (h == 12 || h == 0) {
                h = 12
            } else if (h in 1..11) {
                h += 12
            }
        } else if (isToi) {
            if (h in 6..11) {
                h += 12
            } else if (h in 1..5) {
                h += 18
            }
        }
        return h % 24
    }

    private fun parseAlarmCommand(text: String): NluResult? {
        val unaccented = stripAccents(text.lowercase())

        val isSang = unaccented.contains("sang") || unaccented.contains("som") || unaccented.contains("to mo")
        val isTrua = unaccented.contains("trua")
        val isChieu = unaccented.contains("chieu") || unaccented.contains("xe chieu")
        val isToi = unaccented.contains("toi") || unaccented.contains("chang vang") || unaccented.contains("sam toi") || unaccented.contains("chap toi")
        val isDem = unaccented.contains("dem") || unaccented.contains("khuya")

        // Xử lý kịch bản "Giờ kém" hoặc "Giờ thiếu" (Khẩu ngữ Miền Nam)
        val isKem = unaccented.contains("kem") || unaccented.contains("thieu")
        if (isKem) {
            var payloadKem = unaccented
            for (prefix in ALARM_PREFIXES) {
                payloadKem = payloadKem.replace(prefix, "")
            }

            payloadKem = payloadKem
                .replace("tam khoang", "")
                .replace("khoang", "")
                .replace(Regex("\\btam\\b(?!\\s*(?:gio|giờ|h\\b))"), "")
                .replace("vao luc", "")
                .replace("luc", "")
                .replace("sang", "")
                .replace("trua", "")
                .replace("chieu", "")
                .replace("xe chieu", "")
                .replace("toi", "")
                .replace("chang vang", "")
                .replace("sam toi", "")
                .replace("chap toi", "")
                .replace("dem", "")
                .replace("khuya", "")
                .trim()

            val kemParts = payloadKem.split(Regex("\\s+(?:kem|thieu)\\s+"))
            if (kemParts.size == 2) {
                val rawHourPart = kemParts[0].replace(Regex("\\s*(?:gio|giờ)\\s*|(?<=\\d)\\s*h\\b"), "").trim()
                val rawMinPart = kemParts[1].replace("phut", "").replace("p", "").trim()

                val parsedTargetHour = VietnameseNumberParser.parse(rawHourPart)
                val parsedKemMin = VietnameseNumberParser.parse(rawMinPart)

                if (parsedTargetHour != null && parsedKemMin != null && parsedKemMin in 1..59) {
                    var totalMins = parsedTargetHour * 60 - parsedKemMin
                    if (totalMins < 0) totalMins += 24 * 60

                    val rawHour = (totalMins / 60) % 24
                    val minute = totalMins % 60

                    val hour = normalizeHourPeriod(rawHour, isSang, isTrua, isChieu, isToi, isDem)

                    val args = JSONObject().apply {
                        put("hour", hour)
                        put("minute", minute)
                        put("label", "Báo thức")
                    }
                    return buildNluResult("set_alarm", args, "low", "success", false)
                }
            }
        }

        val isRuoi = unaccented.contains("ruoi")
        var minute = if (isRuoi) 30 else 0

        var payload = unaccented
        for (prefix in ALARM_PREFIXES) {
            payload = payload.replace(prefix, "")
        }

        payload = payload
            .replace("tam khoang", "")
            .replace("khoang", "")
            .replace(Regex("\\btam\\b(?!\\s*(?:gio|giờ|h\\b))"), "")
            .replace("vao luc", "")
            .replace("luc", "")
            .replace("sang", "")
            .replace("som", "")
            .replace("to mo", "")
            .replace("trua", "")
            .replace("chieu", "")
            .replace("xe chieu", "")
            .replace("toi", "")
            .replace("chang vang", "")
            .replace("sam toi", "")
            .replace("chap toi", "")
            .replace("dem", "")
            .replace("khuya", "")
            .replace("ruoi", "")
            .replace("dung", "")
            .replace("tron", "")
            .trim()

        var parsedHour: Int? = null

        // Format kiểu 7:30 hoặc 7h30
        val colonMatcher = Pattern.compile("(\\d{1,2})[h:](\\d{1,2})").matcher(payload)
        if (colonMatcher.find()) {
            parsedHour = colonMatcher.group(1)?.toIntOrNull()
            minute = colonMatcher.group(2)?.toIntOrNull() ?: minute
        } else {
            // Tách theo chữ "giờ", "gio" hoặc ranh giới từ "h"
            val parts = payload.split(Regex("\\s*(?:gio|giờ)\\s*|(?<=\\d)\\s*h\\b|\\bch\\b"))
            if (parts.isNotEmpty()) {
                val hourPart = parts[0].trim()
                parsedHour = VietnameseNumberParser.parse(hourPart)

                if (parts.size > 1 && !isRuoi) {
                    val minPart = parts[1].replace("phut", "").replace("p", "").trim()
                    if (minPart.isNotEmpty()) {
                        val parsedMin = VietnameseNumberParser.parse(minPart)
                        if (parsedMin != null) {
                            minute = parsedMin
                        }
                    }
                }
            } else {
                parsedHour = VietnameseNumberParser.parse(payload)
            }
        }

        if (parsedHour == null) return null

        val hour = normalizeHourPeriod(parsedHour, isSang, isTrua, isChieu, isToi, isDem)

        if (hour in 0..23 && minute in 0..59) {
            val args = JSONObject().apply {
                put("hour", hour)
                put("minute", minute)
                put("label", "Báo thức")
            }
            return buildNluResult("set_alarm", args, "low", "success", false)
        }

        return null
    }

    private fun parseTimerCommand(text: String): NluResult? {
        val unaccented = stripAccents(text.lowercase())

        // Xử lý nửa tiếng / nửa giờ
        if (unaccented.contains("nua tieng") || unaccented.contains("nua gio")) {
            val args = JSONObject().apply {
                put("duration", 30)
                put("unit", "minutes")
                put("label", "Hẹn giờ")
            }
            return buildNluResult("set_timer", args, "low", "success", false)
        }

        var payload = unaccented
        for (prefix in TIMER_PREFIXES) {
            payload = payload.replace(prefix, "")
        }

        payload = payload.trim()

        val unit = when {
            payload.contains("giay") || payload.endsWith("s") -> "seconds"
            payload.contains("tieng") || payload.contains("gio") || payload.endsWith("h") -> "hours"
            else -> "minutes"
        }

        val numStr = payload
            .replace(Regex("\\b(giay|phut|tieng|gio|s|p|h)\\b"), "")
            .trim()

        val duration = VietnameseNumberParser.parse(numStr)
        if (duration != null && duration > 0) {
            val args = JSONObject().apply {
                put("duration", duration)
                put("unit", unit)
                put("label", "Hẹn giờ")
            }
            return buildNluResult("set_timer", args, "low", "success", false)
        }

        return null
    }

    private fun buildNluResult(
        intent: String,
        arguments: JSONObject,
        riskLevel: String,
        status: String,
        requiresConfirmation: Boolean
    ): NluResult {
        val root = JSONObject().apply {
            put("intent", intent)
            put("arguments", arguments)
            put("risk_level", riskLevel)
            put("status", status)
            put("requires_confirmation", requiresConfirmation)
        }
        val rawJson = root.toString(2)
        return NluResult(
            rawJson = rawJson,
            intent = intent,
            status = status,
            riskLevel = riskLevel,
            requiresConfirmation = requiresConfirmation,
            argumentsJson = arguments.toString(2),
            isParsedSuccessfully = true,
            isFastPath = true
        )
    }

    private fun stripAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return DIACRITICS_REGEX.matcher(normalized).replaceAll("")
            .replace('đ', 'd')
            .replace('Đ', 'd')
    }

    private fun normalizePreservingAccents(input: String): String {
        if (input.isBlank()) return ""
        val trimmedLower = input.trim().lowercase()
        val noPunctuation = PUNCTUATION_REGEX.matcher(trimmedLower).replaceAll(" ")
        return MULTIPLE_SPACES_REGEX.matcher(noPunctuation).replaceAll(" ").trim()
    }

    private fun normalizeText(input: String): String {
        if (input.isBlank()) return ""
        val trimmedLower = input.trim().lowercase()
        val unaccented = stripAccents(trimmedLower)
        val noPunctuation = PUNCTUATION_REGEX.matcher(unaccented).replaceAll(" ")
        return MULTIPLE_SPACES_REGEX.matcher(noPunctuation).replaceAll(" ").trim()
    }

    companion object {
        private const val TAG = "FastPathMatcher"

        private val DIACRITICS_REGEX = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        private val PUNCTUATION_REGEX = Pattern.compile("[.,?!;:'\"\\-_]")
        private val MULTIPLE_SPACES_REGEX = Pattern.compile("\\s+")

        private val ALARM_PREFIXES = listOf(
            "dat chuong bao thuc",
            "chuong bao thuc",
            "nhac giup toi",
            "dat bao thuc",
            "hen bao thuc",
            "cai bao thuc",
            "bat bao thuc",
            "mo bao thuc",
            "nhac toi",
            "bao thuc",
            "bao toi",
            "nhac"
        )

        private val TIMER_PREFIXES = listOf(
            "dong ho dem nguoc",
            "bat dem nguoc",
            "mo dem nguoc",
            "dat hen gio",
            "bat hen gio",
            "mo hen gio",
            "hen gio",
            "dem nguoc"
        )

        private val COMMAND_PREFIXES = listOf(
            "vui long mo giup toi",
            "cho toi xem giup toi",
            "cho toi mo giup toi",
            "cho toi xem",
            "cho toi mo",
            "mo giup toi",
            "vui long mo",
            "vao giup toi",
            "bat giup toi",
            "hay mo",
            "cho toi",
            "vui long",
            "giup toi",
            "mo app",
            "mo ung dung",
            "bat app",
            "bat ung dung",
            "vao app",
            "vao ung dung",
            "mo",
            "bat",
            "vao"
        )

        private val FILLER_WORDS = listOf("cai ", "app ", "ung dung ")

        private val COMMON_APPS = listOf(
            "youtube", "zalo", "facebook", "tiktok", "chrome", "camera", "may anh", "cai dat", "messenger", "ban do",
            "shopee", "shop pi", "shoppe", "lazada", "la da da", "grab", "be", "momo", "spotify"
        )

        private val APP_ALIAS_MAP = mapOf(
            "du tup" to "youtube",
            "du tup" to "youtube",
            "diu tup" to "youtube",
            "yutube" to "youtube",
            "iu tup" to "youtube",
            "utube" to "youtube",
            "phay" to "facebook",
            "phay buc" to "facebook",
            "phay bup" to "facebook",
            "phe buc" to "facebook",
            "fb" to "facebook",
            "top top" to "tiktok",
            "toc toc" to "tiktok",
            "tik tok" to "tiktok",
            "tich tac" to "tiktok",
            "guc go map" to "google_maps",
            "ban do" to "google_maps",
            "shop pi" to "shopee",
            "shoppe" to "shopee",
            "la da da" to "lazada",
            "grab bike" to "grab",
            "be bike" to "be",
            "be taxi" to "be",
            "may anh" to "camera",
            "bo suu tap" to "gallery",
            "album anh" to "gallery",
            "may tinh" to "calculator",
            "danh ba" to "contacts",
            "ch play" to "playstore"
        )

        private val GREETING_PATTERN = Pattern.compile("^(?:xin\\s+)?chào(?:\\s+em|\\s+bạn|\\s+emma|\\s+trợ\\s+lý)?$|^(?:hello|hi|hey)(?:\\s+emma)?$", Pattern.CASE_INSENSITIVE)
        private val GOODBYE_PATTERN = Pattern.compile("^(?:tạm\\s+biệt|tam\\s+biet|bye(?:\\s+bye)?|hẹn\\s+gặp\\s+lại|hen\\s+gap\\s+lai|chào\\s+nhé|chao\\s+nhe|nghỉ\\s+thôi|nghi\\s+thoi)$", Pattern.CASE_INSENSITIVE)
        private val MAP_PATTERN = Pattern.compile("^(?:chỉ\\s+đường|chi\\s+duong|tìm\\s+đường|tim\\s+duong|đường\\s+đi|duong\\s+di|dẫn\\s+đường|dan\\s+duong)(?:\\s+(?:đến|den|tới|toi|về|ve|qua))?\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        private val OPEN_APP_PATTERN = Pattern.compile("^(?:mở|mo|bật|bat|vào|vao)(?:\\s+(?:ứng\\s+dụng|ung\\s+dung|app))?\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        private val SMS_CONTENT_PATTERN = Pattern.compile("^(?:nhắn\\s+tin|nhan\\s+tin|gửi\\s+tin\\s+nhắn|gui\\s+tin\\s+nhan|nhắn|nhan)(?:\\s+cho)?\\s+(.+?)(?:\\s+(?:nội\\s+dung|noi\\s+dung|là|la|với\\s+nội\\s+dung|voi\\s+noi\\s+dung))\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        private val SMS_SIMPLE_PATTERN = Pattern.compile("^(?:nhắn\\s+tin|nhan\\s+tin|gửi\\s+tin\\s+nhắn|gui\\s+tin\\s+nhan|nhắn|nhan)(?:\\s+cho)?\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        private val CALL_PATTERN = Pattern.compile("^(?:gọi\\s+điện(?:\\s+(?:cho|đến))?|goi\\s+dien(?:\\s+(?:cho|den))?|gọi(?:\\s+(?:cho|đến))?|goi(?:\\s+(?:cho|den))?|alo\\s+cho)\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        private val VIDEO_PATTERN = Pattern.compile("^(?:mở\\s+youtube\\s+tìm|mo\\s+youtube\\s+tim|tìm\\s+video|tim\\s+video|xem\\s+video|bật\\s+video|bat\\s+video|tìm\\s+clip|tim\\s+clip)\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        private val SONG_PATTERN = Pattern.compile("^(?:mở|mo|bật|bat|phát|phat|nghe)(?:\\s+bài\\s+hát|\\s+bai\\s+hat|\\s+bài|\\s+bai|\\s+ca\\s+khúc|\\s+ca\\s+khuc)\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        private val GENRE_MUSIC_PATTERN = Pattern.compile("^(?:mở|mo|bật|bat|phát|phat|nghe)\\s+nhạc\\s+(.+)$", Pattern.CASE_INSENSITIVE)
    }
}
