package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.data.nlu.VietnameseNumberParser
import org.junit.Assert.assertEquals
import org.junit.Test

class VietnameseNumberParserTest {

    @Test
    fun testDigits() {
        assertEquals(0, VietnameseNumberParser.parse("0"))
        assertEquals(6, VietnameseNumberParser.parse("6"))
        assertEquals(15, VietnameseNumberParser.parse("15"))
        assertEquals(20, VietnameseNumberParser.parse("20"))
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
    fun testUnaccentedSingleDigits() {
        assertEquals(6, VietnameseNumberParser.parse("sau"))
        assertEquals(7, VietnameseNumberParser.parse("bay"))
        assertEquals(8, VietnameseNumberParser.parse("tam"))
        assertEquals(9, VietnameseNumberParser.parse("chin"))
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
        assertEquals(30, VietnameseNumberParser.parse("ba mươi"))
        assertEquals(35, VietnameseNumberParser.parse("ba mươi lăm"))
    }

    @Test
    fun testUnaccentedTensAndComposites() {
        assertEquals(10, VietnameseNumberParser.parse("muoi"))
        assertEquals(11, VietnameseNumberParser.parse("muoi mot"))
        assertEquals(15, VietnameseNumberParser.parse("muoi lam"))
        assertEquals(20, VietnameseNumberParser.parse("hai muoi"))
        assertEquals(21, VietnameseNumberParser.parse("hai muoi mot"))
        assertEquals(25, VietnameseNumberParser.parse("hai muoi lam"))
        assertEquals(24, VietnameseNumberParser.parse("hai muoi tu"))
    }

    @Test
    fun testHundreds() {
        assertEquals(100, VietnameseNumberParser.parse("một trăm"))
        assertEquals(105, VietnameseNumberParser.parse("một trăm linh năm"))
        assertEquals(105, VietnameseNumberParser.parse("một trăm lẻ năm"))
        assertEquals(210, VietnameseNumberParser.parse("hai trăm mười"))
        assertEquals(230, VietnameseNumberParser.parse("hai trăm ba mươi"))
    }
}
