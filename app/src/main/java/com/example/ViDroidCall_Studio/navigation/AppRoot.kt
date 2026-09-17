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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.ViDroidCall_Studio.data.local.OnboardingPreferences
import com.example.ViDroidCall_Studio.data.local.TroLyNoiPreferences
import com.example.ViDroidCall_Studio.feature.overlay.TroLyNoiForegroundController

@Composable
fun AppRoot(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val onboardingPreferences = remember { OnboardingPreferences(context) }
    val troLyNoiPreferences = remember { TroLyNoiPreferences(context) }
    val hasCompletedOnboarding by onboardingPreferences.hasCompletedOnboarding
        .collectAsState(initial = null)
    val troLyNoiEnabled by troLyNoiPreferences.enabledFlow.collectAsState(initial = null)

    LaunchedEffect(troLyNoiEnabled) {
        val enabled = troLyNoiEnabled ?: return@LaunchedEffect
        TroLyNoiForegroundController.sync(context, enabled)
    }

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
