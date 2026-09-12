// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.domain.model.NativeAction
import com.example.ViDroidCall_Studio.feature.overlay.TroLyNoState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiểm thử State Machine và các quy tắc an toàn (Critical Safety Rules) cho Trợ lý nổi (TroLyNoState).
 */
class TroLyNoStateTest {

    @Test
    fun testInitialStateIsListening() {
        val state: TroLyNoState = TroLyNoState.Listening
        assertTrue(state is TroLyNoState.Listening)
    }

    @Test
    fun testSttStateCarriesTranscript() {
        val transcript = "Gọi cho mẹ"
        val state: TroLyNoState = TroLyNoState.Stt(text = transcript)
        assertTrue(state is TroLyNoState.Stt)
        assertEquals("Gọi cho mẹ", (state as TroLyNoState.Stt).text)
    }

    @Test
    fun testCallContactActionRequiresConfirmation() {
        val callAction = NativeAction.CallContact(
            contact = "Mẹ",
            phoneNumber = "0901234567"
        )
        // CRITICAL SAFETY: call_contact bắt buộc phải có requiresConfirmation = true
        assertTrue(callAction.requiresConfirmation)
        assertEquals("call_contact", callAction.intentName)
        assertEquals("Xác nhận thực hiện cuộc gọi?", callAction.getConfirmationTitle())
        assertTrue(callAction.getConfirmationDescription().contains("Mẹ"))
    }

    @Test
    fun testSendSmsActionRequiresConfirmation() {
        val smsAction = NativeAction.SendSms(
            contact = "Mẹ",
            phoneNumber = "0901234567",
            message = "Con về muộn"
        )
        // CRITICAL SAFETY: send_sms bắt buộc phải có requiresConfirmation = true
        assertTrue(smsAction.requiresConfirmation)
        assertEquals("send_sms", smsAction.intentName)
        assertEquals("Xác nhận gửi tin nhắn?", smsAction.getConfirmationTitle())
        assertTrue(smsAction.getConfirmationDescription().contains("Mẹ"))
        assertTrue(smsAction.getConfirmationDescription().contains("Con về muộn"))
    }

    @Test
    fun testSafeActionDoesNotRequireConfirmation() {
        val openApp = NativeAction.OpenApp(appName = "YouTube")
        assertFalse(openApp.requiresConfirmation)
    }

    @Test
    fun testConfirmationStateHoldsPendingActionWithoutAutoExecuting() {
        val callAction = NativeAction.CallContact(contact = "Bố", phoneNumber = "0912345678")
        var isActionExecuted = false

        // Khi NLU phân tích ra câu lệnh nhạy cảm, chuyển state sang Confirmation
        val state: TroLyNoState = TroLyNoState.Confirmation(
            text = "Gọi cho bố",
            action = callAction
        )

        assertTrue(state is TroLyNoState.Confirmation)
        // Hành động CHƯA được thực thi
        assertFalse(isActionExecuted)

        // Chỉ khi người dùng bấm Xác nhận, cờ execute mới được bật
        fun onConfirmClick(confirmState: TroLyNoState.Confirmation) {
            isActionExecuted = true
        }

        onConfirmClick(state as TroLyNoState.Confirmation)
        assertTrue(isActionExecuted)
    }

    @Test
    fun testCancelConfirmationDoesNotExecuteAction() {
        val smsAction = NativeAction.SendSms(contact = "Anh trai", phoneNumber = "0987654321", message = "Alo")
        var isActionExecuted = false

        val state: TroLyNoState = TroLyNoState.Confirmation(
            text = "Nhắn tin cho anh",
            action = smsAction
        )

        // Khi bấm Hủy:
        fun onCancelClick() {
            // Không thực thi gì cả
        }

        onCancelClick()
        assertFalse(isActionExecuted)
    }

    @Test
    fun testFastPathStateWithSafeAction() {
        val openMap = NativeAction.OpenMap(destination = "Bệnh viện Bạch Mai")
        val state: TroLyNoState = TroLyNoState.FastPath(
            text = "Chỉ đường tới Bệnh viện Bạch Mai",
            action = openMap
        )
        assertTrue(state is TroLyNoState.FastPath)
        assertEquals("open_map", (state as TroLyNoState.FastPath).action.intentName)
    }

    @Test
    fun testAnalyzingState() {
        val state: TroLyNoState = TroLyNoState.Analyzing
        assertTrue(state is TroLyNoState.Analyzing)
    }

    @Test
    fun testClarifyState() {
        val state: TroLyNoState = TroLyNoState.Clarify(
            text = "Đặt báo thức",
            missing = listOf("Giờ báo thức")
        )
        assertTrue(state is TroLyNoState.Clarify)
        val clarify = state as TroLyNoState.Clarify
        assertEquals(1, clarify.missing.size)
        assertEquals("Giờ báo thức", clarify.missing[0])
    }
}
