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
