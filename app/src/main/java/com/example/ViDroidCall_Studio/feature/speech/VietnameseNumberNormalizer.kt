// SPDX-License-Identifier: Apache-2.0

package com.example.ViDroidCall_Studio.feature.speech

import java.text.Normalizer
import java.util.regex.Pattern

/**
 * Bộ chuẩn hóa ngược văn bản (Inverse Text Normalization - ITN) tiếng Việt On-Device.
 * Tự động chuyển đổi các chữ số khẩu ngữ ("một một ba", "sáu giờ ba mươi", "mười lăm phút", "bốn con ba")
 * thành định dạng số viết tự nhiên ("113", "6 giờ 30", "15 phút", "3333") với độ trễ < 1ms.
 */
object VietnameseNumberNormalizer {

    private val DIACRITICS_REGEX = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

    private fun stripAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return DIACRITICS_REGEX.matcher(normalized).replaceAll("")
            .replace('đ', 'd')
            .replace('Đ', 'd')
    }

    private val DIGIT_WORDS_MAP = mapOf(
        "khong" to "0",
        "mot" to "1",
        "hai" to "2",
        "ba" to "3",
        "bon" to "4", "tu" to "4",
        "nam" to "5", "lam" to "5",
        "sau" to "6",
        "bay" to "7", "bey" to "7",
        "tam" to "8",
        "chin" to "9"
    )

    /**
     * Chuẩn hóa toàn bộ câu văn bản tiếng Việt từ STT.
     */
    fun normalize(text: String): String {
        var result = text.trim()
        if (result.isEmpty()) return result

        // 1. Chuẩn hóa các số khẩn cấp phổ biến (Emergency short codes: 113, 114, 115, 112, 111)
        result = normalizeEmergencyNumbers(result)

        // 2. Chuẩn hóa các cụm đọc số lặp ("bốn con ba" -> "3333", "ba số chín" -> "999", "đôi tám" -> "88")
        result = normalizeRepeatedDigits(result)

        // 3. Chuẩn hóa cụm thời gian (Giờ / Phút / Giây / Tiếng)
        result = normalizeTimeExpressions(result)

        // 4. Chuẩn hóa chuỗi số rời (Phone numbers, sequence of digits)
        result = normalizeDigitSequences(result)

        // 5. Chuẩn hóa các số đếm thông thường (10 - 99)
        result = normalizeGeneralNumbers(result)

        // 6. Rút gọn các khoảng trắng thừa giữa các cụm chữ số liên tiếp trong số điện thoại (ví dụ: "036 3333 490" -> "0363333490")
        result = compactConsecutiveDigits(result)

        return result
    }

    private fun normalizeEmergencyNumbers(text: String): String {
        var res = text
        // 113: "một một ba", "mot mot ba", "số một một ba"
        res = res.replace(Regex("(?i)(?<!\\p{L})(?:số\\s+|so\\s+)?(?:một|mot)\\s+(?:một|mot)\\s+(?:ba)(?!\\p{L})"), "113")
        // 114: "một một bốn", "mot mot bon", "một một tư"
        res = res.replace(Regex("(?i)(?<!\\p{L})(?:số\\s+|so\\s+)?(?:một|mot)\\s+(?:một|mot)\\s+(?:bốn|bon|tư|tu)(?!\\p{L})"), "114")
        // 115: "một một năm", "mot mot nam", "một một lăm"
        res = res.replace(Regex("(?i)(?<!\\p{L})(?:số\\s+|so\\s+)?(?:một|mot)\\s+(?:một|mot)\\s+(?:năm|nam|lăm|lam)(?!\\p{L})"), "115")
        // 112: "một một hai", "mot mot hai"
        res = res.replace(Regex("(?i)(?<!\\p{L})(?:số\\s+|so\\s+)?(?:một|mot)\\s+(?:một|mot)\\s+(?:hai)(?!\\p{L})"), "112")
        // 111: "một một một", "mot mot mot"
        res = res.replace(Regex("(?i)(?<!\\p{L})(?:số\\s+|so\\s+)?(?:một|mot)\\s+(?:một|mot)\\s+(?:một|mot)(?!\\p{L})"), "111")
        return res
    }

    private fun normalizeRepeatedDigits(text: String): String {
        var res = text
        val digitNameMap = mapOf(
            "không" to "0", "khong" to "0", "0" to "0",
            "một" to "1", "mot" to "1", "mốt" to "1", "1" to "1",
            "hai" to "2", "2" to "2",
            "ba" to "3", "3" to "3",
            "bốn" to "4", "bon" to "4", "tư" to "4", "tu" to "4", "4" to "4",
            "năm" to "5", "nam" to "5", "lăm" to "5", "lam" to "5", "5" to "5",
            "sáu" to "6", "sau" to "6", "6" to "6",
            "bảy" to "7", "bay" to "7", "bẩy" to "7", "7" to "7",
            "tám" to "8", "tam" to "8", "8" to "8",
            "chín" to "9", "chin" to "9", "9" to "9"
        )

        for ((name, digit) in digitNameMap) {
            // Lục quý / 6 con / 6 số
            res = res.replace(Regex("(?i)(?<!\\p{L})(?:lục\\s+quý|luc\\s+quy|6\\s+con|sáu\\s+con|sau\\s+con|6\\s+số|sáu\\s+số|sau\\s+so)\\s+$name(?!\\p{L})"), digit.repeat(6))
            // Ngũ quý / 5 con / 5 số
            res = res.replace(Regex("(?i)(?<!\\p{L})(?:ngũ\\s+quý|ngu\\s+quy|5\\s+con|năm\\s+con|nam\\s+con|5\\s+số|năm\\s+số|nam\\s+so)\\s+$name(?!\\p{L})"), digit.repeat(5))
            // Tứ quý / Tứ / 4 con / 4 số / bốn con / bốn số
            res = res.replace(Regex("(?i)(?<!\\p{L})(?:tứ\\s+quý|tu\\s+quy|tứ|tu|4\\s+con|bốn\\s+con|bon\\s+con|4\\s+số|bốn\\s+số|bon\\s+so)\\s+$name(?!\\p{L})"), digit.repeat(4))
            // Tam hoa / 3 con / 3 số / ba con / ba số (trừ từ "tam" đứng một mình để tránh nhầm với số 8)
            res = res.replace(Regex("(?i)(?<!\\p{L})(?:tam\\s+hoa|3\\s+con|ba\\s+con|3\\s+số|ba\\s+số|ba\\s+so)\\s+$name(?!\\p{L})"), digit.repeat(3))
            // Đôi / Kép / Cặp / 2 con / 2 số / hai con / hai số
            res = res.replace(Regex("(?i)(?<!\\p{L})(?:đôi|doi|kép|kep|cặp|cap|2\\s+con|hai\\s+con|2\\s+số|hai\\s+số|hai\\s+so)\\s+$name(?!\\p{L})"), digit.repeat(2))
        }

        return res
    }

    private fun normalizeDigitSequences(text: String): String {
        val words = text.split(Regex("\\s+"))
        val output = mutableListOf<String>()
        var i = 0
        while (i < words.size) {
            val word = words[i]
            val unaccentedWord = stripAccents(word.lowercase())

            // Kiểm tra xem từ hiện tại có phải là chữ số rời không (hoặc là chuỗi chữ số vừa tạo ra)
            if (DIGIT_WORDS_MAP.containsKey(unaccentedWord) || word.matches(Regex("^[0-9]+$"))) {
                // Thu thập chuỗi các chữ số liên tiếp
                val digitChunk = StringBuilder()
                val originalChunk = mutableListOf<String>()
                var j = i
                while (j < words.size) {
                    val rawW = words[j]
                    val w = stripAccents(rawW.lowercase())
                    val d = DIGIT_WORDS_MAP[w]
                    if (d != null) {
                        digitChunk.append(d)
                        originalChunk.add(rawW)
                        j++
                    } else if (rawW.matches(Regex("^[0-9]+$"))) {
                        digitChunk.append(rawW)
                        originalChunk.add(rawW)
                        j++
                    } else {
                        break
                    }
                }

                // Nếu có >= 3 chữ số liên tiếp, hoặc >= 2 chữ số đứng sau từ "số"/"đến"/"cho", hoặc bắt đầu bằng số 0
                val isAfterNumberKeyword = output.isNotEmpty() && (
                    stripAccents(output.last().lowercase()).let { it == "so" || it == "den" || it == "cho" }
                )
                if (originalChunk.size >= 3 || (originalChunk.size >= 2 && isAfterNumberKeyword) || digitChunk.startsWith("0")) {
                    output.add(digitChunk.toString())
                    i = j
                    continue
                }
            }
            output.add(word)
            i++
        }
        return output.joinToString(" ")
    }

    private fun compactConsecutiveDigits(text: String): String {
        // Gom các khối số bị phân tách bởi khoảng trắng đứng cạnh nhau thành một chuỗi số điện thoại duy nhất (ví dụ: "036 3333 490" -> "0363333490")
        var res = text
        // Lặp cho đến khi không còn khoảng trắng giữa 2 cụm số trong chuỗi số điện thoại
        while (Regex("\\b(0[0-9]{1,4})\\s+([0-9]{1,6})\\b").containsMatchIn(res)) {
            res = res.replace(Regex("\\b(0[0-9]{1,4})\\s+([0-9]{1,6})\\b"), "$1$2")
        }
        while (Regex("\\b([0-9]{3,7})\\s+([0-9]{2,4})\\b").containsMatchIn(res)) {
            res = res.replace(Regex("\\b([0-9]{3,7})\\s+([0-9]{2,4})\\b"), "$1$2")
        }
        return res
    }

    private fun normalizeTimeExpressions(text: String): String {
        var res = text

        // 1. Chuẩn hóa các cụm giờ dạng chữ: "sáu giờ" -> "6 giờ", "mười hai giờ" -> "12 giờ"
        val hourPhrases = listOf(
            "mười hai giờ" to "12 giờ", "muoi hai gio" to "12 giờ",
            "mười một giờ" to "11 giờ", "muoi mot gio" to "11 giờ",
            "mười giờ" to "10 giờ", "muoi gio" to "10 giờ",
            "chín giờ" to "9 giờ", "chin gio" to "9 giờ",
            "tám giờ" to "8 giờ", "tam gio" to "8 giờ",
            "bảy giờ" to "7 giờ", "bay gio" to "7 giờ", "bẩy giờ" to "7 giờ",
            "sáu giờ" to "6 giờ", "sau gio" to "6 giờ",
            "năm giờ" to "5 giờ", "nam gio" to "5 giờ",
            "bốn giờ" to "4 giờ", "bon gio" to "4 giờ", "tư giờ" to "4 giờ",
            "ba giờ" to "3 giờ", "ba gio" to "3 giờ",
            "hai giờ" to "2 giờ", "hai gio" to "2 giờ",
            "một giờ" to "1 giờ", "mot gio" to "1 giờ"
        )
        for ((phrase, replacement) in hourPhrases) {
            res = res.replace(Regex("(?i)(?<!\\p{L})$phrase(?!\\p{L})"), replacement)
        }

        // 2. Chuẩn hóa phút đi sau giờ: "6 giờ ba mươi" -> "6 giờ 30"
        val minuteSuffixes = listOf(
            "năm mươi lăm" to "55", "nam muoi lam" to "55",
            "năm mươi" to "50", "nam muoi" to "50",
            "bốn mươi lăm" to "45", "bon muoi lam" to "45", "bốn lăm" to "45", "bon lam" to "45",
            "bốn mươi" to "40", "bon muoi" to "40",
            "ba mươi lăm" to "35", "ba muoi lam" to "35", "ba lăm" to "35", "ba lam" to "35",
            "ba mươi" to "30", "ba muoi" to "30",
            "hai mươi lăm" to "25", "hai muoi lam" to "25", "hai lăm" to "25", "hai lam" to "25", "hăm lăm" to "25",
            "hai mươi" to "20", "hai muoi" to "20",
            "mười lăm" to "15", "muoi lam" to "15",
            "mười" to "10", "muoi" to "10",
            "năm" to "5", "nam" to "5"
        )
        for ((words, num) in minuteSuffixes) {
            res = res.replace(Regex("(?i)(?<!\\p{L})([0-9]+\\s+giờ)\\s+$words(?!\\p{L})"), "$1 $num")
        }

        // 3. Chuẩn hóa các cụm "X phút" độc lập: "mười lăm phút" -> "15 phút"
        val minutePhrases = listOf(
            "năm mươi lăm phút" to "55 phút", "nam muoi lam phut" to "55 phút", "nam lam phut" to "55 phút",
            "năm mươi phút" to "50 phút", "nam muoi phut" to "50 phút",
            "bốn mươi lăm phút" to "45 phút", "bon lam phut" to "45 phút", "bốn lăm phút" to "45 phút",
            "bốn mươi phút" to "40 phút", "bon muoi phut" to "40 phút",
            "ba mươi lăm phút" to "35 phút", "ba muoi lam phut" to "35 phút", "ba lăm phút" to "35 phút",
            "ba mươi phút" to "30 phút", "ba muoi phut" to "30 phút",
            "hai mươi lăm phút" to "25 phút", "hai muoi lam phut" to "25 phút", "hai lăm phút" to "25 phút",
            "hai mươi phút" to "20 phút", "hai muoi phut" to "20 phút",
            "mười lăm phút" to "15 phút", "muoi lam phut" to "15 phút",
            "mười phút" to "10 phút", "muoi phut" to "10 phút",
            "năm phút" to "5 phút", "nam phut" to "5 phút",
            "sáu mươi phút" to "60 phút", "sau muoi phut" to "60 phút"
        )
        for ((phrase, replacement) in minutePhrases) {
            res = res.replace(Regex("(?i)(?<!\\p{L})$phrase(?!\\p{L})"), replacement)
        }

        // 4. Chuẩn hóa các cụm "X giây" độc lập: "hai mươi giây" -> "20 giây"
        val secondPhrases = listOf(
            "sáu mươi giây" to "60 giây", "sau muoi giay" to "60 giây",
            "bốn mươi lăm giây" to "45 giây", "bon lam giay" to "45 giây",
            "ba mươi giây" to "30 giây", "ba muoi giay" to "30 giây",
            "hai mươi giây" to "20 giây", "hai muoi giay" to "20 giây",
            "mười lăm giây" to "15 giây", "muoi lam giay" to "15 giây",
            "mười giây" to "10 giây", "muoi giay" to "10 giây",
            "năm giây" to "5 giây", "nam giay" to "5 giây"
        )
        for ((phrase, replacement) in secondPhrases) {
            res = res.replace(Regex("(?i)(?<!\\p{L})$phrase(?!\\p{L})"), replacement)
        }

        return res
    }

    private fun normalizeGeneralNumbers(text: String): String {
        var res = text
        val compoundMap = listOf(
            "mười lăm" to "15", "muoi lam" to "15",
            "hai mươi lăm" to "25", "hai muoi lam" to "25", "hai lăm" to "25", "hăm lăm" to "25",
            "ba mươi lăm" to "35", "ba muoi lam" to "35", "ba lăm" to "35",
            "bốn mươi lăm" to "45", "bon muoi lam" to "45", "bốn lăm" to "45",
            "năm mươi lăm" to "55", "nam muoi lam" to "55", "năm lăm" to "55",
            "sáu mươi lăm" to "65", "sau muoi lam" to "65", "sáu lăm" to "65",
            "bảy mươi lăm" to "75", "bay muoi lam" to "75", "bảy lăm" to "75",
            "tám mươi lăm" to "85", "tam muoi lam" to "85", "tám lăm" to "85",
            "chín mươi lăm" to "95", "chin muoi lam" to "95", "chín lăm" to "95",
            "hai mươi" to "20", "hai muoi" to "20",
            "ba mươi" to "30", "ba muoi" to "30",
            "bốn mươi" to "40", "bon muoi" to "40",
            "năm mươi" to "50", "nam muoi" to "50",
            "sáu mươi" to "60", "sau muoi" to "60",
            "bảy mươi" to "70", "bay muoi" to "70",
            "tám mươi" to "80", "tam muoi" to "80",
            "chín mươi" to "90", "chin muoi" to "90"
        )
        for ((words, num) in compoundMap) {
            res = res.replace(Regex("(?i)(?<!\\p{L})$words(?!\\p{L})"), num)
        }
        return res
    }
}
