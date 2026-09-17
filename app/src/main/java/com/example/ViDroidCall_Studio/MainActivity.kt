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

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.ViDroidCall_Studio.data.local.AppTheme
import com.example.ViDroidCall_Studio.data.local.FontSizePreferences
import com.example.ViDroidCall_Studio.data.local.ThemePreferences
import com.example.ViDroidCall_Studio.navigation.AppRoot
import com.example.ViDroidCall_Studio.ui.theme.ViDroidCallTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePreferences = remember { ThemePreferences(applicationContext) }
            val fontSizePreferences = remember { FontSizePreferences(applicationContext) }
            val currentTheme by themePreferences.themeFlow.collectAsState(initial = AppTheme.LIGHT)
            val currentFontScale by fontSizePreferences.fontScaleFlow.collectAsState(initial = FontSizePreferences.DEFAULT_FONT_SCALE)

            val isDark = when (currentTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            ViDroidCallTheme(
                darkTheme = isDark,
                dynamicColor = false,
                fontScale = currentFontScale
            ) {
                AppRoot(modifier = Modifier.fillMaxSize())
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    companion object {
        const val EXTRA_OPEN_SETTINGS = "open_settings"
    }
}
