// SPDX-License-Identifier: Apache-2.0

package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.feature.speech.SpeechTextFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechTextFormatterTest {

    @Test
    fun convertsAllCapsToSentenceCase() {
        assertEquals("Gọi cho mẹ", SpeechTextFormatter.formatDisplay("GỌI CHO MẸ"))
        assertEquals("Mở youtube", SpeechTextFormatter.formatDisplay("MỞ YOUTUBE"))
    }

    @Test
    fun capitalizesEachSentence() {
        assertEquals("Xin chào. Gọi cho mẹ", SpeechTextFormatter.formatDisplay("XIN CHÀO. GỌI CHO MẸ"))
    }

    @Test
    fun preservesNumbersAndNormalizedText() {
        assertEquals("Hẹn giờ 15 phút", SpeechTextFormatter.formatDisplay("HẸN GIỜ 15 PHÚT"))
        assertEquals("Gọi 113", SpeechTextFormatter.formatDisplay("GỌI 113"))
    }

    @Test
    fun handlesVietnameseD() {
        assertEquals("Đặt báo thức 6 giờ", SpeechTextFormatter.formatDisplay("ĐẶT BÁO THỨC 6 GIỜ"))
    }
}
