package com.example.emma_vidroidcall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.emma_vidroidcall.feature.home.HomeScreen
import com.example.emma_vidroidcall.feature.onboarding.OnboardingScreen
import com.example.emma_vidroidcall.ui.theme.EmmaViDroidCallTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EmmaViDroidCallTheme(dynamicColor = false) {
                var isCompletedOnboarding by remember { mutableStateOf(true) }

                if (isCompletedOnboarding) {
                    HomeScreen(modifier = Modifier.fillMaxSize())
                } else {
                    OnboardingScreen(
                        onFinished = {
                            isCompletedOnboarding = true
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
