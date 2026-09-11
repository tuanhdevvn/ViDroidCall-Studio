// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

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

    suspend fun setEnabled(enabled: Boolean) {
        context.troLyNoiDataStore.edit { preferences ->
            preferences[KEY_ENABLED] = enabled
        }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("tro_ly_noi_enabled")
    }
}
