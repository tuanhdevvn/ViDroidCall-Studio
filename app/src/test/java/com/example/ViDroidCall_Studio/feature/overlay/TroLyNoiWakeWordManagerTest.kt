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
            "trợ lí"
        )

        for (input in testCases) {
            val result = TroLyNoiWakeWordManager.parseWakeWordCommand(input)
            assertNotNull("Kỳ vọng nhận diện được từ khóa từ: $input", result)
            assertEquals("Kỳ vọng remainingCommand rỗng cho: $input", "", result?.remainingCommand)
        }
    }

    @Test
    fun parseWakeWordCommand_withTrailingCommand_extractsCorrectCommand() {
        val r1 = TroLyNoiWakeWordManager.parseWakeWordCommand("Trợ lý ơi gọi cho mẹ")
        assertNotNull(r1)
        assertEquals("trợ lý ơi", r1?.matchedKeyword)
        assertEquals("gọi cho mẹ", r1?.remainingCommand)

        val r2 = TroLyNoiWakeWordManager.parseWakeWordCommand("Trợ lý gọi cho mẹ")
        assertNotNull(r2)
        assertEquals("trợ lý", r2?.matchedKeyword)
        assertEquals("gọi cho mẹ", r2?.remainingCommand)

        val r5 = TroLyNoiWakeWordManager.parseWakeWordCommand("Trợ lý ơi đặt báo thức 7 giờ")
        assertNotNull(r5)
        assertEquals("trợ lý ơi", r5?.matchedKeyword)
        assertEquals("đặt báo thức 7 giờ", r5?.remainingCommand)

        val r6 = TroLyNoiWakeWordManager.parseWakeWordCommand("Trợ lý ơi nhắn tin cho mẹ")
        assertNotNull(r6)
        assertEquals("trợ lý ơi", r6?.matchedKeyword)
        assertEquals("nhắn tin cho mẹ", r6?.remainingCommand)

        // Đệm từ phía trước: "Này trợ lý ơi mở youtube"
        val r8 = TroLyNoiWakeWordManager.parseWakeWordCommand("Này trợ lý ơi mở youtube")
        assertNotNull(r8)
        assertEquals("mở youtube", r8?.remainingCommand)
    }

    @Test
    fun parseWakeWordCommand_removedVariants_noLongerMatchAlone() {
        // Các biến thể đã loại (không chứa đúng 「trợ lý」/「trợ lý ơi」như từ khóa chính)
        assertNull(TroLyNoiWakeWordManager.parseWakeWordCommand("vidroidcall"))
        assertNull(TroLyNoiWakeWordManager.parseWakeWordCommand("vidroidcall ơi"))
        assertNull(TroLyNoiWakeWordManager.parseWakeWordCommand("ViDroidCall ơi mở cài đặt"))
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

        val test2 = TroLyNoiWakeWordManager.parseWakeWordCommand("  trợ lí,   mở bản đồ!")
        assertNotNull(test2)
        assertEquals("mở bản đồ", test2?.remainingCommand)
    }
}
