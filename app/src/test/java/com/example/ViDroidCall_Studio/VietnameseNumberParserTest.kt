// SPDX-License-Identifier: Apache-2.0

package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.data.nlu.VietnameseNumberParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VietnameseNumberParserTest {

    @Test
    fun testZeroAndHalf() {
        assertEquals(0, VietnameseNumberParser.parse("0"))
        assertEquals(0, VietnameseNumberParser.parse("không"))
        assertEquals(0, VietnameseNumberParser.parse("KHÔNG"))
        assertEquals(30, VietnameseNumberParser.parse("nửa"))
        assertEquals(30, VietnameseNumberParser.parse("nua"))
    }

    @Test
    fun testDirectDigits() {
        assertEquals(0, VietnameseNumberParser.parse("0"))
        assertEquals(5, VietnameseNumberParser.parse("5"))
        assertEquals(15, VietnameseNumberParser.parse("15"))
        assertEquals(25, VietnameseNumberParser.parse("25"))
        assertEquals(120, VietnameseNumberParser.parse("120"))
        assertEquals(999, VietnameseNumberParser.parse("999"))
    }

    @Test
    fun testSingleDigits() {
        assertEquals(0, VietnameseNumberParser.parse("không"))
        assertEquals(1, VietnameseNumberParser.parse("một"))
        assertEquals(2, VietnameseNumberParser.parse("hai"))
        assertEquals(3, VietnameseNumberParser.parse("ba"))
        assertEquals(4, VietnameseNumberParser.parse("bốn"))
        assertEquals(4, VietnameseNumberParser.parse("tư"))
        assertEquals(5, VietnameseNumberParser.parse("năm"))
        assertEquals(5, VietnameseNumberParser.parse("lăm"))
        assertEquals(6, VietnameseNumberParser.parse("sáu"))
        assertEquals(7, VietnameseNumberParser.parse("bảy"))
        assertEquals(8, VietnameseNumberParser.parse("tám"))
        assertEquals(9, VietnameseNumberParser.parse("chín"))
    }

    @Test
    fun testNormalizationAndCaseHyphens() {
        assertEquals(25, VietnameseNumberParser.parse("Hai Mươi Lăm"))
        assertEquals(25, VietnameseNumberParser.parse("hai  mươi   lăm"))
        assertEquals(25, VietnameseNumberParser.parse("hai-mươi-lăm"))
        assertEquals(25, VietnameseNumberParser.parse("HAI MƯƠI LĂM"))
    }

    @Test
    fun testTensAndComposites() {
        assertEquals(10, VietnameseNumberParser.parse("mười"))
        assertEquals(11, VietnameseNumberParser.parse("mười một"))
        assertEquals(12, VietnameseNumberParser.parse("mười hai"))
        assertEquals(15, VietnameseNumberParser.parse("mười lăm"))
        assertEquals(20, VietnameseNumberParser.parse("hai mươi"))
        assertEquals(21, VietnameseNumberParser.parse("hai mươi mốt"))
        assertEquals(22, VietnameseNumberParser.parse("hai mươi hai"))
        assertEquals(24, VietnameseNumberParser.parse("hai mươi tư"))
        assertEquals(24, VietnameseNumberParser.parse("hai mươi bốn"))
        assertEquals(25, VietnameseNumberParser.parse("hai mươi lăm"))
        assertEquals(35, VietnameseNumberParser.parse("ba mươi lăm"))
        assertEquals(99, VietnameseNumberParser.parse("chín mươi chín"))
    }

    @Test
    fun testHundreds() {
        assertEquals(100, VietnameseNumberParser.parse("một trăm"))
        assertEquals(105, VietnameseNumberParser.parse("một trăm linh năm"))
        assertEquals(105, VietnameseNumberParser.parse("một trăm lẻ năm"))
        assertEquals(120, VietnameseNumberParser.parse("một trăm hai mươi"))
        assertEquals(125, VietnameseNumberParser.parse("một trăm hai mươi lăm"))
        assertEquals(210, VietnameseNumberParser.parse("hai trăm mười"))
        assertEquals(215, VietnameseNumberParser.parse("hai trăm mười lăm"))
        assertEquals(230, VietnameseNumberParser.parse("hai trăm ba mươi"))
        assertEquals(999, VietnameseNumberParser.parse("chín trăm chín mươi chín"))
    }

    @Test
    fun testInvalidNonsenseSequencesReturnNull() {
        assertNull(VietnameseNumberParser.parse("hai ba"))
        assertNull(VietnameseNumberParser.parse("một hai ba"))
        assertNull(VietnameseNumberParser.parse("hai năm mười"))
        assertNull(VietnameseNumberParser.parse("mười mười"))
        assertNull(VietnameseNumberParser.parse("hai trăm linh linh"))
    }
}
