package com.example.emma_vidroidcall

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
import com.example.emma_vidroidcall.data.local.AppTheme
import com.example.emma_vidroidcall.data.local.ThemePreferences
import com.example.emma_vidroidcall.navigation.AppRoot
import com.example.emma_vidroidcall.ui.theme.EmmaViDroidCallTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePreferences = remember { ThemePreferences(applicationContext) }
            val currentTheme by themePreferences.themeFlow.collectAsState(initial = AppTheme.LIGHT)
            val isDark = when (currentTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            EmmaViDroidCallTheme(darkTheme = isDark, dynamicColor = false) {
                AppRoot(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
