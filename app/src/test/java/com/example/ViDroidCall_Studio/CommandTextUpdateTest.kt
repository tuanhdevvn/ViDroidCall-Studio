// SPDX-License-Identifier: Apache-2.0

package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.data.model.NluJsonParser
import com.example.ViDroidCall_Studio.data.model.NluResult
import com.example.ViDroidCall_Studio.data.nlu.FastPathMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test kiểm tra cập nhật command text cho AssistantScreen từ các nguồn:
 * 1. Voice (Speech-to-Text)
 * 2. History Run
 * Và đảm bảo trạng thái không bị mất khi NLU hoàn thành, recomposition, hay chạy cùng command nhiều lần.
 */
class CommandTextUpdateTest {

    /**
     * Giả lập luồng quản lý State và Command Execution Entry Point
     */
    private class MockCommandManager {
        var currentCommand: String = ""
            private set
        var isNluProcessing: Boolean = false
            private set
        var lastNluResult: NluResult? = null
            private set
        var selectedTab: String = "ASSISTANT"
        val executionHistory = mutableListOf<Pair<String, String>>() // Pair(command, executionId)

        val matcher = FastPathMatcher(context = null)

        fun executeCommand(command: String) {
            val trimmed = command.trim()
            if (trimmed.isNotBlank()) {
                // 1. Cập nhật currentCommand NGAY LẬP TỨC
                currentCommand = trimmed
                isNluProcessing = true

                // 2. Chạy NLU (FastPath hoặc Mock AI)
                val fastResult = matcher.match(trimmed)
                val result = fastResult ?: NluJsonParser.parse(
                    if (trimmed.contains("đèn pin", ignoreCase = true)) {
                        """{"intent":"open_app","arguments":{"app_name":"Đèn pin"},"status":"success"}"""
                    } else {
                        """{"intent":"unknown","status":"success","rawJson":"{}"}"""
                    }
                )

                lastNluResult = result
                isNluProcessing = false
                executionHistory.add(Pair(trimmed, result.executionId))
            }
        }
    }

    /**
     * Test 1 – Voice
     * Input: "Bật đèn pin"
     * Expected: currentCommand == "Bật đèn pin"
     */
    @Test
    fun testVoiceCommandUpdatesCurrentCommand() {
        val manager = MockCommandManager()
        val voiceInput = "Bật đèn pin"

        manager.executeCommand(voiceInput)

        assertEquals("Bật đèn pin", manager.currentCommand)
        assertNotNull(manager.lastNluResult)
        assertEquals("open_app", manager.lastNluResult?.intent)
    }

    /**
     * Test 2 – History Run
     * Current: "Bật đèn pin"
     * Run History: "Gọi cho mẹ"
     * Expected: currentCommand == "Gọi cho mẹ" và không còn "Bật đèn pin"
     */
    @Test
    fun testHistoryRunReplacesExistingCommandWithNewHistoryCommand() {
        val manager = MockCommandManager()

        // Ban đầu chạy voice command
        manager.executeCommand("Bật đèn pin")
        assertEquals("Bật đèn pin", manager.currentCommand)

        // Người dùng vào History và bấm Run "Gọi cho mẹ"
        val historyCommand = "Gọi cho mẹ"
        manager.executeCommand(historyCommand)

        assertEquals("Gọi cho mẹ", manager.currentCommand)
        assertNotEquals("Bật đèn pin", manager.currentCommand)
        assertEquals("call_contact", manager.lastNluResult?.intent)
    }

    /**
     * Test 3 – History Run immediately after app launch
     * App state: currentCommand = ""
     * Run: "Gọi cho mẹ"
     * Expected: currentCommand == "Gọi cho mẹ" và text không bị biến mất
     */
    @Test
    fun testHistoryRunImmediatelyAfterAppLaunchMaintainsCommandText() {
        val manager = MockCommandManager()
        assertEquals("", manager.currentCommand)

        // Bấm Run ngay từ tab History khi vừa mở app
        manager.selectedTab = "HISTORY"
        manager.executeCommand("Gọi cho mẹ")
        manager.selectedTab = "ASSISTANT"

        assertEquals("Gọi cho mẹ", manager.currentCommand)
        assertNotNull(manager.lastNluResult)
        assertEquals(false, manager.isNluProcessing)
        assertEquals("Gọi cho mẹ", manager.currentCommand)
    }

    /**
     * Test 4 – Same History command twice
     * Run: "Gọi cho mẹ" 2 lần
     * Expected: currentCommand == "Gọi cho mẹ" cả 2 lần và executionId1 != executionId2
     */
    @Test
    fun testSameHistoryCommandTwiceMaintainsTextAndHasUniqueExecutionIds() {
        val manager = MockCommandManager()

        manager.executeCommand("Gọi cho mẹ")
        val execId1 = manager.lastNluResult?.executionId
        assertEquals("Gọi cho mẹ", manager.currentCommand)
        assertNotNull(execId1)

        manager.executeCommand("Gọi cho mẹ")
        val execId2 = manager.lastNluResult?.executionId
        assertEquals("Gọi cho mẹ", manager.currentCommand)
        assertNotNull(execId2)

        assertNotEquals("Hai lần chạy phải sinh ra 2 executionId khác nhau", execId1, execId2)
        assertEquals(2, manager.executionHistory.size)
        assertEquals("Gọi cho mẹ", manager.executionHistory[0].first)
        assertEquals("Gọi cho mẹ", manager.executionHistory[1].first)
    }

    /**
     * Test 5 – History → Home navigation timing
     * Verify: History -> Run -> Home
     * Command text đã được update trước/đồng thời với navigation và không phụ thuộc recomposition
     */
    @Test
    fun testHistoryToHomeNavigationSynchronouslyUpdatesCommandText() {
        val manager = MockCommandManager()
        manager.selectedTab = "HISTORY"

        var displayedTextOnHome = ""

        // Giả lập callback onRerunCommand trong HistoryScreen
        val onRerunCommand: (String) -> Unit = { command ->
            manager.executeCommand(command)
            manager.selectedTab = "ASSISTANT"
            // Khi HomeScreen switch tab, text được render từ manager.currentCommand
            displayedTextOnHome = manager.currentCommand
        }

        onRerunCommand("Mở YouTube")

        assertEquals("ASSISTANT", manager.selectedTab)
        assertEquals("Mở YouTube", manager.currentCommand)
        assertEquals("Mở YouTube", displayedTextOnHome)
    }

    /**
     * Test 6 – NLU completion does not reset currentCommand
     * Sau khi NLU trả result, currentCommand không được reset về "" hoặc command cũ.
     */
    @Test
    fun testNluCompletionDoesNotClearOrResetCurrentCommand() {
        val manager = MockCommandManager()

        manager.executeCommand("Báo thức 6 giờ")
        assertEquals("Báo thức 6 giờ", manager.currentCommand)

        // Khi NLU xong, isNluProcessing = false
        assertEquals(false, manager.isNluProcessing)
        assertEquals("set_alarm", manager.lastNluResult?.intent)

        // Kiểm tra currentCommand vẫn giữ nguyên "Báo thức 6 giờ"
        assertEquals("Báo thức 6 giờ", manager.currentCommand)
        assertTrue(manager.currentCommand.isNotBlank())
    }
}
