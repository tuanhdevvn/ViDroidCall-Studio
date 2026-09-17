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

import com.example.ViDroidCall_Studio.data.model.NluResult
import com.example.ViDroidCall_Studio.domain.model.NativeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TroLyNoiActionConfirmationTest {

    @Test
    fun nativeAction_callContact_generatesCorrectConfirmationInfo() {
        val action = NativeAction.CallContact(
            contact = "mẹ",
            phoneNumber = "0912345678"
        )

        assertEquals("Xác nhận cuộc gọi?", action.getActionTitle())
        assertEquals("Gọi tới mẹ", action.getActionSummary())
        assertEquals("CALL", action.getActionIconType())
        assertTrue(action.getConfirmationDescription().contains("mẹ"))
        assertTrue(action.getConfirmationDescription().contains("0912345678"))
    }

    @Test
    fun nativeAction_sendSms_generatesCorrectConfirmationInfo() {
        val action = NativeAction.SendSms(
            contact = "bố",
            phoneNumber = "0987654321",
            message = "Con đang về nhà"
        )

        assertEquals("Xác nhận gửi tin nhắn?", action.getActionTitle())
        assertEquals("Gửi tin nhắn tới bố: “Con đang về nhà”", action.getActionSummary())
        assertEquals("SMS", action.getActionIconType())
        assertTrue(action.getConfirmationDescription().contains("Con đang về nhà"))
    }

    @Test
    fun nativeAction_openApp_generatesCorrectConfirmationInfo() {
        val action = NativeAction.OpenApp(
            appName = "youtube"
        )

        assertEquals("Xác nhận mở ứng dụng?", action.getActionTitle())
        assertEquals("Mở ứng dụng YouTube", action.getActionSummary())
        assertEquals("OPEN_APP", action.getActionIconType())
        assertTrue(action.getConfirmationDescription().contains("youtube"))
    }

    @Test
    fun nativeAction_setAlarm_generatesCorrectConfirmationInfo() {
        val action = NativeAction.SetAlarm(
            hour = 7,
            minute = 30,
            label = "Dậy đi làm"
        )

        assertEquals("Xác nhận đặt báo thức?", action.getActionTitle())
        assertEquals("Đặt báo thức lúc 7 giờ 30 phút", action.getActionSummary())
        assertEquals("ALARM", action.getActionIconType())
        assertTrue(action.getConfirmationDescription().contains("7 giờ 30 phút"))
    }

    @Test
    fun nativeAction_setTimer_generatesCorrectConfirmationInfo() {
        val action = NativeAction.SetTimer(
            durationSeconds = 900,
            displayDuration = 15,
            unitText = "phút",
            label = "Nấu ăn"
        )

        assertEquals("Xác nhận hẹn giờ?", action.getActionTitle())
        assertEquals("Hẹn giờ 15 phút", action.getActionSummary())
        assertEquals("TIMER", action.getActionIconType())
        assertTrue(action.getConfirmationDescription().contains("15 phút"))
    }

    @Test
    fun nativeAction_openMap_generatesCorrectConfirmationInfo() {
        val action = NativeAction.OpenMap(
            destination = "bệnh viện Bạch Mai"
        )

        assertEquals("Xác nhận mở bản đồ?", action.getActionTitle())
        assertEquals("Mở bản đồ tới bệnh viện Bạch Mai", action.getActionSummary())
        assertEquals("MAP", action.getActionIconType())
        assertTrue(action.getConfirmationDescription().contains("bệnh viện Bạch Mai"))
    }

    @Test
    fun nativeAction_searchVideo_generatesCorrectConfirmationInfo() {
        val action = NativeAction.SearchVideo(
            query = "nhạc trẻ remix"
        )

        assertEquals("Xác nhận tìm video?", action.getActionTitle())
        assertEquals("Tìm kiếm video “nhạc trẻ remix” trên YouTube", action.getActionSummary())
        assertEquals("YOUTUBE", action.getActionIconType())
    }

    @Test
    fun nativeAction_searchWeb_generatesCorrectConfirmationInfo() {
        val action = NativeAction.SearchWeb(
            query = "thời tiết hôm nay"
        )

        assertEquals("Xác nhận tìm kiếm?", action.getActionTitle())
        assertEquals("Tìm kiếm “thời tiết hôm nay” trên Google", action.getActionSummary())
        assertEquals("SEARCH", action.getActionIconType())
    }

    @Test
    fun nativeAction_playMusic_generatesCorrectConfirmationInfo() {
        val action = NativeAction.PlayMusic(
            songName = "Lạc Trôi",
            artist = "Sơn Tùng",
            genre = "",
            musicQuery = "Lạc Trôi Sơn Tùng"
        )

        assertEquals("Xác nhận phát nhạc?", action.getActionTitle())
        assertEquals("Phát “Lạc Trôi Sơn Tùng”", action.getActionSummary())
        assertEquals("MUSIC", action.getActionIconType())
    }

    @Test
    fun assistantOverlayData_confirmAction_invokesCallbacksProperly() {
        var confirmed = false
        var cancelled = false

        val data = AssistantOverlayData(
            state = AssistantOverlayState.CONFIRM_ACTION,
            recognizedText = "Mở YouTube",
            actionTitle = "Xác nhận mở ứng dụng?",
            actionDescription = "Mở ứng dụng YouTube",
            actionIconType = OverlayActionIconType.OPEN_APP,
            onConfirm = { confirmed = true },
            onCancel = { cancelled = true }
        )

        assertEquals(AssistantOverlayState.CONFIRM_ACTION, data.state)
        assertEquals("Mở YouTube", data.recognizedText)
        assertEquals("Xác nhận mở ứng dụng?", data.actionTitle)
        assertEquals("Mở ứng dụng YouTube", data.actionDescription)
        assertEquals(OverlayActionIconType.OPEN_APP, data.actionIconType)

        assertNotNull(data.onConfirm)
        data.onConfirm?.invoke()
        assertTrue("Callback onConfirm phải được kích hoạt", confirmed)

        assertNotNull(data.onCancel)
        data.onCancel?.invoke()
        assertTrue("Callback onCancel phải được kích hoạt", cancelled)
    }

    @Test
    fun assistantOverlayData_conversationalReply_invokesContinueListening() {
        var continued = false

        val data = AssistantOverlayData(
            state = AssistantOverlayState.CONVERSATIONAL_REPLY,
            recognizedText = "xin chào",
            actionTitle = "Trợ lý phản hồi",
            actionDescription = "Xin chào! Tôi có thể giúp gì cho bạn?",
            continueButtonLabel = "Nói tiếp",
            onContinueListening = { continued = true }
        )

        assertEquals(AssistantOverlayState.CONVERSATIONAL_REPLY, data.state)
        assertEquals("Nói tiếp", data.continueButtonLabel)
        assertNotNull(data.onContinueListening)
        data.onContinueListening?.invoke()
        assertTrue("Callback onContinueListening phải được kích hoạt", continued)
    }

    @Test
    fun assistantOverlayData_goodbye_usesCloseLabel() {
        val data = AssistantOverlayData(
            state = AssistantOverlayState.CONVERSATIONAL_REPLY,
            recognizedText = "tạm biệt",
            actionTitle = "Trợ lý phản hồi",
            actionDescription = "Tạm biệt và hẹn gặp lại!",
            continueButtonLabel = "Đóng",
            onContinueListening = {}
        )

        assertEquals("Đóng", data.continueButtonLabel)
        assertEquals(AssistantOverlayState.CONVERSATIONAL_REPLY, data.state)
    }

    @Test
    fun nativeAction_fromNluResult_createsExpectedAction() {
        val nluResult = NluResult(
            rawJson = """{"intent": "open_app", "arguments": {"app_name": "zalo"}}""",
            intent = "open_app",
            status = "success",
            riskLevel = "low",
            requiresConfirmation = false,
            argumentsJson = """{"app_name": "zalo"}""",
            slots = mapOf("app_name" to "zalo")
        )

        val action = NativeAction.fromNluResult(nluResult)
        assertTrue(action is NativeAction.OpenApp)
        val openApp = action as NativeAction.OpenApp
        assertEquals("zalo", openApp.appName)
        assertEquals("Mở ứng dụng Zalo", openApp.getActionSummary())
        assertEquals("OPEN_APP", openApp.getActionIconType())
    }
}
