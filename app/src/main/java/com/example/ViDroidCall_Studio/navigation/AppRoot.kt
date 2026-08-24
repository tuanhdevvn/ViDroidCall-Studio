package com.example.ViDroidCall_Studio.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.ViDroidCall_Studio.data.local.OnboardingPreferences

@Composable
fun AppRoot(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val onboardingPreferences = remember { OnboardingPreferences(context) }
    val hasCompletedOnboarding by onboardingPreferences.hasCompletedOnboarding
        .collectAsState(initial = null)

    when (hasCompletedOnboarding) {
        null -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            )
        }

        else -> {
            val navController = rememberNavController()
            AppNavHost(
                navController = navController,
                modifier = modifier,
                startDestination = if (hasCompletedOnboarding == true) {
                    AppRoute.HOME
                } else {
                    AppRoute.ONBOARDING
                },
                onOnboardingFinished = onboardingPreferences::setOnboardingCompleted,
            )
        }
    }
}
