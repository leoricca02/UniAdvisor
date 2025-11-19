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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
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

    override fun onPause() {
        super.onPause()
        // Auto-logout when app goes to background
        // Firebase.auth.signOut()  // Uncomment this line to enable auto-logout
    }
}

@Composable
fun UniAdvisorApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authUiState.collectAsState()

    // 🔹 Check login state and auto-navigate
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

    // 🔹 Main navigation routes
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
                },
                // Handle logout
                onLogout = {
                    navController.navigate("login") {
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

        // 🔹 Main dashboard with BottomBar
        composable("dashboard") {
            DashboardScreen(
                authViewModel = authViewModel,
                onLogout = {
                    // Perform logout
                    authViewModel.signOut()
                    // Navigate to login and clear backstack
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
    }

}