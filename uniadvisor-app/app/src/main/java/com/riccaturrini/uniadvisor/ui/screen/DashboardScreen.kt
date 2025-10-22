package com.riccaturrini.uniadvisor.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.riccaturrini.uniadvisor.ui.components.UniAdvisorBottomBar

@Composable
fun DashboardScreen() {
    val navController = rememberNavController()

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
            modifier = Modifier.padding(innerPadding)
        )
    }
}

// 🔹 Grafo di navigazione interno alla dashboard
@Composable
fun DashboardNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") { HomeScreen() }
        composable("faculty") { FacultyScreen() }
        composable("reviews") { ReviewsScreen() }
        composable("notes") { NotesScreen() }
        composable("profile") { ProfileScreen() }
    }
}

// 🔹 Helper per sapere quale sezione è attiva
@Composable
private fun currentDestination(navController: NavHostController): String {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route
    return currentDestination ?: "home"
}
