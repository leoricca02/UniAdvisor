package com.riccaturrini.uniadvisor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.riccaturrini.uniadvisor.ui.screen.*
import com.riccaturrini.uniadvisor.ui.theme.UniAdvisorTheme
import com.riccaturrini.uniadvisor.viewmodel.AuthUiState
import com.riccaturrini.uniadvisor.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UniAdvisorTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    UniAdvisorApp()
                }
            }
        }
    }
}

@Composable
fun UniAdvisorApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authUiState.collectAsState()

    // 🔹 Controllo stato login e navigazione automatica
    LaunchedEffect(authState) {
        when (authState) {
            is AuthUiState.Success -> {
                val userData = authViewModel.currentUserData.value
                if (userData?.faculty_id != null) {
                    navController.navigate("dashboard") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else {
                    navController.navigate("select_faculty") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
                authViewModel.resetState()
            }
            is AuthUiState.ProfileCreationRequired -> {
                navController.navigate("complete_profile") {
                    popUpTo("splash") { inclusive = true }
                }
                authViewModel.resetState()
            }
            else -> Unit
        }
    }

    // 🔹 Definizione delle rotte principali
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToSignUp = { navController.navigate("signup") },
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToCompleteProfile = {
                    navController.navigate("complete_profile") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("signup") {
            SignUpScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToCompleteProfile = {
                    navController.navigate("complete_profile") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("complete_profile") {
            CompleteProfileScreen(
                authViewModel = authViewModel,
                onProfileCreationSuccess = {
                    navController.navigate("select_faculty") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("select_faculty") {
            SelectFacultyScreen(
                onFacultySelected = {
                    navController.navigate("dashboard") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // 🔹 Nuova home vera con BottomBar
        composable("dashboard") {
            DashboardScreen()
        }
    }
}
