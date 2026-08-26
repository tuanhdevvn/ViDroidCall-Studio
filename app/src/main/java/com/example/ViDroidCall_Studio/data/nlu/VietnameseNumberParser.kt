package com.example.ViDroidCall_Studio.data.nlu

import java.text.Normalizer
import java.util.regex.Pattern

/**
 * Bộ phân tích và chuyển đổi chữ số tiếng Việt thành số nguyên (Int)
 * Hỗ trợ tiếng Việt có dấu, không dấu, số đơn, hàng chục, hàng trăm và từ lóng quen thuộc.
 */
object VietnameseNumberParser {

    private val DIACRITICS_REGEX = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

    private fun stripAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return DIACRITICS_REGEX.matcher(normalized).replaceAll("")
            .replace('đ', 'd')
            .replace('Đ', 'd')
    }

    /**
     * Chuyển đổi một chuỗi văn bản (dạng số "15" hoặc dạng chữ "mười lăm") thành Int.
     * @return Int nếu phân tích thành công, null nếu không thể phân tích.
     */
    fun parse(text: String): Int? {
        val rawTrim = text.trim()
        if (rawTrim.isEmpty()) return null

        // 1. Thử parse trực tiếp chữ số
        val directInt = rawTrim.toIntOrNull()
        if (directInt != null) return directInt

        // 2. Chuẩn hóa chuỗi (bỏ dấu, chuyển chữ thường, bỏ khoảng trắng dư)
        val clean = stripAccents(rawTrim.lowercase()).replace(Regex("[.,?!;:'\"\\-_]"), " ").replace(Regex("\\s+"), " ").trim()
        if (clean.isEmpty()) return null

        // 3. Phân tích cụm từ rưỡi / nửa
        if (clean == "nua") return 30

        // 4. Tra cứu nhanh các từ đơn/ghép phổ biến
        NUMBER_MAP[clean]?.let { return it }

        // 5. Thuật toán phân tích tổng quát cho hàng trăm/hàng chục/hàng đơn vị
        return parseCompoundWords(clean)
    }

    private fun parseCompoundWords(clean: String): Int? {
        val words = clean.split("\\s+".toRegex())
        if (words.isEmpty()) return null

        var total = 0
        var currentHundreds = 0
        var i = 0

        while (i < words.size) {
            val word = words[i]

            // Xử lý hàng trăm
            if (word == "tram") {
                val valBefore = if (currentHundreds > 0) currentHundreds else (SINGLE_DIGITS[words.getOrNull(i - 1)] ?: 1)
                total += valBefore * 100
                currentHundreds = 0
                i++
                continue
            }

            // Xử lý "mươi" / "muoi"
            if (word == "muoi") {
                val prevWord = words.getOrNull(i - 1)
                val prevVal = SINGLE_DIGITS[prevWord]
                if (prevVal != null) {
                    // Ví dụ "hai mươi" -> total += 20 (đã cộng prevVal trước đó nên cần điều chỉnh)
                    total += (prevVal * 10 - prevVal)
                } else {
                    // Ví dụ "mười" độc lập
                    total += 10
                }
                i++
                continue
            }

            // Xử lý từ separator linh / le
            if (word == "linh" || word == "le") {
                i++
                continue
            }

            // Xử lý chữ số đơn vị
            val digit = SINGLE_DIGITS[word]
            if (digit != null) {
                total += digit
                currentHundreds = digit
            } else {
                return null
            }

            i++
        }

        return if (total > 0) total else null
    }

    private val SINGLE_DIGITS = mapOf(
        "khong" to 0,
        "mot" to 1,
        "mot" to 1,
        "hai" to 2,
        "ba" to 3,
        "bon" to 4,
        "tu" to 4,
        "nam" to 5,
        "lam" to 5,
        "sau" to 6,
        "bay" to 7,
        "tam" to 8,
        "chin" to 9
    )

    private val NUMBER_MAP = mapOf(
        "khong" to 0,
        "mot" to 1,
        "hai" to 2,
        "ba" to 3,
        "bon" to 4,
        "tu" to 4,
        "nam" to 5,
        "lam" to 5,
        "sau" to 6,
        "bay" to 7,
        "tam" to 8,
        "chin" to 9,

        // Hàng chục 10..19
        "muoi" to 10,
        "muoi mot" to 11,
        "muoi hai" to 12,
        "muoi ba" to 13,
        "muoi bon" to 14,
        "muoi tu" to 14,
        "muoi lam" to 15,
        "muoi sau" to 16,
        "muoi bay" to 17,
        "muoi tam" to 18,
        "muoi chin" to 19,

        // Hàng chục 20..29
        "hai muoi" to 20,
        "hai muoi mot" to 21,
        "hai muoi hai" to 22,
        "hai muoi ba" to 23,
        "hai muoi bon" to 24,
        "hai muoi tu" to 24,
        "hai muoi lam" to 25,
        "hai muoi sau" to 26,
        "hai muoi bay" to 27,
        "hai muoi tam" to 28,
        "hai muoi chin" to 29,

        // Hàng chục 30..39
        "ba muoi" to 30,
        "ba muoi mot" to 31,
        "ba muoi hai" to 32,
        "ba muoi ba" to 33,
        "ba muoi bon" to 34,
        "ba muoi tu" to 34,
        "ba muoi lam" to 35,
        "ba muoi sau" to 36,
        "ba muoi bay" to 37,
        "ba muoi tam" to 38,
        "ba muoi chin" to 39,

        // Hàng chục 40..49
        "bon muoi" to 40,
        "bon muoi mot" to 41,
        "bon muoi hai" to 42,
        "bon muoi ba" to 43,
        "bon muoi bon" to 44,
        "bon muoi tu" to 44,
        "bon muoi lam" to 45,
        "bon muoi sau" to 46,
        "bon muoi bay" to 47,
        "bon muoi tam" to 48,
        "bon muoi chin" to 49,

        // Hàng chục 50..59
        "nam muoi" to 50,
        "nam muoi mot" to 51,
        "nam muoi hai" to 52,
        "nam muoi ba" to 53,
        "nam muoi bon" to 54,
        "nam muoi tu" to 54,
        "nam muoi lam" to 55,
        "nam muoi sau" to 56,
        "nam muoi bay" to 57,
        "nam muoi tam" to 58,
        "nam muoi chin" to 59,

        // Hàng chục 60..90
        "sau muoi" to 60,
        "bay muoi" to 70,
        "tam muoi" to 80,
        "chin muoi" to 90,

        // Hàng trăm
        "mot tram" to 100,
        "mot tram linh nam" to 105,
        "mot tram le nam" to 105,
        "hai tram muoi" to 210,
        "hai tram ba muoi" to 230
    )
}
