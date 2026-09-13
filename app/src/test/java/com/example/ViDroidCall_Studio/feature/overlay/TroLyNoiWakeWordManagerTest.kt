package com.example.ViDroidCall_Studio.feature.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TroLyNoiWakeWordManagerTest {

    @Test
    fun parseWakeWordCommand_exactKeywords_returnsEmptyRemainingCommand() {
        val testCases = listOf(
            "Trợ lý ơi",
            "trợ lý ơi!",
            "Trợ lí ơi...",
            "Trợ lý",
            "trợ lí",
            "Alo trợ lý",
            "alo trợ lí",
            "ê trợ lý",
            "ê trợ lí",
            "hey trợ lý",
            "hey trợ lí",
            "ViDroidCall ơi",
            "vidroidcall",
            "vidroidcall ơi!"
        )

        for (input in testCases) {
            val result = TroLyNoiWakeWordManager.parseWakeWordCommand(input)
            assertNotNull("Kỳ vọng nhận diện được từ khóa từ: $input", result)
            assertEquals("Kỳ vọng remainingCommand rỗng cho: $input", "", result?.remainingCommand)
        }
    }

    @Test
    fun parseWakeWordCommand_withTrailingCommand_extractsCorrectCommand() {
        // 1. "Trợ lý ơi gọi cho mẹ"
        val r1 = TroLyNoiWakeWordManager.parseWakeWordCommand("Trợ lý ơi gọi cho mẹ")
        assertNotNull(r1)
        assertEquals("trợ lý ơi", r1?.matchedKeyword)
        assertEquals("gọi cho mẹ", r1?.remainingCommand)

        // 2. "Trợ lý gọi cho mẹ"
        val r2 = TroLyNoiWakeWordManager.parseWakeWordCommand("Trợ lý gọi cho mẹ")
        assertNotNull(r2)
        assertEquals("trợ lý", r2?.matchedKeyword)
        assertEquals("gọi cho mẹ", r2?.remainingCommand)

        // 3. "Ê trợ lý gọi cho bố"
        val r3 = TroLyNoiWakeWordManager.parseWakeWordCommand("Ê trợ lý gọi cho bố")
        assertNotNull(r3)
        assertEquals("ê trợ lý", r3?.matchedKeyword)
        assertEquals("gọi cho bố", r3?.remainingCommand)

        // 4. "Alo trợ lý mở bản đồ"
        val r4 = TroLyNoiWakeWordManager.parseWakeWordCommand("Alo trợ lý mở bản đồ")
        assertNotNull(r4)
        assertEquals("alo trợ lý", r4?.matchedKeyword)
        assertEquals("mở bản đồ", r4?.remainingCommand)

        // 5. "Trợ lý ơi đặt báo thức 7 giờ"
        val r5 = TroLyNoiWakeWordManager.parseWakeWordCommand("Trợ lý ơi đặt báo thức 7 giờ")
        assertNotNull(r5)
        assertEquals("trợ lý ơi", r5?.matchedKeyword)
        assertEquals("đặt báo thức 7 giờ", r5?.remainingCommand)

        // 6. "Trợ lý ơi nhắn tin cho mẹ"
        val r6 = TroLyNoiWakeWordManager.parseWakeWordCommand("Trợ lý ơi nhắn tin cho mẹ")
        assertNotNull(r6)
        assertEquals("trợ lý ơi", r6?.matchedKeyword)
        assertEquals("nhắn tin cho mẹ", r6?.remainingCommand)

        // 7. "ViDroidCall ơi mở cài đặt"
        val r7 = TroLyNoiWakeWordManager.parseWakeWordCommand("ViDroidCall ơi mở cài đặt")
        assertNotNull(r7)
        assertEquals("vidroidcall ơi", r7?.matchedKeyword)
        assertEquals("mở cài đặt", r7?.remainingCommand)

        // 8. Đệm từ phía trước: "Này trợ lý ơi mở youtube"
        val r8 = TroLyNoiWakeWordManager.parseWakeWordCommand("Này trợ lý ơi mở youtube")
        assertNotNull(r8)
        assertEquals("mở youtube", r8?.remainingCommand)
    }

    @Test
    fun parseWakeWordCommand_nonKeywords_returnsNull() {
        val nonKeywords = listOf(
            "Hôm nay thời tiết thế nào",
            "Alo có ai ở đó không",
            "Gọi cho mẹ",
            "Bật bài hát yêu thích",
            "Xin chào bạn",
            "Mở youtube xem phim",
            "Báo thức lúc 6 giờ",
            "Trợ cấp xã hội",
            "Lý thuyết lượng tử"
        )

        for (input in nonKeywords) {
            val result = TroLyNoiWakeWordManager.parseWakeWordCommand(input)
            assertNull("Kỳ vọng null cho câu nói không chứa từ khóa: $input", result)
        }
    }

    @Test
    fun parseWakeWordCommand_punctuationAndWhitespace_handledGracefully() {
        val test1 = TroLyNoiWakeWordManager.parseWakeWordCommand("  TRỢ LÝ ƠI... ???  gọi cho mẹ  ")
        assertNotNull(test1)
        assertEquals("gọi cho mẹ", test1?.remainingCommand)

        val test2 = TroLyNoiWakeWordManager.parseWakeWordCommand("Alo   trợ lí,   mở bản đồ!")
        assertNotNull(test2)
        assertEquals("mở bản đồ", test2?.remainingCommand)
    }
}
