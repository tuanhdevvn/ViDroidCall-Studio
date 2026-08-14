package com.example.emma_vidroidcall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.emma_vidroidcall.navigation.AppRoot
import com.example.emma_vidroidcall.ui.theme.EmmaViDroidCallTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EmmaViDroidCallTheme(dynamicColor = false) {
                AppRoot(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
