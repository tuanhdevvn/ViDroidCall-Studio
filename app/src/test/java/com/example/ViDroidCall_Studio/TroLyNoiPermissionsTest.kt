// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.util.TroLyNoiPermissions
import org.junit.Assert.assertEquals
import org.junit.Test

class TroLyNoiPermissionsTest {

    @Test
    fun nextStepIsRecordAudioWhenMicMissing() {
        val gate = TroLyNoiPermissions.nextMissingPermission(
            hasRecordAudio = false,
            notificationsRequired = true,
            hasNotifications = false,
            canDrawOverlays = false,
        )
        assertEquals(TroLyNoiPermissions.Gate.RECORD_AUDIO, gate)
    }

    @Test
    fun nextStepIsNotificationsAfterMicOnApi33() {
        val gate = TroLyNoiPermissions.nextMissingPermission(
            hasRecordAudio = true,
            notificationsRequired = true,
            hasNotifications = false,
            canDrawOverlays = false,
        )
        assertEquals(TroLyNoiPermissions.Gate.POST_NOTIFICATIONS, gate)
    }

    @Test
    fun nextStepSkipsNotificationsBelowApi33() {
        val gate = TroLyNoiPermissions.nextMissingPermission(
            hasRecordAudio = true,
            notificationsRequired = false,
            hasNotifications = false,
            canDrawOverlays = false,
        )
        assertEquals(TroLyNoiPermissions.Gate.SYSTEM_ALERT_WINDOW, gate)
    }

    @Test
    fun nextStepIsNoneWhenAllGranted() {
        val gate = TroLyNoiPermissions.nextMissingPermission(
            hasRecordAudio = true,
            notificationsRequired = true,
            hasNotifications = true,
            canDrawOverlays = true,
        )
        assertEquals(TroLyNoiPermissions.Gate.NONE, gate)
    }

    @Test
    fun onlyPersistOnWhenGateIsNone() {
        val incomplete = TroLyNoiPermissions.nextMissingPermission(
            hasRecordAudio = true,
            notificationsRequired = true,
            hasNotifications = true,
            canDrawOverlays = false,
        )
        assertEquals(TroLyNoiPermissions.Gate.SYSTEM_ALERT_WINDOW, incomplete)
        val complete = TroLyNoiPermissions.nextMissingPermission(
            hasRecordAudio = true,
            notificationsRequired = false,
            hasNotifications = false,
            canDrawOverlays = true,
        )
        assertEquals(TroLyNoiPermissions.Gate.NONE, complete)
    }
}
