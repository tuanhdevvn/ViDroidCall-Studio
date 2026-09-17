// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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

        // 3. Xử lý từ đặc biệt FastPath (nửa tiếng/nửa giờ/rưỡi -> 30) và số 0
        if (clean == "nua" || clean == "ruoi") return 30
        if (clean == "khong") return 0

        // 4. Kiểm tra số đơn độc lập (1..9)
        SINGLE_DIGITS[clean]?.let { return it }

        val words = clean.split("\\s+".toRegex())

        // 5. Nếu có từ "tram" hoặc chữ số tròn trăm (100, 200, ...), phân tích theo hàng trăm
        if (words.contains("tram") || (words[0].toIntOrNull() != null && words[0].toInt() in 100..900 && words[0].toInt() % 100 == 0)) {
            val hundredsResult = parseHundreds(words)
            if (hundredsResult != null) return hundredsResult
        }

        // 6. Ngược lại, phân tích theo ngữ pháp hàng chục (10..99) hoặc số lai
        return parseTens(words)
    }

    private fun parseTens(words: List<String>): Int? {
        if (words.isEmpty()) return null

        if (words.size == 1) {
            val d = words[0].toIntOrNull()
            if (d != null && d in 0..999) return d
            return if (words[0] == "muoi") 10 else null
        }

        if (words.size == 2) {
            // Trường hợp hàng chục dạng số kết hợp đơn vị: "90 chín", "20 mốt", "50 lăm", "90 9"
            val tensInt = words[0].toIntOrNull()
            if (tensInt != null && tensInt in listOf(10, 20, 30, 40, 50, 60, 70, 80, 90)) {
                val unit = SINGLE_UNITS[words[1]] ?: words[1].toIntOrNull()
                if (unit != null && unit in 1..9) {
                    return tensInt + unit
                }
            }

            // Dạng "mười một" .. "mười chín" (11..19)
            if (words[0] == "muoi") {
                val unit = SINGLE_UNITS[words[1]] ?: words[1].toIntOrNull()
                return if (unit != null && unit in 1..9) 10 + unit else null
            }
            // Dạng "hai mươi" .. "chín mươi" (20, 30, 40, ..., 90)
            if (words[1] == "muoi") {
                val mult = TENS_MULTIPLIERS[words[0]] ?: words[0].toIntOrNull()
                return if (mult != null && mult in 2..9) mult * 10 else null
            }
            return null
        }

        if (words.size == 3) {
            // Dạng "hai mươi mốt" .. "chín mươi chín" (21..99 trừ 20, 30...)
            if (words[1] == "muoi") {
                val mult = TENS_MULTIPLIERS[words[0]] ?: words[0].toIntOrNull()
                val unit = SINGLE_UNITS[words[2]] ?: words[2].toIntOrNull()
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
        if (tramIndex == 1) {
            val mult = HUNDREDS_MULTIPLIERS[words[0]] ?: words[0].toIntOrNull() ?: return null
            if (mult !in 1..9) return null
            val hundredsVal = mult * 100

            val remainder = words.subList(2, words.size)
            if (remainder.isEmpty()) return hundredsVal

            // Trường hợp "một trăm linh năm" hoặc "một trăm lẻ năm"
            if (remainder[0] == "linh" || remainder[0] == "le") {
                if (remainder.size != 2) return null
                val unit = SINGLE_UNITS[remainder[1]] ?: remainder[1].toIntOrNull()
                return if (unit != null && unit in 1..9) hundredsVal + unit else null
            }

            // Trường hợp phần dư là hàng chục hợp lệ ("hai mươi lăm", "mười lăm", "mười", "90 chín", "25")
            val tensVal = parseTens(remainder)
            return if (tensVal != null) hundredsVal + tensVal else null
        }

        // Trường hợp words[0] là chữ số tròn trăm: "100", "200", ..., "900"
        val firstInt = words[0].toIntOrNull()
        if (firstInt != null && firstInt in 100..900 && firstInt % 100 == 0) {
            val remainder = words.subList(1, words.size)
            if (remainder.isEmpty()) return firstInt
            if (remainder[0] == "linh" || remainder[0] == "le") {
                if (remainder.size != 2) return null
                val unit = SINGLE_UNITS[remainder[1]] ?: remainder[1].toIntOrNull()
                return if (unit != null && unit in 1..9) firstInt + unit else null
            }
            val tensVal = parseTens(remainder)
            return if (tensVal != null) firstInt + tensVal else null
        }

        return null
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
