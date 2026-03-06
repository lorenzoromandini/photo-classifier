package com.example.photoorganizer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.photoorganizer.ui.main.MainScreen
import com.example.photoorganizer.ui.onboarding.OnboardingScreen
import com.example.photoorganizer.ui.onboarding.OnboardingViewModel
import com.example.photoorganizer.ui.settings.SettingsScreen

/**
 * Navigation routes for the Photo Organizer app.
 * Handles onboarding-to-main flow with conditional routing based on completion state.
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

/**
 * Root navigation component for the app.
 *
 * On first launch, shows onboarding flow. Once completed, shows main app.
 * Checks UserPreferences.onboardingCompleted to determine start destination.
 *
 * @param navController Navigation controller (created if not provided)
 */
@Composable
fun PhotoOrganizerNavigation(
    navController: NavHostController = rememberNavController()
) {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    var startDestination by remember { mutableStateOf<String?>(null) }

    // Determine start destination based on onboarding completion
    LaunchedEffect(Unit) {
        val isComplete = onboardingViewModel.isOnboardingComplete()
        startDestination = if (isComplete) {
            Routes.MAIN
        } else {
            Routes.ONBOARDING
        }
    }

    // Show loading while determining start destination
    if (startDestination == null) {
        // Could show a splash screen here
        return
    }

    NavHost(
        navController = navController,
        startDestination = startDestination!!
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onNavigateToMain = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}