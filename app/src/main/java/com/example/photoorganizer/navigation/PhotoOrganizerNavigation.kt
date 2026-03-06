package com.example.photoorganizer.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.photoorganizer.ui.onboarding.OnboardingScreen
import com.example.photoorganizer.ui.onboarding.OnboardingViewModel
import kotlinx.coroutines.delay

/**
 * Navigation routes for the Photo Organizer app.
 * Handles onboarding-to-main flow with conditional routing based on completion state.
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
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
            // Placeholder for main screen
            // Will be implemented in later phases
            MainScreenPlaceholder()
        }
    }
}

/**
 * Placeholder for main screen content.
 * Will be replaced with actual implementation in later phases.
 */
@Composable
private fun MainScreenPlaceholder() {
    val context = LocalContext.current
    
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text("Photo Organizer") }
            )
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = "Main Screen\n\nYour organized photos will appear here",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}