package com.example.ViDroidCall_Studio.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ViDroidCall_Studio.feature.home.HomeScreen
import com.example.ViDroidCall_Studio.feature.onboarding.OnboardingScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    navController: NavHostController,
    onOnboardingFinished: suspend () -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = AppRoute.ONBOARDING,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(AppRoute.ONBOARDING) {
            val scope = rememberCoroutineScope()
            OnboardingScreen(
                onFinished = {
                    scope.launch {
                        onOnboardingFinished()
                        navController.navigate(AppRoute.HOME) {
                            popUpTo(AppRoute.ONBOARDING) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable(AppRoute.HOME) {
            HomeScreen()
        }
    }
}
