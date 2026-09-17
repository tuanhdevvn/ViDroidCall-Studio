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
