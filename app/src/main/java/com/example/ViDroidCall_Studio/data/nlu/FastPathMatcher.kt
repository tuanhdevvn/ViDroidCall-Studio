package com.example.ViDroidCall_Studio.data.nlu

import android.content.Context
import android.util.Log
import com.example.ViDroidCall_Studio.data.model.NluResult
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * Bộ tiền xử lý và khớp quy tắc nhanh (Fast-Path Matcher)
 * Giúp nhận diện và phản hồi tức thì (< 5ms) cho các câu lệnh ngắn gọn,
 * bỏ qua hoàn toàn việc gọi mô hình LLM on-device.
 */
class FastPathMatcher(private val context: Context? = null) {

    private data class RuleEntry(
        val patterns: List<String>,
        val intent: String,
        val arguments: JSONObject,
        val riskLevel: String,
        val status: String,
        val requiresConfirmation: Boolean
    )

    private val exactRules = mutableListOf<RuleEntry>()

    init {
        loadRulesFromAssets()
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
                patterns.add(normalizeText(patternsJson.getString(j)))
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
        // Fallback tích hợp sẵn cho 8 Intent + GREETING + GOODBYE
        addRule(
            patterns = listOf("xin chào", "chào em", "chào bạn", "chào emma", "hello", "hi", "alo", "hey emma", "chào"),
            intent = "greeting",
            args = JSONObject(),
            risk = "low",
            status = "success",
            confirm = false
        )
        addRule(
            patterns = listOf("tạm biệt", "chào tạm biệt", "bye", "bye bye", "hẹn gặp lại", "tắt đi", "kết thúc", "dừng lại"),
            intent = "goodbye",
            args = JSONObject(),
            risk = "low",
            status = "success",
            confirm = false
        )
        addRule(
            patterns = listOf("gọi 113", "gọi cảnh sát", "gọi công an"),
            intent = "call_contact",
            args = JSONObject().put("contact", "113"),
            risk = "high",
            status = "success",
            confirm = true
        )
        addRule(
            patterns = listOf("gọi 114", "gọi cứu hỏa", "báo cháy"),
            intent = "call_contact",
            args = JSONObject().put("contact", "114"),
            risk = "high",
            status = "success",
            confirm = true
        )
        addRule(
            patterns = listOf("gọi 115", "gọi cấp cứu"),
            intent = "call_contact",
            args = JSONObject().put("contact", "115"),
            risk = "high",
            status = "success",
            confirm = true
        )
        addRule(
            patterns = listOf("mở bản đồ", "bản đồ", "google maps", "mở google maps"),
            intent = "open_map",
            args = JSONObject().put("destination", ""),
            risk = "low",
            status = "success",
            confirm = false
        )
        addRule(
            patterns = listOf("gọi", "gọi điện", "gọi điện thoại"),
            intent = "clarify",
            args = JSONObject().put("missing", JSONArray().put("contact")),
            risk = "low",
            status = "needs_clarification",
            confirm = false
        )
        addRule(
            patterns = listOf("nhắn tin", "gửi tin nhắn", "soạn tin nhắn"),
            intent = "clarify",
            args = JSONObject().put("missing", JSONArray().put("contact").put("message")),
            risk = "low",
            status = "needs_clarification",
            confirm = false
        )
        addRule(
            patterns = listOf("hẹn giờ", "đếm ngược"),
            intent = "clarify",
            args = JSONObject().put("missing", JSONArray().put("duration").put("unit")),
            risk = "low",
            status = "needs_clarification",
            confirm = false
        )
        addRule(
            patterns = listOf("đặt báo thức", "báo thức", "hẹn báo thức", "cài báo thức"),
            intent = "clarify",
            args = JSONObject().put("missing", JSONArray().put("hour").put("minute")),
            risk = "low",
            status = "needs_clarification",
            confirm = false
        )
        addRule(
            patterns = listOf("tìm đường", "chỉ đường", "đường đi"),
            intent = "clarify",
            args = JSONObject().put("missing", JSONArray().put("destination")),
            risk = "low",
            status = "needs_clarification",
            confirm = false
        )
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
                patterns = patterns.map { normalizeText(it) },
                intent = intent,
                arguments = args,
                riskLevel = risk,
                status = status,
                requiresConfirmation = confirm
            )
        )
    }

    /**
     * Thử khớp câu lệnh với bộ quy tắc nhanh.
     * @return NluResult nếu khớp, null nếu cần đẩy qua On-Device LLM.
     */
    fun match(query: String): NluResult? {
        return try {
            val normalized = normalizeText(query)
            if (normalized.isEmpty()) return null

            // 1. Khớp Exact Match từ bảng quy tắc
            for (rule in exactRules) {
                if (rule.patterns.contains(normalized)) {
                    return buildNluResult(
                        intent = rule.intent,
                        arguments = rule.arguments,
                        riskLevel = rule.riskLevel,
                        status = rule.status,
                        requiresConfirmation = rule.requiresConfirmation
                    )
                }
            }

            // 2. Khớp Regex Pattern cho các câu lệnh tham số hóa ngắn gọn
            matchDynamicPatterns(normalized)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong quá trình Fast-Path match: ${e.message}", e)
            null
        }
    }

    private fun matchDynamicPatterns(text: String): NluResult? {
        // a. Lời chào ngắn gọn có tiền tố/hậu tố
        if (text.matches(Regex("^(xin\\s+)?chào(\\s+em|\\s+bạn|\\s+emma|\\s+trợ\\s+lý)?$")) ||
            text.matches(Regex("^(hello|hi|hey)(\\s+emma)?$"))
        ) {
            return buildNluResult(
                intent = "greeting",
                arguments = JSONObject(),
                riskLevel = "low",
                status = "success",
                requiresConfirmation = false
            )
        }

        // b. Lời tạm biệt ngắn gọn
        if (text.matches(Regex("^(tạm\\s+biệt|bye(\\s+bye)?|hẹn\\s+gặp\\s+lại|chào\\s+nhé|nghỉ\\s+thôi)$"))) {
            return buildNluResult(
                intent = "goodbye",
                arguments = JSONObject(),
                riskLevel = "low",
                status = "success",
                requiresConfirmation = false
            )
        }

        // c. Đặt báo thức (set_alarm): ví dụ "báo thức 6 giờ", "đặt báo thức 7 giờ 30", "báo thức lúc 8h"
        val alarmPattern = Pattern.compile("^(?:đặt\\s+)?báo\\s+thức(?:\\s+lúc)?\\s+(\\d{1,2})(?:\\s*(?:giờ|h))?(?:\\s*(\\d{1,2}))?(?:\\s*phút)?(?:\\s*(sáng|chiều|tối))?$")
        val alarmMatcher = alarmPattern.matcher(text)
        if (alarmMatcher.find()) {
            var hour = alarmMatcher.group(1)?.toIntOrNull() ?: 0
            val minute = alarmMatcher.group(2)?.toIntOrNull() ?: 0
            val period = alarmMatcher.group(3)
            if (period != null) {
                if ((period == "chiều" || period == "tối") && hour < 12) {
                    hour += 12
                }
            }
            if (hour in 0..23 && minute in 0..59) {
                val args = JSONObject().apply {
                    put("hour", hour)
                    put("minute", minute)
                    put("label", "Báo thức")
                }
                return buildNluResult("set_alarm", args, "low", "success", false)
            }
        }

        // d. Hẹn giờ đếm ngược (set_timer): ví dụ "hẹn giờ 5 phút", "đếm ngược 30 giây", "hẹn giờ 1 tiếng"
        val timerPattern = Pattern.compile("^(?:hẹn\\s+giờ|đếm\\s+ngược)\\s+(\\d+)\\s*(giây|phút|tiếng|giờ)$")
        val timerMatcher = timerPattern.matcher(text)
        if (timerMatcher.find()) {
            val duration = timerMatcher.group(1)?.toIntOrNull() ?: 0
            val rawUnit = timerMatcher.group(2) ?: "phút"
            val unit = when (rawUnit) {
                "giây" -> "seconds"
                "tiếng", "giờ" -> "hours"
                else -> "minutes"
            }
            if (duration > 0) {
                val args = JSONObject().apply {
                    put("duration", duration)
                    put("unit", unit)
                    put("label", "Hẹn giờ")
                }
                return buildNluResult("set_timer", args, "low", "success", false)
            }
        }

        // e. Mở ứng dụng (open_app): ví dụ "mở youtube", "mở zalo", "vào facebook", "bật camera"
        val openAppPattern = Pattern.compile("^(?:mở|bật|vào)(?:\\s+(?:ứng\\s+dụng|app))?\\s+(.+)$", Pattern.UNICODE_CHARACTER_CLASS or Pattern.CASE_INSENSITIVE)
        val openAppMatcher = openAppPattern.matcher(text)
        if (openAppMatcher.find()) {
            val targetApp = openAppMatcher.group(1)?.trim() ?: ""
            val commonApps = listOf(
                "youtube", "zalo", "facebook", "tiktok", "chrome", "camera", "máy ảnh", "cài đặt", "messenger", "bản đồ",
                "shopee", "instagram", "telegram", "viber", "lazada", "momo", "spotify", "gọi điện", "tin nhắn", "điện thoại"
            )
            val lowerText = text.lowercase()
            if (targetApp.isNotEmpty() && (commonApps.contains(targetApp.lowercase()) || lowerText.startsWith("mở ứng dụng") || lowerText.startsWith("mở app") || lowerText.startsWith("bật ứng dụng") || lowerText.startsWith("vào app"))) {
                val args = JSONObject().apply {
                    put("app_name", targetApp)
                }
                return buildNluResult("open_app", args, "low", "success", false)
            }
        }

        // f. Mở bản đồ / Chỉ đường (open_map): ví dụ "chỉ đường đến Hồ Gươm", "tìm đường về nhà", "đường đến Landmark 81"
        val mapPattern = Pattern.compile("^(?:chỉ\\s+đường|tìm\\s+đường|đường\\s+đi|dẫn\\s+đường)(?:\\s+(?:đến|tới|về|qua))?\\s+(.+)$", Pattern.UNICODE_CHARACTER_CLASS or Pattern.CASE_INSENSITIVE)
        val mapMatcher = mapPattern.matcher(text)
        if (mapMatcher.find()) {
            val destination = mapMatcher.group(1)?.trim() ?: ""
            if (destination.isNotEmpty()) {
                val args = JSONObject().apply {
                    put("destination", destination)
                }
                return buildNluResult("open_map", args, "low", "success", false)
            }
        }

        // g. Gửi tin nhắn có nội dung: ví dụ "nhắn tin cho mẹ nội dung về nhà ăn cơm", "nhắn cho bố là con đang bận"
        val smsContentPattern = Pattern.compile("^(?:nhắn\\s+tin|gửi\\s+tin\\s+nhắn|nhắn)(?:\\s+cho)?\\s+(.+?)(?:\\s+(?:nội\\s+dung|là|với\\s+nội\\s+dung))\\s+(.+)$", Pattern.UNICODE_CHARACTER_CLASS or Pattern.CASE_INSENSITIVE)
        val smsContentMatcher = smsContentPattern.matcher(text)
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

        // h. Soạn tin nhắn không nội dung: "nhắn tin cho mẹ", "nhắn cho anh tuấn"
        val smsSimplePattern = Pattern.compile("^(?:nhắn\\s+tin|gửi\\s+tin\\s+nhắn|nhắn)(?:\\s+cho)?\\s+(.+)$", Pattern.UNICODE_CHARACTER_CLASS or Pattern.CASE_INSENSITIVE)
        val smsSimpleMatcher = smsSimplePattern.matcher(text)
        if (smsSimpleMatcher.find()) {
            val contact = smsSimpleMatcher.group(1)?.trim() ?: ""
            if (contact.isNotEmpty() && !contact.contains("nội dung") && !contact.contains(" là ")) {
                val args = JSONObject().apply {
                    put("contact", contact)
                    put("message", "")
                }
                return buildNluResult("send_sms", args, "medium", "success", false)
            }
        }

        // i. Gọi điện (call_contact): ví dụ "gọi cho mẹ", "gọi bác sĩ", "gọi anh hải", "gọi 0901234567"
        val callPattern = Pattern.compile("^(?:gọi\\s+điện(?:\\s+(?:cho|đến))?|gọi(?:\\s+(?:cho|đến))?|alo\\s+cho)\\s+(.+)$", Pattern.UNICODE_CHARACTER_CLASS or Pattern.CASE_INSENSITIVE)
        val callMatcher = callPattern.matcher(text)
        if (callMatcher.find()) {
            val contact = callMatcher.group(1)?.trim() ?: ""
            if (contact.isNotEmpty()) {
                val args = JSONObject().apply {
                    put("contact", contact)
                }
                return buildNluResult("call_contact", args, "high", "success", true)
            }
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

    private fun normalizeText(input: String): String {
        return input.trim()
            .lowercase()
            .replace(Regex("[.,?!;:'\"\\-_]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    companion object {
        private const val TAG = "FastPathMatcher"
    }
}
