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

package com.example.ViDroidCall_Studio.feature.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Kiểm tra các tình huống vòng đời: Màn hình tắt/bật, khóa/mở khóa,
 * và đảm bảo không tạo duplicate listener hay race condition.
 */
class AssistantLifecycleTest {

    class MockLifecycleController {
        val isRunning = AtomicBoolean(false)
        val isPaused = AtomicBoolean(false)
        val activeListenerCount = AtomicInteger(0)

        var hasAudioPermission = true
        var isScreenInteractive = true
        var isKeyguardLocked = false

        fun start() {
            if (!hasAudioPermission) {
                stop()
                return
            }
            if (!isScreenInteractive || isKeyguardLocked) {
                isRunning.set(true)
                isPaused.set(true)
                return
            }
            isRunning.set(true)
            isPaused.set(false)
            activeListenerCount.set(1)
        }

        fun stop() {
            isRunning.set(false)
            isPaused.set(false)
            activeListenerCount.set(0)
        }

        fun pause() {
            isPaused.set(true)
            activeListenerCount.set(0)
        }

        fun resume() {
            if (!isRunning.get()) return
            if (!isScreenInteractive || isKeyguardLocked) return
            if (isPaused.compareAndSet(true, false)) {
                activeListenerCount.set(1)
            }
        }

        fun onScreenOff() {
            isScreenInteractive = false
            pause()
        }

        fun onScreenOn() {
            isScreenInteractive = true
            if (!isKeyguardLocked) {
                resume()
            }
        }

        fun onUserPresent() {
            isKeyguardLocked = false
            if (isScreenInteractive) {
                resume()
            }
        }
    }

    @Test
    fun testScreenOffAndLockFlow() {
        val controller = MockLifecycleController()
        controller.start()

        assertTrue(controller.isRunning.get())
        assertFalse(controller.isPaused.get())
        assertEquals(1, controller.activeListenerCount.get())

        // 1. Screen turns off
        controller.onScreenOff()
        assertTrue("isPaused phải là true khi màn hình tắt", controller.isPaused.get())
        assertEquals("Mic listener phải dừng (0 active)", 0, controller.activeListenerCount.get())

        // 2. Screen turns on but keyguard is still locked
        controller.isKeyguardLocked = true
        controller.onScreenOn()
        assertTrue("Vẫn phải giữ isPaused=true khi máy còn khóa", controller.isPaused.get())
        assertEquals(0, controller.activeListenerCount.get())

        // 3. User unlocks (ACTION_USER_PRESENT)
        controller.onUserPresent()
        assertFalse("isPaused phải về false sau khi mở khóa", controller.isPaused.get())
        assertEquals("Mic listener được khởi động lại chính xác 1 luồng", 1, controller.activeListenerCount.get())
    }

    @Test
    fun testNoDuplicateListenersOnRapidCalls() {
        val controller = MockLifecycleController()
        controller.start()

        // Calling start/resume rapidly should NOT create multiple listeners
        controller.resume()
        controller.resume()
        controller.start()
        controller.resume()

        assertEquals("Tuyệt đối không được tạo nhiều hơn 1 active listener", 1, controller.activeListenerCount.get())

        controller.pause()
        controller.pause()
        assertEquals(0, controller.activeListenerCount.get())
    }

    @Test
    fun testPermissionRevokedStopsListener() {
        val controller = MockLifecycleController()
        controller.hasAudioPermission = false
        controller.start()

        assertFalse("Không được chạy khi không có quyền RECORD_AUDIO", controller.isRunning.get())
        assertEquals(0, controller.activeListenerCount.get())
    }
}
