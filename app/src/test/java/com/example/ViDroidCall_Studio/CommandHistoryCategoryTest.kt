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

import com.example.ViDroidCall_Studio.data.local.history.CommandHistoryRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class CommandHistoryCategoryTest {

    @Test
    fun categoryForIntent_mapsSearchWebVideoMusic() {
        assertEquals("Tìm web", CommandHistoryRepository.categoryForIntent("search_web"))
        assertEquals("Video", CommandHistoryRepository.categoryForIntent("search_video"))
        assertEquals("Nhạc", CommandHistoryRepository.categoryForIntent("play_music"))
    }

    @Test
    fun categoryForIntent_keepsExistingActionLabels() {
        assertEquals("Cuộc gọi", CommandHistoryRepository.categoryForIntent("call_contact"))
        assertEquals("Tin nhắn", CommandHistoryRepository.categoryForIntent("send_sms"))
        assertEquals("Báo thức", CommandHistoryRepository.categoryForIntent("set_alarm"))
        assertEquals("Hẹn giờ", CommandHistoryRepository.categoryForIntent("set_timer"))
        assertEquals("Bản đồ", CommandHistoryRepository.categoryForIntent("open_map"))
        assertEquals("Ứng dụng", CommandHistoryRepository.categoryForIntent("open_app"))
    }

    @Test
    fun categoryForIntent_mapsConversationalIntents() {
        assertEquals("Chào hỏi", CommandHistoryRepository.categoryForIntent("greeting"))
        assertEquals("Tạm biệt", CommandHistoryRepository.categoryForIntent("goodbye"))
        assertEquals("Hỏi lại", CommandHistoryRepository.categoryForIntent("clarify"))
        assertEquals("Không hỗ trợ", CommandHistoryRepository.categoryForIntent("unsupported"))
    }

    @Test
    fun categoryForIntent_fallsBackToHeThong() {
        assertEquals("Hệ thống", CommandHistoryRepository.categoryForIntent(null))
        assertEquals("Hệ thống", CommandHistoryRepository.categoryForIntent("unknown"))
        assertEquals("Hệ thống", CommandHistoryRepository.categoryForIntent(""))
    }
}
