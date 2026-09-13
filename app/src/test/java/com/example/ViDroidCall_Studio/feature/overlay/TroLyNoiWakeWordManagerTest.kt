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
            "Alo trợ lý",
            "alo trợ lí",
            "ê trợ lý",
            "hey trợ lý",
            "ViDroidCall ơi",
            "vidroidcall"
        )

        for (input in testCases) {
            val result = TroLyNoiWakeWordManager.parseWakeWordCommand(input)
            assertNotNull("Kỳ vọng nhận diện được từ khóa từ: $input", result)
            assertEquals("Kỳ vọng remainingCommand rỗng cho: $input", "", result?.remainingCommand)
        }
    }

    @Test
    fun parseWakeWordCommand_withTrailingCommand_extractsCorrectCommand() {
        val result1 = TroLyNoiWakeWordManager.parseWakeWordCommand("Trợ lý ơi gọi cho mẹ")
        assertNotNull(result1)
        assertEquals("gọi cho mẹ", result1?.remainingCommand)

        val result2 = TroLyNoiWakeWordManager.parseWakeWordCommand("Alo trợ lý, tìm đường đi bệnh viện Bạch Mai")
        assertNotNull(result2)
        assertEquals("tìm đường đi bệnh viện bạch mai", result2?.remainingCommand)

        val result3 = TroLyNoiWakeWordManager.parseWakeWordCommand("Ê trợ lý mở youtube")
        assertNotNull(result3)
        assertEquals("mở youtube", result3?.remainingCommand)

        val result4 = TroLyNoiWakeWordManager.parseWakeWordCommand("Này trợ lý ơi gửi tin nhắn cho bố")
        assertNotNull(result4)
        assertEquals("gửi tin nhắn cho bố", result4?.remainingCommand)
    }

    @Test
    fun parseWakeWordCommand_nonKeywords_returnsNull() {
        val nonKeywords = listOf(
            "Hôm nay thời tiết thế nào",
            "Alo có ai ở đó không",
            "Gọi cho mẹ",
            "Bật bài hát yêu thích",
            "Xin chào bạn"
        )

        for (input in nonKeywords) {
            val result = TroLyNoiWakeWordManager.parseWakeWordCommand(input)
            assertNull("Kỳ vọng null cho câu nói không chứa từ khóa: $input", result)
        }
    }
}
