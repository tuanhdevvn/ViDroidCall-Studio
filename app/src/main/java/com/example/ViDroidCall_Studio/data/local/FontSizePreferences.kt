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
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.fontSizeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "font_size_preferences"
)

/**
 * Quản lý lưu trữ kích thước cỡ chữ (Font Scale)
 */
class FontSizePreferences(
    private val context: Context
) {
    val fontScaleFlow: Flow<Float> = context.fontSizeDataStore.data.map { preferences ->
        preferences[KEY_FONT_SCALE] ?: DEFAULT_FONT_SCALE
    }

    suspend fun setFontScale(scale: Float) {
        context.fontSizeDataStore.edit { preferences ->
            preferences[KEY_FONT_SCALE] = scale
        }
    }

    companion object {
        const val DEFAULT_FONT_SCALE = 1.0f
        const val MIN_FONT_SCALE = 0.85f
        const val MAX_FONT_SCALE = 1.35f
        val KEY_FONT_SCALE = floatPreferencesKey("font_scale")

        fun getScaleDescription(scale: Float): String {
            return when {
                scale <= 0.88f -> "Nhỏ"
                scale in 0.89f..1.05f -> "Vừa"
                scale in 1.06f..1.20f -> "Lớn"
                else -> "Rất lớn"
            }
        }
    }
}
