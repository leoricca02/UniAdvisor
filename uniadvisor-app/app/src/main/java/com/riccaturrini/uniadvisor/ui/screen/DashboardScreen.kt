package com.riccaturrini.uniadvisor.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.riccaturrini.uniadvisor.ui.components.UniAdvisorBottomBar
import com.riccaturrini.uniadvisor.viewmodel.AuthViewModel
import com.riccaturrini.uniadvisor.viewmodel.MyReviewsViewModel

@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()

    // ✅ Ensure user data is loaded
    LaunchedEffect(Unit) {
        if (authViewModel.currentUserData.value == null) {
            authViewModel.checkUserProfile()
        }
    }

    Scaffold(
        bottomBar = {
            UniAdvisorBottomBar(
                currentScreen = currentDestination(navController),
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        DashboardNavGraph(
            navController = navController,
            authViewModel = authViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

// 🔹 Navigation graph inside dashboard
@Composable
fun DashboardNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(authViewModel = authViewModel)
        }
        composable("faculty") {
            FacultyScreen(authViewModel = authViewModel)
        }
        composable("reviews") {
            ReviewsScreen(authViewModel = authViewModel)
        }
        composable("notes") {
            NotesScreen(authViewModel = authViewModel)
        }
        composable("profile") {
            ProfileScreen(authViewModel = authViewModel)
        }
    }
}

// 🔹 Helper to know which section is active
@Composable
private fun currentDestination(navController: NavHostController): String {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route
    return currentDestination ?: "home"
}