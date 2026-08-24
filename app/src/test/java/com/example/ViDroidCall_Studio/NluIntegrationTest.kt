package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.data.model.NluIntent
import com.example.ViDroidCall_Studio.data.model.NluJsonParser
import com.example.ViDroidCall_Studio.data.model.NluStatus
import com.example.ViDroidCall_Studio.data.nlu.NluConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NluIntegrationTest {

    @Test
    fun testChatMlPromptBuilding() {
        val prompt = NluConstants.buildChatMlPrompt("Nhắn tin cho mẹ là con đang về rồi")
        assertTrue(prompt.contains("<|im_start|>system"))
        assertTrue(prompt.contains(NluConstants.MANDATORY_SYSTEM_PROMPT))
        assertTrue(prompt.contains("<|im_start|>user\nNhắn tin cho mẹ là con đang về rồi<|im_end|>"))
        assertTrue(prompt.endsWith("<|im_start|>assistant\n"))
    }

    @Test
    fun testJsonParsingExample1_SendSms() {
        val modelRawOutput = """
        ```json
        {
          "intent": "send_sms",
          "arguments": {
            "contact": "mẹ",
            "message": "con đang về rồi"
          },
          "risk_level": "medium",
          "status": "success",
          "requires_confirmation": true
        }
        ```
        """.trimIndent()

        val result = NluJsonParser.parse(modelRawOutput)
        assertTrue(result.isParsedSuccessfully)
        assertEquals("send_sms", result.intent)
        assertEquals(NluIntent.SEND_SMS, result.intentEnum)
        assertEquals("success", result.status)
        assertEquals(NluStatus.SUCCESS, result.statusEnum)
        assertEquals("medium", result.riskLevel)
        assertTrue(result.requiresConfirmation)
    }

    @Test
    fun testJsonParsingExample2_Clarify() {
        val modelRawOutput = """
        {
          "intent": "clarify",
          "arguments": {
            "missing": ["message"]
          },
          "risk_level": "medium",
          "status": "needs_clarification",
          "requires_confirmation": false
        }
        """.trimIndent()

        val result = NluJsonParser.parse(modelRawOutput)
        assertTrue(result.isParsedSuccessfully)
        assertEquals("clarify", result.intent)
        assertEquals(NluIntent.CLARIFY, result.intentEnum)
        assertEquals("needs_clarification", result.status)
        assertEquals(NluStatus.NEEDS_CLARIFICATION, result.statusEnum)
        assertFalse(result.requiresConfirmation)
    }

    @Test
    fun testJsonParsingExample3_InvalidAlarm() {
        val modelRawOutput = """
        {
          "intent": "set_alarm",
          "arguments": {
            "hour": 25,
            "minute": 70
          },
          "risk_level": "low",
          "status": "invalid",
          "requires_confirmation": false
        }
        """.trimIndent()

        val result = NluJsonParser.parse(modelRawOutput)
        assertTrue(result.isParsedSuccessfully)
        assertEquals("set_alarm", result.intent)
        assertEquals(NluIntent.SET_ALARM, result.intentEnum)
        assertEquals("invalid", result.status)
        assertEquals(NluStatus.INVALID, result.statusEnum)
        assertFalse(result.requiresConfirmation)
    }
}
