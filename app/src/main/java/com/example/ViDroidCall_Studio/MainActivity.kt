package com.example.ViDroidCall_Studio

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
}
