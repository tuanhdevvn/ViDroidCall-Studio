// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiểm tra tính độc lập giữa 2 kênh kích hoạt overlay:
 * Kênh 1: Hands-free Wake Word ("Trợ lý ơi" / "Trợ lý")
 * Kênh 2: Nút nhanh trên thông báo foreground ("Nói câu lệnh")
 */
class SettingsIndependentStateTest {

    enum class ActivationChannelState {
        IDLE,
        WAKE_WORD_LISTENING,
        WAKE_WORD_DETECTED,
        ASSISTANT_LISTENING,
        PROCESSING,
        RESULT
    }

    class DualActivationCoordinator(
        var masterSwitchEnabled: Boolean = false,
        var isScreenInteractiveAndUnlocked: Boolean = true
    ) {
        val wakeWordSwitchEnabled: Boolean
            get() = masterSwitchEnabled

        var currentState: ActivationChannelState = ActivationChannelState.IDLE
            private set

        var isWakeWordMicActive: Boolean = false
            private set
        var isAssistantMicActive: Boolean = false
            private set

        fun evaluateWakeWordState() {
            if (masterSwitchEnabled && isScreenInteractiveAndUnlocked && !isAssistantMicActive) {
                isWakeWordMicActive = true
                currentState = ActivationChannelState.WAKE_WORD_LISTENING
            } else {
                isWakeWordMicActive = false
                if (currentState == ActivationChannelState.WAKE_WORD_LISTENING) {
                    currentState = ActivationChannelState.IDLE
                }
            }
        }

        fun onWakeWordDetected(hasCommand: Boolean) {
            if (!isWakeWordMicActive) return
            isWakeWordMicActive = false
            currentState = ActivationChannelState.WAKE_WORD_DETECTED

            if (hasCommand) {
                currentState = ActivationChannelState.PROCESSING
            } else {
                startAssistantListening()
            }
        }

        /** Nút nhanh trên thông báo — chỉ khi master switch bật. */
        fun onNotificationQuickAction() {
            if (!masterSwitchEnabled) return
            if (isWakeWordMicActive) {
                isWakeWordMicActive = false
            }
            startAssistantListening()
        }

        private fun startAssistantListening() {
            isAssistantMicActive = true
            currentState = ActivationChannelState.ASSISTANT_LISTENING
        }

        fun onAssistantDone() {
            isAssistantMicActive = false
            currentState = ActivationChannelState.IDLE
            evaluateWakeWordState()
        }
    }

    @Test
    fun testNotificationRequiresMasterSwitch() {
        val coordinator = DualActivationCoordinator(masterSwitchEnabled = false)

        coordinator.evaluateWakeWordState()
        assertFalse(coordinator.isWakeWordMicActive)

        coordinator.onNotificationQuickAction()
        assertFalse(
            "Nút thông báo không mở overlay khi master switch tắt",
            coordinator.isAssistantMicActive
        )
        assertEquals(ActivationChannelState.IDLE, coordinator.currentState)

        coordinator.masterSwitchEnabled = true
        coordinator.onNotificationQuickAction()
        assertTrue(coordinator.isAssistantMicActive)
        assertEquals(ActivationChannelState.ASSISTANT_LISTENING, coordinator.currentState)
    }

    @Test
    fun testWakeWordStateMachineAndNoMicConflict() {
        val coordinator = DualActivationCoordinator(masterSwitchEnabled = true)

        coordinator.evaluateWakeWordState()
        assertTrue(coordinator.isWakeWordMicActive)
        assertEquals(ActivationChannelState.WAKE_WORD_LISTENING, coordinator.currentState)

        coordinator.onWakeWordDetected(hasCommand = false)
        assertFalse("Wake Word mic phải giải phóng ngay lập tức", coordinator.isWakeWordMicActive)
        assertTrue("Assistant mic phải mở", coordinator.isAssistantMicActive)
        assertEquals(ActivationChannelState.ASSISTANT_LISTENING, coordinator.currentState)

        coordinator.onAssistantDone()
        assertFalse(coordinator.isAssistantMicActive)
        assertTrue("Wake Word mic phải resume lại sau khi Overlay đóng", coordinator.isWakeWordMicActive)
        assertEquals(ActivationChannelState.WAKE_WORD_LISTENING, coordinator.currentState)
    }

    @Test
    fun testScreenOffAndLockPausesWakeWord() {
        val coordinator = DualActivationCoordinator(
            masterSwitchEnabled = true,
            isScreenInteractiveAndUnlocked = true
        )

        coordinator.evaluateWakeWordState()
        assertTrue(coordinator.isWakeWordMicActive)

        coordinator.isScreenInteractiveAndUnlocked = false
        coordinator.evaluateWakeWordState()
        assertFalse("Wake Word mic phải tạm dừng khi màn hình tắt hoặc khóa", coordinator.isWakeWordMicActive)
        assertEquals(ActivationChannelState.IDLE, coordinator.currentState)

        coordinator.isScreenInteractiveAndUnlocked = true
        coordinator.evaluateWakeWordState()
        assertTrue(coordinator.isWakeWordMicActive)
        assertEquals(ActivationChannelState.WAKE_WORD_LISTENING, coordinator.currentState)
    }

    @Test
    fun testMasterSwitchOffStopsEverything() {
        val coordinator = DualActivationCoordinator(masterSwitchEnabled = true)

        coordinator.evaluateWakeWordState()
        assertTrue(coordinator.isWakeWordMicActive)

        coordinator.masterSwitchEnabled = false
        coordinator.evaluateWakeWordState()
        assertFalse(coordinator.isWakeWordMicActive)
        assertEquals(ActivationChannelState.IDLE, coordinator.currentState)
    }
}
