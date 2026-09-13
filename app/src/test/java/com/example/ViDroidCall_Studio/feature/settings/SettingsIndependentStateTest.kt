package com.example.ViDroidCall_Studio.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiểm tra tính độc lập giữa 2 kênh kích hoạt:
 * Kênh 1: Hands-free Wake Word ("Trợ lý ơi")
 * Kênh 2: Default Digital Assistant cấp hệ thống (VoiceInteractionService)
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
        var wakeWordSwitchEnabled: Boolean = false,
        var isSystemDefaultAssistant: Boolean = false,
        var isScreenInteractiveAndUnlocked: Boolean = true
    ) {
        var currentState: ActivationChannelState = ActivationChannelState.IDLE
            private set

        var isWakeWordMicActive: Boolean = false
            private set
        var isAssistantMicActive: Boolean = false
            private set

        fun evaluateWakeWordState() {
            if (masterSwitchEnabled && wakeWordSwitchEnabled && isScreenInteractiveAndUnlocked && !isAssistantMicActive) {
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
            // Release Wake Word mic immediately
            isWakeWordMicActive = false
            currentState = ActivationChannelState.WAKE_WORD_DETECTED

            if (hasCommand) {
                currentState = ActivationChannelState.PROCESSING
            } else {
                startAssistantListening()
            }
        }

        fun onDefaultAssistantTriggered() {
            // Can be triggered even if wakeWordSwitchEnabled is false!
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
    fun testTwoChannelsAreIndependent() {
        val coordinator = DualActivationCoordinator(
            masterSwitchEnabled = true,
            wakeWordSwitchEnabled = false,
            isSystemDefaultAssistant = true
        )

        // 1. Wake word is OFF -> Wake Word mic should NOT be active
        coordinator.evaluateWakeWordState()
        assertFalse("Wake Word mic không được chạy khi wakeWordSwitchEnabled=false", coordinator.isWakeWordMicActive)
        assertEquals(ActivationChannelState.IDLE, coordinator.currentState)

        // 2. Default Assistant can still be triggered by hardware key/gesture
        coordinator.onDefaultAssistantTriggered()
        assertTrue("Assistant overlay mic phải bật khi trigger qua Default Assistant", coordinator.isAssistantMicActive)
        assertFalse("Wake Word mic tuyệt đối không được chạy song song", coordinator.isWakeWordMicActive)
        assertEquals(ActivationChannelState.ASSISTANT_LISTENING, coordinator.currentState)

        // 3. Finish Assistant session
        coordinator.onAssistantDone()
        assertFalse(coordinator.isAssistantMicActive)
        assertFalse(coordinator.isWakeWordMicActive)
        assertEquals(ActivationChannelState.IDLE, coordinator.currentState)
    }

    @Test
    fun testWakeWordStateMachineAndNoMicConflict() {
        val coordinator = DualActivationCoordinator(
            masterSwitchEnabled = true,
            wakeWordSwitchEnabled = true,
            isSystemDefaultAssistant = false
        )

        // 1. Initial evaluate
        coordinator.evaluateWakeWordState()
        assertTrue(coordinator.isWakeWordMicActive)
        assertEquals(ActivationChannelState.WAKE_WORD_LISTENING, coordinator.currentState)

        // 2. Wake word detected without trailing command -> transitions to ASSISTANT_LISTENING
        coordinator.onWakeWordDetected(hasCommand = false)
        assertFalse("Wake Word mic phải giải phóng ngay lập tức", coordinator.isWakeWordMicActive)
        assertTrue("Assistant mic phải mở", coordinator.isAssistantMicActive)
        assertEquals(ActivationChannelState.ASSISTANT_LISTENING, coordinator.currentState)

        // 3. User finishes speaking
        coordinator.onAssistantDone()
        assertFalse("Assistant mic phải giải phóng", coordinator.isAssistantMicActive)
        assertTrue("Wake Word mic phải resume lại sau khi Overlay đóng", coordinator.isWakeWordMicActive)
        assertEquals(ActivationChannelState.WAKE_WORD_LISTENING, coordinator.currentState)
    }

    @Test
    fun testScreenOffAndLockPausesWakeWord() {
        val coordinator = DualActivationCoordinator(
            masterSwitchEnabled = true,
            wakeWordSwitchEnabled = true,
            isSystemDefaultAssistant = true,
            isScreenInteractiveAndUnlocked = true
        )

        coordinator.evaluateWakeWordState()
        assertTrue(coordinator.isWakeWordMicActive)

        // Screen turns OFF / Device Locked
        coordinator.isScreenInteractiveAndUnlocked = false
        coordinator.evaluateWakeWordState()
        assertFalse("Wake Word mic phải tạm dừng khi màn hình tắt hoặc khóa", coordinator.isWakeWordMicActive)
        assertEquals(ActivationChannelState.IDLE, coordinator.currentState)

        // Device Unlocks
        coordinator.isScreenInteractiveAndUnlocked = true
        coordinator.evaluateWakeWordState()
        assertTrue("Wake Word mic phải tự động khôi phục sau khi mở khóa", coordinator.isWakeWordMicActive)
        assertEquals(ActivationChannelState.WAKE_WORD_LISTENING, coordinator.currentState)
    }

    @Test
    fun testMasterSwitchOffStopsEverything() {
        val coordinator = DualActivationCoordinator(
            masterSwitchEnabled = true,
            wakeWordSwitchEnabled = true,
            isSystemDefaultAssistant = true
        )

        coordinator.evaluateWakeWordState()
        assertTrue(coordinator.isWakeWordMicActive)

        // Master switch turned OFF
        coordinator.masterSwitchEnabled = false
        coordinator.evaluateWakeWordState()
        assertFalse("Khi Master switch OFF, mic ngầm phải dừng hoàn toàn", coordinator.isWakeWordMicActive)
        assertEquals(ActivationChannelState.IDLE, coordinator.currentState)
    }
}
