// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

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
    fun categoryForIntent_fallsBackToHeThong() {
        assertEquals("Hệ thống", CommandHistoryRepository.categoryForIntent(null))
        assertEquals("Hệ thống", CommandHistoryRepository.categoryForIntent("unsupported"))
        assertEquals("Hệ thống", CommandHistoryRepository.categoryForIntent("unknown"))
    }
}
