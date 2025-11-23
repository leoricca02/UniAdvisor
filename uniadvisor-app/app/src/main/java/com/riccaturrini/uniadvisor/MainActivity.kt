package com.riccaturrini.uniadvisor

import android.os.Bundle
import android.util.Log
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
                Surface(color = MaterialTheme.colorScheme.background) {
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

    // Log per monitorare ogni cambio di stato
    LaunchedEffect(authState) {
        val user = authViewModel.currentUserData.value
        Log.d("DEBUG_NAV", "🔄 Stato cambiato: $authState")
        Log.d("DEBUG_NAV", "👤 Utente corrente: ${user?.email}, FacultyID: ${user?.faculty_id}")

        if (authState is AuthUiState.Success) {
            if (user?.faculty_id != null) {
                Log.d("DEBUG_NAV", "✅ Utente ha facoltà -> Navigo a DASHBOARD")
                navController.navigate("dashboard") {
                    popUpTo("splash") { inclusive = true }
                    popUpTo("select_faculty") { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                Log.d("DEBUG_NAV", "⚠️ Utente SENZA facoltà -> Navigo a SELECT_FACULTY")
                navController.navigate("select_faculty") {
                    popUpTo("splash") { inclusive = true }
                    launchSingleTop = true
                }
            }
            // Resetta lo stato per evitare loop, ma loggalo
            Log.d("DEBUG_NAV", "🧹 Resetting AuthState to Idle")
            authViewModel.resetState()
        } else if (authState is AuthUiState.ProfileCreationRequired) {
            Log.d("DEBUG_NAV", "🆕 Profilo richiesto -> Navigo a COMPLETE_PROFILE")
            navController.navigate("complete_profile") {
                popUpTo("splash") { inclusive = true }
            }
            authViewModel.resetState()
        }
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            Log.d("DEBUG_NAV", "🎨 Rendering Screen: SPLASH")
            SplashScreen(navController = navController, authViewModel = authViewModel)
        }

        composable("login") {
            Log.d("DEBUG_NAV", "🎨 Rendering Screen: LOGIN")
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToSignUp = { navController.navigate("signup") },
                onLoginSuccess = { Log.d("DEBUG_NAV", "⚡ Login UI callback triggered (gestito da Global Listener)") },
                onNavigateToCompleteProfile = { navController.navigate("complete_profile") }
            )
        }

        composable("signup") {
            Log.d("DEBUG_NAV", "🎨 Rendering Screen: SIGNUP")
            SignUpScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToCompleteProfile = { navController.navigate("complete_profile") }
            )
        }

        composable("complete_profile") {
            Log.d("DEBUG_NAV", "🎨 Rendering Screen: COMPLETE_PROFILE")
            CompleteProfileScreen(
                authViewModel = authViewModel,
                onProfileCreationSuccess = { Log.d("DEBUG_NAV", "⚡ Profile Creation callback (gestito da Global Listener)") },
                onLogout = {
                    navController.navigate("login") { popUpTo("splash") { inclusive = true } }
                }
            )
        }

        composable("select_faculty") {
            Log.d("DEBUG_NAV", "🎨 Rendering Screen: SELECT_FACULTY")
            SelectFacultyScreen(
                authViewModel = authViewModel,
                onFacultySelected = { Log.d("DEBUG_NAV", "⚡ Faculty Selected callback (gestito da Global Listener)") }
            )
        }

        composable("dashboard") {
            Log.d("DEBUG_NAV", "🎨 Rendering Screen: DASHBOARD")
            DashboardScreen(
                authViewModel = authViewModel,
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate("login") { popUpTo("dashboard") { inclusive = true } }
                }
            )
        }
    }
}