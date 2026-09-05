// SPDX-License-Identifier: Apache-2.0

package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.data.local.feedback.NluFeedbackEntry
import com.example.ViDroidCall_Studio.data.local.feedback.NluFeedbackLogRepository
import com.example.ViDroidCall_Studio.data.model.NluJsonParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NluFeedbackLogRepositoryTest {

    @Test
    fun buildFeedbackLine_containsSttTextAndModelOutput() {
        val nluResult = NluJsonParser.parse(
            """
            {
              "intent": "call_contact",
              "arguments": {"contact": "đt chồng"},
              "risk_level": "medium",
              "status": "success",
              "requires_confirmation": true
            }
            """.trimIndent()
        )

        val line = NluFeedbackLogRepository.buildFeedbackLine("gọi cho đt chồng", nluResult)
        val json = JSONObject(line)

        assertEquals("gọi cho đt chồng", json.getString("stt_text"))
        assertEquals("call_contact", json.getJSONObject("model_output").getString("intent"))
        assertNotNull(json.getLong("saved_at"))
    }

    @Test
    fun fromJsonLine_parsesSavedEntry() {
        val line = """{"stt_text":"dt chong","model_output":{"intent":"call_contact"},"saved_at":123}"""
        val entry = NluFeedbackEntry.fromJsonLine(0, line)

        assertNotNull(entry)
        assertEquals("dt chong", entry?.sttText)
        assertEquals("call_contact", JSONObject(entry?.modelOutputJson ?: "{}").getString("intent"))
    }
}
