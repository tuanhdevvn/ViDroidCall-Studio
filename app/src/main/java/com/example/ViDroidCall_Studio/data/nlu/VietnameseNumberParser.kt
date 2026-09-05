// SPDX-License-Identifier: Apache-2.0

package com.example.ViDroidCall_Studio.data.nlu

import java.text.Normalizer
import java.util.regex.Pattern

/**
 * Bộ phân tích và chuyển đổi chữ số tiếng Việt thành số nguyên (Int) trong dải 0..999.
 * Hỗ trợ chữ số trực tiếp ("25"), chữ có dấu/không dấu, chữ hoa/thường, các từ lóng quen thuộc (nửa -> 30).
 * Sử dụng thuật toán phân tích cú pháp ngữ pháp tiếng Việt (hàng trăm, hàng chục, hàng đơn vị)
 * thay vì hardcode map lớn, và trả về null nếu chuỗi từ không hợp ngữ pháp.
 */
object VietnameseNumberParser {

    private val DIACRITICS_REGEX = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    private val PUNCTUATION_REGEX = Pattern.compile("[.,?!;:'\"\\-_]")
    private val MULTIPLE_SPACES_REGEX = Pattern.compile("\\s+")

    private fun stripAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return DIACRITICS_REGEX.matcher(normalized).replaceAll("")
            .replace('đ', 'd')
            .replace('Đ', 'd')
    }

    private fun normalize(input: String): String {
        val unaccented = stripAccents(input.lowercase())
        val noPunctuation = PUNCTUATION_REGEX.matcher(unaccented).replaceAll(" ")
        return MULTIPLE_SPACES_REGEX.matcher(noPunctuation).replaceAll(" ").trim()
    }

    /**
     * Chuyển đổi một chuỗi văn bản (dạng số "15" hoặc dạng chữ "mười lăm") thành Int (0..999).
     * @return Int nếu phân tích thành công, null nếu không thể phân tích hoặc sai ngữ pháp.
     */
    fun parse(text: String): Int? {
        val rawTrim = text.trim()
        if (rawTrim.isEmpty()) return null

        // 1. Thử parse trực tiếp chữ số (ví dụ: "0", "5", "15", "25", "120", "999")
        val directInt = rawTrim.toIntOrNull()
        if (directInt != null) {
            return if (directInt in 0..999) directInt else null
        }

        // 2. Chuẩn hóa văn bản
        val clean = normalize(rawTrim)
        if (clean.isEmpty()) return null

        // 3. Xử lý từ đặc biệt FastPath (nửa tiếng/nửa giờ -> 30) và số 0
        if (clean == "nua") return 30
        if (clean == "khong") return 0

        // 4. Kiểm tra số đơn độc lập (1..9)
        SINGLE_DIGITS[clean]?.let { return it }

        val words = clean.split("\\s+".toRegex())

        // 5. Nếu có từ "tram", phân tích theo ngữ pháp hàng trăm
        if (words.contains("tram")) {
            return parseHundreds(words)
        }

        // 6. Ngược lại, phân tích theo ngữ pháp hàng chục (10..99)
        return parseTens(words)
    }

    private fun parseTens(words: List<String>): Int? {
        if (words.isEmpty()) return null

        if (words.size == 1) {
            return if (words[0] == "muoi") 10 else null
        }

        if (words.size == 2) {
            // Dạng "mười một" .. "mười chín" (11..19)
            if (words[0] == "muoi") {
                val unit = SINGLE_UNITS[words[1]]
                return if (unit != null && unit in 1..9) 10 + unit else null
            }
            // Dạng "hai mươi" .. "chín mươi" (20, 30, 40, ..., 90)
            if (words[1] == "muoi") {
                val mult = TENS_MULTIPLIERS[words[0]]
                return if (mult != null && mult in 2..9) mult * 10 else null
            }
            return null
        }

        if (words.size == 3) {
            // Dạng "hai mươi mốt" .. "chín mươi chín" (21..99 trừ 20, 30...)
            if (words[1] == "muoi") {
                val mult = TENS_MULTIPLIERS[words[0]]
                val unit = SINGLE_UNITS[words[2]]
                return if (mult != null && mult in 2..9 && unit != null && unit in 1..9) {
                    mult * 10 + unit
                } else null
            }
            return null
        }

        return null
    }

    private fun parseHundreds(words: List<String>): Int? {
        val tramIndex = words.indexOf("tram")
        // Từ "tram" phải nằm ở vị trí thứ 2 (chỉ số 1), ví dụ "một trăm"
        if (tramIndex != 1) return null

        val mult = HUNDREDS_MULTIPLIERS[words[0]] ?: return null
        val hundredsVal = mult * 100

        val remainder = words.subList(2, words.size)
        if (remainder.isEmpty()) return hundredsVal

        // Trường hợp "một trăm linh năm" hoặc "một trăm lẻ năm"
        if (remainder[0] == "linh" || remainder[0] == "le") {
            if (remainder.size != 2) return null
            val unit = SINGLE_UNITS[remainder[1]]
            return if (unit != null && unit in 1..9) hundredsVal + unit else null
        }

        // Trường hợp phần dư là hàng chục hợp lệ ("hai mươi lăm", "mười lăm", "mười")
        val tensVal = parseTens(remainder)
        return if (tensVal != null) hundredsVal + tensVal else null
    }

    // Các chữ số đơn lẻ độc lập
    private val SINGLE_DIGITS = mapOf(
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
        "chin" to 9
    )

    // Chữ số đơn vị đi kèm sau "mươi" / "mười" / "trăm linh"
    private val SINGLE_UNITS = mapOf(
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

    // Hệ số hàng chục đứng trước "mươi" (2..9)
    private val TENS_MULTIPLIERS = mapOf(
        "hai" to 2,
        "ba" to 3,
        "bon" to 4,
        "tu" to 4,
        "nam" to 5,
        "sau" to 6,
        "bay" to 7,
        "tam" to 8,
        "chin" to 9
    )

    // Hệ số hàng trăm đứng trước "trăm" (1..9)
    private val HUNDREDS_MULTIPLIERS = mapOf(
        "mot" to 1,
        "hai" to 2,
        "ba" to 3,
        "bon" to 4,
        "tu" to 4,
        "nam" to 5,
        "sau" to 6,
        "bay" to 7,
        "tam" to 8,
        "chin" to 9
    )
}
