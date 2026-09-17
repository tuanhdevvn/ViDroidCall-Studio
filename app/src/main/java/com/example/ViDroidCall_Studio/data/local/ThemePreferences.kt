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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences"
)

enum class AppTheme(val key: String, val title: String, val description: String) {
    LIGHT("light", "Giao diện Sáng", "Tông màu trắng sáng, thanh lịch và tươi tắn"),
    DARK("dark", "Giao diện Tối", "Tông màu đen dịu mắt, tiết kiệm pin"),
    SYSTEM("system", "Theo hệ thống", "Tự động thay đổi theo chế độ của thiết bị")
}

class ThemePreferences(
    private val context: Context,
) {
    val themeFlow: Flow<AppTheme> = context.themeDataStore.data.map { preferences ->
        val themeKey = preferences[KEY_THEME_MODE] ?: AppTheme.LIGHT.key
        AppTheme.entries.find { it.key == themeKey } ?: AppTheme.LIGHT
    }

    suspend fun setTheme(theme: AppTheme) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = theme.key
        }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
