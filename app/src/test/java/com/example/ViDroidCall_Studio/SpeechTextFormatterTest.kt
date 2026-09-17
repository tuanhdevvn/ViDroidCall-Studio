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
