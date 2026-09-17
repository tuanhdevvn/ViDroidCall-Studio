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

package com.example.ViDroidCall_Studio.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.troLyNoiDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tro_ly_noi_preferences"
)

/**
 * Công tắc Trợ lý nổi. Mặc định tắt; chỉ persist `true` khi đã đủ quyền overlay.
 */
class TroLyNoiPreferences(
    private val context: Context,
) {
    val enabledFlow: Flow<Boolean> = context.troLyNoiDataStore.data.map { preferences ->
        preferences[KEY_ENABLED] ?: false
    }

    @Deprecated("Từ phiên bản 1 công tắc, wake word luôn đồng bộ với enabledFlow. Giữ lại để tương thích DataStore.")
    val wakeWordEnabledFlow: Flow<Boolean> = context.troLyNoiDataStore.data.map { preferences ->
        preferences[KEY_WAKE_WORD_ENABLED] ?: false
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.troLyNoiDataStore.edit { preferences ->
            preferences[KEY_ENABLED] = enabled
        }
    }

    @Deprecated("Từ phiên bản 1 công tắc, wake word luôn đồng bộ với enabledFlow. Giữ lại để tương thích DataStore.")
    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.troLyNoiDataStore.edit { preferences ->
            preferences[KEY_WAKE_WORD_ENABLED] = enabled
        }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("tro_ly_noi_enabled")
        val KEY_WAKE_WORD_ENABLED = booleanPreferencesKey("tro_ly_noi_wake_word_enabled")
    }
}
